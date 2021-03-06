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

package swaydb.core.level.zero

import java.nio.channels.{FileChannel, FileLock}
import java.nio.file.{Path, Paths, StandardOpenOption}

import com.typesafe.scalalogging.LazyLogging
import swaydb.Error.Level.ExceptionHandler
import swaydb.core.actor.FileSweeper
import swaydb.core.data.KeyValue.{Put, PutOption}
import swaydb.core.data.Value.FromValue
import swaydb.core.data._
import swaydb.core.function.FunctionStore
import swaydb.core.io.file.Effect
import swaydb.core.level.seek._
import swaydb.core.level.{LevelRef, LevelSeek, NextLevel}
import swaydb.core.map
import swaydb.core.map.serializer.{TimerMapEntryReader, TimerMapEntryWriter}
import swaydb.core.map.timer.Timer
import swaydb.core.map.{MapEntry, Maps, SkipListMerger}
import swaydb.core.segment.format.a.entry.reader.PersistentReader
import swaydb.core.segment.{Segment, SegmentOption, ThreadReadState}
import swaydb.core.util.{MinMax, Options}
import swaydb.data.accelerate.{Accelerator, LevelZeroMeter}
import swaydb.data.compaction.LevelMeter
import swaydb.data.order.{KeyOrder, TimeOrder}
import swaydb.data.slice.{Slice, SliceOption}
import swaydb.data.storage.Level0Storage
import swaydb.data.util.StorageUnits._
import swaydb.{Bag, IO, OK}

import scala.concurrent.duration.{Deadline, _}
import scala.jdk.CollectionConverters._

private[core] object LevelZero extends LazyLogging {

  def apply(mapSize: Long,
            storage: Level0Storage,
            enableTimer: Boolean,
            cacheKeyValueIds: Boolean,
            nextLevel: Option[NextLevel],
            acceleration: LevelZeroMeter => Accelerator,
            throttle: LevelZeroMeter => FiniteDuration)(implicit keyOrder: KeyOrder[Slice[Byte]],
                                                        timeOrder: TimeOrder[Slice[Byte]],
                                                        functionStore: FunctionStore): IO[swaydb.Error.Level, LevelZero] = {
    import swaydb.core.map.serializer.LevelZeroMapEntryReader.Level0Reader
    import swaydb.core.map.serializer.LevelZeroMapEntryWriter._

    if (cacheKeyValueIds)
      PersistentReader.populateBaseEntryIds()
    else
      logger.info("cacheKeyValueIds is false. Key-value IDs cache disabled!")

    //LevelZero does not required FileSweeper since they are all Map files.
    implicit val fileSweeper: FileSweeper = FileSweeper.Disabled

    implicit val skipListMerger: SkipListMerger[SliceOption[Byte], MemoryOption, Slice[Byte], Memory] = LevelZeroSkipListMerger
    val mapsAndPathAndLock =
      storage match {
        case Level0Storage.Persistent(mmap, databaseDirectory, recovery) =>
          val timer =
            if (enableTimer) {
              val timerDir = databaseDirectory.resolve("0").resolve("timer")
              Effect createDirectoriesIfAbsent timerDir
              Timer.persistent(
                path = timerDir,
                mmap = mmap,
                mod = 100000,
                flushCheckpointSize = 1.mb
              )(keyOrder = KeyOrder.default,
                timeOrder = timeOrder,
                functionStore = functionStore,
                writer = TimerMapEntryWriter.TimerPutMapEntryWriter,
                reader = TimerMapEntryReader.TimerPutMapEntryReader)
            } else {
              IO.Right(Timer.empty)
            }

          timer flatMap {
            implicit timer =>
              val path = databaseDirectory.resolve("0")
              logger.info("{}: Acquiring lock.", path)
              val lockFile = path.resolve("LOCK")
              Effect createDirectoriesIfAbsent path
              Effect createFileIfAbsent lockFile
              IO(FileChannel.open(lockFile, StandardOpenOption.WRITE).tryLock()) flatMap {
                lock =>
                  logger.info("{}: Recovering Maps.", path)
                  Maps.persistent[SliceOption[Byte], MemoryOption, Slice[Byte], Memory](
                    path = path,
                    mmap = mmap,
                    fileSize = mapSize,
                    acceleration = acceleration,
                    recovery = recovery,
                    nullKey = Slice.Null,
                    nullValue = Memory.Null
                  ) map {
                    maps =>
                      (maps, path, Some(lock))
                  }
              }
          }

        case Level0Storage.Memory =>
          val timer =
            if (enableTimer)
              LevelRef.firstPersistentPath(nextLevel) match {
                case Some(persistentPath) =>
                  val timerDir = persistentPath.getParent.resolve("0").resolve("timer")
                  Effect createDirectoriesIfAbsent timerDir
                  Timer.persistent(
                    path = timerDir,
                    mmap = LevelRef.hasMMAP(nextLevel),
                    mod = 100000,
                    flushCheckpointSize = 1.mb
                  )(keyOrder = KeyOrder.default,
                    timeOrder = timeOrder,
                    functionStore = functionStore,
                    writer = TimerMapEntryWriter.TimerPutMapEntryWriter,
                    reader = TimerMapEntryReader.TimerPutMapEntryReader)

                case None =>
                  IO.Right(Timer.memory())
              }
            else
              IO.Right(Timer.empty)

          timer map {
            implicit timer =>
              val map =
                Maps.memory[SliceOption[Byte], MemoryOption, Slice[Byte], Memory](
                  fileSize = mapSize,
                  acceleration = acceleration,
                  nullKey = Slice.Null,
                  nullValue = Memory.Null
                )

              (map, Paths.get("MEMORY_DB").resolve(0.toString), None)
          }
      }

    mapsAndPathAndLock map {
      case (maps, path, lock: Option[FileLock]) =>
        new LevelZero(
          path = path,
          mapSize = mapSize,
          maps = maps,
          nextLevel = nextLevel,
          inMemory = storage.memory,
          throttle = throttle,
          lock = lock
        )
    }
  }

  def delete(zero: LevelZero): IO[swaydb.Error.Delete, Unit] =
    zero
      .close
      .flatMap {
        _ =>
          zero
            .nextLevel
            .map(_.delete)
            .getOrElse(IO[swaydb.Error.Delete, Unit](Effect.walkDelete(zero.path.getParent)))
      }
}

private[swaydb] case class LevelZero(path: Path,
                                     mapSize: Long,
                                     maps: Maps[SliceOption[Byte], MemoryOption, Slice[Byte], Memory],
                                     nextLevel: Option[NextLevel],
                                     inMemory: Boolean,
                                     throttle: LevelZeroMeter => FiniteDuration,
                                     private val lock: Option[FileLock])(implicit keyOrder: KeyOrder[Slice[Byte]],
                                                                         timeOrder: TimeOrder[Slice[Byte]],
                                                                         functionStore: FunctionStore) extends LevelRef with LazyLogging {

  logger.info("{}: Level0 started.", path)

  import keyOrder._
  import swaydb.core.map.serializer.LevelZeroMapEntryWriter._

  val levelZeroMeter: LevelZeroMeter =
    maps.meter

  def onNextMapCallback(event: () => Unit): Unit =
    maps onNextMapCallback event

  def releaseLocks: IO[swaydb.Error.Close, Unit] =
    IO[swaydb.Error.Close, Unit](Effect.release(lock)) flatMap {
      _ =>
        nextLevel.map(_.releaseLocks) getOrElse IO.unit
    }

  def validateInput(key: Slice[Byte]): Unit =
    if (key.isEmpty) {
      throw new IllegalArgumentException("key cannot be empty.")
    }

  def validateInput(fromKey: Slice[Byte], toKey: Slice[Byte]): Unit =
    if (fromKey.isEmpty)
      throw new IllegalArgumentException("fromKey cannot be empty.")
    else if (toKey.isEmpty)
      throw new IllegalArgumentException("toKey cannot be empty.")
    else if (fromKey > toKey) //fromKey cannot also be equal to toKey. The invoking this assert should also check for equality and call update on single key-value.
    throw new IllegalArgumentException("fromKey should be less than toKey.")

  def put(key: Slice[Byte]): OK = {
    validateInput(key)
    maps.write(timer => MapEntry.Put[Slice[Byte], Memory.Put](key, Memory.Put(key, Slice.Null, None, timer.next)))
    OK.instance
  }

  def put(key: Slice[Byte], value: Slice[Byte]): OK = {
    validateInput(key)
    maps.write(timer => MapEntry.Put[Slice[Byte], Memory.Put](key, Memory.Put(key, value, None, timer.next)))
    OK.instance
  }

  def put(key: Slice[Byte], value: SliceOption[Byte], removeAt: Deadline): OK = {
    validateInput(key)
    maps.write(timer => MapEntry.Put[Slice[Byte], Memory.Put](key, Memory.Put(key, value, Some(removeAt), timer.next)))
    OK.instance
  }

  def put(key: Slice[Byte], value: SliceOption[Byte]): OK = {
    validateInput(key)
    maps.write(timer => MapEntry.Put[Slice[Byte], Memory.Put](key, Memory.Put(key, value, None, timer.next)))
    OK.instance
  }

  def put(entry: Timer => MapEntry[Slice[Byte], Memory]): OK = {
    maps write entry
    OK.instance
  }

  def remove(key: Slice[Byte]): OK = {
    validateInput(key)
    maps.write(timer => MapEntry.Put[Slice[Byte], Memory.Remove](key, Memory.Remove(key, None, timer.next)))
    OK.instance
  }

  def remove(key: Slice[Byte], at: Deadline): OK = {
    validateInput(key)
    maps.write(timer => MapEntry.Put[Slice[Byte], Memory.Remove](key, Memory.Remove(key, Some(at), timer.next)))
    OK.instance
  }

  def remove(fromKey: Slice[Byte], toKey: Slice[Byte]): OK = {
    validateInput(fromKey, toKey)

    if (fromKey equiv toKey)
      remove(fromKey)
    else
      maps
        .write {
          timer =>
            (MapEntry.Put[Slice[Byte], Memory.Range](fromKey, Memory.Range(fromKey, toKey, Value.FromValue.Null, Value.Remove(None, timer.next))): MapEntry[Slice[Byte], Memory]) ++
              MapEntry.Put[Slice[Byte], Memory.Remove](toKey, Memory.Remove(toKey, None, timer.next))
        }

    OK.instance
  }

  def remove(fromKey: Slice[Byte], toKey: Slice[Byte], at: Deadline): OK = {
    validateInput(fromKey, toKey)

    if (fromKey equiv toKey)
      remove(fromKey)
    else
      maps
        .write {
          timer =>
            (MapEntry.Put[Slice[Byte], Memory.Range](fromKey, Memory.Range(fromKey, toKey, Value.FromValue.Null, Value.Remove(Some(at), timer.next))): MapEntry[Slice[Byte], Memory]) ++
              MapEntry.Put[Slice[Byte], Memory.Remove](toKey, Memory.Remove(toKey, Some(at), timer.next))
        }

    OK.instance
  }

  def update(key: Slice[Byte], value: Slice[Byte]): OK = {
    validateInput(key)
    maps.write(timer => MapEntry.Put[Slice[Byte], Memory.Update](key, Memory.Update(key, value, None, timer.next)))
    OK.instance
  }

  def update(key: Slice[Byte], value: SliceOption[Byte]): OK = {
    validateInput(key)
    maps.write(timer => MapEntry.Put[Slice[Byte], Memory.Update](key, Memory.Update(key, value, None, timer.next)))
    OK.instance
  }

  def update(fromKey: Slice[Byte], toKey: Slice[Byte], value: SliceOption[Byte]): OK = {
    validateInput(fromKey, toKey)

    if (fromKey equiv toKey)
      update(fromKey, value)
    else
      maps
        .write {
          timer =>
            (MapEntry.Put[Slice[Byte], Memory.Range](
              key = fromKey,
              value = Memory.Range(
                fromKey = fromKey,
                toKey = toKey,
                fromValue = Value.FromValue.Null,
                rangeValue = Value.Update(value, None, timer.next)
              )
            ): MapEntry[Slice[Byte], Memory]) ++ MapEntry.Put[Slice[Byte], Memory.Update](toKey, Memory.Update(toKey, value, None, timer.next))
        }

    OK.instance
  }

  def clear(readState: ThreadReadState): OK =
    headKey(readState) match {
      case headKey: Slice[Byte] =>
        lastKey(readState) match {
          case lastKey: Slice[Byte] =>
            remove(headKey, lastKey)

          case Slice.Null =>
            OK.instance //might have been removed by another thread?
        }

      case Slice.Null =>
        OK.instance
    }

  def registerFunction(functionId: Slice[Byte], function: SwayFunction): OK =
    functionStore.put(functionId, function)

  def applyFunction(key: Slice[Byte], function: Slice[Byte]): OK =
    if (functionStore.notExists(function)) {
      throw new IllegalArgumentException(s"Cannot apply unregistered function '${function.readString()}'. Please make sure the function is registered. See http://swaydb.io/api/write/registerFunction.")
    } else {
      validateInput(key)

      maps.write {
        timer =>
          if (timer.empty)
            throw new IllegalArgumentException("Functions are disabled.")
          else
            MapEntry.Put[Slice[Byte], Memory.Function](key, Memory.Function(key, function, timer.next))
      }

      OK.instance
    }

  def applyFunction(fromKey: Slice[Byte], toKey: Slice[Byte], function: Slice[Byte]): OK =
    if (functionStore.notExists(function)) {
      throw new IllegalArgumentException(s"Cannot apply unregistered function '${function.readString()}'. Please make sure the function is registered. See http://swaydb.io/api/write/registerFunction.")
    } else {
      validateInput(fromKey, toKey)

      if (fromKey equiv toKey)
        applyFunction(fromKey, function)
      else
        maps.write {
          timer =>
            if (timer.empty)
              throw new IllegalArgumentException("Functions are disabled.")
            else
              (MapEntry.Put[Slice[Byte], Memory.Range](fromKey, Memory.Range(fromKey, toKey, Value.FromValue.Null, Value.Function(function, timer.next))): MapEntry[Slice[Byte], Memory]) ++
                MapEntry.Put[Slice[Byte], Memory.Function](toKey, Memory.Function(toKey, function, timer.next))
        }

      OK.instance
    }

  private def getFromMap(key: Slice[Byte],
                         currentMap: map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory]): MemoryOption =
    if (currentMap.hasRange)
      currentMap.skipList.floor(key) match {
        case floor: Memory.Fixed if floor.key equiv key =>
          floor

        case floor: Memory.Range if key < floor.toKey =>
          floor

        case _ =>
          Memory.Null
      }
    else
      currentMap
        .skipList
        .get(key)

  private def getFromNextLevel(key: Slice[Byte],
                               readState: ThreadReadState,
                               otherMaps: Slice[map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory]]): KeyValue.PutOption =
    otherMaps.headOption match {
      case Some(nextMap) =>
        find(
          key = key,
          readState = readState,
          currentMap = nextMap,
          otherMaps = otherMaps.dropHead()
        )

      case None =>
        nextLevel match {
          case Some(nextLevel) =>
            nextLevel.get(key, readState)

          case None =>
            KeyValue.Put.Null
        }
    }

  def currentGetter(currentMap: map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory]) =
    new CurrentGetter {
      override def get(key: Slice[Byte], readState: ThreadReadState): MemoryOption =
        getFromMap(key, currentMap)
    }

  def nextGetter(readState: ThreadReadState, otherMaps: Slice[map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory]]) =
    new NextGetter {
      override def get(key: Slice[Byte], readState: ThreadReadState): KeyValue.PutOption =
        getFromNextLevel(key, readState, otherMaps)
    }

  private def find(key: Slice[Byte],
                   readState: ThreadReadState,
                   currentMap: map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory],
                   otherMaps: Slice[map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory]]): KeyValue.PutOption =
    Get.seek(
      key = key,
      readState = readState,
      currentGetter = currentGetter(currentMap),
      nextGetter = nextGetter(readState, otherMaps)
    )

  def get(key: Slice[Byte],
          readState: ThreadReadState): KeyValue.PutOption = {
    val currentMaps = maps.currentMapsSlice
    find(
      key = key,
      readState = readState,
      currentMap = currentMaps.head,
      otherMaps = currentMaps.dropHead()
    )
  }

  def getKey(key: Slice[Byte],
             readState: ThreadReadState): SliceOption[Byte] =
    get(key, readState).mapSliceOptional(_.key)

  def firstKeyFromMaps: SliceOption[Byte] =
    maps.reduce[SliceOption[Byte]](
      nullValue = Slice.Null,
      applier = _.skipList.headKey,
      reduce = MinMax.minFavourLeftC[SliceOption[Byte], Slice[Byte]](_, _)(keyOrder)
    )

  def lastKeyFromMaps: SliceOption[Byte] =
    maps.reduce[SliceOption[Byte]](
      nullValue = Slice.Null,
      applier =
        map =>
          map
            .skipList
            .last()
            .flatMapSomeS(Slice.Null: SliceOption[Byte]) {
              case fixed: KeyValue.Fixed =>
                fixed.key

              case range: KeyValue.Range =>
                range.toKey
            },
      reduce = MinMax.maxFavourLeftC[SliceOption[Byte], Slice[Byte]](_, _)(keyOrder)
    )

  def lastKey(readState: ThreadReadState): SliceOption[Byte] =
    last(readState).mapSliceOptional(_.key)

  override def headKey(readState: ThreadReadState): SliceOption[Byte] =
    head(readState).mapSliceOptional(_.key)

  def head(readState: ThreadReadState): KeyValue.PutOption =
    nextLevel match {
      case Some(nextLevel) =>
        nextLevel.headKey(readState) match {
          case nextLevelFirstKey: Slice[Byte] =>
            MinMax
              .minFavourLeftC[SliceOption[Byte], Slice[Byte]](firstKeyFromMaps, nextLevelFirstKey)(keyOrder)
              .flatMapSomeC(KeyValue.Put.Null: KeyValue.PutOption)(ceiling(_, readState))

          case Slice.Null =>
            firstKeyFromMaps.flatMapSomeC(KeyValue.Put.Null: KeyValue.PutOption)(ceiling(_, readState))
        }

      case None =>
        firstKeyFromMaps.flatMapSomeC(KeyValue.Put.Null: KeyValue.PutOption)(ceiling(_, readState))
    }

  def last(readState: ThreadReadState): KeyValue.PutOption =
    nextLevel match {
      case Some(nextLevel) =>
        nextLevel.lastKey(readState) match {
          case nextLevelLastKey: Slice[Byte] =>
            MinMax
              .maxFavourLeftC[SliceOption[Byte], Slice[Byte]](lastKeyFromMaps, nextLevelLastKey)(keyOrder)
              .flatMapSomeC(KeyValue.Put.Null: KeyValue.PutOption)(floor(_, readState))

          case Slice.Null =>
            lastKeyFromMaps.flatMapSomeC(KeyValue.Put.Null: KeyValue.PutOption)(floor(_, readState))
        }

      case None =>
        lastKeyFromMaps.flatMapSomeC(KeyValue.Put.Null: KeyValue.PutOption)(floor(_, readState))
    }

  def ceiling(key: Slice[Byte],
              readState: ThreadReadState): KeyValue.PutOption = {
    val currentMaps = maps.currentMapsSlice
    ceiling(
      key = key,
      readState = readState,
      currentMap = currentMaps.head,
      otherMaps = currentMaps.dropHead()
    )
  }

  def ceiling(key: Slice[Byte],
              readState: ThreadReadState,
              currentMap: map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory],
              otherMaps: Slice[map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory]]): KeyValue.PutOption =
    find(
      key = key,
      readState = readState,
      currentMap = currentMap,
      otherMaps = otherMaps
    ) orElse {
      findHigher(
        key = key,
        readState = readState,
        currentMap = currentMap,
        otherMaps = otherMaps
      )
    }

  def floor(key: Slice[Byte],
            readState: ThreadReadState): KeyValue.PutOption = {
    val currentMaps = maps.currentMapsSlice
    floor(
      key = key,
      readState = readState,
      currentMap = currentMaps.head,
      otherMaps = currentMaps.dropHead()
    )
  }

  def floor(key: Slice[Byte],
            readState: ThreadReadState,
            currentMap: map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory],
            otherMaps: Slice[map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory]]): KeyValue.PutOption =
    find(
      key = key,
      readState = readState,
      currentMap = currentMap,
      otherMaps = otherMaps
    ) orElse {
      findLower(
        key = key,
        readState = readState,
        currentMap = currentMap,
        otherMaps = otherMaps
      )
    }

  private def higherFromMap(key: Slice[Byte],
                            currentMap: map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory]): MemoryOption =
    if (currentMap.hasRange)
      currentMap.skipList.floor(key) match {
        case floorRange: Memory.Range if key >= floorRange.fromKey && key < floorRange.toKey =>
          floorRange

        case _ =>
          currentMap
            .skipList
            .higher(key)
      }
    else
      currentMap
        .skipList
        .higher(key)

  def findHigherInNextLevel(key: Slice[Byte],
                            readState: ThreadReadState,
                            otherMaps: Slice[map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory]]): KeyValue.PutOption = {
    val nextMapOrNull = otherMaps.headOrNull
    if (nextMapOrNull == null)
      nextLevel match {
        case Some(nextLevel) =>
          nextLevel.higher(key, readState)

        case None =>
          KeyValue.Put.Null
      }
    else
      findHigher(
        key = key,
        readState = readState,
        currentMap = nextMapOrNull,
        otherMaps = otherMaps.dropHead()
      )
  }

  def currentWalker(currentMap: map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory],
                    otherMaps: Slice[map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory]]) =
    new CurrentWalker {
      override def get(key: Slice[Byte], readState: ThreadReadState): KeyValue.PutOption =
        find(
          key = key,
          readState = readState,
          currentMap = currentMap,
          otherMaps = otherMaps
        )

      override def higher(key: Slice[Byte], readState: ThreadReadState): LevelSeek[Memory] =
        LevelSeek(
          segmentId = 0,
          result = higherFromMap(key, currentMap).toOptionS
        )

      override def lower(key: Slice[Byte], readState: ThreadReadState): LevelSeek[Memory] =
        LevelSeek(
          segmentId = 0,
          result = lowerFromMap(key, currentMap).toOptionS
        )

      override def levelNumber: String =
        "current"
    }

  def nextWalker(otherMaps: Slice[map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory]]) =
    new NextWalker {
      override def higher(key: Slice[Byte],
                          readState: ThreadReadState): KeyValue.PutOption =
        findHigherInNextLevel(key, readState, otherMaps)

      override def lower(key: Slice[Byte],
                         readState: ThreadReadState): KeyValue.PutOption =
        findLowerInNextLevel(key, readState, otherMaps)

      override def get(key: Slice[Byte],
                       readState: ThreadReadState): KeyValue.PutOption =
        getFromNextLevel(key, readState, otherMaps)

      override def levelNumber: String =
        s"Map - Remaining maps: ${otherMaps.size}."
    }

  def findHigher(key: Slice[Byte],
                 readState: ThreadReadState,
                 currentMap: map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory],
                 otherMaps: Slice[map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory]]): KeyValue.PutOption =
    Higher.seek(
      key = key,
      readState = readState,
      currentSeek = Seek.Current.Read(Int.MinValue),
      nextSeek = Seek.Next.Read,
      currentWalker = currentWalker(currentMap, otherMaps),
      nextWalker = nextWalker(otherMaps),
      keyOrder = keyOrder,
      timeOrder = timeOrder,
      functionStore = functionStore
    )

  /**
   * Higher cannot use an iterator because a single Map can value read requests multiple times for cases where a Map contains a range
   * to fetch ceiling key.
   *
   * Higher queries require iteration of all maps anyway so a full initial conversion to a List is acceptable.
   */
  def higher(key: Slice[Byte],
             readState: ThreadReadState): KeyValue.PutOption = {
    val currentMaps = maps.currentMapsSlice
    findHigher(
      key = key,
      readState = readState,
      currentMap = currentMaps.head,
      otherMaps = currentMaps.dropHead()
    )
  }

  private def lowerFromMap(key: Slice[Byte],
                           currentMap: map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory]): MemoryOption =
    if (currentMap.hasRange)
      currentMap.skipList.floor(key) match {
        case floorRange: Memory.Range if key > floorRange.fromKey && key <= floorRange.toKey =>
          floorRange

        case _ =>
          currentMap.skipList.lower(key)
      }
    else
      currentMap.skipList.lower(key)

  def findLowerInNextLevel(key: Slice[Byte],
                           readState: ThreadReadState,
                           otherMaps: Slice[map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory]]): KeyValue.PutOption = {
    val nextMapOrNull = otherMaps.headOrNull
    if (nextMapOrNull == null)
      nextLevel match {
        case Some(nextLevel) =>
          nextLevel.lower(key, readState)

        case None =>
          KeyValue.Put.Null
      }
    else
      findLower(
        key = key,
        readState = readState,
        currentMap = nextMapOrNull,
        otherMaps = otherMaps.dropHead()
      )
  }

  def findLower(key: Slice[Byte],
                readState: ThreadReadState,
                currentMap: map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory],
                otherMaps: Slice[map.Map[SliceOption[Byte], MemoryOption, Slice[Byte], Memory]]): KeyValue.PutOption =
    Lower.seek(
      key = key,
      readState = readState,
      currentSeek = Seek.Current.Read(Int.MinValue),
      nextSeek = Seek.Next.Read,
      currentWalker = currentWalker(currentMap, otherMaps),
      nextWalker = nextWalker(otherMaps),
      keyOrder = keyOrder,
      timeOrder = timeOrder,
      functionStore = functionStore
    )

  /**
   * Lower cannot use an iterator because a single Map can value read requests multiple times for cases where a Map contains a range
   * to fetch ceiling key.
   *
   * Lower queries require iteration of all maps anyway so a full initial conversion to a List is acceptable.
   */
  def lower(key: Slice[Byte],
            readState: ThreadReadState): KeyValue.PutOption = {
    val currentMaps = maps.currentMapsSlice
    findLower(
      key = key,
      readState = readState,
      currentMap = currentMaps.head,
      otherMaps = currentMaps.dropHead()
    )
  }

  def contains(key: Slice[Byte],
               readState: ThreadReadState): Boolean =
    get(key, readState).isSome

  def valueSize(key: Slice[Byte],
                readState: ThreadReadState): Option[Int] =
    get(key, readState) match {
      case Put.Null =>
        Options.zero

      case put: Put =>
        Some(put.valueLength)
    }

  def keyValueCount: Int = {
    val keyValueCountInMaps = maps.keyValueCount
    nextLevel match {
      case Some(nextLevel) =>
        nextLevel.keyValueCount + keyValueCountInMaps

      case None =>
        keyValueCountInMaps
    }
  }

  def deadline(key: Slice[Byte],
               readState: ThreadReadState): Option[Deadline] =
    get(
      key = key,
      readState = readState
    ).flatMapOption(_.deadline)

  def sizeOfSegments: Long =
    nextLevel match {
      case Some(nextLevel) =>
        nextLevel.sizeOfSegments

      case None =>
        0L
    }

  def existsOnDisk: Boolean =
    Effect.exists(path)

  def close: IO[swaydb.Error.Close, Unit] = {
    //    Delay.cancelTimer()
    maps
      .close
      .onLeftSideEffect {
        exception =>
          logger.error(s"$path: Failed to close maps", exception)
      }

    releaseLocks

    nextLevel
      .map(_.close)
      .getOrElse(IO.unit)
  }

  def closeSegments: IO[swaydb.Error.Level, Unit] =
    nextLevel
      .map(_.closeSegments())
      .getOrElse(IO.unit)

  def mightContainKey(key: Slice[Byte]): Boolean =
    maps.contains(key) ||
      nextLevel.exists(_.mightContainKey(key))

  private def findFunctionInMaps(functionId: Slice[Byte]): Boolean =
    maps.find[Boolean](
      nullResult = false,
      matcher =
        map =>
          map.skipList.values().asScala exists {
            case _: Memory.Put | _: Memory.Remove | _: Memory.Update =>
              false

            case function: Memory.Function =>
              function.function equiv functionId

            case pendingApply: Memory.PendingApply =>
              FunctionStore.containsFunction(functionId, pendingApply.applies)

            case range: Memory.Range =>
              val values =
                range.fromValue match {
                  case FromValue.Null =>
                    Slice(range.rangeValue)

                  case fromValue: Value.FromValue =>
                    Slice(range.rangeValue, fromValue)
                }

              FunctionStore.containsFunction(functionId, values)
          }
    )

  def mightContainFunctionInMaps(functionId: Slice[Byte]): Boolean =
    maps.queuedMapsCountWithCurrent >= 2 ||
      findFunctionInMaps(functionId)

  def mightContainFunction(functionId: Slice[Byte]): Boolean =
    mightContainFunctionInMaps(functionId) ||
      nextLevel.exists(_.mightContainFunction(functionId))

  override def hasNextLevel: Boolean =
    nextLevel.isDefined

  override def appendixPath: Path =
    nextLevel match {
      case Some(nextLevel) =>
        nextLevel.appendixPath

      case None =>
        throw IO.throwable("LevelZero does not have appendix.")
    }

  override def rootPath: Path =
    path

  override def isEmpty: Boolean =
    maps.isEmpty

  override def segmentsCount(): Int =
    nextLevel
      .map(_.segmentsCount())
      .getOrElse(0)

  override def segmentFilesOnDisk: Seq[Path] =
    nextLevel
      .map(_.segmentFilesOnDisk)
      .getOrElse(Seq.empty)

  override def foreachSegment[T](f: (Slice[Byte], Segment) => T): Unit =
    nextLevel.foreach(_.foreachSegment(f))

  override def containsSegmentWithMinKey(minKey: Slice[Byte]): Boolean =
    nextLevel.exists(_.containsSegmentWithMinKey(minKey))

  override def getSegment(minKey: Slice[Byte]): SegmentOption =
    nextLevel match {
      case Some(segment) =>
        segment.getSegment(minKey)

      case None =>
        Segment.Null
    }

  override def meterFor(levelNumber: Int): Option[LevelMeter] =
    nextLevel.flatMap(_.meterFor(levelNumber))

  override def isTrash: Boolean =
    false

  override def levelNumber: Int = 0

  override def isZero: Boolean = true

  override def stateId: Long =
    maps.stateId

  override def nextCompactionDelay: FiniteDuration =
    throttle(levelZeroMeter)

  override def delete: IO[swaydb.Error.Delete, Unit] =
    LevelZero.delete(this)

  def iterator(state: ThreadReadState): Iterator[PutOption] =
    new Iterator[PutOption] {
      var nextKeyValue: PutOption = _

      override def hasNext: Boolean =
        if (nextKeyValue == null) {
          //          nextKeyValue = run(_.head(state))(Bag.less)
          nextKeyValue = head(state)
          nextKeyValue.isSome
        } else {
          //          nextKeyValue = run(_.higher(nextKeyValue.getKey.getC, state))(Bag.less)
          nextKeyValue = higher(nextKeyValue.getKey.getC, state)
          nextKeyValue.isSome
        }

      override def next(): PutOption =
        nextKeyValue
    }

  def reverseIterator(state: ThreadReadState): Iterator[PutOption] =
    new Iterator[PutOption] {
      var nextKeyValue: PutOption = _

      override def hasNext: Boolean =
        if (nextKeyValue == null) {
          //          nextKeyValue = run(_.last(state))(Bag.less)
          nextKeyValue = last(state)
          nextKeyValue.isSome
        } else {
          //          nextKeyValue = run(_.lower(nextKeyValue.getKey.getC, state))(Bag.less)
          nextKeyValue = lower(nextKeyValue.getKey.getC, state)
          nextKeyValue.isSome
        }

      override def next(): PutOption =
        nextKeyValue
    }

  final def run[R, BAG[_]](apply: LevelZero => R)(implicit bag: Bag[BAG]): BAG[R] =
    bag.suspend {
      try
        bag.success(apply(this))
      catch {
        case throwable: Throwable =>
          val error = IO.ExceptionHandler.toError(throwable)
          IO.Defer(apply(this), error).run(1)
      }
    }
}
