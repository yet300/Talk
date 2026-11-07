package com.yet.talk.data.session

import com.yet.talk.api.source.chat.ChatRemoteSource
import com.yet.talk.api.source.relay.RelaySelection
import com.yet.talk.data.channel.GeoRelayList
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.relay.RelayAuthenticator
import dev.zacsweers.metro.Inject
import kotlin.time.Clock

/**
 * NIP-42: proves the signed-in key to a relay that asks (kind 22242); signed out, the relay sees no one. A relay
 * Talk reaches only for location channels never gets it: that would tie your places to your account.
 */
@Inject
internal class SessionRelayAuthenticator(
    private val session: Session,
    private val selection: RelaySelection,
    private val clock: Clock,
) : RelayAuthenticator {
    override suspend fun authenticate(relayUrl: String, challenge: String): Event? {
        if (isChannelOnly(relayUrl)) return null
        val signer = session.signer() ?: return null
        val tags = listOf(listOf("relay", relayUrl), listOf("challenge", challenge))
        return signer.sign(EventTemplate(Kinds.CLIENT_AUTH, "", tags, clock.now().epochSeconds))
    }

    private suspend fun isChannelOnly(relayUrl: String): Boolean {
        val host = relayUrl.host()
        if (GeoRelayList.none { it.host == host }) return false
        val account = selection.readRelays() + selection.writeRelays() + ChatRemoteSource.DEFAULT_INBOX + ChatRemoteSource.INDEXERS
        return account.none { it.host() == host }
    }

    private fun String.host() = lowercase().removePrefix("wss://").removePrefix("ws://").trimEnd('/').removeSuffix(":443")
}
