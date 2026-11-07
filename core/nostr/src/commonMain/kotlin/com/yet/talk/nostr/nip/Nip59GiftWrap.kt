package com.yet.talk.nostr.nip

import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventHash
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.event.EventVerifier
import com.yet.talk.nostr.key.SecretKey
import com.yet.talk.nostr.message.NostrJson
import com.yet.talk.nostr.signer.LocalKeySigner
import com.yet.talk.nostr.signer.Signer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.random.Random

/** An unsigned event (NIP-59): its id is computed, but it carries no signature, so it proves nothing if leaked. */
@Serializable
data class Rumor(
    val id: String,
    val pubkey: String,
    @SerialName("created_at") val createdAt: Long,
    val kind: Int,
    val tags: List<List<String>>,
    val content: String,
) {
    fun toJson(): String = NostrJson.encodeToString(serializer(), this)

    companion object {
        fun fromJson(json: String): Rumor = NostrJson.decodeFromString(serializer(), json)

        fun of(pubkey: String, template: EventTemplate) = Rumor(
            EventHash.id(pubkey, template.createdAt, template.kind, template.tags, template.content),
            pubkey, template.createdAt, template.kind, template.tags, template.content,
        )
    }
}

/**
 * NIP-59: a rumor sealed (kind 13, signed by the author, encrypted to the recipient) inside a gift wrap (kind 1059,
 * signed by a throwaway key, encrypted again). Relays see only the recipient (`p`) and a random time.
 */
object Nip59 {
    /** Seal and wrap timestamps go up to two days back, so they do not reveal when a message was written. */
    const val MAX_TIME_SHIFT = 2 * 24 * 60 * 60L

    suspend fun wrap(rumor: Rumor, author: Signer, recipient: String, now: Long, extraTags: List<List<String>> = emptyList()): Event {
        val seal = author.sign(
            EventTemplate(Kinds.SEAL, author.nip44Encrypt(NostrJson.encodeToString(Rumor.serializer(), rumor), recipient), emptyList(), randomPast(now)),
        )
        val throwaway = LocalKeySigner(SecretKey.generate())
        return throwaway.sign(
            EventTemplate(
                kind = Kinds.GIFT_WRAP,
                content = throwaway.nip44Encrypt(seal.toJson(), recipient),
                tags = listOf(listOf("p", recipient)) + extraTags,
                createdAt = randomPast(now),
            ),
        )
    }

    /**
     * The rumor inside a gift wrap addressed to [recipient]'s key, or null when it is not for them, is malformed,
     * or its seal is forged (bad signature, or a seal key that is not the rumor's author).
     */
    suspend fun unwrap(wrap: Event, recipient: Signer): Rumor? = runCatching {
        if (wrap.kind != Kinds.GIFT_WRAP) return null
        val seal = Event.fromJson(recipient.nip44Decrypt(wrap.content, wrap.pubkey))
        if (seal.kind != Kinds.SEAL || !EventVerifier.verify(seal)) return null
        val rumor = NostrJson.decodeFromString(Rumor.serializer(), recipient.nip44Decrypt(seal.content, seal.pubkey))
        rumor.takeIf { it.pubkey == seal.pubkey && it.id == EventHash.id(it.pubkey, it.createdAt, it.kind, it.tags, it.content) }
    }.getOrNull()

    private fun randomPast(now: Long) = now - Random.nextLong(0, MAX_TIME_SHIFT)
}
