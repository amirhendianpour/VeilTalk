package com.example.veiltalk.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            try {
                val providers = locationManager.getProviders(true)
                var bestLocation: Location? = null
                
                for (provider in providers) {
                    val l = locationManager.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                        bestLocation = l
                    }
                }

                if (bestLocation != null && (System.currentTimeMillis() - bestLocation.time) < 60000) {
                    // Last known location is fresh enough
                    continuation.resume(bestLocation)
                    return@suspendCancellableCoroutine
                }

                // If no fresh last known location, request a single update
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        locationManager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(location)
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                
                val provider = when {
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    else -> null
                }

                if (provider != null) {
                    locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                    // Timeout after 10 seconds
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (continuation.isActive) {
                            locationManager.removeUpdates(listener)
                            continuation.resume(bestLocation) // fallback to best available
                        }
                    }, 10000)
                } else {
                    continuation.resume(bestLocation)
                }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }
}
