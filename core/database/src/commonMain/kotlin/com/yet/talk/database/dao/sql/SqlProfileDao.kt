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
                queries.insertIfAbsent(pubkey, name, displayName, about, picture, website, nip05, updatedAt)
                queries.updateIfNewer(name, displayName, about, picture, website, nip05, updatedAt, pubkey)
            }
        }
    }

    override fun observe(pubkey: String): Flow<ProfileRecord?> =
        holder.observeOneOrNull(dispatchers.io) {
            profileQueries.selectByPubkey(pubkey) { key, name, displayName, about, picture, website, nip05, updatedAt ->
                ProfileRecord(key, name, displayName, about, picture, website, nip05, updatedAt)
            }
        }
}
