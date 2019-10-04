package swaydb.core.segment

import java.nio.file.Path

import swaydb.core.util.LimitHashMap

import scala.util.Random

private[swaydb] sealed trait ReadState {
  def isSequential(path: Path): Boolean
  def setSequential(path: Path, isSequential: Boolean): Unit
}

private[swaydb] object ReadState {

  def hashMap(): ReadState =
    new HashMapState(new java.util.HashMap[Path, Boolean]())

  def limitHashMap(maxSize: Int,
                   probe: Int): ReadState =
    new LimitHashMapState(LimitHashMap[Path, Boolean](maxSize, probe))

  def limitHashMap(maxSize: Int): ReadState =
    new LimitHashMapState(LimitHashMap[Path, Boolean](maxSize))

  private class HashMapState(map: java.util.HashMap[Path, Boolean]) extends ReadState {

    def isSequential(path: Path): Boolean = {
      val isSeq = map.get(path)
      isSeq == null || isSeq
    }

    def setSequential(path: Path, isSequential: Boolean): Unit =
      map.put(path, isSequential)
  }

  private class LimitHashMapState(map: LimitHashMap[Path, Boolean]) extends ReadState {

    def isSequential(path: Path): Boolean =
      map.get(path).forall(_ == true)

    def setSequential(path: Path, isSequential: Boolean): Unit =
      map.put(path, isSequential)

    override def toString: String =
      map.toString
  }

  def random: ReadState =
    if (scala.util.Random.nextBoolean())
      ReadState.hashMap()
    else if (scala.util.Random.nextBoolean())
      ReadState.limitHashMap(10, Random.nextInt(10))
    else
      ReadState.limitHashMap(20)
}
