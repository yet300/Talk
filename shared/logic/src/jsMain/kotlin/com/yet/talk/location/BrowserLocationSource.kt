package com.yet.talk.location

import com.yet.talk.domain.model.channel.GeoPoint
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.repository.LocationSource
import kotlinx.browser.window
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * `navigator.geolocation`, coarse (no high accuracy): Talk only needs area codes, and the point never leaves the
 * device. A denial or timeout is [DomainError.LocationUnavailable].
 */
internal class BrowserLocationSource : LocationSource {
    override suspend fun current(): Result<GeoPoint> = suspendCancellableCoroutine { continuation ->
        val geolocation = window.navigator.asDynamic().geolocation
        if (geolocation == null) {
            continuation.resume(Result.failure(DomainError.LocationUnavailable))
            return@suspendCancellableCoroutine
        }
        val options: dynamic = js("({ enableHighAccuracy: false, timeout: 15000, maximumAge: 300000 })")
        geolocation.getCurrentPosition(
            { position: dynamic ->
                if (continuation.isActive) {
                    continuation.resume(Result.success(GeoPoint(position.coords.latitude as Double, position.coords.longitude as Double)))
                }
            },
            { _: dynamic -> if (continuation.isActive) continuation.resume(Result.failure(DomainError.LocationUnavailable)) },
            options,
        )
    }
}
