package com.yet.talk.feature.profile.mapper

import com.yet.talk.feature.profile.ProfileComponent
import com.yet.talk.feature.profile.store.ProfileStore
import com.yet.talk.feature.profile.store.isMe

internal val stateToModel: (ProfileStore.State) -> ProfileComponent.Model = { state ->
    ProfileComponent.Model(
        user = state.user,
        stats = state.stats,
        isMe = state.isMe,
        isFollowing = state.isFollowing,
        tab = state.tab,
        canGoBack = state.requested != null,
        error = state.error,
    )
}
