package com.yet.talk.di

import com.yet.talk.api.di.relayHttpClient
import com.yet.talk.database.driver.SqlDriverFactory
import com.yet.talk.database.driver.iosSqlDriverFactory
import dev.zacsweers.metro.AppScope
import com.yet.talk.domain.repository.LocationSource
import com.yet.talk.location.UnavailableLocationSource
import com.yet.talk.domain.repository.ConnectivitySource
import com.yet.talk.network.DeviceConnectivity
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import eu.anifantakis.lib.ksafe.KSafe
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

@DependencyGraph(AppScope::class)
internal interface IosAppGraph : AppGraph, RepositoriesGraph {

    @SingleIn(AppScope::class)
    @Provides
    fun locationSource(): LocationSource = UnavailableLocationSource()

    @Provides
    @SingleIn(AppScope::class)
    fun connectivity(): ConnectivitySource = DeviceConnectivity()

    @Provides
    fun httpClient(): HttpClient = relayHttpClient(Darwin)

    @Provides
    fun sqlDriverFactory(): SqlDriverFactory = iosSqlDriverFactory()

    @SingleIn(AppScope::class)
    @Provides
    fun ksafe(): KSafe = KSafe(fileName = "talk")
}

/** Swift: `IosAppGraphKt.createIosAppGraph()`. */
fun createIosAppGraph(): AppGraph = createGraph<IosAppGraph>()
