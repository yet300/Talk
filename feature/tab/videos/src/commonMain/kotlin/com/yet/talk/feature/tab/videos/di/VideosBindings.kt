package com.yet.talk.feature.tab.videos.di

import com.yet.talk.feature.tab.videos.DefaultVideosComponentFactory
import com.yet.talk.feature.tab.videos.VideosComponent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
@BindingContainer
abstract class VideosBindings {
    @Binds
    internal abstract val DefaultVideosComponentFactory.bindVideosComponentFactory: VideosComponent.Factory
}
