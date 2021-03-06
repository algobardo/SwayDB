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

package swaydb.java

import java.nio.file.Path
import java.util.Optional
import java.util.function.Supplier

import swaydb.OK

import scala.compat.java8.DurationConverters._
import scala.jdk.CollectionConverters._

case class Queue[A](asScala: swaydb.Queue[A]) {

  def path: Path =
    asScala.path

  def push(elem: A): OK =
    asScala.push(elem)

  def push(elem: A, expireAfter: java.time.Duration): OK =
    asScala.push(elem, expireAfter.toScala)

  def push(elems: Stream[A]): OK =
    asScala.push(elems.asScala)

  def push(elems: java.lang.Iterable[A]): OK =
    asScala.push(elems.asScala)

  def push(elems: java.util.Iterator[A]): OK =
    asScala.push(elems.asScala)

  def pop(): Optional[A] =
    Optional.of(asScala.popOrNull())

  def popOrNull(): A =
    asScala.popOrNull()

  def popOrElse(orElse: Supplier[A]): A =
    asScala.popOrElse(orElse.get())

  def stream: Stream[A] =
    new Stream(asScala.stream)

  def close(): Unit =
    asScala.close()

  def delete(): Unit =
    asScala.delete()

  private def copy(): Unit = ()

  override def toString(): String =
    asScala.toString
}
