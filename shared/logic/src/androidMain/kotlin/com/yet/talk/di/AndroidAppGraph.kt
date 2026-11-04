package com.yet.talk.di

import android.content.Context
import com.yet.talk.database.driver.AndroidSqlDriverFactory
import com.yet.talk.database.driver.SqlDriverFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory
import eu.anifantakis.lib.ksafe.KSafe
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets

@DependencyGraph(AppScope::class)
internal interface AndroidAppGraph : AppGraph, RepositoriesGraph {

    @SingleIn(AppScope::class)
    @Provides
    fun httpClient(): HttpClient = HttpClient(OkHttp) { install(WebSockets) }

    @Provides
    fun sqlDriverFactory(context: Context): SqlDriverFactory = AndroidSqlDriverFactory(context)

    @SingleIn(AppScope::class)
    @Provides
    fun ksafe(context: Context): KSafe = KSafe(context, fileName = "talk")

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides context: Context): AndroidAppGraph
    }
}

fun createAndroidAppGraph(context: Context): AppGraph =
    createGraphFactory<AndroidAppGraph.Factory>().create(context.applicationContext)
