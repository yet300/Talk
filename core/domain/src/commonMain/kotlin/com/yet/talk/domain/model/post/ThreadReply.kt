package com.yet.talk.domain.model.post

/** A reply inside a thread; [depth] 0 is a direct reply to the opened post. */
data class ThreadReply(val post: Post, val depth: Int)
