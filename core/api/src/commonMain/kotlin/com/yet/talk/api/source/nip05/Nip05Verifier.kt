package com.yet.talk.api.source.nip05

import io.ktor.client.request.get

fun interface Nip05Verifier {
    /** True when `name@domain` resolves to [pubkey] through `/.well-known/nostr.json`. */
    suspend fun verify(identifier: String, pubkey: String): Boolean
}
