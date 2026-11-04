package com.yet.talk.di

import com.yet.talk.database.driver.SqlDriverFactory
import com.yet.talk.database.driver.WebSqlDriverFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory
import eu.anifantakis.lib.ksafe.KSafe
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.websocket.WebSockets
import org.w3c.dom.Worker

@DependencyGraph(AppScope::class)
internal interface JsAppGraph : AppGraph, RepositoriesGraph {

    @SingleIn(AppScope::class)
    @Provides
    fun httpClient(): HttpClient = HttpClient(Js) { install(WebSockets) }

    @Provides
    fun sqlDriverFactory(worker: Worker): SqlDriverFactory = WebSqlDriverFactory(worker)

    @SingleIn(AppScope::class)
    @Provides
    fun ksafe(): KSafe = KSafe(fileName = "talk")

    @DependencyGraph.Factory
    fun interface Factory {
        /** [sqlWorker] is created by the web app's bundler (see `WebSqlDriverFactory`). */
        fun create(@Provides sqlWorker: Worker): JsAppGraph
    }
}

fun createJsAppGraph(sqlWorker: Worker): AppGraph = createGraphFactory<JsAppGraph.Factory>().create(sqlWorker)
