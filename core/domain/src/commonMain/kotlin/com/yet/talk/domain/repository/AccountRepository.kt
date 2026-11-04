package com.yet.talk.domain.repository

import com.yet.talk.domain.model.Account
import kotlinx.coroutines.flow.Flow

/** The signed-in identity. One account at a time; the secret key never leaves this boundary except through [exportSecretKey]. */
interface AccountRepository {
    /** Null when signed out. */
    val account: Flow<Account?>

    /** Generates a new key and signs in with it. */
    suspend fun createAccount(): Result<Account>

    /** Signs in with an `nsec` or a hex secret key. Fails with [com.yet.talk.domain.model.DomainError.InvalidKey]. */
    suspend fun importKey(secretKey: String): Result<Account>

    /** The `nsec`, for the user's own backup. */
    suspend fun exportSecretKey(): Result<String>

    /** Forgets the key and clears the user's local data. */
    suspend fun signOut()
}
