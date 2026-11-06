package com.yet.talk.feature.post.di

import com.yet.talk.feature.post.component.cards.DefaultPostCardsComponent
import com.yet.talk.feature.post.component.cards.PostCardsComponent
import com.yet.talk.feature.post.component.post.DefaultPostComponentFactory
import com.yet.talk.feature.post.component.post.PostComponent
import com.yet.talk.feature.post.component.revisions.DefaultRevisionsComponentFactory
import com.yet.talk.feature.post.component.revisions.RevisionsComponent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
@BindingContainer
abstract class PostBindings {
    @Binds
    internal abstract val DefaultPostComponentFactory.bindPostComponentFactory: PostComponent.Factory

    @Binds
    internal abstract val DefaultPostCardsComponent.bindPostCardsComponent: PostCardsComponent

    @Binds
    internal abstract val DefaultRevisionsComponentFactory.bindRevisionsComponentFactory: RevisionsComponent.Factory
}
