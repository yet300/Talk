package com.yet.talk.nostr.message

import kotlinx.serialization.json.Json

internal val NostrJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
