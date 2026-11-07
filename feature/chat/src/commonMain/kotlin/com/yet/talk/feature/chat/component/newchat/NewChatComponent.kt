package com.yet.talk.feature.chat.component.newchat

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId

/**
 * New chat (`NewChat`): search people by name, NIP-05 or npub and pick one for a chat, or several (up to
 * [com.yet.talk.domain.repository.ChatRepository.MAX_GROUP_MEMBERS]) for a group with an optional name.
 */
interface NewChatComponent {
    val model: Value<Model>

    fun onQueryChanged(query: String)

    /** Adds [user] to the picks, or removes them; someone who cannot receive chats yet is not added. */
    fun onPick(user: User)

    fun onGroupNameChanged(name: String)

    /** Opens the chat with the one person picked, or starts the group. */
    fun onStart()

    fun onBack()

    fun onErrorShown()

    data class Model(
        val query: String,
        val people: List<User>,
        val picked: List<User>,
        /** Shown from two people picked. */
        val groupName: String,
        val canStart: Boolean,
        val isFull: Boolean,
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
