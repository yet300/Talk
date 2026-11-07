package com.yet.talk.data.channel

import com.yet.talk.data.session.KeyVault
import com.yet.talk.nostr.crypto.hmacSha256
import com.yet.talk.nostr.key.SecretKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A key per area, as bitchat derives them: HMAC-SHA256(device seed, area code || 32-bit counter, big-endian), the
 * first valid secp256k1 key for counter 0..9. The seed is random and only on this device, never the account key,
 * so nobody can link your places to each other or to your account.
 */
@SingleIn(AppScope::class)
@Inject
@OptIn(ExperimentalStdlibApi::class)
internal class ChannelKeys(private val vault: KeyVault) {
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, SecretKey>()

    suspend fun forArea(geohash: String): SecretKey = mutex.withLock {
        cache.getOrPut(geohash) { derive(vault.channelSeed().hexToByteArray(), geohash) }
    }

    companion object {
        fun derive(seed: ByteArray, geohash: String): SecretKey {
            val area = geohash.encodeToByteArray()
            for (i in 0 until 10) {
                val counter = byteArrayOf((i ushr 24).toByte(), (i ushr 16).toByte(), (i ushr 8).toByte(), i.toByte())
                SecretKey.fromHex(hmacSha256(seed, area + counter).toHexString())?.let { return it }
            }
            error("No valid key for $geohash")
        }
    }
}
