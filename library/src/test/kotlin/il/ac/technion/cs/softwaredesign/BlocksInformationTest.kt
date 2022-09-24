package il.ac.technion.cs.softwaredesign

import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.random.Random

class BlocksInformationTest {

    @Nested
    inner class ByteArrayUtilitiesTest {
        @Nested
        inner class ConversionTest {
            @Test
            fun `long number after converting to Number and back is the same `() {
                val number = Random.nextLong()
                val bytes: ByteArray = BlocksInformation.longToBytes(number)
                val numberAfter = BlocksInformation.byteArrayToLong(bytes)
                Assertions.assertEquals(number, numberAfter)
            }

            @Test
            fun `int number after converting to Number and back is the same `() {
                val number = Random.nextInt()
                val bytes: ByteArray = BlocksInformation.intToBytes(number)
                val numberAfter = BlocksInformation.byteArrayToInt(bytes)
                Assertions.assertEquals(number, numberAfter)
            }
        }

        @Nested
        inner class `testing getting one block sized` {
            @Test
            fun `getOneBlockFromData returns null if given index is negative`() {
                val bytes = ByteArray(5)
                val block: ByteArray? = BlocksInformation.getOneBlockFromData(-1, bytes, 6)
                Assertions.assertNull(block)
            }

            @Test
            fun `getOneBlockFromData returns null if given index out of bound`() {
                val bytes = ByteArray(5)
                val block: ByteArray? = BlocksInformation.getOneBlockFromData(1, bytes, 5)
                Assertions.assertNull(block)
            }

            @Test
            fun `getOneBlockFromData get block successfully`() {
                val bytes = byteArrayOf(0x1, 0x2, 0x3, 0x4, 0x5)
                val block: ByteArray? = BlocksInformation.getOneBlockFromData(1, bytes, 2)
                Assertions.assertTrue(byteArrayOf(0x3, 0x4) contentEquals block)
            }

            @Test
            fun `getOneBlockFromData get tinier block successfully when remaining data is smaller than a block size`() {
                val bytes = byteArrayOf(0x1, 0x2, 0x3, 0x4, 0x5)
                val block: ByteArray? = BlocksInformation.getOneBlockFromData(2, bytes, 2)
                Assertions.assertTrue(byteArrayOf(0x5) contentEquals block)
            }
        }
    }

    @Nested
    inner class `testing serialization` {
        @Test

        fun `blocksInformation when deserialized is 100 bytes or less-mockk`() {
            val dataBlock: BlocksInformation = mockk(relaxed = true)
            val bytes: ByteArray = dataBlock.byteSerialize()
            Assertions.assertTrue(bytes.size <= 100)
        }

        @Test
        fun `blocksInformation when deserialized is 100 bytes or less-max sizes`() {
            val dataBlock = BlocksInformation(Int.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)
            val bytes: ByteArray = dataBlock.byteSerialize()
            Assertions.assertTrue(bytes.size <= 100)
        }

        @Test
        fun `blocksInformation when deserialized is 100 bytes or less-random`() {
            val dataBlock = BlocksInformation(Random.nextInt(), Random.nextLong(), Random.nextLong())
            val bytes: ByteArray = dataBlock.byteSerialize()
            Assertions.assertTrue(bytes.size <= 100)
        }

        @Test
        fun `block info is the same before and after serialization-random`() {
            val dataBlock = BlocksInformation(Random.nextInt(), Random.nextLong(), Random.nextLong())
            val bytes: ByteArray = dataBlock.byteSerialize()
            val dataBlockAfter = BlocksInformation(bytes)
            Assertions.assertEquals(dataBlock, dataBlockAfter)
        }
    }
}