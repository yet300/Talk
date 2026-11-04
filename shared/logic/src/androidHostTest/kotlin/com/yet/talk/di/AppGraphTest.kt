package com.yet.talk.di

import android.content.ContextWrapper
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.yet.talk.feature.root.RootComponent
import com.yet.talk.feature.tab.main.MainComponent
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class AppGraphTest {

    // Resolving bindings never touches the context: the database and KSafe open lazily on first use.
    private val graph = createGraphFactory<AndroidAppGraph.Factory>().create(ContextWrapper(null))

    @Test
    fun rootStartsOnHomeTab() {
        val root = graph.rootComponentFactory(DefaultComponentContext(LifecycleRegistry()))

        val main = assertIs<RootComponent.Child.Main>(root.stack.value.active.instance).component
        assertIs<MainComponent.Child.Home>(main.stack.value.active.instance)
    }

    @Test
    fun everyRepositoryResolves() {
        with(graph) {
            listOf(
                accountRepository, postRepository, profileRepository, trendingRepository, reactionRepository,
                bookmarkRepository, followRepository, activityRepository, mediaRepository, relayRepository,
            ).forEach(::assertNotNull)
        }
    }
}
