package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.util.concurrent.CompletableFuture
import kotlin.math.sign

class MessagingHandler(storageFactory: SecureStorageFactory) {
    private val inboxes = SecureStorageWrapper(storageFactory, "inboxes")
    private val idsToUsers =  SecureStorageWrapper(storageFactory, "itu")

    private class MessagesComparator : Comparator<MessageP> {
        override fun compare(p0:MessageP, p1: MessageP): Int {
            val secondsDiff = p0.timeSent.seconds - p1.timeSent.seconds
            val nanosDiff = p0.timeSent.nanos - p1.timeSent.nanos
            if (secondsDiff.sign == 0){
                return nanosDiff
            }
            else {
                return secondsDiff.sign
            }
        }

    }


    fun sendMessage(toUser: String, message : Message) : CompletableFuture<Unit> =
        idsToUsers.write(message.id.encodeToByteArray(), message.fromUser.encodeToByteArray())
            .thenCompose {
                inboxes.read(toUser.encodeToByteArray())
                    .thenCompose { inboxBytes ->
                        if (inboxBytes == null) {
                            val newList: MessageList = messageList { this.messages.add(message.toProtoBuf()) }
                            val newInbox : InboxP = inboxP { this.record.put(message.fromUser, newList) }
                            inboxes.write(toUser.encodeToByteArray(), newInbox.toByteArray())
                        }
                        else {
                            val inbox =  InboxP.parseFrom(inboxBytes)
                            val msg = message.toProtoBuf()
                            val prevMessages = inbox.recordMap[message.fromUser]?.messagesList?.toMutableList() ?: mutableListOf()
                            prevMessages.add(msg)
                            prevMessages.sortWith(MessagesComparator())
                            val prevInbox =  inbox.recordMap.toMutableMap()
                            prevInbox.remove(message.fromUser)
                            prevInbox[message.fromUser] = messageList { this.messages.addAll(prevMessages) }
                            val newInbox : InboxP = inboxP { this.record.putAll(prevInbox) }
                            inboxes.write(toUser.encodeToByteArray(), newInbox.toByteArray())
                        }
                    }
            }

    fun deleteMessage(id: String, username: String) : CompletableFuture<Unit> {
        return inboxes.read(username.encodeToByteArray()).thenCompose { inboxBytes ->
            if (inboxBytes == null)
            {
                throw IllegalArgumentException("A message with the given id does not exist!")
                CompletableFuture.completedFuture(Unit)
            }
            else
            {
                getSendingUserById(id).thenCompose { sendingUser ->
                    if (sendingUser == null)
                    {
                        throw IllegalArgumentException("A message with the given id does not exist!")
                        CompletableFuture.completedFuture(Unit)
                    }
                    else
                    {
                        val inbox = InboxP.parseFrom(inboxBytes)
                        val prevMessages = inbox.recordMap[sendingUser]?.messagesList?.toMutableList()
                        val msgToDelete = prevMessages?.filter { it.id == id }
                        if (msgToDelete.isNullOrEmpty())
                        {
                            throw IllegalArgumentException("A message with the given id does not exist!")
                            CompletableFuture.completedFuture(Unit)
                        }
                        else
                        {
                            prevMessages.removeAll(msgToDelete)
                            val prevInbox =  inbox.recordMap.toMutableMap()
                            prevInbox.remove(sendingUser)
                            prevInbox[sendingUser] = messageList { this.messages.addAll(prevMessages) }
                            val newInbox : InboxP = inboxP { this.record.putAll(prevInbox) }
                            inboxes.write(username.encodeToByteArray(), newInbox.toByteArray())
                        }
                    }
                }
            }
        }
    }

    // returns inbox sorted by time of sending
    fun getInbox(username : String) : CompletableFuture<Inbox> =
        inboxes.read(username.encodeToByteArray()).thenCompose {
            if (it == null)
                { CompletableFuture.completedFuture(mapOf())}
            else{
                CompletableFuture.completedFuture(fromProtoBuf(InboxP.parseFrom(it)))
            }
        }


    fun getSendingUserById(id : String) : CompletableFuture<String?>{
        return idsToUsers.read(id.encodeToByteArray()).thenApply { it?.let { String(it) } }
    }
}