package com.yet.talk.feature.tab.home.mapper

import com.yet.talk.feature.tab.home.HomeComponent
import com.yet.talk.feature.tab.home.store.HomeStore

internal val stateToModel: (HomeStore.State) -> HomeComponent.Model = { state ->
    HomeComponent.Model(feed = state.feed, error = state.error)
}
