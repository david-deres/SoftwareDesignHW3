package il.ac.technion.cs.softwaredesign

import java.util.zip.Deflater
import java.util.zip.Inflater


interface Compressible {
    /**
     * Compress a [ByteArray] identified by [toCompress].
     *
     * @param toCompress - The [ByteArray] to compress.
     *
     * @return A compressed ByteArray or the same ByteArray in case the compression is not efficient.
     */
    fun byteArrayCompress(toCompress: ByteArray): ByteArray {

        val deflated = ByteArray(toCompress.size)
        val compressor = Deflater()
        compressor.setInput(toCompress)
        compressor.finish()
        val compressedSize = compressor.deflate(deflated)
        compressor.end()

        if (compressedSize == toCompress.size) {
            return toCompress
        }

        return deflated.copyOfRange(0, compressedSize)
    }

    /**
     * Decompress a ByteArray identified by [toDecompress] to the length [originalDataSize].
     *
     * @param toDecompress - The [ByteArray] to decompress.
     * @param originalDataSize - The original size (before compression) of [toDecompress].
     *
     * @return A decompressed ByteArray in the length of [originalDataSize].
     */
    fun byteArrayDecompress(toDecompress: ByteArray, originalDataSize: Int): ByteArray {
        if (toDecompress.size == originalDataSize) {
            return toDecompress
        }

        val decompressor = Inflater()
        decompressor.setInput(toDecompress)
        val inflated = ByteArray(originalDataSize)
        decompressor.inflate(inflated)
        decompressor.end()
        return inflated
    }
}


