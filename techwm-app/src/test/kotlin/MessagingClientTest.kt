package il.ac.technion.cs.softwaredesign

import com.google.inject.Guice
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.isEmpty
import dev.misfitlabs.kotlinguice4.getInstance
import org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.Thread.sleep
import java.util.concurrent.CompletionException

class MessagingClientTest {
    private val injector = Guice.createInjector(TestMessagingClientModule())
    private val clientFactory = injector.getInstance<MessagingClientFactory>()

    @Test
    fun `login with wrong password throws exception`() {
        val spongebob = clientFactory.get("spongebob", "pass").join()
        assertThrows<IllegalArgumentException> {
            spongebob.login("wrong_pass")
        }
    }

    @Test
    fun `calling onlineUsers by offline user throws exception`() {
        val spongebob = clientFactory.get("spongebob", "pass").join()
        assertThrows<PermissionException> {
            spongebob.onlineUsers().join()
        }
    }

    @Test
    fun `login changes user to be ONLINE`() {
        val spongebob = clientFactory.get("spongebob", "pass").join()
        val patrick = clientFactory.get("patrick", "pass").join()
        spongebob.login("pass")
            .thenCompose { patrick.login("pass") }
            .thenApply { assertThat(spongebob.onlineUsers().get(), equalTo(listOf("spongebob","patrick"))) }
            .join()
    }

    @Test
    fun `logout on offline user throws exception`() {
        val spongebob = clientFactory.get("spongebob", "pass").join()
        val thrown = assertThrows<IllegalArgumentException> {
            spongebob.logout().join()
        }
    }

    @Test
    fun `logout removes users from online list`() {
        val spongebob = clientFactory.get("spongebob", "pass").join()
        val patrick = clientFactory.get("patrick", "pass").join()
        spongebob.login("pass")
            .thenCompose { patrick.login("pass") }
            .thenApply {
                patrick.onlineUsers().thenApply { assertThat(it, equalTo(listOf("spongebob","patrick"))) }
                patrick.logout()
            }
            .thenApply {
                spongebob.onlineUsers().thenApply { assertThat(it, equalTo(listOf("patrick"))) }
                spongebob.logout()
            }.join()
    }

    @Test
    fun `calling inbox by offline user throws exception`() {
        val spongebob = clientFactory.get("spongebob", "pass").join()
        assertThrows<PermissionException> {
            spongebob.inbox().join()
        }
    }

    @Test
    fun `sending a message by offline user throws exception`() {
        val spongebob = clientFactory.get("spongebob", "pass").join()
        assertThrows<PermissionException> {
            spongebob.sendMessage("patrick","What's up Pat")
        }
    }

    @Test
    fun `sending a long message throws exception`() {
        val msg = "S".repeat(121)

        val spongebob = clientFactory.get("spongebob", "pass").join()
        spongebob.login("pass").join()
        val thrown = assertThrows<IllegalArgumentException> {
            spongebob.sendMessage("patrick",msg)
        }
    }

    @Test
    fun `sending a message to a non-existent user throws exception`() {
        val spongebob = clientFactory.get("spongebob", "pass").join()
        spongebob.login("pass").join()
        val thrown = assertThrows<CompletionException> {
            spongebob.sendMessage("patrick","What's up Pat").join()
        }
        assertThat(thrown.cause!!, isA<IllegalArgumentException>())
    }



    @Test
    fun `A user can send a message to another user`() {
        val msg = "Hello, Patrick!"

        val spongebob = clientFactory.get("spongebob", "pass").join()
        val patrick = clientFactory.get("patrick", "pass").join()

        spongebob.login("pass")
            .thenCompose { patrick.login("pass") }
            .thenCompose { spongebob.sendMessage("patrick", msg) }
            .thenCompose { patrick.inbox() }
            .thenApply { inbox ->
                assertThat(inbox.size, equalTo(1))
                assertThat(inbox["spongebob"]!![0].message, equalTo(msg))
            }
            .join()
    }

    @Test
    fun `users can send messages to each other`() {
        val msg1 = "Hello, Patrick!"
        val msg2 = "How u doin pat?"
        val msg3 = "Great thanks! What's up?"

        val spongebob = clientFactory.get("spongebob", "pass").join()
        val patrick = clientFactory.get("patrick", "pass").join()

        spongebob.login("pass")
            .thenCompose { patrick.login("pass") }
            .thenCompose { spongebob.sendMessage("patrick", msg1) }
            .thenCompose { patrick.inbox() }
            .thenAccept { inbox ->
                assertThat(inbox.size, equalTo(1))
                assertThat(inbox["spongebob"]!![0].message, equalTo(msg1))
            }.join()

        spongebob.sendMessage("patrick", msg2)
            .thenCompose { patrick.inbox() }
            .thenAccept { inbox ->
                assertThat(inbox["spongebob"]!!.size, equalTo(2))
                assertThat(inbox["spongebob"]!![1].message, equalTo(msg2))
            }.join()

        patrick.sendMessage("spongebob", msg3)
            .thenCompose { spongebob.inbox() }
            .thenAccept { inbox ->
                assertThat(inbox.size, equalTo(1))
                assertThat(inbox["patrick"]!![0].message, equalTo(msg3))
            }
            .join()
    }

    @Test
    fun `deleteMessage throws an exception when user not logged in`() {
        val msg = "Hello, Patrick!"

        val spongebob = clientFactory.get("spongebob", "pass").join()
        val patrick = clientFactory.get("patrick", "pass").join()

        val thrown = assertThrows<CompletionException> {
            spongebob.login("pass")
                .thenCompose { patrick.login("pass") }
                .thenCompose { spongebob.sendMessage("patrick", msg) }
                .thenCompose { patrick.logout() }
                .thenCompose { patrick.deleteMessage("no-id") }
                .join()
        }
        assertThat(thrown.cause!!, isA<PermissionException>())
    }

    @Test
    fun `deleteMessage throws an exception for non-existing msg id`() {
        val msg = "Hello, Patrick!"
        val spongebob = clientFactory.get("spongebob", "pass").join()
        val patrick = clientFactory.get("patrick", "pass").join()

        val thrown = assertThrows<CompletionException> {
            spongebob.login("pass")
                .thenCompose { patrick.login("pass") }
                .thenCompose { spongebob.sendMessage("patrick", msg) }
                .thenCompose { patrick.deleteMessage("no-id") }
                .join()
        }
        assertThat(thrown.cause!!, isA<IllegalArgumentException>())
    }

    @Test
    fun `deleteMessage deletes message successfully`() {
        val msg = "Hello, Patrick!"
        val spongebob = clientFactory.get("spongebob", "pass").join()
        val patrick = clientFactory.get("patrick", "pass").join()


        spongebob.login("pass")
            .thenCompose { patrick.login("pass") }
            .thenCompose { spongebob.sendMessage("patrick", msg) }
            .thenCompose { patrick.inbox().thenCompose { inbox -> patrick.deleteMessage(inbox["spongebob"]!!.first().id) } }
            .thenCompose { patrick.inbox().thenApply { inbox -> assertTrue(inbox["spongebob"]!!.isEmpty()) } }
            .join()
    }

    @Test
    fun `messages are ordered by time sent`() {
        val spongebob = clientFactory.get("spongebob", "pass").join()
        val patrick = clientFactory.get("patrick", "pass").join()

        val msg1 = "hi patrick"
        val msg2 = "how are you?"
        val msg3 = "are you going to mr.crab's party?"

        spongebob.login("pass")
            .thenCompose { patrick.login("pass") }
            .join()

        spongebob.sendMessage("patrick",msg1).join()
        sleep(1000)
        spongebob.sendMessage("patrick",msg2).join()
        sleep(1000)
        spongebob.sendMessage("patrick",msg3).join()

        patrick.inbox().thenCompose { inbox ->
            assertThat(inbox["spongebob"]!![0].message, equalTo(msg1))
            patrick.deleteMessage(inbox["spongebob"]!![0].id)
        }.join()

        patrick.inbox().thenApply { inbox ->
            assertThat(inbox["spongebob"]!![1].message, equalTo(msg3))
        }.join()
    }

    @Test
    fun `MessagingClientFactory restart logs off all users `() {
        val spongebob = clientFactory.get("spongebob", "pass").join()
        val patrick = clientFactory.get("patrick", "pass").join()
        val squidward = clientFactory.get("squidward", "pass").join()
        spongebob.login("pass")
            .thenCompose { patrick.login("pass") }
            .thenCompose { squidward.login("pass") }

        val newClientFac = injector.getInstance<MessagingClientFactory>()
        val newSponge = newClientFac.get("spongebob","pass").join()
        newSponge.login("pass").join()
        assertThat(newSponge.onlineUsers().join(), equalTo(listOf("spongebob")))
    }

    @Test
    fun `inboxes persistency test `() {
        val msg1 = "patrick what are we eating today"
        val msg2 = "ayo squid me and pat going to get some food wanna join us?"
        val msg3 = "oh god, no."

        val spongebob = clientFactory.get("spongebob", "pass").join()
        val patrick = clientFactory.get("patrick", "pass").join()
        val squidward = clientFactory.get("squidward", "pass").join()
        spongebob.login("pass")
            .thenCompose { patrick.login("pass") }
            .thenCompose { squidward.login("pass") }
            .thenCompose { spongebob.sendMessage("patrick", msg1) }
            .thenCompose { spongebob.sendMessage("squidward", msg2) }
            .thenCompose { squidward.sendMessage("spongebob", msg3) }
            .join()

        val newClientFac = injector.getInstance<MessagingClientFactory>()

        spongebob.login("pass")
            .thenCompose { patrick.login("pass") }
            .thenCompose { squidward.login("pass") }.join()

        patrick.inbox()
            .thenApply { inbox ->
                assertThat(inbox["spongebob"]!![0].message, equalTo(msg1))
            }.join()

        squidward.inbox()
            .thenApply { inbox ->
                assertThat(inbox["spongebob"]!![0].message, equalTo(msg2))
            }.join()

        spongebob.inbox()
            .thenApply { inbox ->
                assertThat(inbox["squidward"]!![0].message, equalTo(msg3))
            }.join()
    }

    @Test
    fun `calling get twice on user makes him offline`() {
        val spongebob = clientFactory.get("spongebob", "pass").join()
        spongebob.login("pass")
            .thenCompose { clientFactory.get("spongebob","pass") }
            .thenCompose { spongebob.onlineUsers() }
            .thenAccept { list -> assertThat(list, isEmpty) }
    }


}