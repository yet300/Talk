package com.yet.talk.data.relay

import com.yet.talk.api.source.relay.DefaultRelays
import com.yet.talk.api.source.relay.RelaySelection
import com.yet.talk.data.session.Session
import com.yet.talk.database.dao.SocialDao
import com.yet.talk.database.model.RelayRecord
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

/** The signed-in user's NIP-65 relays, or [DefaultRelays] when they have none (or are signed out). */
@ContributesBinding(AppScope::class)
@Inject
class DataRelaySelection(private val session: Session, private val social: SocialDao) : RelaySelection {

    override suspend fun readRelays(): List<String> = relays { it.read }

    override suspend fun writeRelays(): List<String> = relays { it.write }

    private suspend fun relays(use: (RelayRecord) -> Boolean): List<String> {
        val viewer = session.viewer.first()
        if (viewer.isEmpty()) return DefaultRelays.urls
        return social.observeRelays(viewer).first().filter(use).map { it.url }.ifEmpty { DefaultRelays.urls }
    }
}
