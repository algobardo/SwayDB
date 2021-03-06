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

package swaydb.core.segment

import swaydb.core.segment.format.a.block.binarysearch.BinarySearchIndexBlock
import swaydb.core.segment.format.a.block.bloomfilter.BloomFilterBlock
import swaydb.core.segment.format.a.block.hashindex.HashIndexBlock
import swaydb.core.segment.format.a.block.segment.SegmentBlock
import swaydb.core.segment.format.a.block.sortedindex.SortedIndexBlock
import swaydb.core.segment.format.a.block.values.ValuesBlock
import swaydb.data.config.{IOAction, IOStrategy}

private[core] object SegmentIO {

  def defaultSynchronisedStoredIfCompressed =
    new SegmentIO(
      segmentBlockIO = IOStrategy.defaultSynchronised,
      hashIndexBlockIO = IOStrategy.defaultSynchronised,
      bloomFilterBlockIO = IOStrategy.defaultSynchronised,
      binarySearchIndexBlockIO = IOStrategy.defaultSynchronised,
      sortedIndexBlockIO = IOStrategy.defaultSynchronised,
      valuesBlockIO = IOStrategy.defaultSynchronised,
      segmentFooterBlockIO = IOStrategy.defaultSynchronised
    )

  def apply(bloomFilterConfig: BloomFilterBlock.Config,
            hashIndexConfig: HashIndexBlock.Config,
            binarySearchIndexConfig: BinarySearchIndexBlock.Config,
            sortedIndexConfig: SortedIndexBlock.Config,
            valuesConfig: ValuesBlock.Config,
            segmentConfig: SegmentBlock.Config): SegmentIO =
    new SegmentIO(
      segmentBlockIO = segmentConfig.ioStrategy,
      hashIndexBlockIO = hashIndexConfig.ioStrategy,
      bloomFilterBlockIO = bloomFilterConfig.ioStrategy,
      binarySearchIndexBlockIO = binarySearchIndexConfig.ioStrategy,
      sortedIndexBlockIO = sortedIndexConfig.ioStrategy,
      valuesBlockIO = valuesConfig.ioStrategy,
      segmentFooterBlockIO = segmentConfig.ioStrategy
    )
}

private[core] case class SegmentIO(segmentBlockIO: IOAction => IOStrategy,
                                   hashIndexBlockIO: IOAction => IOStrategy,
                                   bloomFilterBlockIO: IOAction => IOStrategy,
                                   binarySearchIndexBlockIO: IOAction => IOStrategy,
                                   sortedIndexBlockIO: IOAction => IOStrategy,
                                   valuesBlockIO: IOAction => IOStrategy,
                                   segmentFooterBlockIO: IOAction => IOStrategy)
