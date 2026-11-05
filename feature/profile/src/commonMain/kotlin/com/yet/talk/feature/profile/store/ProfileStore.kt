package com.yet.talk.feature.profile.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.model.user.UserStats
import com.yet.talk.feature.profile.ProfileComponent

internal interface ProfileStore : Store<ProfileStore.Intent, ProfileStore.State, Nothing> {

    data class State(
        /** The user asked for; null means the viewer. */
        val requested: UserId?,
        val userId: UserId? = requested,
        val viewer: UserId? = null,
        val user: User? = null,
        val stats: UserStats = UserStats(),
        val isFollowing: Boolean = false,
        val tab: ProfileComponent.Tab = ProfileComponent.Tab.Posts,
        val error: DomainError? = null,
    )

    sealed interface Intent {
        data class SelectTab(val tab: ProfileComponent.Tab) : Intent

        data object ToggleFollow : Intent

        data object Refresh : Intent

        data class Like(val post: Post) : Intent

        data class Repost(val post: Post) : Intent

        data class Bookmark(val post: Post) : Intent

        data object DismissError : Intent
    }

    sealed interface Action {
        data object Start : Action
    }

    sealed interface Msg {
        data class ViewerChanged(val viewer: UserId?) : Msg

        data class UserIdChanged(val id: UserId?) : Msg

        data class UserChanged(val user: User) : Msg

        data class StatsChanged(val stats: UserStats) : Msg

        data class FollowingChanged(val following: Boolean) : Msg

        data class TabSelected(val tab: ProfileComponent.Tab) : Msg

        data class Failed(val error: DomainError?) : Msg
    }
}
