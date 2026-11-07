package com.yet.talk.data.fake

import com.yet.talk.data.session.KeyVault

internal class FakeKeyVault : KeyVault {
    var stored: String? = null
    var locked: String? = null
    var sealedChat: String? = null

    override suspend fun read() = stored
    override suspend fun write(secretKeyHex: String) { stored = secretKeyHex; locked = null }
    override suspend fun readLocked() = locked
    override suspend fun writeLocked(ncryptsec: String) { locked = ncryptsec; stored = null }
    override suspend fun clear() { stored = null; locked = null; sealedChat = null }
    override suspend fun sealedChatKey() = sealedChat
    override suspend fun writeSealedChatKey(sealed: String) { sealedChat = sealed }
    override suspend fun takeLegacyChatKey(): String? = null
    override suspend fun clearChatKey() { sealedChat = null }
}
