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

package swaydb

import scala.concurrent.duration.{Deadline, FiniteDuration}

/**
 * Output for [[PureFunction]] instances.
 */
sealed trait Apply[+V]
object Apply {

  /**
   * Function outputs for Map
   */
  sealed trait Map[+V] extends Apply[V]

  /**
   * Function outputs for Set
   */
  sealed trait Set[+V] extends Apply[V]

  case object Nothing extends Map[Nothing] with Set[Nothing]
  case object Remove extends Map[Nothing] with Set[Nothing]
  object Expire {
    def apply(after: FiniteDuration): Expire =
      new Expire(after.fromNow)
  }

  final case class Expire(deadline: Deadline) extends Map[Nothing] with Set[Nothing]

  object Update {
    def apply[V](value: V): Update[V] =
      new Update(value, None)

    def apply[V](value: V, expireAfter: FiniteDuration): Update[V] =
      new Update(value, Some(expireAfter.fromNow))

    def apply[V](value: V, expireAt: Deadline): Update[V] =
      new Update(value, Some(expireAt))
  }

  final case class Update[V](value: V, deadline: Option[Deadline]) extends Map[V]
}