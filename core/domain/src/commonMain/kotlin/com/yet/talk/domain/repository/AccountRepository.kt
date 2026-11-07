package com.yet.talk.domain.repository

import com.yet.talk.domain.model.account.Account
import com.yet.talk.domain.model.account.AppLock
import kotlinx.coroutines.flow.Flow

/** The signed-in identity. One account at a time; the secret key never leaves this boundary except through [exportSecretKey]. */
interface AccountRepository {
    /** Null when signed out. */
    val account: Flow<Account?>

    /**
     * A new secret key as an `nsec`, not saved and not signed in: the user stores it first ("Save your key"),
     * then signs in with [importKey].
     */
    suspend fun generateKey(): Result<String>

    /** Signs in with an `nsec` or a hex secret key. Fails with [com.yet.talk.domain.model.error.DomainError.InvalidKey]. */
    suspend fun importKey(secretKey: String): Result<Account>

    /** The `nsec`, for the user's own backup. */
    suspend fun exportSecretKey(): Result<String>

    /** Forgets the key and clears the user's local data (also Erase everything). */
    suspend fun signOut()

    /** The app lock. */
    val lock: Flow<AppLock>

    /** Opens the app; fails with [com.yet.talk.domain.model.error.DomainError.WrongPasscode]. */
    suspend fun unlock(passcode: String): Result<Unit>

    /** Turns the passcode lock on (web): the key is then stored only encrypted under [passcode] (NIP-49). */
    suspend fun setPasscode(passcode: String): Result<Unit>

    suspend fun removePasscode(): Result<Unit>

    companion object {
        const val MIN_PASSCODE_LENGTH = 6
    }
}
