package com.yet.talk.feature.tab.videos.mapper

import com.yet.talk.feature.tab.videos.VideosComponent
import com.yet.talk.feature.tab.videos.store.VideosStore

internal val stateToModel: (VideosStore.State) -> VideosComponent.Model = { state ->
    VideosComponent.Model(
        list = state.list,
        catalog = state.catalog,
        forYou = state.forYou,
        listVideos = state.listVideos,
        categories = state.categories,
        filters = state.filters,
    )
}
