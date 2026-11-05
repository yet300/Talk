package com.yet.talk.domain.repository

import com.yet.talk.domain.model.post.PostId

/** Portable references to Talk content that any Nostr client opens (NIP-19). */
interface ShareLinks {
    /** `note1…` for a post. */
    fun post(id: PostId): String
}
