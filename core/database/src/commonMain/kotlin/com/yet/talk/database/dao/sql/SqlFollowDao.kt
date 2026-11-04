package com.yet.talk.database.dao.sql

import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.FollowDao
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@ContributesBinding(AppScope::class)
@Inject
class SqlFollowDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : FollowDao {

    override suspend fun replace(follower: String, followees: List<String>, createdAt: Long): Boolean = withContext(dispatchers.io) {
        holder.get().replaceList(follower, KIND_CONTACTS, createdAt) {
            followQueries.deleteFollows(follower)
            followees.distinct().forEach { followQueries.insertFollow(follower, it, createdAt) }
        }
    }

    override suspend fun version(follower: String): Long? = withContext(dispatchers.io) {
        holder.get().listVersion(follower, KIND_CONTACTS)
    }

    override fun observeFollowees(follower: String): Flow<List<String>> =
        holder.observeList(dispatchers.io) { followQueries.selectFollowees(follower) }

    override fun observeIsFollowing(follower: String, followee: String): Flow<Boolean> =
        holder.observeOne(dispatchers.io) { followQueries.isFollowing(follower, followee) }

    override fun observeFollowingCount(follower: String): Flow<Long> =
        holder.observeOne(dispatchers.io) { followQueries.countFollowing(follower) }
}
