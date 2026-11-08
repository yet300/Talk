package com.yet.talk.feature.tab.home.mapper

import com.yet.talk.feature.tab.home.HomeComponent
import com.yet.talk.feature.tab.home.store.HomeStore

/** The selected feed comes from the pages, the one place it is kept. */
internal fun stateToModel(state: HomeStore.State, feed: HomeComponent.Feed): HomeComponent.Model =
    HomeComponent.Model(feed = feed, error = state.error)
