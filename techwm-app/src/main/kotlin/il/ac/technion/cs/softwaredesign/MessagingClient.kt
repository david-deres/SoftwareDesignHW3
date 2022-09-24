package il.ac.technion.cs.softwaredesign


import com.google.protobuf.timestamp
import java.time.Instant
import java.util.concurrent.CompletableFuture

/**
 * A message sent by [fromUser].
 * [id] should be unique over all the messages of users of a MessagingClientFactory.
 */
data class Message(val id: String, val fromUser: String, val message: String){

    fun toProtoBuf() : MessageP {
        val o_id = id
        val o_message = message
        val time : Instant = Instant.now()
        return messageP {
            this.id = o_id
            this.from = fromUser
            this.message = o_message
            this.timeSent = timestamp {
                this.seconds = time.epochSecond
                this.nanos = time.nano
            }
        }
    }

}

typealias Inbox = Map<String, List<Message>>


fun toProtoBuf(inbox: Inbox) : InboxP {
    return inboxP {
        this.record.putAll(inbox.mapValues { messageList{this.messages.addAll(it.value.map { it.toProtoBuf() })}})
    }
}

fun fromProtoBuf(msg: MessageP) : Message {
    return Message(msg.id, msg.from, msg.message)
}


fun fromProtoBuf(inbox: InboxP) : Inbox {
    return inbox.recordMap.mapValues { it.value.messagesList.map { msg -> fromProtoBuf(msg) } }
}


/**
 * This is a class implementing messaging between users
 */
class MessagingClient (private val supplied_username : String, private val supplied_password: String,
                       private val usersManager : UsersManager,
                       private val iDsFactory: IDsFactory, private val messagingHandler: MessagingHandler) {

    private var isConnected = false


//    private fun createDB(name: String): SecureStorageWrapper {
//        return SecureStorageWrapper(storageFactory, name)
//    }
//
//    //TODO: is this unique?
//    private val messageIdsDB = createDB("ids")

    fun disconnect(){
        isConnected = false
    }

    fun connect(){
        isConnected = true
    }



    /**
     * Login with a given password. A successfully logged-in user is considered "online". If the user is already
     * logged in, this is a no-op.
     *
     * @throws IllegalArgumentException If the password was wrong (according to the factory that created the instance)
     */
    fun login(password: String): CompletableFuture<Unit> {
        if (isConnected) {
            return CompletableFuture.completedFuture(Unit)
        }
        if (supplied_password != password) {
            throw IllegalArgumentException("Wrong Password!")
        }
        else{
            this.connect()
            return usersManager.markAsConnected(supplied_username)
        }
    }

    /**
     * Log out of the system. After logging out, a user is no longer considered online.
     *
     * @throws IllegalArgumentException If the user was not previously logged in.
     */
    fun logout(): CompletableFuture<Unit> {
        if (!isConnected) {
            throw IllegalArgumentException("User is not connected")
        }
        this.disconnect()
        return usersManager.markAsDisconnected(supplied_username)
    }

    /**
     * Get online (logged in) users.
     *
     * @throws PermissionException If the user is not logged in.
     * @return A list of usernames which are currently online.
     */
    fun onlineUsers(): CompletableFuture<List<String>> {
        if (!isConnected) {
            throw PermissionException()
        }
        return usersManager.getOnlineUsers()
    }

    /**
     * Get messages currently in your inbox from other users.
     *
     * @return A mapping from usernames to lists of messages (conversations), sorted by time of sending.
     * @throws PermissionException If the user is not logged in.
     */
    fun inbox(): CompletableFuture<Inbox> {
        if (!isConnected) {
            throw PermissionException()
        }
        return messagingHandler.getInbox(supplied_username)
    }

    /**
     * Send a message to a username [toUsername].
     *
     * @throws PermissionException If the user is not logged in.
     * @throws IllegalArgumentException If the target user does not exist, or message contains more than 120 characters.
     */
    fun sendMessage(toUsername: String, message: String): CompletableFuture<Unit>
    {
        if (!isConnected) {
            throw PermissionException()
        }
        if (message.length > 120) {
            throw IllegalArgumentException("Message is too long!")
        }
        return usersManager.isExistingUser(toUsername)
            .thenApply { if (it == false) {throw IllegalArgumentException("Target user does not exist!")} }
            .thenCompose { messagingHandler.sendMessage(toUsername, Message(iDsFactory.createID(), supplied_username, message))
            }
    }

    /**
     * Delete a message from your inbox.
     *
     * @throws PermissionException If the user is not logged in.
     * @throws IllegalArgumentException If a message with the given [id] does not exist
     */
    fun deleteMessage(id: String): CompletableFuture<Unit> {
        if (!isConnected) {
            throw PermissionException()
        }
        return messagingHandler.deleteMessage(id, supplied_username)
    }
}

/**
 * A factory for creating messaging clients that can send messages to each other.
 */
interface MessagingClientFactory {
    /**
     * Get an instance of a [MessagingClient] for a given username and password.
     * You can assume that:
     * 1. different clients will have different usernames.
     * 2. calling get for the first time creates a user with [username] and [password].
     * 3. calling get for an existing client (not the first time) is called with the right password.
     *
     * About persistence:
     * All inboxes of clients should be persistent.
     * Note: restart == a new instance is created and the previous one is not used.
     * When MessagingClientFactory restarts all users should be logged off.
     * When a MessagingClient restarts (another instance is created with [MessagingClientFactory]'s [get]), only the
     *  specific user is logged off.
     */
    fun get(username: String, password: String): CompletableFuture<MessagingClient>
}
