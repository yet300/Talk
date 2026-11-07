package com.yet.talk.feature.video.di

import com.yet.talk.feature.video.DefaultVideoComponentFactory
import com.yet.talk.feature.video.VideoComponent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
@BindingContainer
abstract class VideoBindings {
    @Binds
    internal abstract val DefaultVideoComponentFactory.bindVideoComponentFactory: VideoComponent.Factory
}
