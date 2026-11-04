package com.yet.talk.domain.model.post

import kotlin.jvm.JvmInline

/** A post's id: the id of its original event, stable across revisions. */
@JvmInline
value class PostId(val value: String)
