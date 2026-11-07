package com.yet.talk.di

import android.content.Context
import com.yet.talk.api.di.relayHttpClient
import com.yet.talk.database.driver.SqlDriverFactory
import com.yet.talk.database.driver.androidSqlDriverFactory
import dev.zacsweers.metro.AppScope
import com.yet.talk.domain.repository.LocationSource
import com.yet.talk.location.UnavailableLocationSource
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory
import eu.anifantakis.lib.ksafe.KSafe
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

@DependencyGraph(AppScope::class)
internal interface AndroidAppGraph : AppGraph, RepositoriesGraph {

    @SingleIn(AppScope::class)
    @Provides
    fun locationSource(): LocationSource = UnavailableLocationSource()

    @Provides
    fun httpClient(): HttpClient = relayHttpClient(OkHttp)

    @Provides
    fun sqlDriverFactory(context: Context): SqlDriverFactory = androidSqlDriverFactory(context)

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
