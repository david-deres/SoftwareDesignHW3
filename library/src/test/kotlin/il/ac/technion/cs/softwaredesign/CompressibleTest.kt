package il.ac.technion.cs.softwaredesign

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.random.Random

class CompressibleTest : Compressible {

    @Test
    fun `data after compress and decompress is the same - using random data`() {
        val dataLen = 20000
        val bytes: ByteArray = Random.nextBytes(dataLen)
        val compressedBytes: ByteArray = byteArrayCompress(bytes)
        val decompressedBytes: ByteArray = byteArrayDecompress(compressedBytes, dataLen)

        Assertions.assertArrayEquals(bytes, decompressedBytes)
    }

    @Test
    fun `data after compress and decompress is the same - using long repetitive data`() {
        val dataLen = 20000
        val bytes = ByteArray(dataLen) { 0xAA.toByte() }


        val compressedBytes: ByteArray = byteArrayCompress(bytes)
        val decompressedBytes: ByteArray = byteArrayDecompress(compressedBytes, dataLen)

        Assertions.assertArrayEquals(bytes, decompressedBytes)
    }

    @Test
    fun `data after compress and decompress is the same - using short repetitive data`() {
        val dataLen = 2
        val bytes = ByteArray(dataLen) { 0xAA.toByte() }
        val compressedBytes: ByteArray = byteArrayCompress(bytes)
        val decompressedBytes: ByteArray = byteArrayDecompress(compressedBytes, dataLen)

        Assertions.assertArrayEquals(bytes, decompressedBytes)
    }
}