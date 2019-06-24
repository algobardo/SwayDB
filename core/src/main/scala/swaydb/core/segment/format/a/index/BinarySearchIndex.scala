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

package swaydb.core.segment.format.a.index

import swaydb.core.data.{KeyValue, Persistent}
import swaydb.core.io.reader.Reader
import swaydb.core.segment.format.a.{KeyMatcher, MatchResult, OffsetBase}
import swaydb.core.util.Bytes
import swaydb.data.IO
import swaydb.data.slice.{Reader, Slice}
import swaydb.data.util.ByteSizeOf

import scala.annotation.tailrec

object BinarySearchIndex {

  val formatId: Byte = 1.toByte

  case class Offset(start: Int, size: Int) extends OffsetBase

  object State {
    def apply(largestValue: Int,
              uniqueValuesCount: Int,
              buildFullBinarySearchIndex: Boolean): State =
      State(
        largestValue = largestValue,
        uniqueValuesCount = 0,
        buildFullBinarySearchIndex = buildFullBinarySearchIndex,
        bytes = Slice.create[Byte](optimalBytesRequired(largestValue, uniqueValuesCount))
      )

    def apply(largestValue: Int,
              uniqueValuesCount: Int,
              buildFullBinarySearchIndex: Boolean,
              bytes: Slice[Byte]): State =
      new State(
        byteSizeOfLargestValue = Bytes.sizeOf(largestValue),
        buildFullBinarySearchIndex = buildFullBinarySearchIndex,
        _previousWritten = Int.MinValue,
        writtenValues = 0,
        headerSize =
          optimalHeaderSize(
            largestValue = largestValue,
            valuesCount = uniqueValuesCount
          ),
        uniqueValuesCount = uniqueValuesCount,
        bytes = bytes
      )
  }

  case class State(byteSizeOfLargestValue: Int,
                   uniqueValuesCount: Int,
                   var _previousWritten: Int,
                   var writtenValues: Int,
                   headerSize: Int,
                   buildFullBinarySearchIndex: Boolean,
                   bytes: Slice[Byte]) {

    def incrementWrittenValuesCount() =
      writtenValues += 1

    def previouslyWritten_=(previouslyWritten: Int) =
      this._previousWritten = previouslyWritten

    def previouslyWritten = _previousWritten
  }

  def init(keyValues: Iterable[KeyValue.WriteOnly]) =
    if (keyValues.last.stats.binarySearchIndexSize <= 1)
      None
    else
      Some(
        BinarySearchIndex.State(
          largestValue = keyValues.last.stats.thisKeyValuesAccessIndexOffset,
          uniqueValuesCount = keyValues.last.stats.segmentUniqueKeysCount,
          buildFullBinarySearchIndex = keyValues.last.buildFullBinarySearchIndex
        )
      )

  def optimalBytesRequired(largestValue: Int,
                           valuesCount: Int): Int =
    optimalHeaderSize(
      largestValue = largestValue,
      valuesCount = valuesCount
    ) + (Bytes.sizeOf(largestValue) * valuesCount)

  def optimalHeaderSize(largestValue: Int,
                        valuesCount: Int): Int = {

    val headerSize =
      ByteSizeOf.byte + //formatId
        Bytes.sizeOf(valuesCount) +
        Bytes.sizeOf(largestValue) +
        ByteSizeOf.boolean // buildFullBinarySearchIndex

    Bytes.sizeOf(headerSize) +
      headerSize
  }

  def writeHeader(state: State): IO[Unit] =
    if (state.writtenValues != state.uniqueValuesCount)
      IO.Failure(IO.Error.Fatal(s"Binary search index incomplete. Written: ${state.writtenValues}. Expected: ${state.uniqueValuesCount}"))
    else
      IO {
        state.bytes moveWritePosition 0
        state.bytes addIntUnsigned state.headerSize
        state.bytes add formatId
        state.bytes addIntUnsigned state.uniqueValuesCount
        state.bytes addIntUnsigned state.byteSizeOfLargestValue
        state.bytes addBoolean state.buildFullBinarySearchIndex
      }

  def read(offset: Offset,
           reader: Reader): IO[BinarySearchIndex] = {
    val movedReader = reader.moveTo(offset.start)
    movedReader
      .readIntUnsigned()
      .flatMap {
        headerSize =>
          movedReader
            .read(headerSize)
            .flatMap {
              headBytes =>
                val headerReader = Reader(headBytes)
                headerReader
                  .get()
                  .flatMap {
                    formatId =>
                      if (formatId != this.formatId)
                        IO.Failure(new Exception(s"Invalid formatID: $formatId"))
                      else
                        for {
                          valuesCount <- headerReader.readIntUnsigned()
                          byteSizeOfLargestValue <- headerReader.readIntUnsigned()
                          isFullBinarySearchIndex <- headerReader.readBoolean()
                        } yield
                          BinarySearchIndex(
                            offset = offset,
                            valuesCount = valuesCount,
                            headerSize = headerSize,
                            byteSizeOfLargestValue = byteSizeOfLargestValue,
                            isFullBinarySearchIndex = isFullBinarySearchIndex
                          )
                  }
            }
      }
  }

  def write(value: Int,
            state: State): IO[Unit] =
    if (value == state.previouslyWritten) //do not write duplicate entries.
      IO.unit
    else
      IO {
        if (state.bytes.written == 0) state.bytes moveWritePosition state.headerSize

        if (state.byteSizeOfLargestValue <= ByteSizeOf.int)
          state.bytes addIntUnsigned value
        else
          state.bytes addInt value

        state.incrementWrittenValuesCount()
        state.previouslyWritten = value
      }

  def find(index: BinarySearchIndex,
           assertValue: Int => IO[MatchResult]): IO[Option[Persistent]] = {

    val minimumOffset = index.offset.start + index.headerSize

    @tailrec
    def hop(start: Int, end: Int): IO[Option[Persistent]] = {
      val mid = start + (end - start) / 2

      val valueOffset = minimumOffset + (mid * index.byteSizeOfLargestValue)
      if (start > end)
        IO.none
      else
        assertValue(valueOffset) match {
          case IO.Success(value) =>
            value match {
              case MatchResult.Matched(result) =>
                IO.Success(Some(result))

              case MatchResult.Next =>
                hop(start, mid - 1)

              case MatchResult.Stop =>
                hop(mid + 1, end)
            }
          case IO.Failure(error) =>
            IO.Failure(error)
        }
    }

    hop(start = 0, end = index.valuesCount - 1)
  }

  def get(matcher: KeyMatcher.Get,
          reader: Reader,
          index: BinarySearchIndex,
          sortedIndexOffset: SortedIndex.Offset): IO[Option[Persistent]] =
    find(
      index = index,
      assertValue =
        sortedIndexOffsetValue =>
          SortedIndex.findAndMatch(
            matcher = matcher,
            fromOffset = sortedIndexOffset.start + sortedIndexOffsetValue,
            reader = reader,
            offset = sortedIndexOffset
          )
    )
}

case class BinarySearchIndex(offset: BinarySearchIndex.Offset,
                             valuesCount: Int,
                             headerSize: Int,
                             byteSizeOfLargestValue: Int,
                             isFullBinarySearchIndex: Boolean)