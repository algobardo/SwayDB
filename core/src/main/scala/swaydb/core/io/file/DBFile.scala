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

import com.typesafe.scalalogging.LazyLogging
import swaydb.Error.IO.ExceptionHandler
import swaydb.IO._
import swaydb.core.actor.{FileSweeper, FileSweeperItem}
import swaydb.core.cache.Cache
import swaydb.data.Reserve
import swaydb.data.config.IOStrategy
import swaydb.data.slice.Slice
import swaydb.{Error, IO}

import scala.util.hashing.MurmurHash3

object DBFile extends LazyLogging {

  def fileCache(filePath: Path,
                memoryMapped: Boolean,
                ioStrategy: IOStrategy,
                file: Option[DBFileType],
                blockCacheFileId: Long,
                autoClose: Boolean)(implicit fileSweeper: FileSweeper) = {

    //FIX-ME: need a better solution.
    var self: Cache[Error.IO, Unit, DBFileType] = null

    val closer: FileSweeperItem =
      new FileSweeperItem {
        override def path: Path = filePath
        override def delete(): IO[Error.Segment, Unit] = IO.failed("only closable")
        override def close(): IO[Error.Segment, Unit] = {
          self.get() map {
            fileType =>
              fileType.flatMap(_.close()) map {
                _ =>
                  self.clear()
              }
          } getOrElse IO.unit
        }

        override def isOpen: Boolean =
          self.get().exists(_.exists(_.isOpen))
      }

    val cache =
      Cache.io[swaydb.Error.IO, Error.OpeningFile, Unit, DBFileType](
        strategy = ioStrategy.withCacheOnAccess,
        reserveError = Error.OpeningFile(filePath, Reserve.free(name = s"DBFile: $filePath. MemoryMapped: $memoryMapped")),
        initial = file
      ) {
        _ =>
          logger.debug(s"{}: Opening closed file.", filePath)

          val openResult =
            if (memoryMapped)
              MMAPFile.read(filePath, blockCacheFileId)
            else
              ChannelFile.read(filePath, blockCacheFileId)

          if (autoClose)
            fileSweeper.close(closer)
          openResult
      }

    self = cache

    if (autoClose && file.isDefined) fileSweeper.close(closer)
    cache
  }

  def write(path: Path,
            bytes: Slice[Byte]): IO[swaydb.Error.IO, Path] =
    Effect.write(path, bytes)

  def write(path: Path,
            bytes: Iterable[Slice[Byte]]): IO[swaydb.Error.IO, Path] =
    Effect.write(path, bytes)

  def channelWrite(path: Path,
                   ioStrategy: IOStrategy,
                   blockCacheFileId: Long,
                   autoClose: Boolean)(implicit fileSweeper: FileSweeper,
                                       blockCache: Option[BlockCache.State]): IO[swaydb.Error.IO, DBFile] =
    ChannelFile.write(path, blockCacheFileId) map {
      file =>
        new DBFile(
          path = path,
          memoryMapped = false,
          autoClose = autoClose,
          blockCacheFileId = blockCacheFileId,
          fileCache =
            fileCache(
              filePath = path,
              memoryMapped = false,
              file = Some(file),
              ioStrategy = ioStrategy,
              autoClose = autoClose,
              blockCacheFileId = blockCacheFileId
            )
        )
    }

  def channelRead(path: Path,
                  ioStrategy: IOStrategy,
                  autoClose: Boolean,
                  blockCacheFileId: Long,
                  checkExists: Boolean = true)(implicit fileSweeper: FileSweeper,
                                               blockCache: Option[BlockCache.State]): IO[swaydb.Error.IO, DBFile] =
    if (checkExists && Effect.notExists(path))
      IO.Left[swaydb.Error.IO, DBFile](swaydb.Error.NoSuchFile(path))
    else
      IO {
        new DBFile(
          path = path,
          memoryMapped = false,
          autoClose = autoClose,
          blockCacheFileId = blockCacheFileId,
          fileCache =
            fileCache(
              filePath = path,
              memoryMapped = false,
              file = None,
              ioStrategy = ioStrategy,
              autoClose = autoClose,
              blockCacheFileId = blockCacheFileId
            )
        )
      }

  def mmapWriteAndRead(path: Path,
                       ioStrategy: IOStrategy,
                       autoClose: Boolean,
                       blockCacheFileId: Long,
                       bytes: Iterable[Slice[Byte]])(implicit fileSweeper: FileSweeper,
                                                     blockCache: Option[BlockCache.State]): IO[swaydb.Error.IO, DBFile] =
  //do not write bytes if the Slice has empty bytes.
    bytes.foldLeftIO(0) {
      case (written, bytes) =>
        if (!bytes.isFull)
          IO.failed(swaydb.Exception.FailedToWriteAllBytes(0, bytes.size, bytes.size))
        else
          IO.Right(written + bytes.size)
    } flatMap {
      totalWritten =>
        mmapInit(
          path = path,
          bufferSize = totalWritten,
          ioStrategy = ioStrategy,
          autoClose = autoClose,
          blockCacheFileId = blockCacheFileId
        ) map {
          file =>
            file.append(bytes)
            file
        }
    }

  def mmapWriteAndRead(path: Path,
                       ioStrategy: IOStrategy,
                       autoClose: Boolean,
                       blockCacheFileId: Long,
                       bytes: Slice[Byte])(implicit fileSweeper: FileSweeper,
                                           blockCache: Option[BlockCache.State]): IO[swaydb.Error.IO, DBFile] =
  //do not write bytes if the Slice has empty bytes.
    if (!bytes.isFull)
      IO.failed(swaydb.Exception.FailedToWriteAllBytes(0, bytes.size, bytes.size))
    else
      mmapInit(
        path = path,
        bufferSize = bytes.size,
        ioStrategy = ioStrategy,
        blockCacheFileId = blockCacheFileId,
        autoClose = autoClose
      ) map {
        file =>
          file.append(bytes)
          file
      }

  def mmapRead(path: Path,
               ioStrategy: IOStrategy,
               autoClose: Boolean,
               blockCacheFileId: Long,
               checkExists: Boolean = true)(implicit fileSweeper: FileSweeper,
                                            blockCache: Option[BlockCache.State]): IO[swaydb.Error.IO, DBFile] =
    if (checkExists && Effect.notExists(path))
      IO.Left[swaydb.Error.IO, DBFile](swaydb.Error.NoSuchFile(path))
    else
      IO {
        new DBFile(
          path = path,
          memoryMapped = true,
          autoClose = autoClose,
          blockCacheFileId = blockCacheFileId,
          fileCache =
            fileCache(
              filePath = path,
              memoryMapped = true,
              blockCacheFileId = blockCacheFileId,
              ioStrategy = ioStrategy,
              file = None,
              autoClose = autoClose
            )
        )
      }

  def mmapInit(path: Path,
               ioStrategy: IOStrategy,
               bufferSize: Long,
               blockCacheFileId: Long,
               autoClose: Boolean)(implicit fileSweeper: FileSweeper,
                                   blockCache: Option[BlockCache.State]): IO[swaydb.Error.IO, DBFile] =
    MMAPFile.write(path, bufferSize, blockCacheFileId) map {
      file =>
        new DBFile(
          path = path,
          memoryMapped = true,
          autoClose = autoClose,
          blockCacheFileId = blockCacheFileId,
          fileCache =
            fileCache(
              filePath = path,
              memoryMapped = true,
              file = Some(file),
              blockCacheFileId = blockCacheFileId,
              ioStrategy = ioStrategy,
              autoClose = autoClose
            )
        )
    }
}
/**
 * Wrapper class for different file types of [[DBFileType]].
 *
 * Responsible for lazy loading files for reads and opening closed files in a thread safe manner.
 */
class DBFile(val path: Path,
             memoryMapped: Boolean,
             autoClose: Boolean,
             val blockCacheFileId: Long,
             fileCache: Cache[swaydb.Error.IO, Unit, DBFileType])(implicit blockCache: Option[BlockCache.State]) extends LazyLogging {

  def existsOnDisk =
    Effect.exists(path)

  def blockSize: Option[Int] =
    blockCache.map(_.blockSize)

  def file: IO[Error.IO, DBFileType] =
    fileCache.value()

  def delete(): IO[swaydb.Error.IO, Unit] =
  //close the file
    close flatMap {
      _ =>
        //try delegating the delete to the file itself.
        //If the file is already closed, then delete it from disk.
        //memory files are never closed so the first statement will always be executed for memory files.
        (fileCache.get().map(_.flatMap(_.delete())) getOrElse Effect.deleteIfExists(path)) map {
          _ =>
            fileCache.clear()
        }
    }

  def close: IO[swaydb.Error.IO, Unit] =
    fileCache.get() map {
      fileType =>
        fileType.flatMap(_.close()) map {
          _ =>
            fileCache.clear()
        }
    } getOrElse IO.unit

  //if it's an in memory files return failure as Memory files cannot be copied.
  def copyTo(toPath: Path): IO[swaydb.Error.IO, Path] =
    forceSave() flatMap {
      _ =>
        Effect.copy(path, toPath) map {
          path =>
            logger.trace("{}: Copied: to {}", path, toPath)
            path
        }
    }

  def append(slice: Slice[Byte]) =
    fileCache.value().get.append(slice)

  def append(slice: Iterable[Slice[Byte]]) =
    fileCache.value().get.append(slice)

  def readBlock(position: Int): Option[Slice[Byte]] =
    blockCache map {
      blockCache =>
        read(
          position = position,
          size = blockCache.blockSize,
          blockCache = blockCache
        )
    }

  def read(position: Int, size: Int): Slice[Byte] =
    if (size == 0)
      Slice.emptyBytes
    else
      blockCache match {
        case Some(blockCache) =>
          read(
            position = position,
            size = size,
            blockCache = blockCache
          )

        case None =>
          fileCache.value().get.read(position, size)
      }

  def read(position: Int,
           size: Int,
           blockCache: BlockCache.State): Slice[Byte] =
    if (size == 0)
      Slice.emptyBytes
    else
      BlockCache.getOrSeek(
        position = position,
        size = size,
        file = fileCache.value().get,
        state = blockCache
      )

  def get(position: Int): Byte =
    if (blockCache.isDefined)
      read(position, 1).head
    else
      fileCache.value().get.get(position)

  def readAll: IO[Error.IO, Slice[Byte]] =
    fileCache.value() flatMap (_.readAll)

  def fileSize: Long =
    fileCache.value().get.fileSize

  //memory files are never closed, if it's memory file return true.
  def isOpen: Boolean =
    fileCache.get().exists(_.exists(_.isOpen))

  def isFileDefined: Boolean =
    fileCache.get().isDefined

  def isMemoryMapped: IO[Error.IO, Boolean] =
    fileCache.value() flatMap (_.isMemoryMapped)

  def isLoaded: IO[Error.IO, Boolean] =
    fileCache.value() flatMap (_.isLoaded)

  def isFull: IO[swaydb.Error.IO, Boolean] =
    fileCache.value() flatMap (_.isFull)

  def forceSave(): IO[swaydb.Error.IO, Unit] =
    fileCache.value().map(_.forceSave()) getOrElse IO.unit

  override def equals(that: Any): Boolean =
    that match {
      case other: DBFile =>
        this.path == other.path

      case _ =>
        false
    }

  override def hashCode(): Int =
    MurmurHash3.stringHash(path.toString)
}
