package com.yet.talk.di

import com.yet.talk.database.driver.SqlDriverFactory
import com.yet.talk.database.driver.WebSqlDriverFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import eu.anifantakis.lib.ksafe.KSafe
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.websocket.WebSockets

@DependencyGraph(AppScope::class)
internal interface JsAppGraph : AppGraph, RepositoriesGraph {

    @SingleIn(AppScope::class)
    @Provides
    fun httpClient(): HttpClient = HttpClient(Js) { install(WebSockets) }

    @Provides
    fun sqlDriverFactory(): SqlDriverFactory = WebSqlDriverFactory()

    @SingleIn(AppScope::class)
    @Provides
    fun ksafe(): KSafe = KSafe(fileName = "talk")
}

fun createJsAppGraph(): AppGraph = createGraph<JsAppGraph>()
