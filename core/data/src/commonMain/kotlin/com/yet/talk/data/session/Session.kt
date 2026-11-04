package com.yet.talk.data.session

import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.nostr.key.SecretKey
import com.yet.talk.nostr.signer.Signer
import kotlinx.coroutines.flow.Flow

/** The signed-in key, loaded from the vault on first use. Internal to the data layer. */
interface Session {
    /** The signed-in pubkey, or "" when signed out; the DAOs use "" for "no viewer". */
    val viewer: Flow<String>

    suspend fun signer(): Signer?

    suspend fun requireSigner(): Signer = signer() ?: throw DomainError.NotSignedIn

    suspend fun signIn(key: SecretKey)

    suspend fun signOut()

    suspend fun secretKey(): SecretKey?
}
