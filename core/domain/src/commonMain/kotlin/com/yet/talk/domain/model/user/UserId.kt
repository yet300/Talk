package com.yet.talk.domain.model.user

import kotlin.jvm.JvmInline

/** A user's public key, hex. */
@JvmInline
value class UserId(val value: String)
