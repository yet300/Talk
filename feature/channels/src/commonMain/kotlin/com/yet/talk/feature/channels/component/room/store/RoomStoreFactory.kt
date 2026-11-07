package com.yet.talk.feature.channels.component.room.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.model.channel.LocationChannel
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.repository.ChannelRepository
import com.yet.talk.domain.repository.PreferencesRepository
import com.yet.talk.feature.channels.component.room.store.RoomStore.Intent
import com.yet.talk.feature.channels.component.room.store.RoomStore.Msg
import com.yet.talk.feature.channels.component.room.store.RoomStore.State
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

@Inject
internal class RoomStoreFactory(
    private val storeFactory: StoreFactory,
    private val channels: ChannelRepository,
    private val preferences: PreferencesRepository,
) {
    fun create(channel: LocationChannel): RoomStore =
        object : RoomStore, Store<Intent, State, Nothing> by storeFactory.create(
            name = "RoomStore",
            initialState = State(channel),
            bootstrapper = SimpleBootstrapper(RoomStore.Action.Join),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl : CoroutineExecutor<Intent, RoomStore.Action, State, Msg, Nothing>() {
        override fun executeAction(action: RoomStore.Action) {
            scope.launch { channels.room(state().channel).collect { dispatch(Msg.RoomChanged(it.messages, it.people)) } }
            scope.launch { preferences.channelNickname.collect { dispatch(Msg.NicknameChanged(it)) } }
        }

        override fun executeIntent(intent: Intent) {
            when (intent) {
                is Intent.ChangeText -> dispatch(Msg.TextChanged(intent.text))
                Intent.Send -> if (state().text.isNotBlank()) {
                    val text = state().text
                    dispatch(Msg.TextChanged(""))
                    scope.launch { channels.send(state().channel, text).onFailure { dispatch(Msg.Failed(it as? DomainError)) } }
                }
                Intent.DismissError -> dispatch(Msg.Failed(null))
            }
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State = when (msg) {
            is Msg.RoomChanged -> copy(messages = msg.messages, people = msg.people)
            is Msg.NicknameChanged -> copy(nickname = msg.nickname)
            is Msg.TextChanged -> copy(text = msg.text)
            is Msg.Failed -> copy(error = msg.error)
        }
    }
}
