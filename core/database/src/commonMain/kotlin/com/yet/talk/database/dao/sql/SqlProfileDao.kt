package com.yet.talk.database.dao.sql

import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.ProfileDao
import com.yet.talk.database.model.ProfileRecord
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Inject
internal class SqlProfileDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : ProfileDao {

    override suspend fun upsert(profile: ProfileRecord) = withContext(dispatchers.io) {
        val queries = holder.get().profileQueries
        holder.get().transaction {
            with(profile) {
                queries.insertIfAbsent(pubkey, name, displayName, about, picture, banner, website, nip05, updatedAt, githubLogin, githubProof)
                queries.updateIfNewer(name, displayName, about, picture, banner, website, nip05, updatedAt, githubLogin, githubProof, pubkey)
            }
        }
    }

    override fun observe(pubkey: String): Flow<ProfileRecord?> =
        holder.observeOneOrNull(dispatchers.io) { profileQueries.selectByPubkey(pubkey, ::toRecord) }

    override suspend fun setGitHubCheck(pubkey: String, login: String, proof: String, verified: Boolean, checkedAt: Long) =
        withContext(dispatchers.io) {
            holder.get().profileQueries.setGitHubCheck(if (verified) 1 else 0, checkedAt, pubkey, login, proof)
            Unit
        }

    override fun observeSearch(text: String, limit: Int): Flow<List<ProfileRecord>> =
        holder.observeList(dispatchers.io) { profileQueries.searchProfiles(likePattern(text), limit.toLong(), ::toRecord) }
}

@Suppress("LongParameterList")
private fun toRecord(
    pubkey: String, name: String?, displayName: String?, about: String?, picture: String?, website: String?, nip05: String?,
    updatedAt: Long, banner: String?, githubLogin: String?, githubProof: String?, githubVerified: Long?, githubCheckedAt: Long?,
) = ProfileRecord(
    pubkey, name, displayName, about, picture, website, nip05, updatedAt, banner,
    githubLogin, githubProof, githubVerified?.let { it != 0L }, githubCheckedAt,
)
