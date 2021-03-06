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
 * If you modify this Program, or any covered work, by linking or combining
 * it with other code, such other code is not for that reason alone subject
 * to any of the requirements of the GNU Affero GPL version 3.
 */

package swaydb.data.config.builder

import swaydb.data.accelerate.{Accelerator, LevelZeroMeter}
import swaydb.data.compaction.CompactionExecutionContext
import swaydb.data.config.{ConfigWizard, MemoryLevelZeroConfig}
import swaydb.data.util.Java.JavaFunction

import scala.concurrent.duration.FiniteDuration

/**
 * Java Builder class for [[MemoryLevelZeroConfig]]
 */
class MemoryLevelZeroConfigBuilder {
  private var mapSize: Long = _
  private var compactionExecutionContext: CompactionExecutionContext.Create = _
  private var acceleration: LevelZeroMeter => Accelerator = _
}

object MemoryLevelZeroConfigBuilder {

  class Step0(builder: MemoryLevelZeroConfigBuilder) {
    def mapSize(mapSize: Long) = {
      builder.mapSize = mapSize
      new Step1(builder)
    }
  }

  class Step1(builder: MemoryLevelZeroConfigBuilder) {
    def compactionExecutionContext(compactionExecutionContext: CompactionExecutionContext.Create) = {
      builder.compactionExecutionContext = compactionExecutionContext
      new Step2(builder)
    }
  }

  class Step2(builder: MemoryLevelZeroConfigBuilder) {
    def acceleration(acceleration: JavaFunction[LevelZeroMeter, Accelerator]) = {
      builder.acceleration = acceleration.apply
      new Step3(builder)
    }
  }

  class Step3(builder: MemoryLevelZeroConfigBuilder) {
    def throttle(throttle: JavaFunction[LevelZeroMeter, FiniteDuration]) =
      ConfigWizard.withMemoryLevel0(
        mapSize = builder.mapSize,
        compactionExecutionContext = builder.compactionExecutionContext,
        acceleration = builder.acceleration,
        throttle = throttle.apply
      )
  }

  def builder() = new Step0(new MemoryLevelZeroConfigBuilder())
}
