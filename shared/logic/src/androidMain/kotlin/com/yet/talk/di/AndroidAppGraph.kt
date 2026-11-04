package com.yet.talk.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph

@DependencyGraph(AppScope::class)
interface AndroidAppGraph : AppGraph

fun createAndroidAppGraph(): AppGraph = createGraph<AndroidAppGraph>()
