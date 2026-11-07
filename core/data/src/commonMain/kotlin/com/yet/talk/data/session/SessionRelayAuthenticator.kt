package com.yet.talk.data.session

import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.relay.RelayAuthenticator
import dev.zacsweers.metro.Inject
import kotlin.time.Clock

/** NIP-42: proves the signed-in key to a relay that asks (kind 22242); signed out, the relay sees no one. */
@Inject
internal class SessionRelayAuthenticator(private val session: Session, private val clock: Clock) : RelayAuthenticator {
    override suspend fun authenticate(relayUrl: String, challenge: String): Event? {
        val signer = session.signer() ?: return null
        val tags = listOf(listOf("relay", relayUrl), listOf("challenge", challenge))
        return signer.sign(EventTemplate(Kinds.CLIENT_AUTH, "", tags, clock.now().epochSeconds))
    }
}
