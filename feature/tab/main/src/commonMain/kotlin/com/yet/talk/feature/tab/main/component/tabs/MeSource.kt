package com.yet.talk.feature.tab.main.component.tabs

import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.domain.repository.ProfileRepository
import com.yet.talk.domain.repository.RelayRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import com.yet.talk.domain.model.user.User

/** The signed-in identity for the navigation chrome: the rail's identity row, the phone header avatar. */
@Inject
internal class MeSource(
    private val accounts: AccountRepository,
    private val profiles: ProfileRepository,
    private val relays: RelayRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<TabsComponent.Me> =
        accounts.account.flatMapLatest { account ->
            if (account == null) return@flatMapLatest flowOf(TabsComponent.Me())
            val user = profiles.observeUser(account.userId).map<User, User?> { it }.onStart { emit(null) }
            combine(user, relays.relays) { profile, list ->
                TabsComponent.Me(user = profile, npub = account.npub, relayCount = list.size)
            }
        }
}
