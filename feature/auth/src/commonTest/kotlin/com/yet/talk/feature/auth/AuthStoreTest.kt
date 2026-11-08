package com.yet.talk.feature.auth

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.yet.talk.domain.model.account.Account
import com.yet.talk.domain.model.account.AppLock
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.feature.auth.mapper.stateToModel
import com.yet.talk.feature.auth.store.AuthStore
import com.yet.talk.feature.auth.store.AuthStoreFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthStoreTest {

    private class FakeAccounts : AccountRepository {
        val imported = mutableListOf<String>()
        private val me = Account(UserId("me"), "npub1me")
        override val account: Flow<Account?> = MutableStateFlow(null)

        override suspend fun generateKey() = Result.success("nsec1new")

        override suspend fun importKey(secretKey: String): Result<Account> {
            imported += secretKey
            return if (secretKey.startsWith("nsec1")) Result.success(me) else Result.failure(DomainError.InvalidKey)
        }

        override suspend fun exportSecretKey() = Result.success("nsec1")
        override suspend fun signOut() = Unit
        override val lock = MutableStateFlow(AppLock())
        override suspend fun unlock(passcode: String) = Result.success(Unit)
        override suspend fun setPasscode(passcode: String) = Result.success(Unit)
        override suspend fun removePasscode() = Result.success(Unit)
    }

    private val accounts = FakeAccounts()

    private fun TestScope.store(): AuthStore {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return AuthStoreFactory(DefaultStoreFactory(), accounts).create()
    }

    @BeforeTest
    fun setUp() {
        isAssertOnMainThreadEnabled = false
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun importsThePastedKeyAndForgetsIt() = runTest {
        val store = store()
        store.accept(AuthStore.Intent.ChangeKey("nsec1abc"))
        assertTrue(stateToModel(store.state).canImport)

        store.accept(AuthStore.Intent.ImportKey)
        runCurrent()

        assertEquals(listOf("nsec1abc"), accounts.imported)
        assertEquals("", store.state.key, "the key does not stay in memory")
        assertFalse(store.state.isWorking)
    }

    @Test
    fun aBadKeyStaysForCorrectionWithAnError() = runTest {
        val store = store()
        store.accept(AuthStore.Intent.ChangeKey("npub1nope"))
        store.accept(AuthStore.Intent.ImportKey)
        runCurrent()

        assertEquals(DomainError.InvalidKey, store.state.error)
        assertEquals("npub1nope", store.state.key)

        store.accept(AuthStore.Intent.ChangeKey("nsec1"))
        assertNull(store.state.error, "editing the key clears the error")
    }

    @Test
    fun aNewKeyIsShownOnceAndSignsInOnlyAfterItIsSaved() = runTest {
        val store = store()
        store.accept(AuthStore.Intent.CreateKey)
        runCurrent()

        val shown = stateToModel(store.state)
        assertEquals("nsec1new", shown.newKey)
        assertFalse(shown.canConfirmSaved, "I saved it waits for Copy or 3 seconds")
        store.accept(AuthStore.Intent.NewKeySaved)
        runCurrent()
        assertEquals(emptyList<String>(), accounts.imported, "not signed in before the key is saved")

        advanceTimeBy(3_001)
        assertTrue(stateToModel(store.state).canConfirmSaved)
        store.accept(AuthStore.Intent.NewKeySaved)
        runCurrent()

        assertEquals(listOf("nsec1new"), accounts.imported)
        assertNull(store.state.newKey, "the key leaves memory once used")
    }

    @Test
    fun copyingTheNewKeyUnlocksSavingAtOnce() = runTest {
        val store = store()
        store.accept(AuthStore.Intent.CreateKey)
        runCurrent()
        store.accept(AuthStore.Intent.NewKeyCopied)

        assertTrue(stateToModel(store.state).canConfirmSaved)
        assertTrue(store.state.newKeyCopied)
    }
}
