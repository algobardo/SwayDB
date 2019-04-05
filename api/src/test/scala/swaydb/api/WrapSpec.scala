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
import scala.util.Try
import swaydb.data.io.Wrap
import swaydb.data._

class WrapSpec extends WordSpec with Matchers {

  "tryMap" should {
    "" in {
      implicit val wrap = Wrap.tryWrap

      val result: Stream[Int, Try] = Stream(1 to 100)

      result.foreach(println)

    }
  }

}
