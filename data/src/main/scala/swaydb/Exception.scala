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

package swaydb

import java.nio.channels.OverlappingFileLockException
import java.nio.file.Path

import swaydb.data.Reserve
import swaydb.data.slice.Slice


/**
 * Exception types for all known [[Error]]s that can occur. Each [[Error]] can be converted to
 * Exception which which can then be converted back to [[Error]].
 *
 * SwayDB's code itself does not use these exception it uses [[Error]] type. These types are handy when
 * converting an [[IO]] type to [[scala.util.Try]] by the client using [[toTry]].
 */
object Exception {
  case class Busy(error: Error.Recoverable) extends Exception("Is busy")
  case class OpeningFile(file: Path, reserve: Reserve[Unit]) extends Exception(s"Failed to open file $file")

  case class ReservedResource(reserve: Reserve[Unit]) extends Exception("ReservedResource is busy.")
  case class NullMappedByteBuffer(exception: Exception, reserve: Reserve[Unit]) extends Exception(exception)

  case object OverlappingPushSegment extends Exception("Contains overlapping busy Segments")
  case object NoSegmentsRemoved extends Exception("No Segments Removed")
  case object NotSentToNextLevel extends Exception("Not sent to next Level")
  case class MergeKeyValuesWithoutTargetSegment(keyValueCount: Int) extends Exception(s"Received key-values to merge without target Segment - keyValueCount: $keyValueCount")

  /**
   * [[functionID]] itself is not logged or printed to console since it may contain sensitive data but instead this Exception
   * with the [[functionID]] is returned to the client for reads and the exception's string message is only logged.
   *
   * @param functionID the id of the missing function.
   */
  case class FunctionNotFound(functionID: Slice[Byte]) extends Exception("Function not found for ID.")
  case class OverlappingFileLock(exception: OverlappingFileLockException) extends Exception("Failed to get directory lock.")
  case class FailedToWriteAllBytes(written: Int, expected: Int, bytesSize: Int) extends Exception(s"Failed to write all bytes written: $written, expected : $expected, bytesSize: $bytesSize")
  case class CannotCopyInMemoryFiles(file: Path) extends Exception(s"Cannot copy in-memory files $file")
  case class SegmentFileMissing(path: Path) extends Exception(s"$path: Segment file missing.")
  case class InvalidKeyValueId(id: Int) extends Exception(s"Invalid keyValueId: $id.")

  case class InvalidDecompressorId(id: Int) extends Exception(s"Invalid decompressor id: $id")

  case class NotAnIntFile(path: Path) extends Exception
  case class UnknownExtension(path: Path) extends Exception

  case class GetOnIncompleteDeferredFutureIO(reserve: Reserve[Unit]) extends Exception("Get invoked on in-complete Future within Deferred IO.")
}
