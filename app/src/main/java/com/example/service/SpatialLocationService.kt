package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import java.util.Locale

class SpatialLocationService(private val context: Context) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(): Location? {
        if (!hasPermission()) return null
        return try {
            val gpsLoc = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else null

            val netLoc = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } else null

            when {
                gpsLoc != null && netLoc != null -> {
                    if (gpsLoc.time > netLoc.time) gpsLoc else netLoc
                }
                gpsLoc != null -> gpsLoc
                else -> netLoc
            }
        } catch (e: Exception) {
            Log.e("SpatialLocationService", "Failed to retrieve location safely", e)
            null
        }
    }

    fun hasPermission(): Boolean {
        val fine = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun getSectorName(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = @Suppress("DEPRECATION") geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Unknown City"
                val country = addr.countryName ?: "Global"
                "Sector Nova-Hub [$city, $country]"
            } else {
                matchFallbackSector(latitude, longitude)
            }
        } catch (e: Exception) {
            Log.e("SpatialLocationService", "Geocoding failed, falling back to Sector grids", e)
            matchFallbackSector(latitude, longitude)
        }
    }

    fun matchFallbackSector(lat: Double, lon: Double): String {
        return when {
            lat in 37.0..38.0 && lon in -123.0..-122.0 -> "Sector Alpha-10 [Bay Area Grid]"
            lat in 35.0..36.0 && lon in 139.0..140.0 -> "Sector Zeta-12 [Chiyoda Metro Grid]"
            lat in 22.0..24.0 && lon in 89.0..91.0 -> "Sector Delta-09 [Bengal Cyber Basin]"
            else -> "Sector Terra-Void [Lat: ${String.format(Locale.US, "%.3f", lat)}, Lng: ${String.format(Locale.US, "%.3f", lon)}]"
        }
    }
}
