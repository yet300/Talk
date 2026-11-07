package com.yet.talk.domain.repository

import com.yet.talk.domain.model.channel.GeoPoint

/** The device's location, asked for when Location channels open; it stays on the device. */
interface LocationSource {
    /** Fails with [com.yet.talk.domain.model.error.DomainError.LocationUnavailable] when denied or unknown. */
    suspend fun current(): Result<GeoPoint>
}
