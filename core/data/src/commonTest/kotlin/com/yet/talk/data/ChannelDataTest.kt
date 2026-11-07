package com.yet.talk.data

import com.yet.talk.api.schema.ChannelEvents
import com.yet.talk.data.channel.ChannelKeys
import com.yet.talk.data.channel.GeoRelay
import com.yet.talk.data.channel.GeoRelays
import com.yet.talk.data.fake.TestData
import com.yet.talk.domain.model.channel.ChannelLevel
import com.yet.talk.domain.model.channel.GeoPoint
import com.yet.talk.domain.model.channel.Geohash
import com.yet.talk.nostr.key.SecretKey
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.nip.Nip13
import com.yet.talk.nostr.signer.LocalKeySigner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Location channels (design pass 4, step 5), bitchat-compatible. */
class ChannelDataTest {

    // Jutland, the classic geohash example (u4pruydqqvj).
    private val here = GeoPoint(57.64911, 10.40744)

    @Test
    fun geohashesEncodeAndDecode() {
        assertEquals("u4pruydqqvj", Geohash.encode(here.latitude, here.longitude, 11))
        val (lat, lon) = Geohash.center("u4pru")
        assertEquals("u4pru", Geohash.encode(lat, lon, 5))
        assertTrue(Geohash.isValid("u4pru"))
        assertFalse(Geohash.isValid("u4pra"), "a is not in the alphabet")
        assertFalse(Geohash.isValid("u4p"), "no level has 3 characters")
        // The map outlines the cell: it holds the point, and its centre encodes back to the code.
        val cell = Geohash.bounds("u4pru")
        assertTrue(here.latitude in cell.south..cell.north && here.longitude in cell.west..cell.east)
        assertTrue(cell.north - cell.south < 0.05 && cell.east - cell.west < 0.05)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun everyAreaHasItsOwnKeyFromTheDeviceSeed() {
        val seed = "22".repeat(32).hexToByteArray()
        val city = ChannelKeys.derive(seed, "u4pru")
        assertEquals(city, ChannelKeys.derive(seed, "u4pru"), "the same place, the same identity")
        assertNotEquals(city.publicKey, ChannelKeys.derive(seed, "u4prv").publicKey)
        assertNotEquals(city.publicKey, ChannelKeys.derive("33".repeat(32).hexToByteArray(), "u4pru").publicKey)
    }

    @Test
    fun theNearestRelaysServeAnArea() {
        val relays = listOf(GeoRelay("aarhus", 56.16, 10.2), GeoRelay("tokyo", 35.7, 139.7), GeoRelay("oslo", 59.9, 10.75))
        assertEquals(listOf("wss://aarhus", "wss://oslo"), GeoRelays.nearest("u4pru", count = 2, relays = relays))
        assertEquals(5, GeoRelays.nearest("u4pru").size)
    }

    private fun channelTest(body: suspend TestScope.(TestData) -> Unit) = runTest {
        val factory = testDriverFactory() ?: return@runTest
        body(TestData(factory, backgroundScope, StandardTestDispatcher(testScheduler)))
    }

    @Test
    fun aRoomShowsNicknamesTeleportsAndPeople() = channelTest { data ->
        val channels = data.channels.channelsAround(here)
        assertEquals(listOf("u4", "u4pr", "u4pru", "u4pruy", "u4pruyd"), channels.map { it.geohash })
        val city = channels.first { it.level == ChannelLevel.City }
        val now = data.clock.seconds
        val amber = LocalKeySigner(SecretKey.generate())
        data.network.channelEvents.emit(amber.sign(ChannelEvents.message("u4pru", "market open?", "amber-fox", false, now - 60)))
        data.network.channelEvents.emit(amber.sign(ChannelEvents.message("u4prv", "elsewhere", "amber-fox", false, now - 60)))
        val visitor = LocalKeySigner(SecretKey.generate())
        data.network.channelEvents.emit(visitor.sign(ChannelEvents.presence("u4pru", now - 30)))

        data.preferences.setChannelNickname("quiet-heron")
        data.channels.send(city, "it is").getOrThrow()
        val room = data.channels.room(city).first { it.messages.size == 2 }
        val (theirs, mine) = room.messages
        assertEquals("amber-fox#" + amber.publicKey().takeLast(4), theirs.displayName)
        assertTrue(mine.isMine && mine.displayName.startsWith("quiet-heron#"))
        assertEquals(3, room.people, "amber and I wrote, the visitor announced")

        val sent = data.network.published.single { it.kind == Kinds.GEOHASH_MESSAGE }
        assertEquals(listOf(listOf("g", "u4pru"), listOf("n", "quiet-heron")), sent.tags.dropLast(1))
        assertTrue(Nip13.difficulty(sent.id) >= 12, "mined, as bitchat clients that filter by work expect")
        assertNotEquals(data.vault.stored, sent.pubkey, "never the account key")
        assertEquals(GeoRelays.nearest("u4pru"), data.network.channelRelays.last().toList())
    }

    @Test
    fun aTeleportIsMarkedAndNeverAnnounced() = channelTest { data ->
        val far = data.channels.channel("#U4PRU", here = GeoPoint(35.7, 139.7))!!
        assertTrue(far.isTeleport)
        assertFalse(data.channels.channel("u4pru", here)!!.isTeleport)
        assertNull(data.channels.channel("u4p", here))

        data.channels.send(far, "hello from afar").getOrThrow()
        assertTrue(listOf("t", "teleport") in data.network.published.single().tags)
        assertNull(data.channels.announce(listOf(far)).firstOrNull(), "nothing to announce for a place you are not in")
    }

    @Test
    fun presenceGoesOnlyToRegionProvinceAndCity() = channelTest { data ->
        data.channels.announce(data.channels.channelsAround(here)).first()
        val presence = data.network.published.filter { it.kind == Kinds.GEOHASH_PRESENCE }
        assertEquals(listOf("u4", "u4pr", "u4pru"), presence.map { ChannelEvents.geohash(it) })
        assertTrue(presence.all { it.content.isEmpty() && it.tags.size == 1 })
        assertEquals(3, presence.map { it.pubkey }.toSet().size, "a different identity per area")
    }
}
