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

package swaydb.core.io.file

import java.nio.file.Path

import swaydb.IO
import swaydb.data.io.Core
import swaydb.data.slice.Slice

private[file] trait DBFileType {

  def path: Path

  def delete(): IO[Core.Error.Private, Unit]

  def close(): IO[Core.Error.Private, Unit]

  def append(slice: Slice[Byte]): IO[Core.Error.Private, Unit]

  def append(slice: Iterable[Slice[Byte]]): IO[Core.Error.Private, Unit]

  def read(position: Int, size: Int): IO[Core.Error.Private, Slice[Byte]]

  def get(position: Int): IO[Core.Error.Private, Byte]

  def readAll: IO[Core.Error.Private, Slice[Byte]]

  def fileSize: IO[Core.Error.Private, Long]

  def isMemoryMapped: IO[Core.Error.Private, Boolean]

  def isLoaded: IO[Core.Error.Private, Boolean]

  def isOpen: Boolean

  def isFull: IO[Core.Error.Private, Boolean]

  def memory: Boolean

  def persistent: Boolean =
    !memory

  def forceSave(): IO[Core.Error.Private, Unit]
}
