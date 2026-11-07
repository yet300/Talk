package com.yet.talk.domain.repository

import com.yet.talk.domain.model.moderation.Moderation
import com.yet.talk.domain.model.moderation.ReportReason
import com.yet.talk.domain.model.moderation.ReportTarget
import kotlinx.coroutines.flow.Flow

interface ModerationRepository {
    val moderation: Flow<Moderation>

    /** Fetches the reports made by the viewer's network from relays. */
    suspend fun refresh(): Result<Unit>

    /** Publishes a public NIP-56 report; the reported post is hidden for the viewer at once. */
    suspend fun report(target: ReportTarget, reason: ReportReason): Result<Unit>
}
