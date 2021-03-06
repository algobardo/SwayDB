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

package swaydb.memory

import java.util.concurrent.atomic.AtomicLong

import com.typesafe.scalalogging.LazyLogging
import swaydb.data.accelerate.{Accelerator, LevelZeroMeter}
import swaydb.data.compaction.{LevelMeter, Throttle}
import swaydb.data.config._
import swaydb.data.order.KeyOrder
import swaydb.data.slice.Slice
import swaydb.data.util.StorageUnits._
import swaydb.serializers.Serializer
import swaydb.{Bag, Error, IO}

import scala.concurrent.duration.FiniteDuration

object Queue extends LazyLogging {

  /**
   * For custom configurations read documentation on website: http://www.swaydb.io/configuring-levels
   */
  def apply[A](mapSize: Int = 4.mb,
               minSegmentSize: Int = 2.mb,
               maxKeyValuesPerSegment: Int = Int.MaxValue,
               fileCache: FileCache.Enable = DefaultConfigs.fileCache(),
               deleteSegmentsEventually: Boolean = true,
               acceleration: LevelZeroMeter => Accelerator = Accelerator.noBrakes(),
               levelZeroThrottle: LevelZeroMeter => FiniteDuration = DefaultConfigs.levelZeroThrottle,
               lastLevelThrottle: LevelMeter => Throttle = DefaultConfigs.lastLevelThrottle,
               threadStateCache: ThreadStateCache = ThreadStateCache.Limit(hashMapMaxSize = 100, maxProbe = 10))(implicit serializer: Serializer[A]): IO[Error.Boot, swaydb.Queue[A]] = {

    implicit val queueSerialiser: Serializer[(Long, A)] =
      swaydb.Queue.serialiser[A](serializer)

    implicit val keyOrder: KeyOrder[Slice[Byte]] =
      swaydb.Queue.ordering

    Set[(Long, A), Nothing, Bag.Less](
      mapSize = mapSize,
      minSegmentSize = minSegmentSize,
      maxKeyValuesPerSegment = maxKeyValuesPerSegment,
      fileCache = fileCache,
      deleteSegmentsEventually = deleteSegmentsEventually,
      acceleration = acceleration,
      levelZeroThrottle = levelZeroThrottle,
      lastLevelThrottle = lastLevelThrottle,
      threadStateCache = threadStateCache
    ) map {
      set =>
        val first: Long =
          set.headOption match {
            case Some((first, _)) =>
              first

            case None =>
              0
          }

        val last: Long =
          set.lastOption match {
            case Some((used, _)) =>
              used + 1

            case None =>
              0
          }

        swaydb.Queue(
          set = set,
          pushIds = new AtomicLong(last),
          popIds = new AtomicLong(first)
        )
    }
  }
}
