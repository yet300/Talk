package com.yet.talk.di

import com.yet.talk.api.di.relayHttpClient
import com.yet.talk.database.driver.SqlDriverFactory
import com.yet.talk.database.driver.webSqlDriverFactory
import dev.zacsweers.metro.AppScope
import com.yet.talk.domain.repository.LocationSource
import com.yet.talk.location.BrowserLocationSource
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import eu.anifantakis.lib.ksafe.KSafe
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

@DependencyGraph(AppScope::class)
internal interface JsAppGraph : AppGraph, RepositoriesGraph {

    @SingleIn(AppScope::class)
    @Provides
    fun locationSource(): LocationSource = BrowserLocationSource()

    @Provides
    fun httpClient(): HttpClient = relayHttpClient(Js)

    @Provides
    fun sqlDriverFactory(): SqlDriverFactory = webSqlDriverFactory()

    @SingleIn(AppScope::class)
    @Provides
    fun ksafe(): KSafe = KSafe(fileName = "talk")
}

fun createJsAppGraph(): AppGraph = createGraph<JsAppGraph>()
