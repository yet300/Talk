package com.yet.talk.location

import com.yet.talk.domain.model.channel.GeoPoint
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.repository.LocationSource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyKilometer
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSinceDate
import platform.darwin.NSObject

/**
 * The device's coarse location from Core Location: Talk only needs area codes, and the point never leaves the device.
 * Asks for "while using" permission the first time (the location step's Allow; however long the alert stays open),
 * then waits up to 15 s for one fix;
 * denied, restricted or no fix is [DomainError.LocationUnavailable]. Runs on the main thread, where the manager
 * delivers its callbacks.
 */
internal class IosLocationSource : LocationSource {

    override suspend fun current(): Result<GeoPoint> = withContext(Dispatchers.Main) {
        val request = Request()
        val point = request.run()
        request.stop()
        point?.let { Result.success(it) } ?: Result.failure(DomainError.LocationUnavailable)
    }

    @OptIn(ExperimentalForeignApi::class)
    private class Request : NSObject(), CLLocationManagerDelegateProtocol {
        private val manager = CLLocationManager()
        private var authorized = CompletableDeferred<Boolean>()
        private val fix = CompletableDeferred<GeoPoint?>()

        init {
            manager.delegate = this
            manager.desiredAccuracy = kCLLocationAccuracyKilometer
        }

        suspend fun run(): GeoPoint? {
            when (manager.authorizationStatus) {
                kCLAuthorizationStatusNotDetermined -> {
                    manager.requestWhenInUseAuthorization()
                    if (!authorized.await()) return null
                }
                kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> Unit
                else -> return null
            }
            manager.location?.takeIf { it.isRecent() }?.let { return it.toPoint() }
            manager.requestLocation()
            // Only the fix is timed: the permission alert above waits for the person, however long they read it.
            return withTimeoutOrNull(TIMEOUT_MS) { fix.await() }
        }

        fun stop() {
            manager.stopUpdatingLocation()
            manager.delegate = null
        }

        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            when (manager.authorizationStatus) {
                kCLAuthorizationStatusNotDetermined -> Unit
                kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> authorized.complete(true)
                else -> authorized.complete(false)
            }
        }

        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            fix.complete((didUpdateLocations.lastOrNull() as? CLLocation)?.toPoint())
        }

        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            fix.complete(null)
        }

        private fun CLLocation.isRecent() = NSDate().timeIntervalSinceDate(timestamp) < MAX_AGE_S

        private fun CLLocation.toPoint() = coordinate.useContents { GeoPoint(latitude, longitude) }
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L
        const val MAX_AGE_S = 5 * 60.0
    }
}
