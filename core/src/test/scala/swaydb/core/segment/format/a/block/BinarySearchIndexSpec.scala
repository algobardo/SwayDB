package swaydb.core.segment.format.a.block

import org.scalatest.{Matchers, WordSpec}
import swaydb.core.CommonAssertions.eitherOne
import swaydb.core.RunThis._
import swaydb.core.TestData.{randomBytesSlice, randomCompression, randomIntMax}
import swaydb.core.segment.format.a.MatchResult
import swaydb.core.util.Bytes
import swaydb.data.IO
import swaydb.data.order.KeyOrder
import swaydb.data.slice.Slice
import swaydb.serializers.Default._
import swaydb.serializers._

class BinarySearchIndexSpec extends WordSpec with Matchers {

  implicit val keyOrder = KeyOrder.default

  def assertSearch(bytes: Slice[Byte],
                   values: Seq[Int],
                   unAlteredIndex: BinarySearchIndex) =
    runThis(10.times) {
      val randomBytes = randomBytesSlice(randomIntMax(100))

      val (adjustedOffset, alteredBytes) =
        eitherOne(
          (unAlteredIndex.offset, bytes),
          (unAlteredIndex.offset, bytes ++ randomBytesSlice(randomIntMax(100))),
          (unAlteredIndex.offset.copy(start = randomBytes.size), randomBytes ++ bytes.close()),
          (unAlteredIndex.offset.copy(start = randomBytes.size), randomBytes ++ bytes ++ randomBytesSlice(randomIntMax(100)))
        )

      val largestValue = values.last

      def matcher(valueToFind: Int)(valueFound: Int): IO[MatchResult] =
        IO {
          if (valueToFind == valueFound)
            MatchResult.Matched(null)
          else if (valueToFind < valueFound)
            MatchResult.AheadOrNoneOrEnd
          else
            MatchResult.Behind
        }

      val alteredIndex =
        unAlteredIndex.copy(offset = adjustedOffset)

      values foreach {
        value =>
          BinarySearchIndex.search(
            reader = alteredIndex.createBlockReader(SegmentBlock.createUnblockedReader(alteredBytes).get),
            start = None,
            end = None,
            assertValue = matcher(valueToFind = value)
          ).get shouldBe defined
      }

      //check for items not in the index.
      val notInIndex = (values.head - 100 until values.head) ++ (largestValue + 1 to largestValue + 100)

      notInIndex foreach {
        i =>
          BinarySearchIndex.search(
            reader = alteredIndex.createBlockReader(SegmentBlock.createUnblockedReader(alteredBytes).get),
            start = None,
            end = None,
            assertValue = matcher(valueToFind = i)
          ).get shouldBe empty
      }
    }

  "write full index" when {
    "all values have the same size" in {
      runThis(10.times) {
        Seq(0 to 127, 128 to 300, 16384 to 16384 + 200, Int.MaxValue - 5000 to Int.MaxValue - 1000) foreach {
          values =>
            val valuesCount = values.size
            val largestValue = values.last
            val state =
              BinarySearchIndex.State(
                largestValue = largestValue,
                uniqueValuesCount = valuesCount,
                isFullIndex = true,
                minimumNumberOfKeys = 0,
                compressions = eitherOne(Seq.empty, Seq(randomCompression()))
              ).get

            values foreach {
              offset =>
                BinarySearchIndex.write(value = offset, state = state).get
            }

            BinarySearchIndex.close(state).get

            state.writtenValues shouldBe values.size

            state.bytes.isFull shouldBe true

            val index =
              BinarySearchIndex.read(
                offset = BinarySearchIndex.Offset(0, state.bytes.size),
                reader = SegmentBlock.createUnblockedReader(state.bytes).get
              ).get

            index.valuesCount shouldBe state.writtenValues

            //byte size of Int.MaxValue is 5, but the index will switch to using 4 byte ints.
            index.bytesPerValue should be <= 4

            assertSearch(
              bytes = state.bytes,
              values = values,
              unAlteredIndex = index
            )
        }
      }
    }

    "all values have unique size" in {
      runThis(10.times) {
        val values = (126 to 130) ++ (16384 - 2 to 16384)
        val valuesCount = values.size
        val largestValue = values.last
        val compression = eitherOne(Seq.empty, Seq(randomCompression()))
        val state =
          BinarySearchIndex.State(
            largestValue = largestValue,
            uniqueValuesCount = valuesCount,
            isFullIndex = true,
            minimumNumberOfKeys = 0,
            compressions = compression
          ).get

        values foreach {
          value =>
            BinarySearchIndex.write(value = value, state = state).get
        }
        BinarySearchIndex.close(state).get

        state.writtenValues shouldBe values.size

        val index =
          BinarySearchIndex.read(
            offset = BinarySearchIndex.Offset(0, state.bytes.size),
            reader = SegmentBlock.createUnblockedReader(state.bytes).get
          ).get

        index.bytesPerValue shouldBe Bytes.sizeOf(largestValue)

        val headerSize =
          BinarySearchIndex.optimalHeaderSize(
            largestValue = largestValue,
            valuesCount = values.size,
            hasCompression = compression.nonEmpty
          )

        index.headerSize shouldBe headerSize
        index.valuesCount shouldBe values.size

        assertSearch(
          bytes = state.bytes,
          values = values,
          unAlteredIndex = index
        )
      }
    }
  }
}
