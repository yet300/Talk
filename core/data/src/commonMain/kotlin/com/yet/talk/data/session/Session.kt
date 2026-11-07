package com.yet.talk.data.session

import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.nostr.key.SecretKey
import com.yet.talk.nostr.signer.Signer
import kotlinx.coroutines.flow.Flow

/** The signed-in key, loaded from the vault on first use. Internal to the data layer. */
internal interface Session {
    /** The signed-in pubkey, or "" when signed out or locked; the DAOs use "" for "no viewer". */
    val viewer: Flow<String>

    /** The app lock: whether it is on, and whether the key is still locked. */
    val lock: Flow<LockState>

    suspend fun signer(): Signer?

    suspend fun requireSigner(): Signer = signer() ?: throw DomainError.NotSignedIn

    suspend fun signIn(key: SecretKey)

    suspend fun signOut()

    suspend fun secretKey(): SecretKey?

    /** Opens a locked key with the passcode; false for a wrong one. */
    suspend fun unlock(passcode: String): Boolean

    /** Keeps the key only as an `ncryptsec` under [passcode] (the web's app lock). */
    suspend fun setPasscode(passcode: String)

    suspend fun removePasscode()

    data class LockState(val enabled: Boolean = false, val locked: Boolean = false)
}
