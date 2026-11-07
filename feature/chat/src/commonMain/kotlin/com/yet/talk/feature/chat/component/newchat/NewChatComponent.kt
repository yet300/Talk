package com.yet.talk.feature.chat.component.newchat

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId

/** New chat: search people by name, NIP-05 or npub and pick one. Groups come later. */
interface NewChatComponent {
    val model: Value<Model>

    fun onQueryChanged(query: String)

    /** Opens the chat with [user], unless they cannot receive chats yet. */
    fun onPick(user: User)

    fun onBack()

    fun onErrorShown()

    data class Model(
        val query: String,
        val people: List<User>,
        val isSearching: Boolean,
        /** The person being checked for inbox relays. */
        val checking: UserId?,
        /** People without inbox relays: "Can't receive chats yet", not pickable. */
        val cannotReceive: Set<UserId>,
        val error: DomainError?,
    )

    sealed interface Output {
        data object Back : Output

        data class Open(val id: ChatId) : Output
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, output: (Output) -> Unit): NewChatComponent
    }
}
