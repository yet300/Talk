package com.yet.talk.domain.model.channel

import kotlin.time.Instant

/**
 * The levels of location channels (bitchat's precisions). People at Region, Province and City announce themselves
 * (presence), so those show a count; finer levels show "? people".
 */
enum class ChannelLevel(val precision: Int, val sendsPresence: Boolean) {
    Region(2, true),
    Province(4, true),
    City(5, true),
    Neighborhood(6, false),
    Block(7, false),
    ;

    companion object {
        fun of(geohash: String): ChannelLevel? = entries.firstOrNull { it.precision == geohash.length }
    }
}

/** A place to talk: an area code at a level; [isTeleport] when you are not there (opened by code). */
data class LocationChannel(val geohash: String, val level: ChannelLevel, val isTeleport: Boolean)

/** Someone's message in a channel (bitchat kind 20000): public, not kept by relays. */
data class ChannelMessage(
    val id: String,
    /** The sender's key in this area (a different one in every place). */
    val author: String,
    /** `nick#abcd`: the chosen nickname and the key's last four hex digits. */
    val displayName: String,
    val content: String,
    val createdAt: Instant,
    val isMine: Boolean,
    val isTeleported: Boolean,
)

/** What is said in a channel while you are there, and how many people are around (null where nobody announces). */
data class ChannelRoom(val messages: List<ChannelMessage>, val people: Int?)

/** A point from the device's location, used only to compute area codes on the device. */
data class GeoPoint(val latitude: Double, val longitude: Double)
