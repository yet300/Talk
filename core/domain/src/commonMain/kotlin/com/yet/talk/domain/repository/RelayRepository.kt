package com.yet.talk.domain.repository

import com.yet.talk.domain.model.relay.Relay
import kotlinx.coroutines.flow.Flow

interface RelayRepository {
    val relays: Flow<List<Relay>>

    suspend fun setRelays(relays: List<Relay>): Result<Unit>
}
