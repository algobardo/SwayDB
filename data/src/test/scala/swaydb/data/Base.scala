/*
 * Copyright (c) 2020 Simer JS Plaha (simer.j@gmail.com - @simerplaha)
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
 *
 * Additional permission under the GNU Affero GPL version 3 section 7:
 * If you modify this Program or any covered work, only by linking or
 * combining it with separate works, the licensors of this Program grant
 * you additional permission to convey the resulting work.
 */

package swaydb.data

import java.io.FileNotFoundException
import java.nio.channels.{AsynchronousCloseException, ClosedChannelException}
import java.nio.file.Paths

import swaydb.Exception.NullMappedByteBuffer

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

object Base {

  implicit class AwaitImplicits[T](f: Future[T]) {
    def await: T =
      Await.result(f, 10.seconds)
  }

  def busyErrors(busyBoolean: Reserve[Unit] = Reserve.free(name = "busyError")): List[swaydb.Error.Recoverable] =
    List(
      swaydb.Error.OpeningFile(Paths.get("/some/path"), busyBoolean),
      swaydb.Error.NoSuchFile(Some(Paths.get("/some/path")), None),
      swaydb.Error.FileNotFound(new FileNotFoundException("")),
      swaydb.Error.AsynchronousClose(new AsynchronousCloseException()),
      swaydb.Error.ClosedChannel(new ClosedChannelException),
      swaydb.Error.NullMappedByteBuffer(NullMappedByteBuffer(new NullPointerException, busyBoolean)),
      swaydb.Error.ReservedResource(busyBoolean)
    )

  def randomBusyError(busyBoolean: Reserve[Unit] = Reserve.free(name = "randomBusyError")): swaydb.Error.Recoverable =
    Random.shuffle(busyErrors(busyBoolean)).head
}
