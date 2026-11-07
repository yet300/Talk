package com.yet.talk.domain.repository

import com.yet.talk.domain.model.moderation.Moderation
import com.yet.talk.domain.model.moderation.ReportReason
import com.yet.talk.domain.model.moderation.ReportTarget
import com.yet.talk.domain.model.user.UserId
import kotlinx.coroutines.flow.Flow

interface ModerationRepository {
    val moderation: Flow<Moderation>

    /** Fetches the reports made by the viewer's network from relays. */
    suspend fun refresh(): Result<Unit>

    /** Publishes a public NIP-56 report; the reported post is hidden for the viewer at once. */
    suspend fun report(target: ReportTarget, reason: ReportReason): Result<Unit>

    /** Blocks [id]: a private entry in the viewer's NIP-51 mute list (kind 10000, encrypted to themselves). */
    suspend fun block(id: UserId): Result<Unit>

    suspend fun unblock(id: UserId): Result<Unit>
}
