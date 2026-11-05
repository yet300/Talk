package com.yet.talk.api.source.relay

/** Used until the user has a relay list of their own. All support NIP-01 tag filters such as `#L`. */
object DefaultRelays {
    val urls = listOf("wss://nos.lol", "wss://relay.primal.net", "wss://relay.damus.io")

    /** Relays that answer NIP-50 search (checked 2026-11). */
    val searchUrls = listOf("wss://search.nos.today", "wss://relay.ditto.pub", "wss://nostr.wine")
}
