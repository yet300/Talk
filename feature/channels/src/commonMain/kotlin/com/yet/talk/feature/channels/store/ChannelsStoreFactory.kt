package com.yet.talk.feature.channels.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.model.channel.Geohash
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.repository.ChannelRepository
import com.yet.talk.domain.repository.LocationSource
import com.yet.talk.domain.repository.PreferencesRepository
import com.yet.talk.feature.channels.ChannelsComponent.Location
import com.yet.talk.feature.channels.ChannelsComponent.Step
import com.yet.talk.feature.channels.store.ChannelsStore.Intent
import com.yet.talk.feature.channels.store.ChannelsStore.Label
import com.yet.talk.feature.channels.store.ChannelsStore.Msg
import com.yet.talk.feature.channels.store.ChannelsStore.State
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Inject
internal class ChannelsStoreFactory(
    private val storeFactory: StoreFactory,
    private val channels: ChannelRepository,
    private val preferences: PreferencesRepository,
    private val location: LocationSource,
) {
    fun create(): ChannelsStore =
        object : ChannelsStore, Store<Intent, State, Label> by storeFactory.create(
            name = "ChannelsStore",
            initialState = State(),
            bootstrapper = SimpleBootstrapper(ChannelsStore.Action.Load),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl : CoroutineExecutor<Intent, ChannelsStore.Action, State, Msg, Label>() {
        private var around: Job? = null

        override fun executeAction(action: ChannelsStore.Action) {
            scope.launch { preferences.channelNickname.collect { dispatch(Msg.NicknameChanged(it)) } }
            scope.launch {
                if (preferences.channelIntroSeen.first()) {
                    dispatch(Msg.StepChanged(Step.List))
                    locate()
                } else {
                    dispatch(Msg.StepChanged(Step.Intro))
                }
            }
        }

        override fun executeIntent(intent: Intent) {
            when (intent) {
                Intent.IntroContinue -> dispatch(Msg.StepChanged(Step.LocationStep))
                Intent.AllowLocation -> {
                    toNickname()
                    locate()
                }
                Intent.TeleportInstead -> toNickname()
                is Intent.ChangeNickname -> dispatch(Msg.DraftChanged(intent.nickname))
                Intent.SaveNickname -> scope.launch {
                    preferences.setChannelNickname(state().nicknameDraft)
                    preferences.setChannelIntroSeen()
                    dispatch(Msg.StepChanged(Step.List))
                }
                Intent.EditNickname -> toNickname()
                is Intent.Teleport -> teleport(intent)
                is Intent.ShowTeleport -> dispatch(Msg.TeleportShown(intent.shown))
                Intent.DismissError -> dispatch(Msg.Failed(null))
            }
        }

        private fun toNickname() {
            dispatch(Msg.DraftChanged(state().nickname.takeIf { it != ChannelRepository.DEFAULT_NICKNAME }.orEmpty()))
            dispatch(Msg.StepChanged(Step.Nickname))
        }

        /** The area codes are computed here, on the device; then the counts, and presence while this is open. */
        private fun locate() {
            if (state().location == Location.Locating) return
            dispatch(Msg.LocationChanged(Location.Locating, null, emptyList()))
            scope.launch {
                location.current()
                    .onSuccess { point ->
                        val here = channels.channelsAround(point)
                        dispatch(Msg.LocationChanged(Location.Known, point, here))
                        around?.cancel()
                        around = scope.launch {
                            launch { channels.peopleCounts(here).collect { dispatch(Msg.PeopleChanged(it)) } }
                            launch { channels.announce(here).collect { } }
                        }
                    }
                    .onFailure {
                        dispatch(Msg.LocationChanged(Location.Unavailable, null, emptyList()))
                        dispatch(Msg.Failed(it as? DomainError))
                    }
            }
        }

        private fun teleport(intent: Intent.Teleport) {
            val code = Geohash.normalize(intent.code)
            val channel = code.takeIf { it.length >= intent.level.precision }
                ?.take(intent.level.precision)
                ?.let { channels.channel(it, state().point) }
            if (channel == null) return dispatch(Msg.Failed(DomainError.InvalidAreaCode))
            dispatch(Msg.TeleportShown(false))
            publish(Label.Open(channel))
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State = when (msg) {
            is Msg.StepChanged -> copy(step = msg.step)
            is Msg.LocationChanged -> copy(location = msg.location, point = msg.point, here = msg.here)
            is Msg.PeopleChanged -> copy(people = msg.people)
            is Msg.NicknameChanged -> copy(nickname = msg.nickname)
            is Msg.DraftChanged -> copy(nicknameDraft = msg.draft)
            is Msg.TeleportShown -> copy(isTeleportOpen = msg.shown)
            is Msg.Failed -> copy(error = msg.error)
        }
    }
}
