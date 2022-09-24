package il.ac.technion.cs.softwaredesign

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.util.concurrent.CompletableFuture

class MessagingClientManager @Inject private constructor (secureStorageFactory : SecureStorageFactory, private val iDsFactory: IDsFactory) : MessagingClientFactory  {

    private val usersManager = UsersManager(secureStorageFactory)

    private val messagingHandler = MessagingHandler(secureStorageFactory)


    override fun get(username: String, password: String): CompletableFuture<MessagingClient> {
        val msgClient = MessagingClient(username, password, usersManager, iDsFactory, messagingHandler)
        return usersManager.isExistingUser(username).thenCompose {
            if (it == false) {
                usersManager.addNewUser(username, password)
                    .thenCompose { CompletableFuture.completedFuture(msgClient) }
            }
            else {
                usersManager.markAsDisconnected(username).thenApply { msgClient }
            }
        }
    }
}
