package com.yet.talk.feature.tab.bookmarks.mapper

import com.yet.talk.feature.tab.bookmarks.BookmarksComponent
import com.yet.talk.feature.tab.bookmarks.store.BookmarksStore

internal val stateToModel: (BookmarksStore.State) -> BookmarksComponent.Model = { state ->
    BookmarksComponent.Model(error = state.error)
}
