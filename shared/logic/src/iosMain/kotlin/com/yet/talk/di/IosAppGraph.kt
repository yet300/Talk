package com.yet.talk.di

import com.yet.talk.database.driver.IosSqlDriverFactory
import com.yet.talk.database.driver.SqlDriverFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import eu.anifantakis.lib.ksafe.KSafe
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.websocket.WebSockets

@DependencyGraph(AppScope::class)
internal interface IosAppGraph : AppGraph, RepositoriesGraph {

    @SingleIn(AppScope::class)
    @Provides
    fun httpClient(): HttpClient = HttpClient(Darwin) { install(WebSockets) }

    @Provides
    fun sqlDriverFactory(): SqlDriverFactory = IosSqlDriverFactory()

    @SingleIn(AppScope::class)
    @Provides
    fun ksafe(): KSafe = KSafe(fileName = "talk")
}

/** Swift: `IosAppGraphKt.createIosAppGraph()`. */
fun createIosAppGraph(): AppGraph = createGraph<IosAppGraph>()
