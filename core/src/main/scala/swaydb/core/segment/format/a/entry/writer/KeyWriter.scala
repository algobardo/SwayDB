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

package swaydb.core.segment.format.a.entry.writer

import swaydb.core.data.Memory
import swaydb.core.segment.format.a.entry.id.{BaseEntryId, MemoryToKeyValueIdBinder}
import swaydb.core.util.Bytes
import swaydb.core.util.Options._
import swaydb.data.slice.Slice

private[core] object KeyWriter {

  /**
   * Format - keySize|key|keyValueId|accessIndex?|deadline|valueOffset|valueLength|time
   */
  def write(current: Memory,
            builder: EntryWriter.Builder,
            deadlineId: BaseEntryId.Deadline)(implicit binder: MemoryToKeyValueIdBinder[_]): Unit =
    when(builder.enablePrefixCompression)(builder.previous) flatMap {
      previous =>
        writeCompressed(
          current = current,
          builder = builder,
          deadlineId = deadlineId,
          previous = previous
        )
    } getOrElse {
      writeUncompressed(
        current = current,
        builder = builder,
        deadlineId = deadlineId
      )
    }

  private def writeCompressed(current: Memory,
                              builder: EntryWriter.Builder,
                              deadlineId: BaseEntryId.Deadline,
                              previous: Memory)(implicit binder: MemoryToKeyValueIdBinder[_]): Option[Unit] =
    Bytes.compress(key = current.mergedKey, previous = previous, minimumCommonBytes = 3) map {
      case (commonBytes, remainingBytes) =>
        write(
          current = current,
          builder = builder,
          commonBytes = commonBytes,
          headerBytes = remainingBytes,
          deadlineId = deadlineId,
          isKeyCompressed = true
        )
    }

  private def writeUncompressed(current: Memory,
                                builder: EntryWriter.Builder,
                                deadlineId: BaseEntryId.Deadline)(implicit binder: MemoryToKeyValueIdBinder[_]): Unit =

    write(
      current = current,
      builder = builder,
      commonBytes = -1,
      headerBytes = current.mergedKey,
      deadlineId = deadlineId,
      isKeyCompressed = false
    )

  private def write(current: Memory,
                    builder: EntryWriter.Builder,
                    commonBytes: Int,
                    headerBytes: Slice[Byte],
                    deadlineId: BaseEntryId.Deadline,
                    isKeyCompressed: Boolean)(implicit binder: MemoryToKeyValueIdBinder[_]): Unit = {
    val id =
      binder.keyValueId.adjustBaseIdToKeyValueIdKey(
        baseId = deadlineId.baseId,
        isKeyCompressed = isKeyCompressed
      )

    if(isKeyCompressed)
      builder.setSegmentHasPrefixCompression()

    val sortedIndexAccessPosition =
      if (builder.enableAccessPositionIndex)
        if (builder.isCurrentPrefixCompressed)
          builder.accessPositionIndex
        else
          builder.accessPositionIndex + 1
      else
        -1

    val byteSizeOfCommonBytes = Bytes.sizeOfUnsignedInt(commonBytes)

    if (isKeyCompressed) {
      //keySize includes the size of the commonBytes and the key. This is so that when reading key-value in
      //SortedIndexBlock and estimating max entry size the commonBytes are also accounted. This also makes it
      //easy parsing key in KeyReader.
      builder.bytes addUnsignedInt (headerBytes.size + byteSizeOfCommonBytes)
      builder.bytes addUnsignedInt commonBytes
    } else {
      builder.bytes addUnsignedInt headerBytes.size
    }

    builder.bytes addAll headerBytes

    builder.bytes addUnsignedInt id

    if (sortedIndexAccessPosition > 0) {
      builder.bytes addUnsignedInt sortedIndexAccessPosition
      builder.accessPositionIndex = sortedIndexAccessPosition
    }
  }
}
