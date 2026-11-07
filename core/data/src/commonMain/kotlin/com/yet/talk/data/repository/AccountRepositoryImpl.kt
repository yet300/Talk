package com.yet.talk.data.repository

import com.yet.talk.data.session.Session
import com.yet.talk.data.sync.RemoteSync
import com.yet.talk.data.sync.remote
import com.yet.talk.database.dao.MaintenanceDao
import com.yet.talk.domain.model.account.Account
import com.yet.talk.domain.model.account.AppLock
import com.yet.talk.nostr.nip.Nip49
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.nostr.encoding.Nip19
import com.yet.talk.nostr.key.SecretKey
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Inject
internal class AccountRepositoryImpl(
    private val session: Session,
    private val maintenance: MaintenanceDao,
    private val sync: RemoteSync,
    private val scope: CoroutineScope,
) : AccountRepository {

    override val account: Flow<Account?> =
        session.viewer.map { pubkey -> pubkey.takeIf { it.isNotEmpty() }?.let { Account(UserId(it), Nip19.npub(it)) } }

    override suspend fun generateKey(): Result<String> = Result.success(Nip19.nsec(SecretKey.generate()))

    override suspend fun importKey(secretKey: String): Result<Account> {
        val input = secretKey.trim()
        val key = (Nip19.decode(input) as? Nip19.Entity.PrivateKey)?.secretKey ?: SecretKey.fromHex(input)
        return key?.let { Result.success(switchTo(it)) } ?: Result.failure(DomainError.InvalidKey)
    }

    override suspend fun exportSecretKey(): Result<String> =
        session.secretKey()?.let { Result.success(Nip19.nsec(it)) } ?: Result.failure(DomainError.NotSignedIn)

    override suspend fun signOut() {
        session.signOut()
        maintenance.clearUserData()
    }

    override val lock: Flow<AppLock> =
        session.lock.map { AppLock(passcodeSupported = Nip49.isSupported, enabled = it.enabled, locked = it.locked) }

    override suspend fun unlock(passcode: String): Result<Unit> =
        if (session.unlock(passcode)) Result.success(Unit) else Result.failure(DomainError.WrongPasscode)

    override suspend fun setPasscode(passcode: String): Result<Unit> = when {
        !Nip49.isSupported -> Result.failure(UnsupportedOperationException("The app lock uses the device lock here"))
        passcode.length < AccountRepository.MIN_PASSCODE_LENGTH -> Result.failure(DomainError.PasscodeTooShort(AccountRepository.MIN_PASSCODE_LENGTH))
        else -> runCatching { session.setPasscode(passcode) }
    }

    override suspend fun removePasscode(): Result<Unit> = runCatching { session.removePasscode() }

    private suspend fun switchTo(key: SecretKey): Account {
        // A different identity must not see the previous one's cached lists and drafts.
        if (session.secretKey() != key) maintenance.clearUserData()
        session.signIn(key)
        scope.launch { remote { sync.syncLists(key.publicKey) } }
        return Account(UserId(key.publicKey), Nip19.npub(key.publicKey))
    }
}
