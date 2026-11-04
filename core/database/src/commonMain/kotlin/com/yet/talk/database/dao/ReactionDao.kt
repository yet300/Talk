package com.yet.talk.database.dao

import com.yet.talk.database.model.ReactionRecord

interface ReactionDao {
    /** False when it already exists or was deleted. */
    suspend fun insert(reaction: ReactionRecord): Boolean

    /** Event ids of [user]'s reactions of [type] on [postId], to delete them. */
    suspend fun reactionIds(postId: String, user: String, type: String): List<String>

    /** Deletes [user]'s reactions among [eventIds] and remembers them as deleted. */
    suspend fun delete(eventIds: Collection<String>, user: String)
}
