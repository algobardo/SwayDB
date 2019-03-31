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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with SwayDB. If not, see <https://www.gnu.org/licenses/>.
 */

package swaydb.api

import org.scalatest.{Matchers, WordSpec}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Try
import swaydb.Wrap._
import swaydb._
import swaydb.core.RunThis._
import swaydb.data.IO

class StreamFutureSpec extends StreamSpec[Future] {
  override def get[A](a: Future[A]): A = Await.result(a, 60.seconds)
}

class StreamIOSpec extends StreamSpec[IO] {
  override def get[A](a: IO[A]): A = a.get
}

class StreamTrySpec extends StreamSpec[Try] {
  override def get[A](a: Try[A]): A = a.get
}

sealed abstract class StreamSpec[T[_]](implicit wrap: Wrap[T]) extends WordSpec with Matchers {

  def get[A](a: T[A]): A

  implicit class Get[A](a: T[A]) {
    def await = get(a)
  }

  "Stream" should {

    "headOption" in {
      Stream[Int, T](1 to 100).headOption.await should contain(1)
    }

    "lastOptionLinear" in {
      Stream[Int, T](1 to 100).lastOptionLinear.await should contain(100)
    }

    "map" in {
      Stream[Int, T](1 to 1000)
        .map(_ + " one")
        .map(_ + " two")
        .map(_ + " three")
        .run
        .await shouldBe (1 to 1000).map(_ + " one two three")
    }

    "drop, take and map" in {
      Stream[Int, T](1 to 1000)
        .map(_.toString)
        .drop(10)
        .take(1)
        .map(_.toInt)
        .run
        .await should contain only 11
    }

    "drop, take and foreach" in {
      Stream[Int, T](1 to 1000)
        .map(_.toString)
        .drop(10)
        .take(1)
        .map(_.toInt)
        .foreach(println)
        .run
        .await should have size 1
    }

    "flatMap" in {
      Stream[Int, T](1 to 10)
        .flatMap(_ => Stream[Int, T](1 to 10))
        .await
        .run
        .await shouldBe Array.fill(10)(1 to 10).flatten
    }

    "filter" in {
      Stream[Int, T](1 to 10)
        .map(_ + 10)
        .filter(_ % 2 == 0)
        .take(2)
        .run
        .await should contain only(12, 14)
    }

    "filterNot" in {
      Stream[Int, T](1 to 10)
        .filterNot(_ % 2 == 0)
        .take(2)
        .run
        .await shouldBe (1 to 10).filter(_ % 2 != 0).take(2)
    }

    "not stack overflow" in {
      Stream[Int, T](1 to 1000000)
        .filter(_ % 100000 == 0)
        .run
        .await should contain only(100000, 200000, 300000, 400000, 500000, 600000, 700000, 800000, 900000, 1000000)
    }
  }
}