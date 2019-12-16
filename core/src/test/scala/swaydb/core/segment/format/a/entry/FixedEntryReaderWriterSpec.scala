///*
// * Copyright (c) 2019 Simer Plaha (@simerplaha)
// *
// * This file is a part of SwayDB.
// *
// * SwayDB is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as
// * published by the Free Software Foundation, either version 3 of the
// * License, or (at your option) any later version.
// *
// * SwayDB is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU Affero General Public License
// * along with SwayDB. If not, see <https://www.gnu.org/licenses/>.
// */
//
//package swaydb.core.segment.format.a.entry
//
//import org.scalatest.WordSpec
//import swaydb.core.CommonAssertions._
//import swaydb.core.RunThis._
//import swaydb.core.TestData._
//import swaydb.core.TestTimer
//import swaydb.core.data.Memory
//import swaydb.core.segment.format.a.entry.reader.EntryReader
//import swaydb.data.order.KeyOrder
//import swaydb.data.slice.Slice
//import swaydb.serializers.Default._
//import swaydb.serializers._
//
//import scala.util.Random
//
//class FixedEntryReaderWriterSpec extends WordSpec {
//
//  implicit val keyOrder: KeyOrder[Slice[Byte]] = KeyOrder.default
//
//  "write and read single Fixed entry" in {
//    runThis(1000.times) {
//      implicit val testTimer = TestTimer.Empty
//      val entry = randomFixedKeyValue(key = randomIntMax()).toTransient
//
//      //if normalise is true, use normalised entry.
//      val normalisedEntry =
//        if (entry.sortedIndexConfig.normaliseIndex)
//          Memory.normalise(Slice(entry)).head
//        else
//          Slice(entry).head
//
//      val read =
//        EntryReader.parse(
//          headerInteger = normalisedEntry.indexEntryBytes.readUnsignedInt(),
//          indexEntry = normalisedEntry.indexEntryBytes.dropUnsignedInt(),
//          mightBeCompressed = entry.stats.hasPrefixCompression,
//          sortedIndexEndOffset = normalisedEntry.indexEntryBytes.size - 1,
//          valuesReader = entry.valueEntryBytes.map(buildSingleValueReader),
//          normalisedByteSize = if (entry.sortedIndexConfig.normaliseIndex) normalisedEntry.indexEntryBytes.size else 0,
//          indexOffset = 0,
//          hasAccessPositionIndex = entry.sortedIndexConfig.enableAccessPositionIndex,
//          previous = None
//        )
//
//      //      println("read:  " + read)
//      read shouldBe entry
//    }
//  }
//
//  "write and read two fixed entries" in {
//    runThis(100.times, log = true) {
//      implicit val testTimer = TestTimer.random
//
//      val previousNotNormalised = randomizedKeyValues(count = 1).head
//      val duplicateValues = if (Random.nextBoolean()) previousNotNormalised.value else randomStringOption
//      val duplicateDeadline = if (Random.nextBoolean()) previousNotNormalised.deadline else randomDeadlineOption
//      val nextNotNormalised = randomFixedKeyValue(randomIntMax() + 1, deadline = duplicateDeadline, value = duplicateValues).toTransient(previous = Some(previousNotNormalised))
//
//      val (previous: Memory, next: Memory) =
//        if (nextNotNormalised.sortedIndexConfig.normaliseIndex) {
//          val normalised = Memory.normalise(Slice(previousNotNormalised, nextNotNormalised))
//          (normalised.head, normalised.last)
//        } else {
//          (previousNotNormalised, nextNotNormalised)
//        }
//
////      println
////      println("write previous: " + previous)
////      println("write next: " + next)
//
//      val indexEntryBytes: Slice[Byte] = previous.indexEntryBytes ++ next.indexEntryBytes
//      val valueBytes: Slice[Byte] = previous.valueEntryBytes ++ next.valueEntryBytes
//
//      val sortedIndexEndOffset = indexEntryBytes.size - 1
//
//      val previousRead =
//        EntryReader.parse(
//          headerInteger = indexEntryBytes.readUnsignedInt(),
//          indexEntry = indexEntryBytes.dropUnsignedInt(),
//          mightBeCompressed = next.stats.hasPrefixCompression,
//          sortedIndexEndOffset = sortedIndexEndOffset,
//          valuesReader = Some(buildSingleValueReader(valueBytes)),
//          normalisedByteSize = if (next.sortedIndexConfig.normaliseIndex) next.indexEntryBytes.size else 0,
//          indexOffset = 0,
//          hasAccessPositionIndex = next.sortedIndexConfig.enableAccessPositionIndex,
//          previous = None
//        )
//
//      previousRead shouldBe previous
//
//      val nextRead =
//        EntryReader.parse(
//          headerInteger = next.indexEntryBytes.readUnsignedInt(),
//          indexEntry = next.indexEntryBytes.dropUnsignedInt(),
//          mightBeCompressed = next.stats.hasPrefixCompression,
//          sortedIndexEndOffset = sortedIndexEndOffset,
//          valuesReader = Some(buildSingleValueReader(valueBytes)),
//          normalisedByteSize = if (next.sortedIndexConfig.normaliseIndex) next.indexEntryBytes.size else 0,
//          indexOffset = previousRead.nextIndexOffset,
//          hasAccessPositionIndex = next.sortedIndexConfig.enableAccessPositionIndex,
//          previous = Some(previousRead)
//        )
//
//      //      val nextRead = EntryReader.read(Reader(next.indexEntryBytes), Reader(valueBytes), 0, 0, 0, Some(previousRead)).runIO
//      nextRead shouldBe next
//
//      //      println("read previous:  " + previousRead)
//      //      println("read next:  " + nextRead)
//      //      println
//    }
//  }
//}
