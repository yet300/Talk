package com.yet.talk.feature.chat

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.yet.talk.domain.model.chat.Chat
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.chat.ChatMessage
import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.domain.model.user.ProfileEdit
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.model.user.UserStats
import com.yet.talk.domain.repository.ChatRepository
import com.yet.talk.domain.repository.ProfileRepository
import com.yet.talk.feature.chat.component.conversation.store.ConversationStore
import com.yet.talk.feature.chat.component.conversation.store.ConversationStoreFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class ConversationStoreTest {

    private val alice = UserId("a".repeat(64))
    private val id = ChatId.of(listOf(alice))

    private inner class FakeChats(val receives: Boolean = true) : ChatRepository {
        val messages = MutableStateFlow(emptyList<ChatMessage>())
        var read = 0
        var accepted = false
        var deleted = false

        override suspend fun prepare() = Result.success(Unit)
        override fun chats(): Flow<List<Chat>> = flowOf(listOf(Chat(id, emptyList(), null, Instant.fromEpochSeconds(0), null, 0, isRequest = true)))
        override fun unreadCount(): Flow<Int> = flowOf(0)
        override fun messages(id: ChatId): Flow<List<ChatMessage>> = messages
        override suspend fun canReceive(id: ChatId) = Result.success(receives)
        override suspend fun send(id: ChatId, text: String): Result<Unit> {
            messages.value += ChatMessage("m${messages.value.size}", id, UserId("me"), true, text, Instant.fromEpochSeconds(1), PublishState.Pending)
            return Result.success(Unit)
        }
        override suspend fun retry(messageId: String) = Result.success(Unit)
        override suspend fun markRead(id: ChatId) { read++ }
        override suspend fun accept(id: ChatId) { accepted = true }
        override suspend fun delete(id: ChatId) { deleted = true }
    }

    private object FakeProfiles : ProfileRepository {
        override fun observeUser(id: UserId): Flow<User> = flowOf(User(id, "npub1alice", name = "alice"))
        override fun observeStats(id: UserId): Flow<UserStats> = error("unused")
        override suspend fun refresh(id: UserId) = Result.success(Unit)
        override suspend fun updateMyProfile(edit: ProfileEdit) = error("unused")
        override suspend fun verifyNip05(id: UserId, identifier: String) = error("unused")
    }

    private fun store(chats: ChatRepository) = ConversationStoreFactory(DefaultStoreFactory(), chats, FakeProfiles).create(id)

    @BeforeTest
    fun setUp() {
        isAssertOnMainThreadEnabled = false
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun sendingClearsTheTextAndOpenMarksRead() = runTest {
        val chats = FakeChats()
        val store = store(chats)
        assertEquals(listOf("alice"), store.state.members.map { it.name })
        assertEquals(true, store.state.canReceive)

        store.accept(ConversationStore.Intent.ChangeText("hi"))
        store.accept(ConversationStore.Intent.Send)

        assertEquals("", store.state.text)
        assertEquals(listOf("hi"), store.state.messages.map { it.content })
        assertTrue(chats.read >= 2)
    }

    @Test
    fun aRequestIsAcceptedOrDeleted() = runTest {
        val chats = FakeChats()
        val store = store(chats)
        assertTrue(store.state.chat!!.isRequest)
        store.accept(ConversationStore.Intent.Accept)
        assertTrue(chats.accepted && store.state.accepted)
        store.accept(ConversationStore.Intent.Delete)
        assertTrue(chats.deleted)
    }

    @Test
    fun someoneWithoutInboxRelaysCannotBeWritten() = runTest {
        val store = store(FakeChats(receives = false))
        assertFalse(store.state.canReceive!!)
    }
}
