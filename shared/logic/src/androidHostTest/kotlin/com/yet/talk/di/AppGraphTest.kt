package com.yet.talk.di

import android.content.ContextWrapper
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.yet.talk.feature.root.RootComponent
import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class AppGraphTest {

    // Resolving bindings never touches the context: the database and KSafe open lazily on first use.
    private val graph = createGraphFactory<AndroidAppGraph.Factory>().create(ContextWrapper(null))

    @Test
    fun rootWaitsOnSplashUntilTheAccountIsKnown() {
        isAssertOnMainThreadEnabled = false
        // Never advanced: the account is not read here, so the root stays on the splash.
        Dispatchers.setMain(StandardTestDispatcher())
        try {
            val root = graph.rootComponentFactory(DefaultComponentContext(LifecycleRegistry()))
            assertIs<RootComponent.Child.Splash>(root.stack.value.active.instance)
        } finally {
            Dispatchers.resetMain()
        }
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
