package com.yet.talk.domain.model

import kotlin.jvm.JvmInline

/** A user's public key, hex. */
@JvmInline
value class UserId(val value: String)

/** A post's id: the id of its original event, stable across revisions. */
@JvmInline
value class PostId(val value: String)
