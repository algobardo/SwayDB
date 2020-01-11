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

package swaydb.core.segment.format.a.block.segment.data

import java.nio.file.Path

import swaydb.core.actor.MemorySweeper
import swaydb.core.data.{Memory, Persistent, Time, Value}
import swaydb.core.io.reader.Reader
import swaydb.core.segment.format.a.block.binarysearch.BinarySearchIndexBlock
import swaydb.core.segment.format.a.block.bloomfilter.BloomFilterBlock
import swaydb.core.segment.format.a.block.hashindex.HashIndexBlock
import swaydb.core.segment.format.a.block.reader.{BlockRefReader, UnblockedReader}
import swaydb.core.segment.format.a.block.segment.SegmentBlock
import swaydb.core.segment.format.a.block.segment.footer.SegmentFooterBlock
import swaydb.core.segment.format.a.block.sortedindex.SortedIndexBlock
import swaydb.core.segment.format.a.block.values.ValuesBlock
import swaydb.core.segment.{SegmentIO, SegmentRef}
import swaydb.data.MaxKey
import swaydb.data.order.KeyOrder
import swaydb.data.slice.Slice
import swaydb.data.util.ByteSizeOf

object TransientSegmentSerialiser {

  def toKeyValue(one: TransientSegment.One,
                 offset: Int,
                 size: Int): Slice[Memory] =
    one.maxKey match {
      case MaxKey.Fixed(maxKey) =>
        val value = Slice.create[Byte](ByteSizeOf.byte + (ByteSizeOf.varInt * 2))
        value add 0 //fixed maxKey id
        value addUnsignedInt offset
        value addUnsignedInt size

        Slice(
          Memory.Range(one.minKey, maxKey, Value.FromValue.Null, Value.Update(value, None, Time.empty)),
          //this put currently not used but is stored to be backward compatible if onDisk binary search
          //is refered
          Memory.Put(maxKey, value, None, Time.empty)
        )

      case MaxKey.Range(fromKey, maxKey) =>
        val value = Slice.create[Byte](ByteSizeOf.byte + (ByteSizeOf.varInt * 2) + fromKey.size)
        value add 1 //range maxKey id
        value addUnsignedInt offset
        value addUnsignedInt size
        value addAll fromKey

        Slice(Memory.Range(one.minKey, maxKey, Value.FromValue.Null, Value.Update(value, None, Time.empty)))
    }

  def toSegmentRef(path: Path,
                   reader: BlockRefReader[SegmentBlock.Offset],
                   range: Persistent.Range,
                   valuesReaderCacheable: Option[UnblockedReader[ValuesBlock.Offset, ValuesBlock]],
                   sortedIndexReaderCacheable: Option[UnblockedReader[SortedIndexBlock.Offset, SortedIndexBlock]],
                   hashIndexReaderCacheable: Option[UnblockedReader[HashIndexBlock.Offset, HashIndexBlock]],
                   binarySearchIndexReaderCacheable: Option[UnblockedReader[BinarySearchIndexBlock.Offset, BinarySearchIndexBlock]],
                   bloomFilterReaderCacheable: Option[UnblockedReader[BloomFilterBlock.Offset, BloomFilterBlock]],
                   footerCacheable: Option[SegmentFooterBlock])(implicit keyOrder: KeyOrder[Slice[Byte]],
                                                                segmentIO: SegmentIO,
                                                                blockCacheMemorySweeper: Option[MemorySweeper.Block],
                                                                keyValueMemorySweeper: Option[MemorySweeper.KeyValue]): SegmentRef =
    range.fetchRangeValueUnsafe match {
      case Value.Update(value, deadline, time) =>
        val valueReader = Reader(value.getC)
        val maxKeyId = valueReader.get()
        if (maxKeyId == 0) {
          val segmentOffset = valueReader.readUnsignedInt()
          val segmentSize = valueReader.readUnsignedInt()
          SegmentRef(
            path = path.resolve(s".ref.$segmentOffset"),
            minKey = range.fromKey,
            maxKey = MaxKey.Fixed(range.toKey),
            blockRef =
              BlockRefReader(
                ref = reader,
                start = segmentOffset,
                size = segmentSize
              ),
            segmentIO = segmentIO,
            valuesReaderCacheable = valuesReaderCacheable,
            sortedIndexReaderCacheable = sortedIndexReaderCacheable,
            hashIndexReaderCacheable = hashIndexReaderCacheable,
            binarySearchIndexReaderCacheable = binarySearchIndexReaderCacheable,
            bloomFilterReaderCacheable = bloomFilterReaderCacheable,
            footerCacheable = footerCacheable
          )
        } else if (maxKeyId == 1) {
          val segmentOffset = valueReader.readUnsignedInt()
          val segmentSize = valueReader.readUnsignedInt()
          val maxKeyMinKey = valueReader.readRemaining()
          SegmentRef(
            path = path.resolve(s".ref.$segmentOffset"),
            minKey = range.fromKey,
            maxKey = MaxKey.Range(maxKeyMinKey, range.toKey),
            blockRef =
              BlockRefReader(
                ref = reader,
                start = segmentOffset,
                size = segmentSize
              ),
            segmentIO = segmentIO,
            valuesReaderCacheable = valuesReaderCacheable,
            sortedIndexReaderCacheable = sortedIndexReaderCacheable,
            hashIndexReaderCacheable = hashIndexReaderCacheable,
            binarySearchIndexReaderCacheable = binarySearchIndexReaderCacheable,
            bloomFilterReaderCacheable = bloomFilterReaderCacheable,
            footerCacheable = footerCacheable
          )
        } else {
          throw new Exception(s"Invalid maxKeyId: $maxKeyId")
        }

      case _: Value =>
        throw new Exception("Invalid value. Update expected")
    }

}