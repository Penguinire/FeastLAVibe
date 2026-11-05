package com.example.feastvibe

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

data class UserArea(
    val point: GeoPoint,
    val city: String
)

object DeviceLocation {

    private const val DEFAULT_CITY = "Pretoria"

    private val fallbackPoint =
        GeoPoint.fromLngLat(28.1881, -25.7461)

    suspend fun getUserArea(context: Context): UserArea {
        val appContext = context.applicationContext

        val hasFine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            return UserArea(fallbackPoint, DEFAULT_CITY)
        }

        return try {
            val client =
                LocationServices.getFusedLocationProviderClient(appContext)

            val location = client.lastLocation.await()
                ?: client.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    null
                ).await()

            if (location == null) {
                UserArea(fallbackPoint, DEFAULT_CITY)
            } else {
                val point = GeoPoint.fromLngLat(
                    location.longitude,
                    location.latitude
                )

                val city = getCityName(appContext, location)
                    ?: DEFAULT_CITY

                UserArea(point, city)
            }
        } catch (exception: Exception) {
            UserArea(fallbackPoint, DEFAULT_CITY)
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun getCityName(
        context: Context,
        location: Location
    ): String? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) {
                return@withContext null
            }

            val geocoder = Geocoder(context, Locale.getDefault())

            val addresses = geocoder.getFromLocation(
                location.latitude,
                location.longitude,
                1
            )

            val address = addresses?.firstOrNull()

            address?.locality
                ?: address?.subAdminArea
                ?: address?.adminArea

        } catch (exception: Exception) {
            null
        }
    }
}