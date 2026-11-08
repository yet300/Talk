package com.yet.talk.feature.profile.mapper

import com.yet.talk.feature.profile.ProfileComponent
import com.yet.talk.feature.profile.store.ProfileStore
import com.yet.talk.feature.profile.store.isMe

/** The selected tab comes from the pages, the one place it is kept. */
internal fun stateToModel(state: ProfileStore.State, tab: ProfileComponent.Tab): ProfileComponent.Model =
    ProfileComponent.Model(
        user = state.user,
        stats = state.stats,
        isMe = state.isMe,
        isFollowing = state.isFollowing,
        tab = tab,
        canGoBack = state.requested != null,
        error = state.error,
    )
