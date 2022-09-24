package il.ac.technion.cs.softwaredesign

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream

interface ByteSerializable {
    /**
     * This function serialize the object's fields to [ByteArray]
     * @return the given object as [ByteArray]
     */
    fun byteSerialize(): ByteArray {
        val objectAsByteArray = ByteArrayOutputStream()
        val objectStream = ObjectOutputStream(objectAsByteArray)
        objectStream.writeObject(this)
        return objectAsByteArray.toByteArray()
    }
}