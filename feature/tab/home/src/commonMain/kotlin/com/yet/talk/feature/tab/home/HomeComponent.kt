package com.yet.talk.feature.tab.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value

/** The home timeline with the KarpathyTalk feeds. */
interface HomeComponent {
    val model: Value<Model>

    fun onFeedSelected(feed: Feed)

    data class Model(val feed: Feed = Feed.Latest)

    enum class Feed { Latest, Following, Trending }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext): HomeComponent
    }
}
