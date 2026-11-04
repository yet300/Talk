package com.yet.talk.nostr.relay

data class PublishResult(
    val accepted: Set<String>,
    val rejected: Map<String, String>,
    val noAnswer: Set<String>,
) {
    val isAccepted: Boolean get() = accepted.isNotEmpty()
}
