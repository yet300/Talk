package com.yet.talk.feature.auth

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.yet.talk.domain.model.account.Account
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.feature.auth.mapper.stateToModel
import com.yet.talk.feature.auth.store.AuthStore
import com.yet.talk.feature.auth.store.AuthStoreFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

class AuthStoreTest {

    private class FakeAccounts : AccountRepository {
        val imported = mutableListOf<String>()
        var created = 0
        var gate: CompletableDeferred<Unit>? = null
        private val me = Account(UserId("me"), "npub1me")
        override val account: Flow<Account?> = MutableStateFlow(null)

        override suspend fun createAccount(): Result<Account> {
            created++
            gate?.await()
            return Result.success(me)
        }

        override suspend fun importKey(secretKey: String): Result<Account> {
            imported += secretKey
            return if (secretKey.startsWith("nsec1")) Result.success(me) else Result.failure(DomainError.InvalidKey)
        }

        override suspend fun exportSecretKey() = Result.success("nsec1")
        override suspend fun signOut() = Unit
    }

    private val accounts = FakeAccounts()
    private val store by lazy { AuthStoreFactory(DefaultStoreFactory(), accounts).create() }

    @BeforeTest
    fun setUp() {
        isAssertOnMainThreadEnabled = false
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun importsThePastedKeyAndForgetsIt() = runTest {
        store.accept(AuthStore.Intent.ChangeKey("nsec1abc"))
        assertTrue(stateToModel(store.state).canImport)

        store.accept(AuthStore.Intent.ImportKey)

        assertEquals(listOf("nsec1abc"), accounts.imported)
        assertEquals("", store.state.key, "the key does not stay in memory")
        assertFalse(store.state.isWorking)
    }

    @Test
    fun aBadKeyStaysForCorrectionWithAnError() = runTest {
        store.accept(AuthStore.Intent.ChangeKey("npub1nope"))
        store.accept(AuthStore.Intent.ImportKey)

        assertEquals(DomainError.InvalidKey, store.state.error)
        assertEquals("npub1nope", store.state.key)

        store.accept(AuthStore.Intent.ChangeKey("nsec1"))
        assertEquals(null, store.state.error, "editing the key clears the error")
    }

    @Test
    fun ignoresRepeatedTapsWhileWorking() = runTest {
        accounts.gate = CompletableDeferred()
        store.accept(AuthStore.Intent.CreateKey)
        store.accept(AuthStore.Intent.CreateKey)
        assertTrue(store.state.isWorking)

        accounts.gate!!.complete(Unit)
        assertEquals(1, accounts.created)
        assertFalse(store.state.isWorking)
    }
}
