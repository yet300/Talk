package com.yet.talk.feature.tab.bookmarks.di

import com.yet.talk.feature.tab.bookmarks.BookmarksComponent
import com.yet.talk.feature.tab.bookmarks.DefaultBookmarksComponentFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
@BindingContainer
abstract class BookmarksBindings {
    @Binds
    internal abstract val DefaultBookmarksComponentFactory.bindBookmarksComponentFactory: BookmarksComponent.Factory
}
