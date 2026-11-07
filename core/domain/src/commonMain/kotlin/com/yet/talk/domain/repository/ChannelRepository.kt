package com.yet.talk.domain.repository

import com.yet.talk.domain.model.channel.ChannelRoom
import com.yet.talk.domain.model.channel.GeoPoint
import com.yet.talk.domain.model.channel.LocationChannel
import kotlinx.coroutines.flow.Flow

/**
 * Location channels (bitchat-compatible kinds 20000 and 20001): public rooms per area code. Each area gets its own
 * key from a device seed, so places are not linked to each other or to the account; messages are ephemeral.
 */
interface ChannelRepository {
    /** The five channels around [point], Region to Block, computed on the device. */
    fun channelsAround(point: GeoPoint): List<LocationChannel>

    /** The channel for an area code (teleport when it is not around [here]); null when the code is not valid. */
    fun channel(geohash: String, here: GeoPoint?): LocationChannel?

    /** People seen in the last five minutes, per area code; null where nobody announces (Neighborhood, Block). */
    fun peopleCounts(channels: List<LocationChannel>): Flow<Map<String, Int?>>

    /** The room: messages from the last hour and live, and the people count. */
    fun room(channel: LocationChannel): Flow<ChannelRoom>

    suspend fun send(channel: LocationChannel, text: String): Result<Unit>

    /**
     * Announces you (kind 20001) at Region, Province and City of where you are, every 40 to 80 seconds while
     * collected; never for a teleport.
     */
    fun announce(channels: List<LocationChannel>): Flow<Unit>

    companion object {
        const val DEFAULT_NICKNAME = "anon"
        const val MAX_NICKNAME_LENGTH = 32
    }
}
