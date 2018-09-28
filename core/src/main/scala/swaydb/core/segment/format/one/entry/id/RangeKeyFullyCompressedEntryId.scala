
/*
 * Copyright (C) 2018 Simer Plaha (@simerplaha)
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

package swaydb.core.segment.format.one.entry.id

/** ******************************************
  * ************ GENERATED CLASS *************
  * ******************************************/

import swaydb.core.segment.format.one.entry.id.EntryId._
import swaydb.macros.SealedList
import swaydb.core.util.PipeOps._

/**
  * This code is not used in actual production.
  *
  * It is a base template class for generating IDs for all other key-value type using
  * [[swaydb.core.segment.format.one.entry.generators.IdsGenerator]] which gives all key-values types unique ids.
  *
  * will remove that line for the target generated class.
  *
  * TO DO - switch to using macros.
  */
sealed abstract class RangeKeyFullyCompressedEntryId(override val id: Int) extends EntryId(id)
object RangeKeyFullyCompressedEntryId {

  def keyIdsList: List[RangeKeyFullyCompressedEntryId] = SealedList.list[RangeKeyFullyCompressedEntryId].sortBy(_.id)

  private val (headId, lastId) = keyIdsList ==> {
    keyIdsList =>
      (keyIdsList.head.id, keyIdsList.last.id)
  }

  def contains(id: Int): Option[Int] =
    if (id >= headId && id <= lastId)
      Some(id)
    else
      None

  sealed trait KeyFullyCompressed extends Key.FullyCompressed {
    override val valueFullyCompressed: Value.FullyCompressed = KeyFullyCompressed.ValueFullyCompressed
    override val valueUncompressed: Value.Uncompressed = KeyFullyCompressed.ValueUncompressed
    override val noValue: Value.NoValue = KeyFullyCompressed.NoValue
  }
  case object KeyFullyCompressed extends KeyFullyCompressed {

    sealed trait NoValue extends Value.NoValue with KeyFullyCompressed {
      override val noDeadline: Deadline.NoDeadline = NoValue.NoDeadline
      override val deadlineOneCompressed: Deadline.OneCompressed = NoValue.DeadlineOneCompressed
      override val deadlineTwoCompressed: Deadline.TwoCompressed = NoValue.DeadlineTwoCompressed
      override val deadlineThreeCompressed: Deadline.ThreeCompressed = NoValue.DeadlineThreeCompressed
      override val deadlineFourCompressed: Deadline.FourCompressed = NoValue.DeadlineFourCompressed
      override val deadlineFiveCompressed: Deadline.FiveCompressed = NoValue.DeadlineFiveCompressed
      override val deadlineSixCompressed: Deadline.SixCompressed = NoValue.DeadlineSixCompressed
      override val deadlineSevenCompressed: Deadline.SevenCompressed = NoValue.DeadlineSevenCompressed
      override val deadlineFullyCompressed: Deadline.FullyCompressed = NoValue.DeadlineFullyCompressed
      override val deadlineUncompressed: Deadline.Uncompressed = NoValue.DeadlineUncompressed
    }
    object NoValue extends NoValue {
      case object NoDeadline extends RangeKeyFullyCompressedEntryId(1798) with Deadline.NoDeadline with NoValue
      case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1799) with Deadline.OneCompressed with NoValue
      case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(1800) with Deadline.TwoCompressed with NoValue
      case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(1801) with Deadline.ThreeCompressed with NoValue
      case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(1802) with Deadline.FourCompressed with NoValue
      case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(1803) with Deadline.FiveCompressed with NoValue
      case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(1804) with Deadline.SixCompressed with NoValue
      case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(1805) with Deadline.SevenCompressed with NoValue
      case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(1806) with Deadline.FullyCompressed with NoValue
      case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(1807) with Deadline.Uncompressed with NoValue
    }

    sealed trait ValueFullyCompressed extends Value.FullyCompressed with KeyFullyCompressed {
      override val noDeadline: Deadline.NoDeadline = ValueFullyCompressed.NoDeadline
      override val deadlineOneCompressed: Deadline.OneCompressed = ValueFullyCompressed.DeadlineOneCompressed
      override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueFullyCompressed.DeadlineTwoCompressed
      override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueFullyCompressed.DeadlineThreeCompressed
      override val deadlineFourCompressed: Deadline.FourCompressed = ValueFullyCompressed.DeadlineFourCompressed
      override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueFullyCompressed.DeadlineFiveCompressed
      override val deadlineSixCompressed: Deadline.SixCompressed = ValueFullyCompressed.DeadlineSixCompressed
      override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueFullyCompressed.DeadlineSevenCompressed
      override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueFullyCompressed.DeadlineFullyCompressed
      override val deadlineUncompressed: Deadline.Uncompressed = ValueFullyCompressed.DeadlineUncompressed
    }
    object ValueFullyCompressed extends ValueFullyCompressed {
      case object NoDeadline extends RangeKeyFullyCompressedEntryId(1808) with Deadline.NoDeadline with ValueFullyCompressed
      case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1809) with Deadline.OneCompressed with ValueFullyCompressed
      case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(1810) with Deadline.TwoCompressed with ValueFullyCompressed
      case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(1811) with Deadline.ThreeCompressed with ValueFullyCompressed
      case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(1812) with Deadline.FourCompressed with ValueFullyCompressed
      case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(1813) with Deadline.FiveCompressed with ValueFullyCompressed
      case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(1814) with Deadline.SixCompressed with ValueFullyCompressed
      case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(1815) with Deadline.SevenCompressed with ValueFullyCompressed
      case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(1816) with Deadline.FullyCompressed with ValueFullyCompressed
      case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(1817) with Deadline.Uncompressed with ValueFullyCompressed
    }

    sealed trait ValueUncompressed extends Value.Uncompressed with KeyFullyCompressed {
      override val valueOffsetOneCompressed: ValueOffset.OneCompressed = ValueUncompressed.ValueOffsetOneCompressed
      override val valueOffsetTwoCompressed: ValueOffset.TwoCompressed = ValueUncompressed.ValueOffsetTwoCompressed
      override val valueOffsetThreeCompressed: ValueOffset.ThreeCompressed = ValueUncompressed.ValueOffsetThreeCompressed
      override val valueOffsetUncompressed: ValueOffset.Uncompressed = ValueUncompressed.ValueOffsetUncompressed
    }
    object ValueUncompressed extends ValueUncompressed {

      sealed trait ValueOffsetOneCompressed extends ValueOffset.OneCompressed with ValueUncompressed {
        override val valueLengthOneCompressed: ValueLength.OneCompressed = ValueOffsetOneCompressed.ValueLengthOneCompressed
        override val valueLengthTwoCompressed: ValueLength.TwoCompressed = ValueOffsetOneCompressed.ValueLengthTwoCompressed
        override val valueLengthThreeCompressed: ValueLength.ThreeCompressed = ValueOffsetOneCompressed.ValueLengthThreeCompressed
        override val valueLengthFullyCompressed: ValueLength.FullyCompressed = ValueOffsetOneCompressed.ValueLengthFullyCompressed
        override val valueLengthUncompressed: ValueLength.Uncompressed = ValueOffsetOneCompressed.ValueLengthUncompressed
      }
      object ValueOffsetOneCompressed extends ValueOffsetOneCompressed {
        sealed trait ValueLengthOneCompressed extends ValueLength.OneCompressed with ValueOffsetOneCompressed {
          override val noDeadline: Deadline.NoDeadline = ValueLengthOneCompressed.NoDeadline
          override val deadlineOneCompressed: Deadline.OneCompressed = ValueLengthOneCompressed.DeadlineOneCompressed
          override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueLengthOneCompressed.DeadlineTwoCompressed
          override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueLengthOneCompressed.DeadlineThreeCompressed
          override val deadlineFourCompressed: Deadline.FourCompressed = ValueLengthOneCompressed.DeadlineFourCompressed
          override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueLengthOneCompressed.DeadlineFiveCompressed
          override val deadlineSixCompressed: Deadline.SixCompressed = ValueLengthOneCompressed.DeadlineSixCompressed
          override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueLengthOneCompressed.DeadlineSevenCompressed
          override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueLengthOneCompressed.DeadlineFullyCompressed
          override val deadlineUncompressed: Deadline.Uncompressed = ValueLengthOneCompressed.DeadlineUncompressed
        }

        object ValueLengthOneCompressed extends ValueLengthOneCompressed {
          case object NoDeadline extends RangeKeyFullyCompressedEntryId(1818) with Deadline.NoDeadline with ValueLengthOneCompressed
          case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1819) with Deadline.OneCompressed with ValueLengthOneCompressed
          case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(1820) with Deadline.TwoCompressed with ValueLengthOneCompressed
          case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(1821) with Deadline.ThreeCompressed with ValueLengthOneCompressed
          case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(1822) with Deadline.FourCompressed with ValueLengthOneCompressed
          case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(1823) with Deadline.FiveCompressed with ValueLengthOneCompressed
          case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(1824) with Deadline.SixCompressed with ValueLengthOneCompressed
          case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(1825) with Deadline.SevenCompressed with ValueLengthOneCompressed
          case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(1826) with Deadline.FullyCompressed with ValueLengthOneCompressed
          case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(1827) with Deadline.Uncompressed with ValueLengthOneCompressed
        }

        sealed trait ValueLengthTwoCompressed extends ValueLength.TwoCompressed with ValueOffsetOneCompressed {
          override val noDeadline: Deadline.NoDeadline = ValueLengthTwoCompressed.NoDeadline
          override val deadlineOneCompressed: Deadline.OneCompressed = ValueLengthTwoCompressed.DeadlineOneCompressed
          override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueLengthTwoCompressed.DeadlineTwoCompressed
          override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueLengthTwoCompressed.DeadlineThreeCompressed
          override val deadlineFourCompressed: Deadline.FourCompressed = ValueLengthTwoCompressed.DeadlineFourCompressed
          override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueLengthTwoCompressed.DeadlineFiveCompressed
          override val deadlineSixCompressed: Deadline.SixCompressed = ValueLengthTwoCompressed.DeadlineSixCompressed
          override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueLengthTwoCompressed.DeadlineSevenCompressed
          override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueLengthTwoCompressed.DeadlineFullyCompressed
          override val deadlineUncompressed: Deadline.Uncompressed = ValueLengthTwoCompressed.DeadlineUncompressed
        }

        object ValueLengthTwoCompressed extends ValueLengthTwoCompressed {
          case object NoDeadline extends RangeKeyFullyCompressedEntryId(1828) with Deadline.NoDeadline with ValueLengthTwoCompressed
          case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1829) with Deadline.OneCompressed with ValueLengthTwoCompressed
          case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(1830) with Deadline.TwoCompressed with ValueLengthTwoCompressed
          case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(1831) with Deadline.ThreeCompressed with ValueLengthTwoCompressed
          case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(1832) with Deadline.FourCompressed with ValueLengthTwoCompressed
          case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(1833) with Deadline.FiveCompressed with ValueLengthTwoCompressed
          case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(1834) with Deadline.SixCompressed with ValueLengthTwoCompressed
          case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(1835) with Deadline.SevenCompressed with ValueLengthTwoCompressed
          case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(1836) with Deadline.FullyCompressed with ValueLengthTwoCompressed
          case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(1837) with Deadline.Uncompressed with ValueLengthTwoCompressed
        }

        sealed trait ValueLengthThreeCompressed extends ValueLength.ThreeCompressed with ValueOffsetOneCompressed {
          override val noDeadline: Deadline.NoDeadline = ValueLengthThreeCompressed.NoDeadline
          override val deadlineOneCompressed: Deadline.OneCompressed = ValueLengthThreeCompressed.DeadlineOneCompressed
          override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueLengthThreeCompressed.DeadlineTwoCompressed
          override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueLengthThreeCompressed.DeadlineThreeCompressed
          override val deadlineFourCompressed: Deadline.FourCompressed = ValueLengthThreeCompressed.DeadlineFourCompressed
          override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueLengthThreeCompressed.DeadlineFiveCompressed
          override val deadlineSixCompressed: Deadline.SixCompressed = ValueLengthThreeCompressed.DeadlineSixCompressed
          override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueLengthThreeCompressed.DeadlineSevenCompressed
          override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueLengthThreeCompressed.DeadlineFullyCompressed
          override val deadlineUncompressed: Deadline.Uncompressed = ValueLengthThreeCompressed.DeadlineUncompressed
        }

        object ValueLengthThreeCompressed extends ValueLengthThreeCompressed {
          case object NoDeadline extends RangeKeyFullyCompressedEntryId(1838) with Deadline.NoDeadline with ValueLengthThreeCompressed
          case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1839) with Deadline.OneCompressed with ValueLengthThreeCompressed
          case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(1840) with Deadline.TwoCompressed with ValueLengthThreeCompressed
          case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(1841) with Deadline.ThreeCompressed with ValueLengthThreeCompressed
          case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(1842) with Deadline.FourCompressed with ValueLengthThreeCompressed
          case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(1843) with Deadline.FiveCompressed with ValueLengthThreeCompressed
          case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(1844) with Deadline.SixCompressed with ValueLengthThreeCompressed
          case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(1845) with Deadline.SevenCompressed with ValueLengthThreeCompressed
          case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(1846) with Deadline.FullyCompressed with ValueLengthThreeCompressed
          case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(1847) with Deadline.Uncompressed with ValueLengthThreeCompressed
        }

        sealed trait ValueLengthFullyCompressed extends ValueLength.FullyCompressed with ValueOffsetOneCompressed {
          override val noDeadline: Deadline.NoDeadline = ValueLengthFullyCompressed.NoDeadline
          override val deadlineOneCompressed: Deadline.OneCompressed = ValueLengthFullyCompressed.DeadlineOneCompressed
          override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueLengthFullyCompressed.DeadlineTwoCompressed
          override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueLengthFullyCompressed.DeadlineThreeCompressed
          override val deadlineFourCompressed: Deadline.FourCompressed = ValueLengthFullyCompressed.DeadlineFourCompressed
          override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueLengthFullyCompressed.DeadlineFiveCompressed
          override val deadlineSixCompressed: Deadline.SixCompressed = ValueLengthFullyCompressed.DeadlineSixCompressed
          override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueLengthFullyCompressed.DeadlineSevenCompressed
          override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueLengthFullyCompressed.DeadlineFullyCompressed
          override val deadlineUncompressed: Deadline.Uncompressed = ValueLengthFullyCompressed.DeadlineUncompressed
        }

        object ValueLengthFullyCompressed extends ValueLengthFullyCompressed {
          case object NoDeadline extends RangeKeyFullyCompressedEntryId(1848) with Deadline.NoDeadline with ValueLengthFullyCompressed
          case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1849) with Deadline.OneCompressed with ValueLengthFullyCompressed
          case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(1850) with Deadline.TwoCompressed with ValueLengthFullyCompressed
          case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(1851) with Deadline.ThreeCompressed with ValueLengthFullyCompressed
          case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(1852) with Deadline.FourCompressed with ValueLengthFullyCompressed
          case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(1853) with Deadline.FiveCompressed with ValueLengthFullyCompressed
          case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(1854) with Deadline.SixCompressed with ValueLengthFullyCompressed
          case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(1855) with Deadline.SevenCompressed with ValueLengthFullyCompressed
          case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(1856) with Deadline.FullyCompressed with ValueLengthFullyCompressed
          case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(1857) with Deadline.Uncompressed with ValueLengthFullyCompressed
        }

        sealed trait ValueLengthUncompressed extends ValueLength.Uncompressed with ValueOffsetOneCompressed {
          override val noDeadline: Deadline.NoDeadline = ValueLengthUncompressed.NoDeadline
          override val deadlineOneCompressed: Deadline.OneCompressed = ValueLengthUncompressed.DeadlineOneCompressed
          override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueLengthUncompressed.DeadlineTwoCompressed
          override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueLengthUncompressed.DeadlineThreeCompressed
          override val deadlineFourCompressed: Deadline.FourCompressed = ValueLengthUncompressed.DeadlineFourCompressed
          override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueLengthUncompressed.DeadlineFiveCompressed
          override val deadlineSixCompressed: Deadline.SixCompressed = ValueLengthUncompressed.DeadlineSixCompressed
          override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueLengthUncompressed.DeadlineSevenCompressed
          override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueLengthUncompressed.DeadlineFullyCompressed
          override val deadlineUncompressed: Deadline.Uncompressed = ValueLengthUncompressed.DeadlineUncompressed
        }

        object ValueLengthUncompressed extends ValueLengthUncompressed {
          case object NoDeadline extends RangeKeyFullyCompressedEntryId(1858) with Deadline.NoDeadline with ValueLengthUncompressed
          case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1859) with Deadline.OneCompressed with ValueLengthUncompressed
          case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(1860) with Deadline.TwoCompressed with ValueLengthUncompressed
          case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(1861) with Deadline.ThreeCompressed with ValueLengthUncompressed
          case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(1862) with Deadline.FourCompressed with ValueLengthUncompressed
          case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(1863) with Deadline.FiveCompressed with ValueLengthUncompressed
          case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(1864) with Deadline.SixCompressed with ValueLengthUncompressed
          case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(1865) with Deadline.SevenCompressed with ValueLengthUncompressed
          case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(1866) with Deadline.FullyCompressed with ValueLengthUncompressed
          case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(1867) with Deadline.Uncompressed with ValueLengthUncompressed
        }
      }

      sealed trait ValueOffsetTwoCompressed extends ValueOffset.TwoCompressed with ValueUncompressed {
        override val valueLengthOneCompressed: ValueLength.OneCompressed = ValueOffsetTwoCompressed.ValueLengthOneCompressed
        override val valueLengthTwoCompressed: ValueLength.TwoCompressed = ValueOffsetTwoCompressed.ValueLengthTwoCompressed
        override val valueLengthThreeCompressed: ValueLength.ThreeCompressed = ValueOffsetTwoCompressed.ValueLengthThreeCompressed
        override val valueLengthFullyCompressed: ValueLength.FullyCompressed = ValueOffsetTwoCompressed.ValueLengthFullyCompressed
        override val valueLengthUncompressed: ValueLength.Uncompressed = ValueOffsetTwoCompressed.ValueLengthUncompressed
      }
      object ValueOffsetTwoCompressed extends ValueOffsetTwoCompressed {
        sealed trait ValueLengthOneCompressed extends ValueLength.OneCompressed with ValueOffsetTwoCompressed {
          override val noDeadline: Deadline.NoDeadline = ValueLengthOneCompressed.NoDeadline
          override val deadlineOneCompressed: Deadline.OneCompressed = ValueLengthOneCompressed.DeadlineOneCompressed
          override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueLengthOneCompressed.DeadlineTwoCompressed
          override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueLengthOneCompressed.DeadlineThreeCompressed
          override val deadlineFourCompressed: Deadline.FourCompressed = ValueLengthOneCompressed.DeadlineFourCompressed
          override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueLengthOneCompressed.DeadlineFiveCompressed
          override val deadlineSixCompressed: Deadline.SixCompressed = ValueLengthOneCompressed.DeadlineSixCompressed
          override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueLengthOneCompressed.DeadlineSevenCompressed
          override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueLengthOneCompressed.DeadlineFullyCompressed
          override val deadlineUncompressed: Deadline.Uncompressed = ValueLengthOneCompressed.DeadlineUncompressed
        }

        object ValueLengthOneCompressed extends ValueLengthOneCompressed {
          case object NoDeadline extends RangeKeyFullyCompressedEntryId(1868) with Deadline.NoDeadline with ValueLengthOneCompressed
          case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1869) with Deadline.OneCompressed with ValueLengthOneCompressed
          case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(1870) with Deadline.TwoCompressed with ValueLengthOneCompressed
          case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(1871) with Deadline.ThreeCompressed with ValueLengthOneCompressed
          case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(1872) with Deadline.FourCompressed with ValueLengthOneCompressed
          case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(1873) with Deadline.FiveCompressed with ValueLengthOneCompressed
          case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(1874) with Deadline.SixCompressed with ValueLengthOneCompressed
          case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(1875) with Deadline.SevenCompressed with ValueLengthOneCompressed
          case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(1876) with Deadline.FullyCompressed with ValueLengthOneCompressed
          case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(1877) with Deadline.Uncompressed with ValueLengthOneCompressed
        }

        sealed trait ValueLengthTwoCompressed extends ValueLength.TwoCompressed with ValueOffsetTwoCompressed {
          override val noDeadline: Deadline.NoDeadline = ValueLengthTwoCompressed.NoDeadline
          override val deadlineOneCompressed: Deadline.OneCompressed = ValueLengthTwoCompressed.DeadlineOneCompressed
          override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueLengthTwoCompressed.DeadlineTwoCompressed
          override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueLengthTwoCompressed.DeadlineThreeCompressed
          override val deadlineFourCompressed: Deadline.FourCompressed = ValueLengthTwoCompressed.DeadlineFourCompressed
          override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueLengthTwoCompressed.DeadlineFiveCompressed
          override val deadlineSixCompressed: Deadline.SixCompressed = ValueLengthTwoCompressed.DeadlineSixCompressed
          override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueLengthTwoCompressed.DeadlineSevenCompressed
          override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueLengthTwoCompressed.DeadlineFullyCompressed
          override val deadlineUncompressed: Deadline.Uncompressed = ValueLengthTwoCompressed.DeadlineUncompressed
        }

        object ValueLengthTwoCompressed extends ValueLengthTwoCompressed {
          case object NoDeadline extends RangeKeyFullyCompressedEntryId(1878) with Deadline.NoDeadline with ValueLengthTwoCompressed
          case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1879) with Deadline.OneCompressed with ValueLengthTwoCompressed
          case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(1880) with Deadline.TwoCompressed with ValueLengthTwoCompressed
          case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(1881) with Deadline.ThreeCompressed with ValueLengthTwoCompressed
          case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(1882) with Deadline.FourCompressed with ValueLengthTwoCompressed
          case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(1883) with Deadline.FiveCompressed with ValueLengthTwoCompressed
          case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(1884) with Deadline.SixCompressed with ValueLengthTwoCompressed
          case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(1885) with Deadline.SevenCompressed with ValueLengthTwoCompressed
          case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(1886) with Deadline.FullyCompressed with ValueLengthTwoCompressed
          case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(1887) with Deadline.Uncompressed with ValueLengthTwoCompressed
        }

        sealed trait ValueLengthThreeCompressed extends ValueLength.ThreeCompressed with ValueOffsetTwoCompressed {
          override val noDeadline: Deadline.NoDeadline = ValueLengthThreeCompressed.NoDeadline
          override val deadlineOneCompressed: Deadline.OneCompressed = ValueLengthThreeCompressed.DeadlineOneCompressed
          override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueLengthThreeCompressed.DeadlineTwoCompressed
          override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueLengthThreeCompressed.DeadlineThreeCompressed
          override val deadlineFourCompressed: Deadline.FourCompressed = ValueLengthThreeCompressed.DeadlineFourCompressed
          override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueLengthThreeCompressed.DeadlineFiveCompressed
          override val deadlineSixCompressed: Deadline.SixCompressed = ValueLengthThreeCompressed.DeadlineSixCompressed
          override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueLengthThreeCompressed.DeadlineSevenCompressed
          override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueLengthThreeCompressed.DeadlineFullyCompressed
          override val deadlineUncompressed: Deadline.Uncompressed = ValueLengthThreeCompressed.DeadlineUncompressed
        }

        object ValueLengthThreeCompressed extends ValueLengthThreeCompressed {
          case object NoDeadline extends RangeKeyFullyCompressedEntryId(1888) with Deadline.NoDeadline with ValueLengthThreeCompressed
          case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1889) with Deadline.OneCompressed with ValueLengthThreeCompressed
          case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(1890) with Deadline.TwoCompressed with ValueLengthThreeCompressed
          case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(1891) with Deadline.ThreeCompressed with ValueLengthThreeCompressed
          case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(1892) with Deadline.FourCompressed with ValueLengthThreeCompressed
          case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(1893) with Deadline.FiveCompressed with ValueLengthThreeCompressed
          case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(1894) with Deadline.SixCompressed with ValueLengthThreeCompressed
          case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(1895) with Deadline.SevenCompressed with ValueLengthThreeCompressed
          case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(1896) with Deadline.FullyCompressed with ValueLengthThreeCompressed
          case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(1897) with Deadline.Uncompressed with ValueLengthThreeCompressed
        }

        sealed trait ValueLengthFullyCompressed extends ValueLength.FullyCompressed with ValueOffsetTwoCompressed {
          override val noDeadline: Deadline.NoDeadline = ValueLengthFullyCompressed.NoDeadline
          override val deadlineOneCompressed: Deadline.OneCompressed = ValueLengthFullyCompressed.DeadlineOneCompressed
          override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueLengthFullyCompressed.DeadlineTwoCompressed
          override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueLengthFullyCompressed.DeadlineThreeCompressed
          override val deadlineFourCompressed: Deadline.FourCompressed = ValueLengthFullyCompressed.DeadlineFourCompressed
          override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueLengthFullyCompressed.DeadlineFiveCompressed
          override val deadlineSixCompressed: Deadline.SixCompressed = ValueLengthFullyCompressed.DeadlineSixCompressed
          override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueLengthFullyCompressed.DeadlineSevenCompressed
          override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueLengthFullyCompressed.DeadlineFullyCompressed
          override val deadlineUncompressed: Deadline.Uncompressed = ValueLengthFullyCompressed.DeadlineUncompressed
        }

        object ValueLengthFullyCompressed extends ValueLengthFullyCompressed {
          case object NoDeadline extends RangeKeyFullyCompressedEntryId(1898) with Deadline.NoDeadline with ValueLengthFullyCompressed
          case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1899) with Deadline.OneCompressed with ValueLengthFullyCompressed
          case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(1900) with Deadline.TwoCompressed with ValueLengthFullyCompressed
          case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(1901) with Deadline.ThreeCompressed with ValueLengthFullyCompressed
          case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(1902) with Deadline.FourCompressed with ValueLengthFullyCompressed
          case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(1903) with Deadline.FiveCompressed with ValueLengthFullyCompressed
          case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(1904) with Deadline.SixCompressed with ValueLengthFullyCompressed
          case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(1905) with Deadline.SevenCompressed with ValueLengthFullyCompressed
          case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(1906) with Deadline.FullyCompressed with ValueLengthFullyCompressed
          case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(1907) with Deadline.Uncompressed with ValueLengthFullyCompressed
        }

        sealed trait ValueLengthUncompressed extends ValueLength.Uncompressed with ValueOffsetTwoCompressed {
          override val noDeadline: Deadline.NoDeadline = ValueLengthUncompressed.NoDeadline
          override val deadlineOneCompressed: Deadline.OneCompressed = ValueLengthUncompressed.DeadlineOneCompressed
          override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueLengthUncompressed.DeadlineTwoCompressed
          override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueLengthUncompressed.DeadlineThreeCompressed
          override val deadlineFourCompressed: Deadline.FourCompressed = ValueLengthUncompressed.DeadlineFourCompressed
          override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueLengthUncompressed.DeadlineFiveCompressed
          override val deadlineSixCompressed: Deadline.SixCompressed = ValueLengthUncompressed.DeadlineSixCompressed
          override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueLengthUncompressed.DeadlineSevenCompressed
          override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueLengthUncompressed.DeadlineFullyCompressed
          override val deadlineUncompressed: Deadline.Uncompressed = ValueLengthUncompressed.DeadlineUncompressed
        }

        object ValueLengthUncompressed extends ValueLengthUncompressed {
          case object NoDeadline extends RangeKeyFullyCompressedEntryId(1908) with Deadline.NoDeadline with ValueLengthUncompressed
          case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1909) with Deadline.OneCompressed with ValueLengthUncompressed
          case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(1910) with Deadline.TwoCompressed with ValueLengthUncompressed
          case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(1911) with Deadline.ThreeCompressed with ValueLengthUncompressed
          case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(1912) with Deadline.FourCompressed with ValueLengthUncompressed
          case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(1913) with Deadline.FiveCompressed with ValueLengthUncompressed
          case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(1914) with Deadline.SixCompressed with ValueLengthUncompressed
          case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(1915) with Deadline.SevenCompressed with ValueLengthUncompressed
          case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(1916) with Deadline.FullyCompressed with ValueLengthUncompressed
          case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(1917) with Deadline.Uncompressed with ValueLengthUncompressed
        }
      }

      sealed trait ValueOffsetThreeCompressed extends ValueOffset.ThreeCompressed with ValueUncompressed {
        override val valueLengthOneCompressed: ValueLength.OneCompressed = ValueOffsetThreeCompressed.ValueLengthOneCompressed
        override val valueLengthTwoCompressed: ValueLength.TwoCompressed = ValueOffsetThreeCompressed.ValueLengthTwoCompressed
        override val valueLengthThreeCompressed: ValueLength.ThreeCompressed = ValueOffsetThreeCompressed.ValueLengthThreeCompressed
        override val valueLengthFullyCompressed: ValueLength.FullyCompressed = ValueOffsetThreeCompressed.ValueLengthFullyCompressed
        override val valueLengthUncompressed: ValueLength.Uncompressed = ValueOffsetThreeCompressed.ValueLengthUncompressed
      }
      object ValueOffsetThreeCompressed extends ValueOffsetThreeCompressed {
        sealed trait ValueLengthOneCompressed extends ValueLength.OneCompressed with ValueOffsetThreeCompressed {
          override val noDeadline: Deadline.NoDeadline = ValueLengthOneCompressed.NoDeadline
          override val deadlineOneCompressed: Deadline.OneCompressed = ValueLengthOneCompressed.DeadlineOneCompressed
          override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueLengthOneCompressed.DeadlineTwoCompressed
          override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueLengthOneCompressed.DeadlineThreeCompressed
          override val deadlineFourCompressed: Deadline.FourCompressed = ValueLengthOneCompressed.DeadlineFourCompressed
          override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueLengthOneCompressed.DeadlineFiveCompressed
          override val deadlineSixCompressed: Deadline.SixCompressed = ValueLengthOneCompressed.DeadlineSixCompressed
          override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueLengthOneCompressed.DeadlineSevenCompressed
          override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueLengthOneCompressed.DeadlineFullyCompressed
          override val deadlineUncompressed: Deadline.Uncompressed = ValueLengthOneCompressed.DeadlineUncompressed
        }

        object ValueLengthOneCompressed extends ValueLengthOneCompressed {
          case object NoDeadline extends RangeKeyFullyCompressedEntryId(1918) with Deadline.NoDeadline with ValueLengthOneCompressed
          case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1919) with Deadline.OneCompressed with ValueLengthOneCompressed
          case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(1920) with Deadline.TwoCompressed with ValueLengthOneCompressed
          case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(1921) with Deadline.ThreeCompressed with ValueLengthOneCompressed
          case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(1922) with Deadline.FourCompressed with ValueLengthOneCompressed
          case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(1923) with Deadline.FiveCompressed with ValueLengthOneCompressed
          case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(1924) with Deadline.SixCompressed with ValueLengthOneCompressed
          case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(1925) with Deadline.SevenCompressed with ValueLengthOneCompressed
          case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(1926) with Deadline.FullyCompressed with ValueLengthOneCompressed
          case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(1927) with Deadline.Uncompressed with ValueLengthOneCompressed
        }

        sealed trait ValueLengthTwoCompressed extends ValueLength.TwoCompressed with ValueOffsetThreeCompressed {
          override val noDeadline: Deadline.NoDeadline = ValueLengthTwoCompressed.NoDeadline
          override val deadlineOneCompressed: Deadline.OneCompressed = ValueLengthTwoCompressed.DeadlineOneCompressed
          override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueLengthTwoCompressed.DeadlineTwoCompressed
          override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueLengthTwoCompressed.DeadlineThreeCompressed
          override val deadlineFourCompressed: Deadline.FourCompressed = ValueLengthTwoCompressed.DeadlineFourCompressed
          override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueLengthTwoCompressed.DeadlineFiveCompressed
          override val deadlineSixCompressed: Deadline.SixCompressed = ValueLengthTwoCompressed.DeadlineSixCompressed
          override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueLengthTwoCompressed.DeadlineSevenCompressed
          override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueLengthTwoCompressed.DeadlineFullyCompressed
          override val deadlineUncompressed: Deadline.Uncompressed = ValueLengthTwoCompressed.DeadlineUncompressed
        }

        object ValueLengthTwoCompressed extends ValueLengthTwoCompressed {
          case object NoDeadline extends RangeKeyFullyCompressedEntryId(1928) with Deadline.NoDeadline with ValueLengthTwoCompressed
          case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1929) with Deadline.OneCompressed with ValueLengthTwoCompressed
          case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(1930) with Deadline.TwoCompressed with ValueLengthTwoCompressed
          case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(1931) with Deadline.ThreeCompressed with ValueLengthTwoCompressed
          case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(1932) with Deadline.FourCompressed with ValueLengthTwoCompressed
          case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(1933) with Deadline.FiveCompressed with ValueLengthTwoCompressed
          case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(1934) with Deadline.SixCompressed with ValueLengthTwoCompressed
          case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(1935) with Deadline.SevenCompressed with ValueLengthTwoCompressed
          case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(1936) with Deadline.FullyCompressed with ValueLengthTwoCompressed
          case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(1937) with Deadline.Uncompressed with ValueLengthTwoCompressed
        }

        sealed trait ValueLengthThreeCompressed extends ValueLength.ThreeCompressed with ValueOffsetThreeCompressed {
          override val noDeadline: Deadline.NoDeadline = ValueLengthThreeCompressed.NoDeadline
          override val deadlineOneCompressed: Deadline.OneCompressed = ValueLengthThreeCompressed.DeadlineOneCompressed
          override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueLengthThreeCompressed.DeadlineTwoCompressed
          override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueLengthThreeCompressed.DeadlineThreeCompressed
          override val deadlineFourCompressed: Deadline.FourCompressed = ValueLengthThreeCompressed.DeadlineFourCompressed
          override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueLengthThreeCompressed.DeadlineFiveCompressed
          override val deadlineSixCompressed: Deadline.SixCompressed = ValueLengthThreeCompressed.DeadlineSixCompressed
          override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueLengthThreeCompressed.DeadlineSevenCompressed
          override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueLengthThreeCompressed.DeadlineFullyCompressed
          override val deadlineUncompressed: Deadline.Uncompressed = ValueLengthThreeCompressed.DeadlineUncompressed
        }

        object ValueLengthThreeCompressed extends ValueLengthThreeCompressed {
          case object NoDeadline extends RangeKeyFullyCompressedEntryId(1938) with Deadline.NoDeadline with ValueLengthThreeCompressed
          case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1939) with Deadline.OneCompressed with ValueLengthThreeCompressed
          case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(1940) with Deadline.TwoCompressed with ValueLengthThreeCompressed
          case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(1941) with Deadline.ThreeCompressed with ValueLengthThreeCompressed
          case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(1942) with Deadline.FourCompressed with ValueLengthThreeCompressed
          case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(1943) with Deadline.FiveCompressed with ValueLengthThreeCompressed
          case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(1944) with Deadline.SixCompressed with ValueLengthThreeCompressed
          case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(1945) with Deadline.SevenCompressed with ValueLengthThreeCompressed
          case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(1946) with Deadline.FullyCompressed with ValueLengthThreeCompressed
          case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(1947) with Deadline.Uncompressed with ValueLengthThreeCompressed
        }

        sealed trait ValueLengthFullyCompressed extends ValueLength.FullyCompressed with ValueOffsetThreeCompressed {
          override val noDeadline: Deadline.NoDeadline = ValueLengthFullyCompressed.NoDeadline
          override val deadlineOneCompressed: Deadline.OneCompressed = ValueLengthFullyCompressed.DeadlineOneCompressed
          override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueLengthFullyCompressed.DeadlineTwoCompressed
          override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueLengthFullyCompressed.DeadlineThreeCompressed
          override val deadlineFourCompressed: Deadline.FourCompressed = ValueLengthFullyCompressed.DeadlineFourCompressed
          override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueLengthFullyCompressed.DeadlineFiveCompressed
          override val deadlineSixCompressed: Deadline.SixCompressed = ValueLengthFullyCompressed.DeadlineSixCompressed
          override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueLengthFullyCompressed.DeadlineSevenCompressed
          override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueLengthFullyCompressed.DeadlineFullyCompressed
          override val deadlineUncompressed: Deadline.Uncompressed = ValueLengthFullyCompressed.DeadlineUncompressed
        }

        object ValueLengthFullyCompressed extends ValueLengthFullyCompressed {
          case object NoDeadline extends RangeKeyFullyCompressedEntryId(1948) with Deadline.NoDeadline with ValueLengthFullyCompressed
          case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1949) with Deadline.OneCompressed with ValueLengthFullyCompressed
          case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(1950) with Deadline.TwoCompressed with ValueLengthFullyCompressed
          case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(1951) with Deadline.ThreeCompressed with ValueLengthFullyCompressed
          case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(1952) with Deadline.FourCompressed with ValueLengthFullyCompressed
          case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(1953) with Deadline.FiveCompressed with ValueLengthFullyCompressed
          case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(1954) with Deadline.SixCompressed with ValueLengthFullyCompressed
          case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(1955) with Deadline.SevenCompressed with ValueLengthFullyCompressed
          case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(1956) with Deadline.FullyCompressed with ValueLengthFullyCompressed
          case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(1957) with Deadline.Uncompressed with ValueLengthFullyCompressed
        }

        sealed trait ValueLengthUncompressed extends ValueLength.Uncompressed with ValueOffsetThreeCompressed {
          override val noDeadline: Deadline.NoDeadline = ValueLengthUncompressed.NoDeadline
          override val deadlineOneCompressed: Deadline.OneCompressed = ValueLengthUncompressed.DeadlineOneCompressed
          override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueLengthUncompressed.DeadlineTwoCompressed
          override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueLengthUncompressed.DeadlineThreeCompressed
          override val deadlineFourCompressed: Deadline.FourCompressed = ValueLengthUncompressed.DeadlineFourCompressed
          override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueLengthUncompressed.DeadlineFiveCompressed
          override val deadlineSixCompressed: Deadline.SixCompressed = ValueLengthUncompressed.DeadlineSixCompressed
          override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueLengthUncompressed.DeadlineSevenCompressed
          override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueLengthUncompressed.DeadlineFullyCompressed
          override val deadlineUncompressed: Deadline.Uncompressed = ValueLengthUncompressed.DeadlineUncompressed
        }

        object ValueLengthUncompressed extends ValueLengthUncompressed {
          case object NoDeadline extends RangeKeyFullyCompressedEntryId(1958) with Deadline.NoDeadline with ValueLengthUncompressed
          case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1959) with Deadline.OneCompressed with ValueLengthUncompressed
          case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(1960) with Deadline.TwoCompressed with ValueLengthUncompressed
          case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(1961) with Deadline.ThreeCompressed with ValueLengthUncompressed
          case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(1962) with Deadline.FourCompressed with ValueLengthUncompressed
          case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(1963) with Deadline.FiveCompressed with ValueLengthUncompressed
          case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(1964) with Deadline.SixCompressed with ValueLengthUncompressed
          case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(1965) with Deadline.SevenCompressed with ValueLengthUncompressed
          case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(1966) with Deadline.FullyCompressed with ValueLengthUncompressed
          case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(1967) with Deadline.Uncompressed with ValueLengthUncompressed
        }
      }

      sealed trait ValueOffsetUncompressed extends ValueOffset.Uncompressed with ValueUncompressed {
        override val valueLengthOneCompressed: ValueLength.OneCompressed = ValueOffsetUncompressed.ValueLengthOneCompressed
        override val valueLengthTwoCompressed: ValueLength.TwoCompressed = ValueOffsetUncompressed.ValueLengthTwoCompressed
        override val valueLengthThreeCompressed: ValueLength.ThreeCompressed = ValueOffsetUncompressed.ValueLengthThreeCompressed
        override val valueLengthFullyCompressed: ValueLength.FullyCompressed = ValueOffsetUncompressed.ValueLengthFullyCompressed
        override val valueLengthUncompressed: ValueLength.Uncompressed = ValueOffsetUncompressed.ValueLengthUncompressed
      }
      object ValueOffsetUncompressed extends ValueOffsetUncompressed {
        sealed trait ValueLengthOneCompressed extends ValueLength.OneCompressed with ValueOffsetUncompressed {
          override val noDeadline: Deadline.NoDeadline = ValueLengthOneCompressed.NoDeadline
          override val deadlineOneCompressed: Deadline.OneCompressed = ValueLengthOneCompressed.DeadlineOneCompressed
          override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueLengthOneCompressed.DeadlineTwoCompressed
          override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueLengthOneCompressed.DeadlineThreeCompressed
          override val deadlineFourCompressed: Deadline.FourCompressed = ValueLengthOneCompressed.DeadlineFourCompressed
          override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueLengthOneCompressed.DeadlineFiveCompressed
          override val deadlineSixCompressed: Deadline.SixCompressed = ValueLengthOneCompressed.DeadlineSixCompressed
          override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueLengthOneCompressed.DeadlineSevenCompressed
          override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueLengthOneCompressed.DeadlineFullyCompressed
          override val deadlineUncompressed: Deadline.Uncompressed = ValueLengthOneCompressed.DeadlineUncompressed
        }

        object ValueLengthOneCompressed extends ValueLengthOneCompressed {
          case object NoDeadline extends RangeKeyFullyCompressedEntryId(1968) with Deadline.NoDeadline with ValueLengthOneCompressed
          case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1969) with Deadline.OneCompressed with ValueLengthOneCompressed
          case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(1970) with Deadline.TwoCompressed with ValueLengthOneCompressed
          case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(1971) with Deadline.ThreeCompressed with ValueLengthOneCompressed
          case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(1972) with Deadline.FourCompressed with ValueLengthOneCompressed
          case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(1973) with Deadline.FiveCompressed with ValueLengthOneCompressed
          case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(1974) with Deadline.SixCompressed with ValueLengthOneCompressed
          case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(1975) with Deadline.SevenCompressed with ValueLengthOneCompressed
          case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(1976) with Deadline.FullyCompressed with ValueLengthOneCompressed
          case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(1977) with Deadline.Uncompressed with ValueLengthOneCompressed
        }

        sealed trait ValueLengthTwoCompressed extends ValueLength.TwoCompressed with ValueOffsetUncompressed {
          override val noDeadline: Deadline.NoDeadline = ValueLengthTwoCompressed.NoDeadline
          override val deadlineOneCompressed: Deadline.OneCompressed = ValueLengthTwoCompressed.DeadlineOneCompressed
          override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueLengthTwoCompressed.DeadlineTwoCompressed
          override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueLengthTwoCompressed.DeadlineThreeCompressed
          override val deadlineFourCompressed: Deadline.FourCompressed = ValueLengthTwoCompressed.DeadlineFourCompressed
          override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueLengthTwoCompressed.DeadlineFiveCompressed
          override val deadlineSixCompressed: Deadline.SixCompressed = ValueLengthTwoCompressed.DeadlineSixCompressed
          override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueLengthTwoCompressed.DeadlineSevenCompressed
          override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueLengthTwoCompressed.DeadlineFullyCompressed
          override val deadlineUncompressed: Deadline.Uncompressed = ValueLengthTwoCompressed.DeadlineUncompressed
        }

        object ValueLengthTwoCompressed extends ValueLengthTwoCompressed {
          case object NoDeadline extends RangeKeyFullyCompressedEntryId(1978) with Deadline.NoDeadline with ValueLengthTwoCompressed
          case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1979) with Deadline.OneCompressed with ValueLengthTwoCompressed
          case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(1980) with Deadline.TwoCompressed with ValueLengthTwoCompressed
          case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(1981) with Deadline.ThreeCompressed with ValueLengthTwoCompressed
          case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(1982) with Deadline.FourCompressed with ValueLengthTwoCompressed
          case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(1983) with Deadline.FiveCompressed with ValueLengthTwoCompressed
          case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(1984) with Deadline.SixCompressed with ValueLengthTwoCompressed
          case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(1985) with Deadline.SevenCompressed with ValueLengthTwoCompressed
          case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(1986) with Deadline.FullyCompressed with ValueLengthTwoCompressed
          case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(1987) with Deadline.Uncompressed with ValueLengthTwoCompressed
        }

        sealed trait ValueLengthThreeCompressed extends ValueLength.ThreeCompressed with ValueOffsetUncompressed {
          override val noDeadline: Deadline.NoDeadline = ValueLengthThreeCompressed.NoDeadline
          override val deadlineOneCompressed: Deadline.OneCompressed = ValueLengthThreeCompressed.DeadlineOneCompressed
          override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueLengthThreeCompressed.DeadlineTwoCompressed
          override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueLengthThreeCompressed.DeadlineThreeCompressed
          override val deadlineFourCompressed: Deadline.FourCompressed = ValueLengthThreeCompressed.DeadlineFourCompressed
          override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueLengthThreeCompressed.DeadlineFiveCompressed
          override val deadlineSixCompressed: Deadline.SixCompressed = ValueLengthThreeCompressed.DeadlineSixCompressed
          override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueLengthThreeCompressed.DeadlineSevenCompressed
          override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueLengthThreeCompressed.DeadlineFullyCompressed
          override val deadlineUncompressed: Deadline.Uncompressed = ValueLengthThreeCompressed.DeadlineUncompressed
        }

        object ValueLengthThreeCompressed extends ValueLengthThreeCompressed {
          case object NoDeadline extends RangeKeyFullyCompressedEntryId(1988) with Deadline.NoDeadline with ValueLengthThreeCompressed
          case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1989) with Deadline.OneCompressed with ValueLengthThreeCompressed
          case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(1990) with Deadline.TwoCompressed with ValueLengthThreeCompressed
          case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(1991) with Deadline.ThreeCompressed with ValueLengthThreeCompressed
          case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(1992) with Deadline.FourCompressed with ValueLengthThreeCompressed
          case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(1993) with Deadline.FiveCompressed with ValueLengthThreeCompressed
          case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(1994) with Deadline.SixCompressed with ValueLengthThreeCompressed
          case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(1995) with Deadline.SevenCompressed with ValueLengthThreeCompressed
          case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(1996) with Deadline.FullyCompressed with ValueLengthThreeCompressed
          case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(1997) with Deadline.Uncompressed with ValueLengthThreeCompressed
        }

        sealed trait ValueLengthFullyCompressed extends ValueLength.FullyCompressed with ValueOffsetUncompressed {
          override val noDeadline: Deadline.NoDeadline = ValueLengthFullyCompressed.NoDeadline
          override val deadlineOneCompressed: Deadline.OneCompressed = ValueLengthFullyCompressed.DeadlineOneCompressed
          override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueLengthFullyCompressed.DeadlineTwoCompressed
          override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueLengthFullyCompressed.DeadlineThreeCompressed
          override val deadlineFourCompressed: Deadline.FourCompressed = ValueLengthFullyCompressed.DeadlineFourCompressed
          override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueLengthFullyCompressed.DeadlineFiveCompressed
          override val deadlineSixCompressed: Deadline.SixCompressed = ValueLengthFullyCompressed.DeadlineSixCompressed
          override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueLengthFullyCompressed.DeadlineSevenCompressed
          override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueLengthFullyCompressed.DeadlineFullyCompressed
          override val deadlineUncompressed: Deadline.Uncompressed = ValueLengthFullyCompressed.DeadlineUncompressed
        }

        object ValueLengthFullyCompressed extends ValueLengthFullyCompressed {
          case object NoDeadline extends RangeKeyFullyCompressedEntryId(1998) with Deadline.NoDeadline with ValueLengthFullyCompressed
          case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(1999) with Deadline.OneCompressed with ValueLengthFullyCompressed
          case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(2000) with Deadline.TwoCompressed with ValueLengthFullyCompressed
          case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(2001) with Deadline.ThreeCompressed with ValueLengthFullyCompressed
          case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(2002) with Deadline.FourCompressed with ValueLengthFullyCompressed
          case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(2003) with Deadline.FiveCompressed with ValueLengthFullyCompressed
          case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(2004) with Deadline.SixCompressed with ValueLengthFullyCompressed
          case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(2005) with Deadline.SevenCompressed with ValueLengthFullyCompressed
          case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(2006) with Deadline.FullyCompressed with ValueLengthFullyCompressed
          case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(2007) with Deadline.Uncompressed with ValueLengthFullyCompressed
        }

        sealed trait ValueLengthUncompressed extends ValueLength.Uncompressed with ValueOffsetUncompressed {
          override val noDeadline: Deadline.NoDeadline = ValueLengthUncompressed.NoDeadline
          override val deadlineOneCompressed: Deadline.OneCompressed = ValueLengthUncompressed.DeadlineOneCompressed
          override val deadlineTwoCompressed: Deadline.TwoCompressed = ValueLengthUncompressed.DeadlineTwoCompressed
          override val deadlineThreeCompressed: Deadline.ThreeCompressed = ValueLengthUncompressed.DeadlineThreeCompressed
          override val deadlineFourCompressed: Deadline.FourCompressed = ValueLengthUncompressed.DeadlineFourCompressed
          override val deadlineFiveCompressed: Deadline.FiveCompressed = ValueLengthUncompressed.DeadlineFiveCompressed
          override val deadlineSixCompressed: Deadline.SixCompressed = ValueLengthUncompressed.DeadlineSixCompressed
          override val deadlineSevenCompressed: Deadline.SevenCompressed = ValueLengthUncompressed.DeadlineSevenCompressed
          override val deadlineFullyCompressed: Deadline.FullyCompressed = ValueLengthUncompressed.DeadlineFullyCompressed
          override val deadlineUncompressed: Deadline.Uncompressed = ValueLengthUncompressed.DeadlineUncompressed
        }

        object ValueLengthUncompressed extends ValueLengthUncompressed {
          case object NoDeadline extends RangeKeyFullyCompressedEntryId(2008) with Deadline.NoDeadline with ValueLengthUncompressed
          case object DeadlineOneCompressed extends RangeKeyFullyCompressedEntryId(2009) with Deadline.OneCompressed with ValueLengthUncompressed
          case object DeadlineTwoCompressed extends RangeKeyFullyCompressedEntryId(2010) with Deadline.TwoCompressed with ValueLengthUncompressed
          case object DeadlineThreeCompressed extends RangeKeyFullyCompressedEntryId(2011) with Deadline.ThreeCompressed with ValueLengthUncompressed
          case object DeadlineFourCompressed extends RangeKeyFullyCompressedEntryId(2012) with Deadline.FourCompressed with ValueLengthUncompressed
          case object DeadlineFiveCompressed extends RangeKeyFullyCompressedEntryId(2013) with Deadline.FiveCompressed with ValueLengthUncompressed
          case object DeadlineSixCompressed extends RangeKeyFullyCompressedEntryId(2014) with Deadline.SixCompressed with ValueLengthUncompressed
          case object DeadlineSevenCompressed extends RangeKeyFullyCompressedEntryId(2015) with Deadline.SevenCompressed with ValueLengthUncompressed
          case object DeadlineFullyCompressed extends RangeKeyFullyCompressedEntryId(2016) with Deadline.FullyCompressed with ValueLengthUncompressed
          case object DeadlineUncompressed extends RangeKeyFullyCompressedEntryId(2017) with Deadline.Uncompressed with ValueLengthUncompressed
        }
      }
    }
  }
}