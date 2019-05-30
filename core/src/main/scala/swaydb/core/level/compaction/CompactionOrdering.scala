package swaydb.core.level.compaction

import swaydb.core.level.zero.LevelZero
import swaydb.core.level.{Level, LevelRef, TrashLevel}

object CompactionOrdering {

  def ordering(zero: LevelZero,
               compactionState: LevelRef => CompactionState) =
    new Ordering[LevelRef] {
      override def compare(left: LevelRef, right: LevelRef): Int = {
        (left, right) match {
          //Level
          case (left: Level, right: Level) => order(left, right, compactionState(left), compactionState(right))
          case (left: Level, right: LevelZero) => order(right, left, compactionState(left), compactionState(right)) * -1
          case (_: Level, TrashLevel) => 1
          //LevelZero
          case (left: LevelZero, right: Level) => order(left, right, compactionState(left), compactionState(right))
          case (_: LevelZero, _: LevelZero) => 0
          case (_: LevelZero, TrashLevel) => 1
          //LevelZero
          case (TrashLevel, _: Level) => -1
          case (TrashLevel, _: LevelZero) => -1
          case (TrashLevel, TrashLevel) => 0
        }
      }
    }

  def order(left: LevelZero,
            right: Level,
            leftState: CompactionState,
            rightState: CompactionState): Int =
    if (left.level0Meter.mapsCount >= 4)
      1
    else
      -1

  def order(left: Level,
            right: Level,
            leftState: CompactionState,
            rightState: CompactionState): Int =
    if (right.nextLevel.isEmpty) //last Level is always the lowest priority.
      1
    else
      left.throttle(left.meter).pushDelay compareTo right.throttle(right.meter).pushDelay
}
