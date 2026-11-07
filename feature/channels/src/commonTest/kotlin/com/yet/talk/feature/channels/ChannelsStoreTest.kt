package com.yet.talk.feature.channels

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.yet.talk.domain.model.channel.ChannelLevel
import com.yet.talk.domain.model.channel.ChannelRoom
import com.yet.talk.domain.model.channel.GeoPoint
import com.yet.talk.domain.model.channel.Geohash
import com.yet.talk.domain.model.channel.LocationChannel
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.settings.ThemeMode
import com.yet.talk.domain.repository.ChannelRepository
import com.yet.talk.domain.repository.LocationSource
import com.yet.talk.domain.repository.PreferencesRepository
import com.yet.talk.feature.channels.ChannelsComponent.Location
import com.yet.talk.feature.channels.ChannelsComponent.Step
import com.yet.talk.feature.channels.store.ChannelsStore
import com.yet.talk.feature.channels.store.ChannelsStore.Intent
import com.yet.talk.feature.channels.store.ChannelsStoreFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChannelsStoreTest {

    private val here = GeoPoint(57.64911, 10.40744)

    private class Preferences : PreferencesRepository {
        override val theme = MutableStateFlow(ThemeMode.System)
        override suspend fun setTheme(mode: ThemeMode) = Unit
        override val channelNickname = MutableStateFlow("anon")
        override suspend fun setChannelNickname(nickname: String) { channelNickname.value = nickname }
        override val channelIntroSeen = MutableStateFlow(false)
        override suspend fun setChannelIntroSeen() { channelIntroSeen.value = true }
    }

    private class Channels : ChannelRepository {
        val announced = mutableListOf<List<LocationChannel>>()
        override fun channelsAround(point: GeoPoint) =
            ChannelLevel.entries.map { LocationChannel(Geohash.encode(point.latitude, point.longitude, it.precision), it, false) }
        override fun channel(geohash: String, here: GeoPoint?) =
            ChannelLevel.of(geohash)?.takeIf { Geohash.isValid(geohash) }?.let { LocationChannel(geohash, it, isTeleport = true) }
        override fun peopleCounts(channels: List<LocationChannel>): Flow<Map<String, Int?>> = flowOf(channels.associate { it.geohash to 1 })
        override fun room(channel: LocationChannel): Flow<ChannelRoom> = emptyFlow()
        override suspend fun send(channel: LocationChannel, text: String) = Result.success(Unit)
        override fun announce(channels: List<LocationChannel>): Flow<Unit> = emptyFlow<Unit>().also { announced += channels }
    }

    private val preferences = Preferences()
    private val channels = Channels()

    private fun store(location: Result<GeoPoint>) =
        ChannelsStoreFactory(DefaultStoreFactory(), channels, preferences, object : LocationSource {
            override suspend fun current() = location
        }).create()

    @BeforeTest
    fun setUp() {
        isAssertOnMainThreadEnabled = false
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun theFirstRunAsksForLocationThenANickname() = runTest {
        val store = store(Result.success(here))
        assertEquals(Step.Intro, store.state.step)
        store.accept(Intent.IntroContinue)
        assertEquals(Step.LocationStep, store.state.step)
        store.accept(Intent.AllowLocation)
        assertEquals(Step.Nickname, store.state.step)
        assertEquals(Location.Known, store.state.location)
        assertEquals("u4pru", store.state.here.first { it.level == ChannelLevel.City }.geohash)
        assertEquals(listOf(store.state.here), channels.announced, "announced where you are while this is open")

        store.accept(Intent.ChangeNickname("quiet-heron"))
        store.accept(Intent.SaveNickname)
        assertEquals(Step.List, store.state.step)
        assertEquals("quiet-heron", preferences.channelNickname.value)
        assertTrue(preferences.channelIntroSeen.value)
    }

    @Test
    fun withoutALocationOnlyTeleportRemains() = runTest {
        preferences.channelIntroSeen.value = true
        val store = store(Result.failure(DomainError.LocationUnavailable))
        assertEquals(Step.List, store.state.step)
        assertEquals(Location.Unavailable, store.state.location)
        assertTrue(store.state.here.isEmpty())

        val opened = mutableListOf<ChannelsStore.Label>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { store.labels.collect { opened += it } }
        store.accept(Intent.Teleport("u4p", ChannelLevel.City))
        assertEquals(DomainError.InvalidAreaCode, store.state.error)
        store.accept(Intent.Teleport("#u4pruydqqvj", ChannelLevel.City))
        assertEquals(listOf<ChannelsStore.Label>(ChannelsStore.Label.Open(LocationChannel("u4pru", ChannelLevel.City, true))), opened)
    }
}
