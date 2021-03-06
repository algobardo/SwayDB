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

package swaydb.data.config

sealed trait PrefixCompression {
  private[swaydb] def shouldCompress(interval: Int): Boolean
  def enabled: Boolean
  def keysOnly: Boolean
  def normaliseIndexForBinarySearch: Boolean
}
object PrefixCompression {
  case class Disable(normaliseIndexForBinarySearch: Boolean) extends PrefixCompression {
    override def keysOnly = false

    override def enabled: Boolean = false

    private[swaydb] override def shouldCompress(interval: Int): Boolean = false
  }

  sealed trait Interval {
    private[swaydb] def shouldCompress(index: Int): Boolean
  }

  def resetCompressionAt(indexInterval: Int): Interval =
    Interval.ResetCompressionAt(indexInterval)

  def compressAt(indexInterval: Int): Interval =
    Interval.CompressAt(indexInterval)

  object Interval {

    case class ResetCompressionAt(indexInterval: Int) extends Interval {
      private[swaydb] override def shouldCompress(index: Int): Boolean =
        indexInterval != 0 && index % this.indexInterval != 0
    }

    case class CompressAt(indexInterval: Int) extends Interval {
      private[swaydb] override def shouldCompress(index: Int): Boolean =
        indexInterval != 0 && index % this.indexInterval == 0
    }
  }

  case class Enable(keysOnly: Boolean, interval: Interval) extends PrefixCompression {
    override def normaliseIndexForBinarySearch: Boolean = false

    override def enabled: Boolean = true

    private[swaydb] override def shouldCompress(interval: Int): Boolean =
      this.interval.shouldCompress(interval)
  }
}
