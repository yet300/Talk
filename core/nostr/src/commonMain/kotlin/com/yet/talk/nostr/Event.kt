package com.yet.talk.nostr

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A signed NIP-01 event, exactly as it travels between clients and relays. Keys, ids and signatures are lowercase hex. */
@Serializable
data class Event(
    val id: String,
    val pubkey: String,
    @SerialName("created_at") val createdAt: Long,
    val kind: Int,
    val tags: List<List<String>>,
    val content: String,
    val sig: String,
) {
    /** The id recomputed from the content; equals [id] for an untampered event. */
    fun computeId(): String = EventHash.id(pubkey, createdAt, kind, tags, content)
}

/** An event before signing. The [Signer] fills in the public key, the id and the signature. */
data class EventTemplate(
    val kind: Int,
    val content: String,
    val tags: List<List<String>> = emptyList(),
    val createdAt: Long,
)
