package com.yet.talk.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph

@DependencyGraph(AppScope::class)
interface IosAppGraph : AppGraph

/** Swift: `IosAppGraphKt.createIosAppGraph()`. */
fun createIosAppGraph(): AppGraph = createGraph<IosAppGraph>()
