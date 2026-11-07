package com.yet.talk.data.repository

import com.yet.talk.api.source.relay.RelayStatusSource
import com.yet.talk.data.sync.Outbox
import com.yet.talk.database.dao.OutboxDao
import com.yet.talk.domain.model.network.NetworkStatus
import com.yet.talk.domain.model.network.RelayState
import com.yet.talk.domain.repository.ConnectivitySource
import com.yet.talk.domain.repository.NetworkRepository
import com.yet.talk.domain.repository.RelayRepository
import com.yet.talk.nostr.relay.RelayStatus
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

@SingleIn(AppScope::class)
@Inject
internal class NetworkRepositoryImpl(
    connectivity: ConnectivitySource,
    statuses: RelayStatusSource,
    relays: RelayRepository,
    outbox: OutboxDao,
    sender: Outbox,
    scope: CoroutineScope,
) : NetworkRepository {

    override val status: Flow<NetworkStatus> =
        combine(connectivity.online, statuses.statuses(), relays.relays, outbox.observePending()) { online, states, mine, waiting ->
            NetworkStatus(
                deviceOnline = online,
                relays = mine.associate { it.url to (states[it.url] ?: RelayStatus.Idle).toState() },
                waitingToSend = waiting.toInt(),
            )
        }.distinctUntilChanged().shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    init {
        // Back online, or one more relay reachable: send what is waiting now.
        scope.launch {
            combine(connectivity.online, statuses.statuses().map { all -> all.count { it.value == RelayStatus.Connected } }) { online, connected -> online to connected }
                .distinctUntilChanged()
                .drop(1)
                .filter { (online, _) -> online }
                .collect { sender.wake() }
        }
    }

    private fun RelayStatus.toState() = when (this) {
        RelayStatus.Idle -> RelayState.NotUsedYet
        RelayStatus.Connecting -> RelayState.Connecting
        RelayStatus.Connected -> RelayState.Connected
        RelayStatus.Disconnected -> RelayState.Unreachable
    }
}
