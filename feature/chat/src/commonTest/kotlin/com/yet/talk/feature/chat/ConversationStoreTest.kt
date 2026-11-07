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
import com.yet.talk.domain.repository.ModerationRepository
import com.yet.talk.domain.model.chat.Disappearing
import com.yet.talk.domain.model.chat.SafetyCode
import com.yet.talk.domain.model.media.ImageFile
import com.yet.talk.domain.model.moderation.Moderation
import com.yet.talk.domain.model.moderation.ReportReason
import com.yet.talk.domain.model.moderation.ReportTarget
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import kotlinx.coroutines.launch
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
        val chat = Chat(id, emptyList(), null, null, false, false, Instant.fromEpochSeconds(0), null, 0, isRequest = true, isMuted = false, disappearing = null)
        override fun chats(): Flow<List<Chat>> = flowOf(listOf(chat))
        override fun chat(id: ChatId): Flow<Chat?> = flowOf(chat)
        override fun unreadCount(): Flow<Int> = flowOf(0)
        override fun messages(id: ChatId): Flow<List<ChatMessage>> = messages
        override suspend fun canReceive(id: ChatId) = Result.success(receives)
        override suspend fun send(id: ChatId, text: String, replyTo: String?): Result<Unit> {
            val quote = messages.value.firstOrNull { it.id == replyTo }?.let { ChatMessage.Quote(it.id, it.author, it.content, false) }
            messages.value += ChatMessage("m${messages.value.size}", id, UserId("me"), true, text, Instant.fromEpochSeconds(1), PublishState.Pending, replyTo = quote)
            return Result.success(Unit)
        }
        val reacted = mutableListOf<Pair<String, String>>()
        override suspend fun createGroup(id: ChatId, name: String?) = Result.success(Unit)
        override suspend fun sendImage(id: ChatId, file: ImageFile) = Result.success(Unit)
        override suspend fun image(messageId: String) = Result.success(byteArrayOf(1, 2, 3))
        override suspend fun react(messageId: String, emoji: String) = Result.success(Unit).also { reacted += messageId to emoji }
        override suspend fun deleteForEveryone(messageId: String) = Result.success(Unit).also { messages.value = messages.value.filterNot { it.id == messageId } }
        override suspend fun deleteForMe(messageId: String) { messages.value = messages.value.filterNot { it.id == messageId } }
        override suspend fun setDisappearing(id: ChatId, disappearing: Disappearing) = Unit
        override suspend fun setMuted(id: ChatId, muted: Boolean) = Unit
        override suspend fun leave(id: ChatId) = Unit
        override suspend fun safetyCode(person: UserId) = Result.success(SafetyCode(listOf("00000"), "x"))
        override fun verified(): Flow<Map<UserId, Instant>> = flowOf(emptyMap())
        override suspend fun setVerified(person: UserId, verified: Boolean) = Unit
        override suspend fun verifyScanned(person: UserId, payload: String) = Result.success(false)
        override suspend fun eraseAll() = Unit
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

    private object FakeModeration : ModerationRepository {
        val blocked = mutableListOf<UserId>()
        override val moderation: Flow<Moderation> = flowOf(Moderation())
        override suspend fun refresh() = Result.success(Unit)
        override suspend fun report(target: ReportTarget, reason: ReportReason) = Result.success(Unit)
        override suspend fun block(id: UserId) = Result.success(Unit).also { blocked += id }
        override suspend fun unblock(id: UserId) = Result.success(Unit)
    }

    private fun store(chats: ChatRepository) = ConversationStoreFactory(DefaultStoreFactory(), chats, FakeProfiles, FakeModeration).create(id)

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
    fun aReplyQuotesItsMessageAndReactionsGoOut() = runTest {
        val chats = FakeChats()
        val store = store(chats)
        store.accept(ConversationStore.Intent.ChangeText("first"))
        store.accept(ConversationStore.Intent.Send)
        val first = store.state.messages.single()
        store.accept(ConversationStore.Intent.Reply(first))
        store.accept(ConversationStore.Intent.ChangeText("second"))
        store.accept(ConversationStore.Intent.Send)
        assertEquals("first", store.state.messages.last().replyTo?.text)
        assertEquals(null, store.state.replyingTo, "sending clears the reply")

        store.accept(ConversationStore.Intent.React(first, "👍"))
        assertEquals(listOf(first.id to "👍"), chats.reacted)
        store.accept(ConversationStore.Intent.DeleteForMe(first))
        assertEquals(listOf("second"), store.state.messages.map { it.content })
    }

    @Test
    fun blockingARequestClosesIt() = runTest {
        val store = store(FakeChats())
        val labels = mutableListOf<ConversationStore.Label>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { store.labels.collect { labels += it } }
        store.accept(ConversationStore.Intent.Block)
        assertEquals(listOf(alice), FakeModeration.blocked.takeLast(1))
        assertEquals(listOf<ConversationStore.Label>(ConversationStore.Label.Closed), labels)
    }

    @Test
    fun someoneWithoutInboxRelaysCannotBeWritten() = runTest {
        val store = store(FakeChats(receives = false))
        assertFalse(store.state.canReceive!!)
    }
}
