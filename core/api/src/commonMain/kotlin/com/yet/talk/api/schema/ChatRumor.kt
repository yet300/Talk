package com.yet.talk.api.schema

import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.nip.Rumor
import com.yet.talk.nostr.nip.firstValue
import com.yet.talk.nostr.nip.values
import com.yet.talk.nostr.nip.withName

/** What an encrypted chat file needs besides its URL (NIP-17 kind 15 tags). */
data class ChatFileMeta(
    val mimeType: String,
    /** Hex AES-256 key and 12-byte nonce. */
    val key: String,
    val nonce: String,
    /** SHA-256 of the ciphertext (the Blossom blob) and of the original file. */
    val hash: String,
    val originalHash: String,
    val size: Long,
) {
    companion object {
        const val AES_GCM = "aes-gcm"
    }
}

/** A chat rumor (NIP-17) as Talk understands it; anything else inside a wrap is ignored. */
sealed interface ChatRumor {
    val rumor: Rumor

    /** Everyone tagged plus the author: the chat is this set minus the viewer. */
    val participants: List<String> get() = (rumor.tags.values("p") + rumor.pubkey).distinct()

    /** NIP-40, carried inside the rumor so every member's device deletes it. */
    val expiresAt: Long? get() = rumor.tags.firstValue("expiration")?.toLongOrNull()

    sealed interface Message : ChatRumor {
        val replyTo: String? get() = rumor.tags.withName("e").firstOrNull()?.getOrNull(1)
        val subject: String? get() = rumor.tags.firstValue("subject")
    }

    data class Text(override val rumor: Rumor) : Message

    data class File(override val rumor: Rumor, val url: String, val meta: ChatFileMeta) : Message

    data class Reaction(override val rumor: Rumor, val target: String, val emoji: String) : ChatRumor

    data class Deletion(override val rumor: Rumor, val targets: List<String>) : ChatRumor

    companion object {
        fun parse(rumor: Rumor): ChatRumor? = when (rumor.kind) {
            Kinds.CHAT_MESSAGE -> Text(rumor)
            Kinds.CHAT_FILE -> file(rumor)
            Kinds.REACTION -> rumor.tags.values("e").lastOrNull()?.let { target ->
                Reaction(rumor, target, rumor.content.ifBlank { "+" }.take(MAX_REACTION_LENGTH))
            }
            Kinds.DELETION -> rumor.tags.values("e").takeIf { it.isNotEmpty() }?.let { Deletion(rumor, it) }
            else -> null
        }

        private fun file(rumor: Rumor): File? {
            val tags = rumor.tags
            if (tags.firstValue("encryption-algorithm") != ChatFileMeta.AES_GCM) return null
            val url = rumor.content.takeIf { it.startsWith("https://") } ?: return null
            val meta = ChatFileMeta(
                mimeType = tags.firstValue("file-type") ?: return null,
                key = tags.firstValue("decryption-key")?.takeIf { it.length == 64 } ?: return null,
                nonce = tags.firstValue("decryption-nonce") ?: return null,
                hash = tags.firstValue("x") ?: return null,
                originalHash = tags.firstValue("ox").orEmpty(),
                size = tags.firstValue("size")?.toLongOrNull() ?: 0,
            )
            return File(rumor, url, meta)
        }

        private const val MAX_REACTION_LENGTH = 16
    }
}
