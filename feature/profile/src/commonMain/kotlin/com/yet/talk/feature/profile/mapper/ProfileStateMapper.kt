package com.yet.talk.feature.profile.mapper

import com.yet.talk.feature.profile.ProfileComponent
import com.yet.talk.feature.profile.store.ProfileStore

internal val stateToModel: (ProfileStore.State) -> ProfileComponent.Model = { state ->
    ProfileComponent.Model(
        user = state.user,
        stats = state.stats,
        isMe = state.userId != null && state.userId == state.viewer,
        isFollowing = state.isFollowing,
        tab = state.tab,
        canGoBack = state.requested != null,
        error = state.error,
    )
}
