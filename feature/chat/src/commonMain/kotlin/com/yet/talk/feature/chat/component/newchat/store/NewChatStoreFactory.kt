package com.yet.talk.feature.chat.component.newchat.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.domain.repository.ChatRepository
import com.yet.talk.domain.repository.SearchRepository
import com.yet.talk.feature.chat.component.newchat.store.NewChatStore.Intent
import com.yet.talk.feature.chat.component.newchat.store.NewChatStore.Label
import com.yet.talk.feature.chat.component.newchat.store.NewChatStore.Msg
import com.yet.talk.feature.chat.component.newchat.store.NewChatStore.State
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Inject
internal class NewChatStoreFactory(
    private val storeFactory: StoreFactory,
    private val search: SearchRepository,
    private val chats: ChatRepository,
    private val accounts: AccountRepository,
) {
    fun create(): NewChatStore =
        object : NewChatStore, Store<Intent, State, Label> by storeFactory.create(
            name = "NewChatStore",
            initialState = State(),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl : CoroutineExecutor<Intent, Nothing, State, Msg, Label>() {
        private var observing: Job? = null
        private var searching: Job? = null

        override fun executeIntent(intent: Intent) {
            when (intent) {
                is Intent.ChangeQuery -> changeQuery(intent.query)
                is Intent.Pick -> pick(intent)
                is Intent.ChangeGroupName -> dispatch(Msg.GroupNameChanged(intent.name))
                Intent.Start -> start()
                Intent.DismissError -> dispatch(Msg.Failed(null))
            }
        }

        /** As in Search: local people on every keystroke, relays once typing pauses. You are left out. */
        private fun changeQuery(query: String) {
            dispatch(Msg.QueryChanged(query))
            observing?.cancel()
            observing = scope.launch {
                val me = accounts.account.first()?.userId
                search.observeResults(query).collect { results -> dispatch(Msg.PeopleChanged(results.users.filter { it.id != me })) }
            }
            searching?.cancel()
            if (query.trim().length < SearchRepository.MIN_QUERY_LENGTH) {
                dispatch(Msg.Searching(false))
                return
            }
            searching = scope.launch {
                delay(DEBOUNCE)
                dispatch(Msg.Searching(true))
                val error = search.search(query).exceptionOrNull() as? DomainError
                dispatch(Msg.Searching(false))
                if (error != null) dispatch(Msg.Failed(error))
            }
        }

        private fun pick(intent: Intent.Pick) {
            val user = intent.user
            val state = state()
            if (state.picked.any { it.id == user.id }) return dispatch(Msg.Picked(state.picked.filterNot { it.id == user.id }))
            if (state.checking != null || user.id in state.cannotReceive || state.picked.size >= ChatRepository.MAX_GROUP_MEMBERS) return
            dispatch(Msg.Checking(user.id))
            scope.launch {
                chats.canReceive(ChatId.of(listOf(user.id)))
                    .onSuccess { if (it) dispatch(Msg.Picked(state().picked + user)) else dispatch(Msg.CannotReceive(user.id)) }
                    .onFailure { dispatch(Msg.Failed(it as? DomainError)) }
                dispatch(Msg.Checking(null))
            }
        }

        private fun start() {
            val state = state()
            if (state.picked.isEmpty()) return
            val id = ChatId.of(state.picked.map { it.id })
            if (!id.isGroup) return publish(Label.Open(id))
            scope.launch {
                chats.createGroup(id, state.groupName)
                    .onSuccess { publish(Label.Open(id)) }
                    .onFailure { dispatch(Msg.Failed(it as? DomainError)) }
            }
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State = when (msg) {
            is Msg.QueryChanged -> copy(query = msg.query)
            is Msg.PeopleChanged -> copy(people = msg.people)
            is Msg.Searching -> copy(isSearching = msg.searching)
            is Msg.Checking -> copy(checking = msg.id)
            is Msg.CannotReceive -> copy(cannotReceive = cannotReceive + msg.id)
            is Msg.Picked -> copy(picked = msg.picked)
            is Msg.GroupNameChanged -> copy(groupName = msg.name)
            is Msg.Failed -> copy(error = msg.error)
        }
    }

    private companion object {
        val DEBOUNCE = 350.milliseconds
    }
}
