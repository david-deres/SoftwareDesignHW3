package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import java.io.Serializable
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture


/**
 * This is a class to represent data in blocks
 *
 * Assumption: saving values in consecutive blocks in [SecureStorageWrapper]
 *
 * @property [startBlock] - is the start block of sequenced data
 * @property [endBlock] - is the end block of sequenced data (the data includes the [endBlock])
 * @property [originalDataSize] - is the size of the value- before [byteArrayCompress]
 *
 */
data class BlocksInformation(
    private var originalDataSize: Int = 0,
    private var startBlock: Long = 0,
    private var endBlock: Long = 0
) :
    Compressible, Serializable, ByteSerializable {
    /**
     * This companion object contains utilities for [ByteArray]
     */
    companion object ByteArrayUtilities {

        /**
         * get ByteBuffer of sub [array] from index 0 to [dataSize]
         *
         * @param array - contains data in bytes
         * @param dataSize - the size of the data we want to get sub [array] from
         *
         * @return the ByteBuffer of sub [array] from index 0 to [dataSize]
         */
        private fun subByteArrayToByteBuffer(array: ByteArray, dataSize: Int): ByteBuffer {
            val startBlockSubArray = array.copyOfRange(0, dataSize)
            val buffer = ByteBuffer.allocate(dataSize)
            buffer.put(startBlockSubArray)
            buffer.flip() // This flip is for switching between reading and writing from ByteBuffer
            return buffer
        }

        /**
         * converts the bytes in the beginning of [ByteArray] that representing [Int], to [Int]
         *
         * @param array - contains bytes representing Int
         *
         * @return the [Int] represented in the beginning of the given [ByteArray]
         */
        fun byteArrayToInt(array: ByteArray): Int {
            return subByteArrayToByteBuffer(array, Int.SIZE_BYTES).int
        }

        /**
         * converts the bytes in the beginning of [ByteArray] that representing [Long], to [Long]
         *
         * @param array - contains bytes representing Long
         *
         * @return the [Long] represented in [ByteArray]
         */
        fun byteArrayToLong(array: ByteArray): Long {
            return subByteArrayToByteBuffer(array, Long.SIZE_BYTES).long
        }

        /**
         * converts a [Int] to [ByteArray]
         *
         * @param toConvert - the Int we want to convert to [ByteArray]
         *
         * @return a [ByteArray] representing the [Int] we converted
         */
        fun intToBytes(toConvert: Int): ByteArray {
            return ByteBuffer.allocate(Integer.SIZE / Byte.SIZE_BITS).putInt(toConvert).array()
        }

        /**
         * converts a [Long] to [ByteArray]
         *
         * @param toConvert - the Long we want to convert to [ByteArray]
         *
         * @return a [ByteArray] representing the Long we converted
         */
        fun longToBytes(toConvert: Long): ByteArray {
            return ByteBuffer.allocate(Long.SIZE_BITS / Byte.SIZE_BITS).putLong(toConvert).array()
        }

        /**
         * get an indexed block of bytes in [blockSize] from [data]
         *
         * **note**: blocks are indexed from 0.
         *
         * @param blockIndex - index of block of bytes.
         * For example block 0 is the first [blockSize] bytes in [data] and block 1 is the next [blockSize] bytes in [data].
         * @param data - data in bytes
         *
         * @return - null if [blockIndex] is out of the [data] bounds (0 to [data].size)
         * @return - otherwise, The indexed block from data.
         *
         * if the bytes in [data] from [blockIndex] are less than [blockSize], then only the bytes those bytes will return.
         *
         * In this case the ByteArray size will be less than [blockSize]
         */
        fun getOneBlockFromData(blockIndex: Int, data: ByteArray, blockSize: Int): ByteArray? {
            val startByteOfData = blockIndex * blockSize

            if (startByteOfData >= data.size || blockIndex < 0) {
                return null
            }

            var endByteOfData = startByteOfData + blockSize

            if (endByteOfData > data.size) {
                endByteOfData = data.size
            }

            return data.copyOfRange(startByteOfData, endByteOfData)
        }
    }

    /**
     * constructor which converts a [ByteArray] to a [BlocksInformation] object.
     *
     * needed for deserialization.
     *
     * @param array - contains the parameters for [BlocksInformation], as described in [byteSerialize]
     *
     * @return a [BlocksInformation] with the [startBlock] , [endBlock] and [originalDataSize] as found in [array]
     */
    constructor(array: ByteArray) : this() {
        var arr = array
        startBlock = byteArrayToLong(arr)
        arr = arr.drop(Long.SIZE_BYTES).toByteArray()
        endBlock = byteArrayToLong(arr)
        arr = arr.drop(Long.SIZE_BYTES).toByteArray()
        originalDataSize = byteArrayToInt(arr)
    }

    /**
     * Get the data represented in the blocks decompressed.
     *
     * **note**: assumes the blocks have data in them (not null).
     *
     * @param memory the blocks memory the data is at
     *
     * @return The data in the blocks decompressed in ByteArray
     *
     */
    fun getDecompressedDataInBlocks(memory: CompletableFuture<SecureStorage>): CompletableFuture<ByteArray?> {
        var unitedBlocksData: CompletableFuture<ByteArray> = CompletableFuture.completedFuture(ByteArray(0))

        for (blockNum in startBlock..endBlock) {
            val blockData: CompletableFuture<ByteArray?> =
                memory.thenCompose { it.read(blockNum.toString().toByteArray()) }

            unitedBlocksData = unitedBlocksData
                .thenCompose { united ->
                    blockData
                        .thenApply { block -> block?.let { united.plus(it) } }
                }
        }
        return unitedBlocksData.thenApply { united -> byteArrayDecompress(united, originalDataSize) }
    }

    override fun toString(): String {
        return "BlocksInformation(originalDataSize=$originalDataSize, startBlock=$startBlock, endBlock=$endBlock)"
    }

    /**
     * Serialization for [BlocksInformation].
     *
     * Due to 100 Byte size restriction,
     *
     * [BlocksInformation] fields are serialized in the same order:
     *
     * [startBlock], [endBlock], and [originalDataSize] respectively.
     *
     * @return the fields of [BlocksInformation] as [ByteArray]
     */
    override fun byteSerialize(): ByteArray {
        val startBlockBuffer = longToBytes(startBlock)
        val endBlockBuffer = longToBytes(endBlock)
        val originalSizeBlockBuffer = intToBytes(originalDataSize)
        return startBlockBuffer.plus(endBlockBuffer).plus(originalSizeBlockBuffer)
    }
}
