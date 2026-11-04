package com.yet.talk.di

import com.yet.talk.feature.root.RootComponent

/**
 * What the platform apps take from the graph. The `@DependencyGraph` itself lives in each
 * platform source set so it can see platform-only bindings (database driver, KSafe, signer).
 */
interface AppGraph {
    val rootComponentFactory: RootComponent.Factory
}
