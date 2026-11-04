package com.yet.talk.data.fake

import com.yet.talk.data.session.KeyVault

class FakeKeyVault : KeyVault {
    var stored: String? = null
    override suspend fun read() = stored
    override suspend fun write(secretKeyHex: String) { stored = secretKeyHex }
    override suspend fun clear() { stored = null }
}
