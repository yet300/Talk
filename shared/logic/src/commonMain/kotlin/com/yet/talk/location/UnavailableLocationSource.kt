package com.yet.talk.location

import com.yet.talk.domain.model.channel.GeoPoint
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.repository.LocationSource

/** iOS: location comes with its app; until then channels open by area code (teleport) only. */
internal class UnavailableLocationSource : LocationSource {
    override suspend fun current(): Result<GeoPoint> = Result.failure(DomainError.LocationUnavailable)
}
