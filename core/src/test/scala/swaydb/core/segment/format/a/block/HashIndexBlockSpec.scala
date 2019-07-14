/*
 * Copyright (c) 2019 Simer Plaha (@simerplaha)
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

package swaydb.core.segment.format.a.block

import swaydb.core.CommonAssertions._
import swaydb.core.RunThis._
import swaydb.core.TestBase
import swaydb.core.TestData._
import swaydb.core.data.Transient
import swaydb.core.io.reader.Reader
import swaydb.data.IO
import swaydb.data.config.RandomKeyIndex.RequiredSpace
import swaydb.data.order.KeyOrder
import swaydb.data.slice.Slice

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class HashIndexBlockSpec extends TestBase {

  implicit val keyOrder = KeyOrder.default

  val keyValueCount = 10000

  import keyOrder._

  "optimalBytesRequired" should {
    "allocate optimal byte" in {
      HashIndexBlock.optimalBytesRequired(
        keyCounts = 1,
        largestValue = 1,
        allocateSpace = _.requiredSpace,
        hasCompression = false,
        minimumNumberOfKeys = 0
      ) shouldBe
        HashIndexBlock.headerSize(
          keyCounts = 1,
          hasCompression = false,
          writeAbleLargestValueSize = 1
        ) + 1 + 1
    }
  }

  "it" should {
    "write compressed HashIndex and result in the same as uncompressed HashIndex" in {
      runThis(1.times) {
        val maxProbe = 10

        def allocateMoreSpace(requiredSpace: RequiredSpace) = requiredSpace.requiredSpace * 3

        val keyValues =
          randomKeyValues(
            count = 10,
            addRandomRemoves = true,
            addRandomFunctions = true,
            addRandomRemoveDeadlines = true,
            addRandomUpdates = true,
            addRandomPendingApply = true,
            hashIndexConfig =
              HashIndexBlock.Config.random.copy(
                allocateSpace = allocateMoreSpace,
                compressions = _ => Seq.empty,
                maxProbe = maxProbe
              )
          )

        keyValues should not be empty

        val uncompressedState =
          HashIndexBlock.init(keyValues = keyValues).get

        val compressedState =
          HashIndexBlock.init(
            keyValues =
              keyValues
                .updateStats(
                  hashIndexConfig =
                    HashIndexBlock.Config(
                      allocateSpace = allocateMoreSpace,
                      compressions = _ => randomCompressionsLZ4OrSnappy(),
                      maxProbe = maxProbe,
                      minimumNumberOfKeys = 0,
                      minimumNumberOfHits = 0,
                      blockIO = _ => randomIOAccess()
                    )
                )
          ).get

        keyValues foreach {
          keyValue =>
            HashIndexBlock.write(
              key = keyValue.key,
              value = keyValue.stats.thisKeyValuesAccessIndexOffset,
              state = uncompressedState
            ).get

            HashIndexBlock.write(
              key = keyValue.key,
              value = keyValue.stats.thisKeyValuesAccessIndexOffset,
              state = compressedState
            ).get
        }

        HashIndexBlock.close(uncompressedState).get
        HashIndexBlock.close(compressedState).get

        //compressed bytes should be smaller
        compressedState.bytes.size should be <= uncompressedState.bytes.size

        val uncompressedOffset = HashIndexBlock.Offset(0, uncompressedState.bytes.size)
        val compressedOffset = HashIndexBlock.Offset(0, compressedState.bytes.size)

        val uncompressedHashIndex =
          HashIndexBlock.read(
            uncompressedOffset,
            SegmentBlock(
              offset = SegmentBlock.Offset(0, uncompressedState.bytes.size),
              headerSize = 0,
              compressionInfo = None
            ).createBlockReader(SegmentBlock.createUnblockedReader(uncompressedState.bytes).get)
          ).get
        val compressedHashIndex =
          HashIndexBlock.read(
            compressedOffset,
            SegmentBlock(
              offset = SegmentBlock.Offset(0, compressedState.bytes.size),
              headerSize = 0,
              compressionInfo = None
            ).createBlockReader(SegmentBlock.createUnblockedReader(compressedState.bytes).get)
          ).get

        uncompressedHashIndex.compressionInfo shouldBe empty
        compressedHashIndex.compressionInfo shouldBe defined

        uncompressedHashIndex.bytesToReadPerIndex shouldBe compressedHashIndex.bytesToReadPerIndex
        uncompressedHashIndex.hit shouldBe compressedHashIndex.hit
        uncompressedHashIndex.miss shouldBe compressedHashIndex.miss
        uncompressedHashIndex.maxProbe shouldBe compressedHashIndex.maxProbe
        uncompressedHashIndex.writeAbleLargestValueSize shouldBe compressedHashIndex.writeAbleLargestValueSize
        uncompressedHashIndex.offset.start shouldBe compressedHashIndex.offset.start
        uncompressedHashIndex.offset.size should be >= compressedHashIndex.offset.size

        val blockDecompressor = compressedHashIndex.compressionInfo.get
        //        blockDecompressor.decompressedBytes shouldBe empty
        //        blockDecompressor.isBusy shouldBe false
        ???
      }
    }
  }

  "build index" when {
    "the hash is perfect" in {
      runThis(100.times) {
        val maxProbe = 1000
        val startId = Some(0)

        val compressions = randomCompressionsOrEmpty()

        val keyValues =
          randomizedKeyValues(
            count = randomIntMax(1000) max 1,
            startId = startId,
            addPut = true,
            hashIndexConfig =
              HashIndexBlock.Config(
                allocateSpace = _.requiredSpace * 5,
                compressions = _ => compressions,
                maxProbe = maxProbe,
                minimumNumberOfKeys = 0,
                minimumNumberOfHits = 0,
                blockIO = _ => randomIOAccess()
              )
          )

        keyValues should not be empty

        val state =
          HashIndexBlock.init(keyValues = keyValues).get

        val allocatedBytes = state.bytes.allocatedSize

        keyValues foreach {
          keyValue =>
            HashIndexBlock.write(
              key = keyValue.key,
              value = keyValue.stats.thisKeyValuesAccessIndexOffset,
              state = state
            ).get
        }

        println(s"hit: ${state.hit}")
        println(s"miss: ${state.miss}")
        println

        HashIndexBlock.close(state).get

        println(s"Bytes allocated: ${state.bytes.allocatedSize}")
        println(s"Bytes written: ${state.bytes.size}")

        state.hit should be(keyValues.size)
        state.miss shouldBe 0
        state.hit + state.miss shouldBe keyValues.size

        val offset = HashIndexBlock.Offset(0, state.bytes.size)

        val randomBytes = randomBytesSlice(randomIntMax(100))

        val (adjustedOffset, alteredBytes) =
          eitherOne(
            (offset, state.bytes),
            (offset, state.bytes ++ randomBytesSlice(randomIntMax(100))),
            (offset.copy(start = randomBytes.size), randomBytes ++ state.bytes),
            (offset.copy(start = randomBytes.size), randomBytes ++ state.bytes ++ randomBytesSlice(randomIntMax(100)))
          )

        val hashIndex = HashIndexBlock.read(adjustedOffset, SegmentBlock.createUnblockedReader(alteredBytes).get).get

        hashIndex shouldBe
          HashIndexBlock(
            offset = adjustedOffset,
            compressionInfo = hashIndex.compressionInfo,
            maxProbe = state.maxProbe,
            hit = state.hit,
            miss = state.miss,
            writeAbleLargestValueSize = state.writeAbleLargestValueSize,
            headerSize =
              HashIndexBlock.headerSize(
                keyCounts = keyValues.last.stats.segmentUniqueKeysCount,
                writeAbleLargestValueSize = state.writeAbleLargestValueSize,
                hasCompression = compressions.nonEmpty
              ),
            allocatedBytes = allocatedBytes
          )

        println("Building ListMap")
        val indexOffsetMap = mutable.HashMap.empty[Int, ListBuffer[Transient]]

        keyValues foreach {
          keyValue =>
            indexOffsetMap.getOrElseUpdate(keyValue.stats.thisKeyValuesAccessIndexOffset, ListBuffer(keyValue)) += keyValue
        }

        println(s"ListMap created with size: ${indexOffsetMap.size}")

        def findKey(indexOffset: Int, key: Slice[Byte]): IO[Option[Transient]] =
          indexOffsetMap.get(indexOffset) match {
            case Some(keyValues) =>
              IO(keyValues.find(_.key equiv key))

            case None =>
              IO.Failure(IO.Error.Fatal(s"Got index that does not exist: $indexOffset"))
          }

        keyValues foreach {
          keyValue =>
            val found =
              HashIndexBlock.search(
                key = keyValue.key,
                blockReader = hashIndex.createBlockReader(SegmentBlock.createUnblockedReader(alteredBytes).get),
                assertValue = findKey(_, keyValue.key)
              ).get.get
            (found.key equiv keyValue.key) shouldBe true
        }
      }
    }
  }
  //
  //    "searching a segment" should {
  //      "value" in {
  //        val keyValues =
  //          randomizedKeyValues(
  //            count = 100,
  //            startId = Some(1),
  //            addRandomGroups = false,
  //            compressDuplicateValues = randomBoolean(),
  //            enableBinarySearchIndex = false,
  //            addRandomRangeRemoves = false,
  //            addRandomRanges = false,
  //            buildFullBinarySearchIndex = true,
  //            resetPrefixCompressionEvery = 2,
  //            allocateSpace = _.requiredSpace * 3
  //          )
  //
  //        val segment = SegmentWriter.write(keyValues, segmentCompressions = randomSegmentCompression(), 0, 5).get.flattenSegmentBytes
  //        val indexes = readBlocks(Reader(segment)).get
  //
  //        indexes._4.get.block.miss shouldBe 0
  //
  //        Random.shuffle(keyValues) foreach {
  //          keyValue =>
  //            indexes._4 shouldBe defined
  //            val got = HashIndex.get(KeyMatcher.Get.WhilePrefixCompressed(keyValue.key), indexes._4.get, indexes._3, indexes._2).get.get
  //            got shouldBe keyValue
  //        }
  //      }
  //    }
  //  }
}