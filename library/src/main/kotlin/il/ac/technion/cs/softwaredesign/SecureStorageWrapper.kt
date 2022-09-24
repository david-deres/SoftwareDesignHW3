package il.ac.technion.cs.softwaredesign

import com.google.inject.Inject
import com.google.inject.name.Named
import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * This class is a wrapper to SecureStorage.
 *
 * Its main goal is to store data in blocks.
 *
 * the reason for that is to handle the restriction the value size in [write] (up to [blockSize] per write)
 *
 * The data that being written to the SecureStorageWrapper is cached:
 * now when writing, it writes to both persistent and non-persistent memory.
 * If the data is stored in the non-persistent memory - reading takes the data from there, otherwise it gets the data from the
 * persistent memory and cached it (put it in the non-persistent memory) for later use.
 *
 * @property secureMemory- the storage where the real value is stored:
 * values larger than [blockSize] bytes are saved in multiple blocks
 * @property blocksMap - here we save the data in a simplified format as described in [BlocksInformation]
 * @property blockSize- the maximum size of the block we can write.
 * @property [maxFreeBlockFuture]- the last free block in [secureMemory]
 * data is saved consecutively and [maxFreeBlockFuture]
 * incremented as a number for [BlocksInformation]
 * @property [saveFreeBlockKey]- the key in [secureMemory] where we save [maxFreeBlockFuture]
 * saved for persistence
 */

class SecureStorageWrapper constructor(secureMemoryFactory: SecureStorageFactory, secureStorageName: String) :
    Compressible {
    private val secureMemory: CompletableFuture<SecureStorage> = secureMemoryFactory.open("memory".toByteArray())
    private val blocksMap: CompletableFuture<SecureStorage> = secureMemoryFactory.open(secureStorageName.toByteArray())
    private val name: String = secureStorageName
    private val blockSize: Int = 100
    private var maxFreeBlockFuture: CompletableFuture<Long> = CompletableFuture.completedFuture(1)
    private var saveFreeBlockKey: String = "0"

    init {
        restoreFreeBlock()
    }

    /**
     * assigns the last [maxFreeBlockFuture] saved by [saveFreeBlockKey]
     *
     * in order to maintain a persistent database
     */
    private fun restoreFreeBlock() {
        val currMaxFreeBlock: CompletableFuture<ByteArray?> = secureMemory
            .thenCompose { memory -> memory.read(saveFreeBlockKey.toByteArray()) }

        maxFreeBlockFuture = currMaxFreeBlock
            .thenCombine(maxFreeBlockFuture) { currFreeBlock, maxFreeBlock ->
                if (currFreeBlock != null) {
                    String(currFreeBlock).toLong()
                } else {
                    maxFreeBlock
                }
            }
    }

    /**
     * Backup current [maxFreeBlockFuture] to the persistent database
     */
    private fun backupFreeBlock() {
        secureMemory
            .thenCombine(maxFreeBlockFuture) { memory, freeBlock ->
                memory.write(saveFreeBlockKey.toByteArray(), freeBlock.toString().toByteArray())
            }
        //secureMemory.write(saveFreeBlockKey.toByteArray(), maxFreeBlock.toString().toByteArray())
    }

    /**
     * Generate a new block number to save block sized memory at
     * It backups the next available block for persistence
     */
    private fun generateNewBlock(): CompletableFuture<Long> {
        restoreFreeBlock()
        val oldBlock = maxFreeBlockFuture
        maxFreeBlockFuture = maxFreeBlockFuture
            .thenApply { max -> max?.let { max + 1 } }

        backupFreeBlock()
        return oldBlock
    }

    /**
     * Write data in block size to a new block
     *
     * @param blockValueToWrite block of data to write
     *
     * @return null in case blockValueToWrite is bigger than the size of a block that is defined in [SecureStorageWrapper]
     * @return the number of block that the data was written to
     */
    private fun writeToNewBlock(blockValueToWrite: ByteArray): CompletableFuture<Long> {
        val block: CompletableFuture<Long> = generateNewBlock()

        secureMemory
            .thenCombine(block) { memory, writeBlock ->
                memory.write(writeBlock.toString().toByteArray(), blockValueToWrite)
            }

        return block
    }

    /**
     * read the value that is mapped to [key]
     *
     * @param key the unique key to the value we want
     *
     * @return null if value doesn't exist, the value otherwise
     */
    fun read(key: ByteArray): CompletableFuture<ByteArray?> {
        return blocksMap
            .thenCompose { map -> map.read(key) }
            .thenApply { it?.let { BlocksInformation(it) } }
            .thenCompose {
                if (it == null) {
                    CompletableFuture.completedFuture(null)
                } else {
                    val valueFuture: CompletableFuture<ByteArray?> =
                        it.getDecompressedDataInBlocks(secureMemory)
                    valueFuture
                }
            }
    }

    /**
     * write the key ,value pair to persistent memory
     *
     * @param key the unique key in ByteArray
     * @param value the value in ByteArray that will be mapped to [key]
     *
     * @note: There is no limitation on [value] size
     */
    fun write(key: ByteArray, value: ByteArray): CompletableFuture<Unit> {
        var firstBlock: CompletableFuture<Long> = CompletableFuture.completedFuture<Long>(-1)
        val compressedData: ByteArray = byteArrayCompress(value)
        var blockNumInData = 0
        var blockToWrite: ByteArray? = BlocksInformation.getOneBlockFromData(blockNumInData, compressedData, blockSize)
        var writtenBlock: CompletableFuture<Long> = CompletableFuture.completedFuture<Long>(-1)

        while (blockToWrite != null) {
            writtenBlock = writeToNewBlock(blockToWrite)

            firstBlock = firstBlock
                .thenCompose {
                    if (it == -1L) {
                        writtenBlock
                    } else {
                        CompletableFuture.completedFuture(it)
                    }
                }
            blockNumInData++
            blockToWrite = BlocksInformation.getOneBlockFromData(blockNumInData, compressedData, blockSize)
        }

        val lastBlock: CompletableFuture<Long> = writtenBlock

        firstBlock.thenCombine(lastBlock) { first, last ->
            if (first != -1L && last != -1L) {
                val blocksInformationSerialized = BlocksInformation(value.size, first, last).byteSerialize()
                blocksMap.thenCompose { map -> map.write(key, blocksInformationSerialized) }
            }
        }
        return CompletableFuture.completedFuture(Unit)
    }

    /**
     * returns the name of the storage
     *
     * @return the name of the storage
     */
    fun getName(): String {
        return name
    }

}
