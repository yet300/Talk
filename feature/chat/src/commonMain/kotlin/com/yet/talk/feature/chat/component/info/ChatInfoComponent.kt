package com.yet.talk.feature.chat.component.info

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.chat.Disappearing
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId

/**
 * Chat info (`ChatInfo`): the person or the group's members, disappearing messages, mute, block (a person), leave
 * (a group), Verify (a person); Report and the encryption explainer are drawn by the UI.
 */
interface ChatInfoComponent {
    val model: Value<Model>

    fun onDisappearingChanged(disappearing: Disappearing)

    fun onMutedChanged(muted: Boolean)

    /** Blocks or unblocks the person of a one-to-one chat. */
    fun onBlockedChanged(blocked: Boolean)

    /** Leaves a group on this device. */
    fun onLeave()

    fun onOpenMember(id: UserId)

    /** Verify the person of a one-to-one chat. */
    fun onVerify()

    fun onBack()

    fun onErrorShown()

    data class Model(
        val id: ChatId,
        val members: List<User>,
        val name: String?,
        val isGroup: Boolean,
        val disappearing: Disappearing,
        val isMuted: Boolean,
        val isBlocked: Boolean,
        /** A one-to-one chat whose person you verified. */
        val isVerified: Boolean,
        val error: DomainError?,
    )

    sealed interface Output {
        data object Back : Output

        /** Left the group, or blocked the person: the chat closes. */
        data object Closed : Output

        data class OpenProfile(val id: UserId) : Output

        data class Verify(val person: UserId) : Output
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, chatId: ChatId, output: (Output) -> Unit): ChatInfoComponent
    }
}
