package com.yet.talk.nostr.event

import com.yet.talk.nostr.signer.Signer

/** An event before signing. The [Signer] fills in the public key, the id and the signature. */
data class EventTemplate(
    val kind: Int,
    val content: String,
    val tags: List<List<String>> = emptyList(),
    val createdAt: Long,
)
