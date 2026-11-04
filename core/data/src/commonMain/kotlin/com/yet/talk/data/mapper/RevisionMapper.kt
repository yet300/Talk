package com.yet.talk.data.mapper

import com.yet.talk.database.model.RevisionRecord
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.post.Revision
import kotlin.time.Instant

internal fun RevisionRecord.toRevision() = Revision(PostId(postId), number, content, Instant.fromEpochSeconds(createdAt))
