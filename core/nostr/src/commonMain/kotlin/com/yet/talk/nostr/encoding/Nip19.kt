package com.yet.talk.nostr.encoding

import com.yet.talk.nostr.key.SecretKey

/** NIP-19 bare entities: `npub`, `nsec` and `note`. Values are for display and input only, never for events. */
object Nip19 {

    sealed interface Entity {
        data class PublicKey(val hex: String) : Entity

        class PrivateKey(val secretKey: SecretKey) : Entity

        data class Note(val eventId: String) : Entity
    }

    fun npub(publicKeyHex: String): String = Bech32.encode("npub", publicKeyHex.hexToBytes())

    fun nsec(secretKey: SecretKey): String = Bech32.encode("nsec", secretKey.bytes)

    fun note(eventIdHex: String): String = Bech32.encode("note", eventIdHex.hexToBytes())

    /** Null for anything that is not a well-formed `npub`, `nsec` or `note`. */
    fun decode(value: String): Entity? {
        val (hrp, data) = Bech32.decode(value.trim().removePrefix("nostr:")) ?: return null
        if (data.size != 32) return null
        return when (hrp) {
            "npub" -> Entity.PublicKey(data.toHex())
            "nsec" -> SecretKey.fromBytes(data)?.let(Entity::PrivateKey)
            "note" -> Entity.Note(data.toHex())
            else -> null
        }
    }
}
