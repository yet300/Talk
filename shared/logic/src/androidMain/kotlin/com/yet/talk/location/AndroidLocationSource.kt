package com.yet.talk.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import com.yet.talk.domain.model.channel.GeoPoint
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.repository.LocationSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * The device's coarse location from the platform's [LocationManager] (no Play services): Talk only needs area codes,
 * and the point never leaves the device. The UI asks for `ACCESS_COARSE_LOCATION` first; without it, or without a fix
 * within 15 s, this is [DomainError.LocationUnavailable].
 */
internal class AndroidLocationSource(private val context: Context) : LocationSource {

    override suspend fun current(): Result<GeoPoint> {
        val granted = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val manager = context.getSystemService(LocationManager::class.java)
        if (!granted || manager == null) return Result.failure(DomainError.LocationUnavailable)
        val location = recent(manager) ?: withTimeoutOrNull(TIMEOUT_MS) { fresh(manager) }
        return location?.let { Result.success(GeoPoint(it.latitude, it.longitude)) } ?: Result.failure(DomainError.LocationUnavailable)
    }

    /** A fix from the last five minutes, as the web's `maximumAge`. */
    @SuppressLint("MissingPermission")
    private fun recent(manager: LocationManager): Location? =
        manager.getProviders(true)
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { System.currentTimeMillis() - it.time < MAX_AGE_MS }
            .maxByOrNull { it.time }

    @SuppressLint("MissingPermission")
    private suspend fun fresh(manager: LocationManager): Location? = suspendCancellableCoroutine { continuation ->
        val provider = when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> null
        }
        if (provider == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val cancel = CancellationSignal()
            continuation.invokeOnCancellation { cancel.cancel() }
            manager.getCurrentLocation(provider, cancel, context.mainExecutor) { if (continuation.isActive) continuation.resume(it) }
        } else {
            val listener = LocationListener { if (continuation.isActive) continuation.resume(it) }
            continuation.invokeOnCancellation { manager.removeUpdates(listener) }
            @Suppress("DEPRECATION")
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L
        const val MAX_AGE_MS = 5 * 60_000L
    }
}
