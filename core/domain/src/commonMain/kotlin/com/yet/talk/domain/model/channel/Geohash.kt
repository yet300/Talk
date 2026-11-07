package com.yet.talk.domain.model.channel


/** Geohash (base 32): an area code computed on the device; the location itself never leaves it. */
object Geohash {
    private const val ALPHABET = "0123456789bcdefghjkmnpqrstuvwxyz"

    fun encode(latitude: Double, longitude: Double, precision: Int): String {
        var latRange = -90.0 to 90.0
        var lonRange = -180.0 to 180.0
        val out = StringBuilder()
        var bit = 0
        var value = 0
        var even = true
        while (out.length < precision) {
            if (even) {
                val mid = (lonRange.first + lonRange.second) / 2
                if (longitude >= mid) { value = value * 2 + 1; lonRange = mid to lonRange.second } else { value *= 2; lonRange = lonRange.first to mid }
            } else {
                val mid = (latRange.first + latRange.second) / 2
                if (latitude >= mid) { value = value * 2 + 1; latRange = mid to latRange.second } else { value *= 2; latRange = latRange.first to mid }
            }
            even = !even
            if (++bit == 5) {
                out.append(ALPHABET[value])
                bit = 0
                value = 0
            }
        }
        return out.toString()
    }

    /** The cell a geohash names: south, west, north, east. */
    data class Bounds(val south: Double, val west: Double, val north: Double, val east: Double)

    fun bounds(geohash: String): Bounds {
        var latRange = -90.0 to 90.0
        var lonRange = -180.0 to 180.0
        var even = true
        geohash.lowercase().forEach { c ->
            val index = ALPHABET.indexOf(c)
            require(index >= 0) { "Not a geohash: $geohash" }
            for (shift in 4 downTo 0) {
                val on = (index shr shift) and 1 == 1
                if (even) {
                    val mid = (lonRange.first + lonRange.second) / 2
                    lonRange = if (on) mid to lonRange.second else lonRange.first to mid
                } else {
                    val mid = (latRange.first + latRange.second) / 2
                    latRange = if (on) mid to latRange.second else latRange.first to mid
                }
                even = !even
            }
        }
        return Bounds(latRange.first, lonRange.first, latRange.second, lonRange.second)
    }

    /** The centre of [geohash] as latitude and longitude. */
    fun center(geohash: String): Pair<Double, Double> =
        bounds(geohash).let { (south, west, north, east) -> (south + north) / 2 to (west + east) / 2 }

    /** A code Talk can open: base 32 at one of the [ChannelLevel] precisions. */
    fun isValid(geohash: String): Boolean =
        geohash.length in ChannelLevel.entries.map { it.precision } && geohash.lowercase().all { it in ALPHABET }

    fun normalize(input: String): String = input.trim().removePrefix("#").lowercase()

}
