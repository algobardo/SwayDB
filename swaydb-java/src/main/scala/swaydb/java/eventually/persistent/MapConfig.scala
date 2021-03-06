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

package swaydb.java.eventually.persistent

import java.nio.file.Path
import java.util.Collections

import swaydb.core.util.Eithers
import swaydb.data.accelerate.{Accelerator, LevelZeroMeter}
import swaydb.data.config._
import swaydb.data.order.KeyOrder
import swaydb.data.slice.Slice
import swaydb.data.util.Java.JavaFunction
import swaydb.data.util.StorageUnits._
import swaydb.eventually.persistent.DefaultConfigs
import swaydb.java.data.slice.ByteSlice
import swaydb.java.serializers.{SerializerConverter, Serializer => JavaSerializer}
import swaydb.java.{KeyComparator, KeyOrderConverter, PureFunction, Return}
import swaydb.serializers.Serializer
import swaydb.{Apply, Bag}

import scala.compat.java8.FunctionConverters._
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

object MapConfig {

  final class Config[K, V, F](dir: Path,
                              private var mapSize: Int = 4.mb,
                              private var maxMemoryLevelSize: Int = 100.mb,
                              private var maxSegmentsToPush: Int = 5,
                              private var memoryLevelSegmentSize: Int = 2.mb,
                              private var memoryLevelMaxKeyValuesCountPerSegment: Int = 200000,
                              private var persistentLevelAppendixFlushCheckpointSize: Int = 2.mb,
                              private var otherDirs: java.util.Collection[Dir] = Collections.emptyList(),
                              private var cacheKeyValueIds: Boolean = true,
                              private var mmapPersistentLevelAppendix: Boolean = true,
                              private var deleteMemorySegmentsEventually: Boolean = true,
                              private var acceleration: JavaFunction[LevelZeroMeter, Accelerator] = (Accelerator.noBrakes() _).asJava,
                              private var persistentLevelSortedKeyIndex: SortedKeyIndex = DefaultConfigs.sortedKeyIndex(),
                              private var persistentLevelRandomKeyIndex: RandomKeyIndex = DefaultConfigs.randomKeyIndex(),
                              private var binarySearchIndex: BinarySearchIndex = DefaultConfigs.binarySearchIndex(),
                              private var mightContainKeyIndex: MightContainIndex = DefaultConfigs.mightContainKeyIndex(),
                              private var valuesConfig: ValuesConfig = DefaultConfigs.valuesConfig(),
                              private var segmentConfig: SegmentConfig = DefaultConfigs.segmentConfig(),
                              private var fileCache: FileCache.Enable = DefaultConfigs.fileCache(),
                              private var memoryCache: MemoryCache = DefaultConfigs.memoryCache(),
                              private var threadStateCache: ThreadStateCache = ThreadStateCache.Limit(hashMapMaxSize = 100, maxProbe = 10),
                              private var byteComparator: KeyComparator[ByteSlice] = null,
                              private var typedComparator: KeyComparator[K] = null,
                              keySerializer: Serializer[K],
                              valueSerializer: Serializer[V],
                              functionClassTag: ClassTag[_]) {

    def setMapSize(mapSize: Int) = {
      this.mapSize = mapSize
      this
    }

    def setMaxMemoryLevelSize(maxMemoryLevelSize: Int) = {
      this.maxMemoryLevelSize = maxMemoryLevelSize
      this
    }

    def setMaxSegmentsToPush(maxSegmentsToPush: Int) = {
      this.maxSegmentsToPush = maxSegmentsToPush
      this
    }

    def setMemoryLevelSegmentSize(memoryLevelSegmentSize: Int) = {
      this.memoryLevelSegmentSize = memoryLevelSegmentSize
      this
    }

    def setMemoryLevelMaxKeyValuesCountPerSegment(memoryLevelMaxKeyValuesCountPerSegment: Int) = {
      this.memoryLevelMaxKeyValuesCountPerSegment = memoryLevelMaxKeyValuesCountPerSegment
      this
    }

    def setPersistentLevelAppendixFlushCheckpointSize(persistentLevelAppendixFlushCheckpointSize: Int) = {
      this.persistentLevelAppendixFlushCheckpointSize = persistentLevelAppendixFlushCheckpointSize
      this
    }

    def setOtherDirs(otherDirs: java.util.Collection[Dir]) = {
      this.otherDirs = otherDirs
      this
    }

    def setCacheKeyValueIds(cacheKeyValueIds: Boolean) = {
      this.cacheKeyValueIds = cacheKeyValueIds
      this
    }

    def setMmapPersistentLevelAppendix(mmapPersistentLevelAppendix: Boolean) = {
      this.mmapPersistentLevelAppendix = mmapPersistentLevelAppendix
      this
    }

    def setDeleteMemorySegmentsEventually(deleteMemorySegmentsEventually: Boolean) = {
      this.deleteMemorySegmentsEventually = deleteMemorySegmentsEventually
      this
    }

    def setAcceleration(acceleration: JavaFunction[LevelZeroMeter, Accelerator]) = {
      this.acceleration = acceleration
      this
    }

    def setPersistentLevelSortedKeyIndex(persitentLevelSortedKeyIndex: SortedKeyIndex) = {
      this.persistentLevelSortedKeyIndex = persitentLevelSortedKeyIndex
      this
    }

    def setPersistentLevelRandomKeyIndex(persitentLevelRandomKeyIndex: RandomKeyIndex) = {
      this.persistentLevelRandomKeyIndex = persitentLevelRandomKeyIndex
      this
    }

    def setBinarySearchIndex(binarySearchIndex: BinarySearchIndex) = {
      this.binarySearchIndex = binarySearchIndex
      this
    }

    def setMightContainKeyIndex(mightContainKeyIndex: MightContainIndex) = {
      this.mightContainKeyIndex = mightContainKeyIndex
      this
    }

    def setValuesConfig(valuesConfig: ValuesConfig) = {
      this.valuesConfig = valuesConfig
      this
    }

    def setSegmentConfig(segmentConfig: SegmentConfig) = {
      this.segmentConfig = segmentConfig
      this
    }

    def setFileCache(fileCache: FileCache.Enable) = {
      this.fileCache = fileCache
      this
    }

    def setMemoryCache(memoryCache: MemoryCache) = {
      this.memoryCache = memoryCache
      this
    }

    def setThreadStateCache(threadStateCache: ThreadStateCache) = {
      this.threadStateCache = threadStateCache
      this
    }

    def setByteComparator(byteComparator: KeyComparator[ByteSlice]) = {
      this.byteComparator = byteComparator
      this
    }

    def setTypedComparator(typedComparator: KeyComparator[K]) = {
      this.typedComparator = typedComparator
      this
    }

    private val functions = swaydb.Map.Functions[K, V, swaydb.PureFunction[K, V, Apply.Map[V]]]()(keySerializer, valueSerializer)

    def registerFunctions(functions: F*): Config[K, V, F] = {
      functions.foreach(registerFunction(_))
      this
    }

    def registerFunction(function: F): Config[K, V, F] = {
      functions.register(PureFunction.asScala(function.asInstanceOf[swaydb.java.PureFunction[K, V, Return.Map[V]]]))
      this
    }

    def removeFunction(function: F): Config[K, V, F] = {
      functions.core.remove(function.asInstanceOf[swaydb.java.PureFunction[K, V, Return.Map[V]]].id.asInstanceOf[Slice[Byte]])
      this
    }

    def get(): swaydb.java.Map[K, V, F] = {
      val comparator: Either[KeyComparator[ByteSlice], KeyComparator[K]] =
        Eithers.nullCheck(
          left = byteComparator,
          right = typedComparator,
          default = KeyComparator.lexicographic
        )

      val scalaKeyOrder: KeyOrder[Slice[Byte]] = KeyOrderConverter.toScalaKeyOrder(comparator, keySerializer)

      val scalaMap =
        swaydb.eventually.persistent.Map[K, V, swaydb.PureFunction[K, V, Apply.Map[V]], Bag.Less](
          dir = dir,
          mapSize = mapSize,
          maxMemoryLevelSize = maxMemoryLevelSize,
          maxSegmentsToPush = maxSegmentsToPush,
          memoryLevelSegmentSize = memoryLevelSegmentSize,
          memoryLevelMaxKeyValuesCountPerSegment = memoryLevelMaxKeyValuesCountPerSegment,
          persistentLevelAppendixFlushCheckpointSize = persistentLevelAppendixFlushCheckpointSize,
          otherDirs = otherDirs.asScala.toSeq,
          cacheKeyValueIds = cacheKeyValueIds,
          mmapPersistentLevelAppendix = mmapPersistentLevelAppendix,
          deleteMemorySegmentsEventually = deleteMemorySegmentsEventually,
          acceleration = acceleration.apply,
          persistentLevelSortedKeyIndex = persistentLevelSortedKeyIndex,
          persistentLevelRandomKeyIndex = persistentLevelRandomKeyIndex,
          binarySearchIndex = binarySearchIndex,
          mightContainKeyIndex = mightContainKeyIndex,
          valuesConfig = valuesConfig,
          segmentConfig = segmentConfig,
          fileCache = fileCache,
          memoryCache = memoryCache,
          threadStateCache = threadStateCache
        )(keySerializer = keySerializer,
          valueSerializer = valueSerializer,
          functions = functions.asInstanceOf[swaydb.Map.Functions[K, V, swaydb.PureFunction[K, V, Apply.Map[V]]]],
          functionClassTag = functionClassTag.asInstanceOf[ClassTag[swaydb.PureFunction[K, V, Apply.Map[V]]]],
          bag = Bag.less,
          byteKeyOrder = scalaKeyOrder
        ).get

      swaydb.java.Map[K, V, F](scalaMap)
    }
  }

  def functionsOn[K, V](dir: Path,
                        keySerializer: JavaSerializer[K],
                        valueSerializer: JavaSerializer[V]): Config[K, V, swaydb.java.PureFunction[K, V, Return.Map[V]]] =
    new Config(
      dir = dir,
      keySerializer = SerializerConverter.toScala(keySerializer),
      valueSerializer = SerializerConverter.toScala(valueSerializer),
      functionClassTag = ClassTag.Any.asInstanceOf[ClassTag[swaydb.PureFunction[K, V, Apply.Map[V]]]]
    )

  def functionsOff[K, V](dir: Path,
                         keySerializer: JavaSerializer[K],
                         valueSerializer: JavaSerializer[V]): Config[K, V, Void] =
    new Config[K, V, Void](
      dir = dir,
      keySerializer = SerializerConverter.toScala(keySerializer),
      valueSerializer = SerializerConverter.toScala(valueSerializer),
      functionClassTag = ClassTag.Nothing.asInstanceOf[ClassTag[Void]]
    )

}
