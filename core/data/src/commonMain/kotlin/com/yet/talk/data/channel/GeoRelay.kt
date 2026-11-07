package com.yet.talk.data.channel

import com.yet.talk.domain.model.channel.Geohash
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal data class GeoRelay(val host: String, val latitude: Double, val longitude: Double)

/**
 * The relays for an area, as bitchat picks them: the [count] nearest the area's centre (haversine), ties by host.
 * The list is [GeoRelayList] (permissionlesstech/georelays, refreshed weekly).
 */
internal object GeoRelays {
    const val COUNT = 5

    fun nearest(geohash: String, count: Int = COUNT, relays: List<GeoRelay> = GeoRelayList): List<String> {
        val (lat, lon) = Geohash.center(geohash)
        return relays.sortedWith(compareBy<GeoRelay>({ distance(lat, lon, it.latitude, it.longitude) }, { it.host }))
            .take(count)
            .map { "wss://${it.host}" }
    }

    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = (lat2 - lat1).rad()
        val dLon = (lon2 - lon1).rad()
        val a = sin(dLat / 2).pow(2) + cos(lat1.rad()) * cos(lat2.rad()) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(a))
    }

    private fun Double.rad() = this * PI / 180

    private const val EARTH_RADIUS_M = 6_371_000.0
}
