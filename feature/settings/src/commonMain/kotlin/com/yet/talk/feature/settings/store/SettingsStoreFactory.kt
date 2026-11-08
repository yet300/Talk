package com.yet.talk.feature.settings.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.relay.Relay
import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.domain.repository.ChatRepository
import com.yet.talk.domain.repository.NetworkRepository
import com.yet.talk.domain.repository.PreferencesRepository
import com.yet.talk.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.collectLatest
import com.yet.talk.domain.repository.RelayRepository
import com.yet.talk.domain.rule.RelayRules
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

@Inject
internal class SettingsStoreFactory(
    private val storeFactory: StoreFactory,
    private val accounts: AccountRepository,
    private val relays: RelayRepository,
    private val profiles: ProfileRepository,
    private val preferences: PreferencesRepository,
    private val chats: ChatRepository,
    private val network: NetworkRepository,
) {
    fun create(): SettingsStore =
        object : SettingsStore, Store<SettingsStore.Intent, SettingsStore.State, Nothing> by storeFactory.create(
            name = "SettingsStore",
            initialState = SettingsStore.State(),
            bootstrapper = SimpleBootstrapper(SettingsStore.Action.Observe),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl :
        CoroutineExecutor<SettingsStore.Intent, SettingsStore.Action, SettingsStore.State, SettingsStore.Msg, Nothing>() {

        override fun executeAction(action: SettingsStore.Action) {
            scope.launch { accounts.account.collect { dispatch(SettingsStore.Msg.AccountChanged(it)) } }
            scope.launch { relays.relays.collect { dispatch(SettingsStore.Msg.RelaysChanged(it)) } }
            scope.launch { preferences.theme.collect { dispatch(SettingsStore.Msg.ThemeChanged(it)) } }
            scope.launch { accounts.lock.collect { dispatch(SettingsStore.Msg.LockChanged(it)) } }
            scope.launch { preferences.privacy.collect { dispatch(SettingsStore.Msg.PrivacyChanged(it)) } }
            scope.launch { network.status.collect { dispatch(SettingsStore.Msg.RelayStatesChanged(it.relays)) } }
            scope.launch {
                accounts.account.collectLatest { account ->
                    dispatch(SettingsStore.Msg.MeChanged(null))
                    if (account != null) profiles.observeUser(account.userId).collect { dispatch(SettingsStore.Msg.MeChanged(it)) }
                }
            }
        }

        override fun executeIntent(intent: SettingsStore.Intent) {
            when (intent) {
                is SettingsStore.Intent.ChangeRelayInput -> dispatch(SettingsStore.Msg.RelayInputChanged(intent.url))
                SettingsStore.Intent.AddRelay -> addRelay()
                is SettingsStore.Intent.RemoveRelay -> saveRelays(state().relays.filterNot { it.url == intent.url })
                SettingsStore.Intent.ExportKey -> scope.launch {
                    accounts.exportSecretKey()
                        .onSuccess { dispatch(SettingsStore.Msg.KeyExported(it)) }
                        .onFailure { dispatch(SettingsStore.Msg.Failed(it as? DomainError)) }
                }
                SettingsStore.Intent.HideKey -> dispatch(SettingsStore.Msg.KeyExported(null))
                is SettingsStore.Intent.SelectTheme -> scope.launch { preferences.setTheme(intent.theme) }
                SettingsStore.Intent.SignOut -> scope.launch { accounts.signOut() }
                is SettingsStore.Intent.SetPasscode -> if (intent.passcode != intent.confirm) {
                    dispatch(SettingsStore.Msg.Failed(DomainError.PasscodesDontMatch))
                } else {
                    // scrypt takes a moment: show it working.
                    dispatch(SettingsStore.Msg.Working(true))
                    scope.launch {
                        val error = accounts.setPasscode(intent.passcode).exceptionOrNull()
                        dispatch(SettingsStore.Msg.Working(false))
                        if (error != null) dispatch(SettingsStore.Msg.Failed(error as? DomainError))
                    }
                }
                SettingsStore.Intent.RemovePasscode -> scope.launch {
                    accounts.removePasscode().onFailure { dispatch(SettingsStore.Msg.Failed(it as? DomainError)) }
                }
                is SettingsStore.Intent.SetDeviceLock -> scope.launch { preferences.setDeviceLock(intent.enabled) }
                is SettingsStore.Intent.SetHideInScreenshots -> scope.launch { preferences.setHideInScreenshots(intent.hidden) }
                SettingsStore.Intent.EraseChats -> scope.launch { chats.eraseAll() }
                SettingsStore.Intent.DismissError -> dispatch(SettingsStore.Msg.Failed(null))
            }
        }

        private fun addRelay() {
            RelayRules.normalize(state().relayInput)
                .onSuccess { url ->
                    if (state().relays.none { it.url == url }) saveRelays(state().relays + Relay(url))
                    dispatch(SettingsStore.Msg.RelayInputChanged(""))
                }
                .onFailure { dispatch(SettingsStore.Msg.Failed(it as? DomainError)) }
        }

        private fun saveRelays(updated: List<Relay>) {
            dispatch(SettingsStore.Msg.Working(true))
            scope.launch {
                val error = relays.setRelays(updated).exceptionOrNull() as? DomainError
                dispatch(SettingsStore.Msg.Working(false))
                if (error != null) dispatch(SettingsStore.Msg.Failed(error))
            }
        }
    }

    private object ReducerImpl : Reducer<SettingsStore.State, SettingsStore.Msg> {
        override fun SettingsStore.State.reduce(msg: SettingsStore.Msg): SettingsStore.State =
            when (msg) {
                is SettingsStore.Msg.AccountChanged -> copy(account = msg.account, exportedKey = null)
                is SettingsStore.Msg.RelaysChanged -> copy(relays = msg.relays)
                is SettingsStore.Msg.MeChanged -> copy(me = msg.me)
                is SettingsStore.Msg.ThemeChanged -> copy(theme = msg.theme)
                is SettingsStore.Msg.LockChanged -> copy(lock = msg.lock)
                is SettingsStore.Msg.PrivacyChanged -> copy(privacy = msg.privacy)
                is SettingsStore.Msg.RelayStatesChanged -> copy(relayStates = msg.states)
                is SettingsStore.Msg.RelayInputChanged -> copy(relayInput = msg.url)
                is SettingsStore.Msg.KeyExported -> copy(exportedKey = msg.key)
                is SettingsStore.Msg.Working -> copy(isWorking = msg.working)
                is SettingsStore.Msg.Failed -> copy(error = msg.error)
            }
    }
}
