/*
 * Copyright (c) 2020 Simer Plaha (@simerplaha)
 *
 * This file is a part of SwayDB.
 *
 * SwayDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * SwayDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with SwayDB. If not, see <https://www.gnu.org/licenses/>.
 */

package swaydb.core.segment

import java.nio.file.Paths

import org.scalamock.scalatest.MockFactory
import swaydb.core.CommonAssertions._
import swaydb.core.RunThis._
import swaydb.core.TestBase
import swaydb.core.TestData._
import swaydb.core.actor.MemorySweeper
import swaydb.core.data.{Persistent, PersistentOptional, Time}
import swaydb.core.segment.format.a.block.binarysearch.BinarySearchIndexBlock
import swaydb.core.segment.format.a.block.hashindex.HashIndexBlock
import swaydb.core.segment.format.a.block.reader.UnblockedReader
import swaydb.core.segment.format.a.block.{BloomFilterBlock, SegmentSearcher, SortedIndexBlock, ValuesBlock}
import swaydb.data.MaxKey
import swaydb.data.order.KeyOrder
import swaydb.data.slice.Slice
import swaydb.serializers.Default._
import swaydb.serializers._

class SegmentCacheGetBehaviorSpec extends TestBase with MockFactory {

  implicit val keyOrder = KeyOrder.default
  implicit val partialKeyOrder: KeyOrder[Persistent.Partial] = KeyOrder(Ordering.by[Persistent.Partial, Slice[Byte]](_.key)(keyOrder))
  implicit val persistentKeyOrder: KeyOrder[Persistent] = KeyOrder(Ordering.by[Persistent, Slice[Byte]](_.key)(keyOrder))

  implicit val cacheMemorySweeper: Option[MemorySweeper.Cache] = None
  implicit val blockMemorySweeper: Option[MemorySweeper.Block] = None

  "get" when {
    implicit val keyValueMemorySweeper: Option[MemorySweeper.KeyValue] = None

    "key is > MaxKey.Fixed" should {
      "return none" in {
        implicit val segmentSearcher = mock[SegmentSearcher]
        implicit val segmentCache = new SegmentCache(Paths.get("1"), MaxKey.Fixed[Slice[Byte]](100), minKey = 0, None, null)
        SegmentCache.get(key = 101, readState = null) shouldBe Persistent.Null

      }
    }

    "key is > MaxKey.Range" should {
      "return none" in {
        implicit val segmentSearcher = mock[SegmentSearcher]
        implicit val segmentCache = new SegmentCache(Paths.get("1"), MaxKey.Range[Slice[Byte]](90, 100), minKey = 0, None, null)
        SegmentCache.get(key = 100, readState = null) shouldBe Persistent.Null
        SegmentCache.get(key = 101, readState = null) shouldBe Persistent.Null

      }
    }
  }

  "GET - Behaviour" in {
    runThis(10.times, log = true) {
      implicit val keyValueMemorySweeper: Option[MemorySweeper.KeyValue] = None

      //test data
      val path = Paths.get("1")

      val keyValues = randomKeyValues(1)
      val segmentBlockCache = getSegmentBlockCache(keyValues, bloomFilterConfig = BloomFilterBlock.Config.disabled)

      implicit val segmentSearcher = mock[SegmentSearcher]

      implicit val segmentCache =
        new SegmentCache(
          path = path,
          maxKey = MaxKey.Fixed[Slice[Byte]](Int.MaxValue),
          minKey = 0,
          skipList = None,
          segmentBlockCache = segmentBlockCache.head
        )

      val threadState = ThreadReadState.random

      threadState.getSegmentState(path).isNoneS shouldBe true

      def testKeyValue(key: Slice[Byte], indexOffset: Int, nextIndexOffset: Int) =
        Persistent.Remove(
          _key =
            eitherOne(
              //tests that sorted keys are unsliced.
              (key ++ Slice.fill[Byte](10)(randomByte())).dropRight(10),
              (Slice.fill[Byte](10)(randomByte()) ++ key).drop(10)
            ),
          deadline = None,
          _time = Time.empty,
          indexOffset = indexOffset,
          nextIndexOffset = nextIndexOffset,
          nextKeySize = 4,
          sortedIndexAccessPosition = 0
        )

      val keyValue1 = testKeyValue(key = 1, indexOffset = 0, nextIndexOffset = 1)
      val keyValue2 = testKeyValue(key = 2, indexOffset = 1, nextIndexOffset = 2)
      val keyValue3 = testKeyValue(key = 3, indexOffset = 2, nextIndexOffset = 3)

      val keyValue100 = testKeyValue(key = 100, indexOffset = 99, nextIndexOffset = 100)
      val keyValue101 = testKeyValue(key = 101, indexOffset = 100, nextIndexOffset = 101)

      /**
       * First perform sequential read - keyValue1
       */
      {
        (segmentSearcher
          .searchSequential(
            _: Slice[Byte],
            _: PersistentOptional,
            _: UnblockedReader[SortedIndexBlock.Offset, SortedIndexBlock],
            _: UnblockedReader[ValuesBlock.Offset, ValuesBlock])(_: KeyOrder[Slice[Byte]], _: KeyOrder[Persistent.Partial])
          )
          .expects(*, *, *, *, *, *)
          .onCall {
            (key: Slice[Byte], startFrom: PersistentOptional, _, _, _, _) =>
              key shouldBe keyValue1.key
              startFrom.isNoneS shouldBe true
              keyValue1
          }

        SegmentCache.get(key = keyValue1.key, readState = threadState) shouldBe keyValue1

        val segmentState = threadState.getSegmentState(path).getS
        segmentState.keyValue shouldBe keyValue1
        keyValue1.key.underlyingArraySize shouldBe 4
        segmentState.isSequential shouldBe true
      }

      /**
       * and then perform sequential read again - keyValue2
       */
      {
        (segmentSearcher
          .searchSequential(
            _: Slice[Byte],
            _: PersistentOptional,
            _: UnblockedReader[SortedIndexBlock.Offset, SortedIndexBlock],
            _: UnblockedReader[ValuesBlock.Offset, ValuesBlock])(_: KeyOrder[Slice[Byte]], _: KeyOrder[Persistent.Partial])
          )
          .expects(*, *, *, *, *, *)
          .onCall {
            (key: Slice[Byte], startFrom: PersistentOptional, _, _, _, _) =>
              key shouldBe keyValue2.key
              startFrom shouldBe keyValue1
              keyValue2
          }

        SegmentCache.get(key = keyValue2.key, readState = threadState) shouldBe keyValue2

        val segmentState = threadState.getSegmentState(path).getS
        segmentState.keyValue shouldBe keyValue2
        keyValue2.key.underlyingArraySize shouldBe 4
        segmentState.isSequential shouldBe true
      }

      /**
       * and then perform sequential read again but fails and reverts to random seek because the next key is random - keyValue100
       */
      {
        (segmentSearcher
          .searchSequential(
            _: Slice[Byte],
            _: PersistentOptional,
            _: UnblockedReader[SortedIndexBlock.Offset, SortedIndexBlock],
            _: UnblockedReader[ValuesBlock.Offset, ValuesBlock])(_: KeyOrder[Slice[Byte]], _: KeyOrder[Persistent.Partial])
          )
          .expects(*, *, *, *, *, *)
          .onCall {
            (key: Slice[Byte], startFrom: PersistentOptional, _, _, _, _) =>
              key shouldBe keyValue100.key
              startFrom shouldBe keyValue2
              Persistent.Null
          }

        (segmentSearcher
          .searchRandom(
            _: Slice[Byte],
            _: PersistentOptional,
            _: PersistentOptional,
            _: UnblockedReader[HashIndexBlock.Offset, HashIndexBlock],
            _: UnblockedReader[BinarySearchIndexBlock.Offset, BinarySearchIndexBlock],
            _: UnblockedReader[SortedIndexBlock.Offset, SortedIndexBlock],
            _: UnblockedReader[ValuesBlock.Offset, ValuesBlock],
            _: Boolean,
            _: Int)(_: KeyOrder[Slice[Byte]], _: KeyOrder[Persistent.Partial]))
          .expects(*, *, *, *, *, *, *, *, *, *, *)
          .onCall {
            result =>
              result match {
                case (key: Slice[Byte], startFrom: PersistentOptional, _, _, _, _, _, hasRange: Boolean, keyValueCount: Function0[Int], _, _) =>
                  key shouldBe keyValue100.key
                  startFrom shouldBe keyValue2
                  hasRange shouldBe keyValues.exists(_.isRange)
                  keyValueCount() shouldBe keyValues.size
                  keyValue100
              }
          }

        SegmentCache.get(key = keyValue100.key, readState = threadState) shouldBe keyValue100

        val segmentState = threadState.getSegmentState(path).getS
        segmentState.keyValue shouldBe keyValue100
        segmentState.isSequential shouldBe false
      }

      /**
       * and then perform random read to start with and set sequential to true - keyValue101
       */
      {
        //101 is sequential to 100 - isSequential will be true.
        (segmentSearcher
          .searchRandom(
            _: Slice[Byte],
            _: PersistentOptional,
            _: PersistentOptional,
            _: UnblockedReader[HashIndexBlock.Offset, HashIndexBlock],
            _: UnblockedReader[BinarySearchIndexBlock.Offset, BinarySearchIndexBlock],
            _: UnblockedReader[SortedIndexBlock.Offset, SortedIndexBlock],
            _: UnblockedReader[ValuesBlock.Offset, ValuesBlock],
            _: Boolean,
            _: Int)(_: KeyOrder[Slice[Byte]], _: KeyOrder[Persistent.Partial]))
          .expects(*, *, *, *, *, *, *, *, *, *, *)
          .onCall {
            result =>
              result match {
                case (key: Slice[Byte], startFrom: PersistentOptional, _, _, _, _, _, hasRange: Boolean, keyValueCount: Function0[Int], _, _) =>
                  key shouldBe keyValue101.key
                  startFrom shouldBe keyValue100
                  hasRange shouldBe keyValues.exists(_.isRange)
                  keyValueCount() shouldBe keyValues.size
                  keyValue101
              }
          }

        SegmentCache.get(key = keyValue101.key, readState = threadState) shouldBe keyValue101

        val segmentState = threadState.getSegmentState(path).getS
        segmentState.keyValue shouldBe keyValue101
        segmentState.isSequential shouldBe true
      }

      /**
       * and then perform sequential read to start with but fail because keyValue3 is not sequential to keyValue101 - keyValue3
       */
      {
        (segmentSearcher
          .searchSequential(
            _: Slice[Byte],
            _: PersistentOptional,
            _: UnblockedReader[SortedIndexBlock.Offset, SortedIndexBlock],
            _: UnblockedReader[ValuesBlock.Offset, ValuesBlock])(_: KeyOrder[Slice[Byte]], _: KeyOrder[Persistent.Partial])
          )
          .expects(*, *, *, *, *, *)
          .onCall {
            (key: Slice[Byte], startFrom: PersistentOptional, _, _, _, _) =>
              key shouldBe keyValue3.key
              //start from is None because cached keyValue10's key > keyValue3's key
              startFrom.isNoneS shouldBe true
              Persistent.Null
          }

        (segmentSearcher
          .searchRandom(
            _: Slice[Byte],
            _: PersistentOptional,
            _: PersistentOptional,
            _: UnblockedReader[HashIndexBlock.Offset, HashIndexBlock],
            _: UnblockedReader[BinarySearchIndexBlock.Offset, BinarySearchIndexBlock],
            _: UnblockedReader[SortedIndexBlock.Offset, SortedIndexBlock],
            _: UnblockedReader[ValuesBlock.Offset, ValuesBlock],
            _: Boolean,
            _: Int)(_: KeyOrder[Slice[Byte]], _: KeyOrder[Persistent.Partial]))
          .expects(*, *, *, *, *, *, *, *, *, *, *)
          .onCall {
            result =>
              result match {
                case (key: Slice[Byte], startFrom: PersistentOptional, _, _, _, _, _, hasRange: Boolean, keyValueCount: Function0[Int], _, _) =>
                  key shouldBe keyValue3.key
                  //start from is None because cached keyValue10's key > keyValue3's key
                  startFrom.isNoneS shouldBe true
                  hasRange shouldBe keyValues.exists(_.isRange)
                  keyValueCount() shouldBe keyValues.size
                  keyValue3
              }
          }

        SegmentCache.get(key = keyValue3.key, readState = threadState) shouldBe keyValue3

        val segmentState = threadState.getSegmentState(path).getS
        segmentState.keyValue shouldBe keyValue3
        segmentState.isSequential shouldBe false
      }
    }
  }
}
