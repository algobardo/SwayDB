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

package swaydb.core.data

import swaydb.core.segment.format.a.SegmentWriter
import swaydb.core.segment.format.a.index.{BinarySearchIndex, BloomFilter, HashIndex}
import swaydb.core.util.Bytes
import swaydb.data.slice.Slice
import swaydb.data.util.ByteSizeOf

import scala.concurrent.duration.Deadline

private[core] object Stats {

  def apply(indexEntry: Slice[Byte],
            value: Option[Slice[Byte]],
            falsePositiveRate: Double,
            isRemoveRange: Boolean,
            isRange: Boolean,
            isGroup: Boolean,
            isPut: Boolean,
            numberOfRanges: Int,
            thisKeyValuesUniqueKeys: Int,
            isPrefixCompressed: Boolean,
            minimumNumberOfKeysForHashIndex: Int,
            hashIndexCompensation: Int => Int,
            enableBinarySearchIndex: Boolean,
            buildFullBinarySearchIndex: Boolean,
            previous: Option[KeyValue.WriteOnly],
            deadline: Option[Deadline]): Stats = {

    val previousStats =
      previous.map(_.stats)

    val valueLength =
      value.map(_.size).getOrElse(0)

    val hasRemoveRange =
      previous.exists(_.stats.segmentHasRemoveRange) || isRemoveRange

    val segmentHasRange =
      hasRemoveRange || previous.exists(_.stats.segmentHasRange) || isRange

    val segmentHasPut =
      previous.exists(_.stats.segmentHasPut) || isPut

    val chainPosition =
      previousStats.map(_.chainPosition + 1) getOrElse 1

    val segmentTotalNumberOfRanges =
      previousStats.map(_.segmentTotalNumberOfRanges + numberOfRanges) getOrElse numberOfRanges

    val groupsCount =
      if (isGroup)
        previousStats.map(_.groupsCount + 1) getOrElse 1
      else
        previousStats.map(_.groupsCount) getOrElse 0

    val thisKeyValuesSortedIndexSize =
      Bytes.sizeOf(indexEntry.size) + indexEntry.size

    val thisKeyValuesRealIndexOffset =
      previousStats map {
        previous =>
          previous.thisKeyValueIndexOffset + previous.thisKeyValuesIndexSizeWithoutFooter
      } getOrElse 0

    //starts from 0. Do not need the actual index offset for space efficiency. The actual indexOffset can be adjust during read.
    val thisKeyValuesAccessIndexOffset =
      if (isPrefixCompressed)
        previousStats.map(_.thisKeyValuesAccessIndexOffset) getOrElse thisKeyValuesRealIndexOffset
      else
        thisKeyValuesRealIndexOffset

    val thisKeyValuesSegmentKeyAndValueSize =
      valueLength +
        thisKeyValuesSortedIndexSize

    //Items to add to BloomFilters is different to the position because a Group can contain
    //multiple inner key-values but the Group's key itself does not find added to the BloomFilter.
    val segmentUniqueKeysCount =
    previousStats.map(_.segmentUniqueKeysCount + thisKeyValuesUniqueKeys) getOrElse thisKeyValuesUniqueKeys

    val segmentHashIndexSize =
      if (segmentUniqueKeysCount < minimumNumberOfKeysForHashIndex)
        0
      else
        HashIndex.optimalBytesRequired(
          keyCounts = segmentUniqueKeysCount,
          largestValue = thisKeyValuesAccessIndexOffset,
          compensate = hashIndexCompensation
        )

    val segmentBinarySearchIndexSize =
      if (enableBinarySearchIndex)
        BinarySearchIndex.optimalBytesRequired(
          largestValue = thisKeyValuesAccessIndexOffset,
          valuesCount = if (buildFullBinarySearchIndex) segmentUniqueKeysCount else segmentTotalNumberOfRanges
        )
      else
        0

    val segmentValuesSize: Int =
      previousStats.map(_.segmentValuesSize).getOrElse(0) + valueLength

    val segmentSortedIndexSize =
      previousStats.map(_.segmentSortedIndexSize).getOrElse(0) + thisKeyValuesSortedIndexSize

    val segmentOptimalBloomFilterSize =
      if (falsePositiveRate <= 0.0 || (hasRemoveRange && !enableBinarySearchIndex))
        0
      else
        BloomFilter.optimalSegmentBloomFilterByteSize(
          numberOfKeys = segmentUniqueKeysCount,
          falsePositiveRate = falsePositiveRate
        )

    val segmentSizeWithoutFooter: Int =
      previousStats.map(previous => previous.segmentSizeWithoutFooter - previous.segmentHashIndexSize - previous.binarySearchIndexSize).getOrElse(0) +
        segmentHashIndexSize +
        segmentBinarySearchIndexSize

    //calculates the size of Segment after the last Group. This is used for size based grouping/compression.
    val segmentSizeWithoutFooterForNextGroup: Int =
      if (previous.exists(_.isGroup)) //if previous is a group, restart the size calculation
        segmentValuesSize +
          segmentSortedIndexSize +
          segmentHashIndexSize +
          segmentBinarySearchIndexSize +
          segmentOptimalBloomFilterSize
      else //if previous is not a group, add previous key-values set segment size since the last group to this key-values Segment size.
        previousStats.map(_.segmentSizeWithoutFooterForNextGroup).getOrElse(0) +
          valueLength +
          thisKeyValuesSortedIndexSize +
          segmentHashIndexSize +
          segmentBinarySearchIndexSize +
          segmentOptimalBloomFilterSize

    val segmentFooterSize =
      Bytes.sizeOf(SegmentWriter.formatId) + //1 byte for format
        1 + //created in level
        1 + //isGrouped
        1 + //hasRange
        1 + //hasPut
        ByteSizeOf.long + //for CRC. This cannot be unsignedLong because the size of the crc long bytes is not fixed.
        Bytes.sizeOf(segmentValuesSize max 0) + //index offset.
        Bytes.sizeOf((segmentValuesSize + segmentSortedIndexSize) max 1) + //hash index offset. HashIndex offset will never be 0 since that's reserved for index is values are none..
        Bytes.sizeOf(chainPosition) + //key-values count
        Bytes.sizeOf(segmentUniqueKeysCount)

    val segmentSize: Int =
      segmentSizeWithoutFooter +
        segmentFooterSize +
        ByteSizeOf.int //to store footer offset.

    val segmentUncompressedKeysSize: Int =
      previousStats.map(_.segmentUncompressedKeysSize).getOrElse(0) + indexEntry.size

    new Stats(
      valueSize = valueLength,
      segmentSize = segmentSize,
      chainPosition = chainPosition,
      groupsCount = groupsCount,
      segmentUniqueKeysCount = segmentUniqueKeysCount,
      segmentValuesSize = segmentValuesSize,
      segmentSortedIndexSize = segmentSortedIndexSize,
      segmentUncompressedKeysSize = segmentUncompressedKeysSize,
      segmentSizeWithoutFooter = segmentSizeWithoutFooter,
      segmentSizeWithoutFooterForNextGroup = segmentSizeWithoutFooterForNextGroup,
      keySize = indexEntry.size,
      thisKeyValuesSegmentKeyAndValueSize = thisKeyValuesSegmentKeyAndValueSize,
      thisKeyValuesIndexSizeWithoutFooter = thisKeyValuesSortedIndexSize,
      thisKeyValuesAccessIndexOffset = thisKeyValuesAccessIndexOffset,
      thisKeyValueIndexOffset = thisKeyValuesRealIndexOffset,
      segmentHashIndexSize = segmentHashIndexSize,
      bloomFilterSize = segmentOptimalBloomFilterSize,
      binarySearchIndexSize = segmentBinarySearchIndexSize,
      segmentFooterSize = segmentFooterSize,
      segmentTotalNumberOfRanges = segmentTotalNumberOfRanges,
      segmentHasRemoveRange = hasRemoveRange,
      segmentHasRange = segmentHasRange,
      segmentHasPut = segmentHasPut,
      isGroup = isGroup
    )
  }
}

private[core] case class Stats(valueSize: Int,
                               segmentSize: Int,
                               chainPosition: Int,
                               groupsCount: Int,
                               segmentUniqueKeysCount: Int,
                               segmentValuesSize: Int,
                               segmentSortedIndexSize: Int,
                               segmentUncompressedKeysSize: Int,
                               segmentSizeWithoutFooter: Int,
                               segmentSizeWithoutFooterForNextGroup: Int,
                               keySize: Int,
                               thisKeyValuesSegmentKeyAndValueSize: Int,
                               thisKeyValuesIndexSizeWithoutFooter: Int,
                               thisKeyValuesAccessIndexOffset: Int,
                               thisKeyValueIndexOffset: Int,
                               segmentHashIndexSize: Int,
                               bloomFilterSize: Int,
                               binarySearchIndexSize: Int,
                               segmentFooterSize: Int,
                               segmentTotalNumberOfRanges: Int,
                               segmentHasRemoveRange: Boolean,
                               segmentHasRange: Boolean,
                               segmentHasPut: Boolean,
                               isGroup: Boolean) {
  def segmentHasGroup: Boolean =
    groupsCount > 0

  def memorySegmentSize =
    segmentUncompressedKeysSize + segmentValuesSize

  def thisKeyValueMemorySize =
    keySize + valueSize
}
