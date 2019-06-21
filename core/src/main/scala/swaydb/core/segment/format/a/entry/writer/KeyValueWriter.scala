/*
 * Copyright (c) 2019 Simer Plaha (@simerplaha)
 *
 * This file is a part of SwayDB.
 *
 * SwayDB is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
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

import swaydb.core.data.{KeyValue, Time}
import swaydb.core.segment.format.a.entry.id.BaseEntryId.BaseEntryIdFormat
import swaydb.core.segment.format.a.entry.id.{BaseEntryIdFormatA, TransientToKeyValueIdBinder}
import swaydb.core.util.Bytes._
import swaydb.data.slice.Slice

private[core] object KeyValueWriter {

  case class Result(indexBytes: Slice[Byte],
                    valueBytes: Option[Slice[Byte]],
                    valueStartOffset: Int,
                    valueEndOffset: Int) {
    //TODO check if companion object function unapply returning an Option[Result] is cheaper than this unapply function.
    def unapply =
      (indexBytes, valueBytes, valueStartOffset, valueEndOffset)
  }

  /**
    * Returns the index bytes and value bytes for the key-value and also the used
    * value offset information for writing the next key-value.
    *
    * Each key also has a meta block which can be used to backward compatibility to store
    * more information for that key in the future that does not fit the current key format.
    *
    * Currently all keys are being stored under EmptyMeta.
    *
    * Note: No extra bytes are required to differentiate between a key that has meta or no meta block.
    *
    * @param binder                  [[BaseEntryIdFormat]] for this key-value's type.
    * @param compressDuplicateValues Compresses duplicate values if set to true.
    * @return indexEntry, valueBytes, valueOffsetBytes, nextValuesOffsetPosition
    */
  def write[T <: KeyValue.WriteOnly](current: T,
                                     currentTime: Time,
                                     compressDuplicateValues: Boolean,
                                     enablePrefixCompression: Boolean)(implicit binder: TransientToKeyValueIdBinder[T]): KeyValueWriter.Result =
    current.previous flatMap {
      previous =>
        if (enablePrefixCompression)
          writeCompressed(
            current = current,
            previous = previous,
            currentTime = currentTime,
            compressDuplicateValues = compressDuplicateValues,
            enablePrefixCompression = enablePrefixCompression
          )
        else
          None
    } getOrElse {
      writeUncompressed(
        current = current,
        currentTime = currentTime,
        compressDuplicateValues = compressDuplicateValues,
        enablePrefixCompression = enablePrefixCompression
      )
    }

  private def writeCompressed[T <: KeyValue.WriteOnly](current: T,
                                                       previous: KeyValue.WriteOnly,
                                                       currentTime: Time,
                                                       compressDuplicateValues: Boolean,
                                                       enablePrefixCompression: Boolean)(implicit binder: TransientToKeyValueIdBinder[T]) =
    compress(key = current.fullKey, previous = previous, minimumCommonBytes = 2) map {
      case (commonBytes, remainingBytes) =>
        val writeResult =
          TimeWriter.write(
            current = current,
            currentTime = currentTime,
            compressDuplicateValues = compressDuplicateValues,
            entryId = BaseEntryIdFormatA.format.start,
            enablePrefixCompression = enablePrefixCompression,
            isKeyCompressed = true,
            plusSize = sizeOf(commonBytes) + remainingBytes.size //write the size of keys compressed and also the uncompressed Bytes
          )

        writeResult
          .indexBytes
          .addIntUnsigned(commonBytes)
          .addAll(remainingBytes)

        writeResult
    }

  private def writeUncompressed[T <: KeyValue.WriteOnly](current: T,
                                                         currentTime: Time,
                                                         compressDuplicateValues: Boolean,
                                                         enablePrefixCompression: Boolean)(implicit binder: TransientToKeyValueIdBinder[T]) = {
    //no common prefixes or no previous write without compression
    val writeResult =
      TimeWriter.write(
        current = current,
        currentTime = currentTime,
        compressDuplicateValues = compressDuplicateValues,
        entryId = BaseEntryIdFormatA.format.start,
        enablePrefixCompression = enablePrefixCompression,
        isKeyCompressed = false,
        plusSize = current.fullKey.size //write key bytes.
      )

    writeResult
      .indexBytes
      .addAll(current.fullKey)

    writeResult
  }
}