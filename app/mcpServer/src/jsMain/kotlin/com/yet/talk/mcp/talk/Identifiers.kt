package com.yet.talk.mcp.talk

import com.yet.talk.mcp.agent.ToolError
import com.yet.talk.nostr.encoding.Nip19
import com.yet.talk.nostr.key.isPublicKeyHex

/** A post id as hex or `note1…`. */
internal fun eventIdOf(value: String): String {
    val text = value.trim()
    if (isPublicKeyHex(text)) return text.lowercase() // the same shape as an event id
    return (Nip19.decode(text) as? Nip19.Entity.Note)?.eventId ?: throw ToolError("Not a post id: expected 64 hex characters or a note1 address")
}

/** A person as hex or `npub1…`. */
internal fun pubkeyOf(value: String): String {
    val text = value.trim()
    if (isPublicKeyHex(text)) return text.lowercase()
    return (Nip19.decode(text) as? Nip19.Entity.PublicKey)?.hex ?: throw ToolError("Not a public key: expected 64 hex characters or an npub1 address")
}
