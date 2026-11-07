package com.yet.talk.feature.settings

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.yet.talk.domain.model.account.Account
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.relay.Relay
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.domain.repository.RelayRepository
import com.yet.talk.domain.repository.PreferencesRepository
import com.yet.talk.domain.repository.ProfileRepository
import com.yet.talk.domain.model.settings.ThemeMode
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserStats
import com.yet.talk.domain.model.user.ProfileEdit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import com.yet.talk.feature.settings.store.SettingsStore
import com.yet.talk.feature.settings.store.SettingsStoreFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

import com.yet.talk.domain.model.account.AppLock
import com.yet.talk.domain.model.chat.Chat
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.chat.ChatMessage
import com.yet.talk.domain.model.chat.Disappearing
import com.yet.talk.domain.model.media.ImageFile
import com.yet.talk.domain.repository.ChatRepository
import kotlin.time.Instant

class SettingsStoreTest {

    private class FakeRelays : RelayRepository {
        override val relays = MutableStateFlow(listOf(Relay("wss://a.example")))
        override suspend fun setRelays(relays: List<Relay>): Result<Unit> {
            this.relays.value = relays
            return Result.success(Unit)
        }
    }

    private class FakeAccounts : AccountRepository {
        var signedOut = false
        override val account = MutableStateFlow<Account?>(Account(UserId("me"), "npub1me"))
        override suspend fun generateKey() = error("unused")
        override suspend fun importKey(secretKey: String) = error("unused")
        override suspend fun exportSecretKey() = Result.success("nsec1secret")
        override suspend fun signOut() {
            signedOut = true
            account.value = null
        }
        var passcode: String? = null
        override val lock = MutableStateFlow(AppLock(passcodeSupported = true))
        override suspend fun unlock(passcode: String) = error("unused")
        override suspend fun setPasscode(passcode: String) = Result.success(Unit).also {
            this.passcode = passcode
            lock.value = lock.value.copy(enabled = true)
        }
        override suspend fun removePasscode() = Result.success(Unit).also { lock.value = lock.value.copy(enabled = false) }
    }

    /** Only Erase chats is reached from Settings. */
    private class FakeChats : ChatRepository {
        var erased = false
        override suspend fun eraseAll() { erased = true }
        override suspend fun prepare() = error("unused")
        override fun chats(): Flow<List<Chat>> = error("unused")
        override fun unreadCount(): Flow<Int> = error("unused")
        override fun chat(id: ChatId): Flow<Chat?> = error("unused")
        override fun messages(id: ChatId): Flow<List<ChatMessage>> = error("unused")
        override suspend fun canReceive(id: ChatId) = error("unused")
        override suspend fun createGroup(id: ChatId, name: String?) = error("unused")
        override suspend fun send(id: ChatId, text: String, replyTo: String?) = error("unused")
        override suspend fun sendImage(id: ChatId, file: ImageFile) = error("unused")
        override suspend fun image(messageId: String) = error("unused")
        override suspend fun retry(messageId: String) = error("unused")
        override suspend fun react(messageId: String, emoji: String) = error("unused")
        override suspend fun deleteForEveryone(messageId: String) = error("unused")
        override suspend fun deleteForMe(messageId: String) = error("unused")
        override suspend fun setDisappearing(id: ChatId, disappearing: Disappearing) = error("unused")
        override suspend fun setMuted(id: ChatId, muted: Boolean) = error("unused")
        override suspend fun leave(id: ChatId) = error("unused")
        override suspend fun safetyCode(person: UserId) = error("unused")
        override fun verified(): Flow<Map<UserId, Instant>> = error("unused")
        override suspend fun setVerified(person: UserId, verified: Boolean) = error("unused")
        override suspend fun verifyScanned(person: UserId, payload: String) = error("unused")
        override suspend fun markRead(id: ChatId) = error("unused")
        override suspend fun accept(id: ChatId) = error("unused")
        override suspend fun delete(id: ChatId) = error("unused")
    }

    private class FakeProfiles : ProfileRepository {
        override fun observeUser(id: UserId): Flow<User> = flowOf(User(id, "npub1me", name = "Me"))
        override fun observeStats(id: UserId): Flow<UserStats> = flowOf(UserStats())
        override suspend fun refresh(id: UserId) = Result.success(Unit)
        override suspend fun updateMyProfile(edit: ProfileEdit) = Result.success(Unit)
        override suspend fun verifyNip05(id: UserId, identifier: String) = Result.success(false)
    }

    private class FakePreferences : PreferencesRepository {
        override val theme = MutableStateFlow(ThemeMode.System)
        override suspend fun setTheme(mode: ThemeMode) {
            theme.value = mode
        }
        override val channelNickname = MutableStateFlow("anon")
        override suspend fun setChannelNickname(nickname: String) = Unit
        override val channelIntroSeen = MutableStateFlow(false)
        override suspend fun setChannelIntroSeen() = Unit
    }

    private val preferences = FakePreferences()
    private val relays = FakeRelays()
    private val accounts = FakeAccounts()
    private val chats = FakeChats()
    private val store by lazy { SettingsStoreFactory(DefaultStoreFactory(), accounts, relays, FakeProfiles(), preferences, chats, object : com.yet.talk.domain.repository.NetworkRepository {
        override val status = kotlinx.coroutines.flow.flowOf(com.yet.talk.domain.model.network.NetworkStatus())
    }).create() }

    @Test
    fun aPasscodeIsTypedTwiceAndErasingChatsErasesThem() = runTest {
        store.accept(SettingsStore.Intent.SetPasscode("correct horse", "correct hose"))
        assertEquals(DomainError.PasscodesDontMatch, store.state.error)
        assertEquals(null, accounts.passcode)
        store.accept(SettingsStore.Intent.SetPasscode("correct horse", "correct horse"))
        assertEquals("correct horse", accounts.passcode)
        assertEquals(true, store.state.lock.enabled)

        store.accept(SettingsStore.Intent.EraseChats)
        assertEquals(true, chats.erased)
    }

    @BeforeTest
    fun setUp() {
        isAssertOnMainThreadEnabled = false
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun addsANormalizedRelayOnceAndRemovesIt() = runTest {
        store.accept(SettingsStore.Intent.ChangeRelayInput(" WSS://B.example/ "))
        store.accept(SettingsStore.Intent.AddRelay)
        store.accept(SettingsStore.Intent.ChangeRelayInput("wss://b.example"))
        store.accept(SettingsStore.Intent.AddRelay)

        assertEquals(listOf("wss://a.example", "wss://b.example"), store.state.relays.map { it.url })
        assertEquals("", store.state.relayInput)

        store.accept(SettingsStore.Intent.RemoveRelay("wss://a.example"))
        assertEquals(listOf("wss://b.example"), store.state.relays.map { it.url })
    }

    @Test
    fun showsWhoIsSignedInAndStoresTheTheme() = runTest {
        assertEquals("Me", store.state.me?.name)
        store.accept(SettingsStore.Intent.SelectTheme(ThemeMode.Light))
        assertEquals(ThemeMode.Light, preferences.theme.value)
        assertEquals(ThemeMode.Light, store.state.theme)
    }

    @Test
    fun rejectsAnAddressThatIsNotARelay() = runTest {
        store.accept(SettingsStore.Intent.ChangeRelayInput("https://example.com"))
        store.accept(SettingsStore.Intent.AddRelay)

        assertEquals(DomainError.InvalidRelayUrl, store.state.error)
        assertEquals(1, store.state.relays.size)
    }

    @Test
    fun revealsTheKeyUntilHiddenAndSignsOut() = runTest {
        store.accept(SettingsStore.Intent.ExportKey)
        assertEquals("nsec1secret", store.state.exportedKey)
        store.accept(SettingsStore.Intent.HideKey)
        assertEquals(null, store.state.exportedKey)

        store.accept(SettingsStore.Intent.SignOut)
        assertEquals(true, accounts.signedOut)
        assertEquals(null, store.state.account)
    }
}
