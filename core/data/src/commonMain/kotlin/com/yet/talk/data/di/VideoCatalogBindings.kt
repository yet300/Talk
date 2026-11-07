package com.yet.talk.data.di

import com.yet.talk.data.video.catalog.BundledVideoCatalog
import com.yet.talk.data.video.catalog.VideoCatalogSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/** On its own so tests can replace the 6,000-video catalog with a few videos. */
@ContributesTo(AppScope::class)
@BindingContainer
abstract class VideoCatalogBindings {
    companion object {
        @Provides
        fun videoCatalog(): VideoCatalogSource = BundledVideoCatalog
    }
}
