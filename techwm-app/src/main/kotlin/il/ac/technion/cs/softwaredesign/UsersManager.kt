package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.util.concurrent.CompletableFuture

class UsersManager(secureStorageFactory : SecureStorageFactory) {

    private val onlineUsers : MutableList<String> = mutableListOf()
    private val users = SecureStorageWrapper(secureStorageFactory, "users")


    fun addNewUser(username: String, password: String) : CompletableFuture<Unit> =
        users.write(username.encodeToByteArray(), password.encodeToByteArray())


    fun markAsConnected(username : String): CompletableFuture<Unit> {
        return CompletableFuture.supplyAsync { onlineUsers.add(username) }
    }

    fun markAsDisconnected(username: String) : CompletableFuture<Unit>  {
        return CompletableFuture.supplyAsync { onlineUsers.remove(username) }
    }

    fun getOnlineUsers() : CompletableFuture<List<String>> = CompletableFuture.completedFuture(onlineUsers)



    fun isExistingUser(username: String) : CompletableFuture<Boolean>{
        return users.read(username.encodeToByteArray()).thenApply { it != null }
    }
}