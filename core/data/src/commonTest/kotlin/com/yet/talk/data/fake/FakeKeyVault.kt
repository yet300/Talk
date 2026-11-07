package com.yet.talk.data.fake

import com.yet.talk.data.session.KeyVault

internal class FakeKeyVault : KeyVault {
    var stored: String? = null
    override suspend fun read() = stored
    override suspend fun write(secretKeyHex: String) { stored = secretKeyHex }
    override suspend fun clear() { stored = null; chat = null }
    private var chat: String? = null
    override suspend fun chatKey() = chat ?: "11".repeat(32).also { chat = it }
}
