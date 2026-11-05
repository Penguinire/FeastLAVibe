package com.example.feastvibe

/**
 * Coordinates independent of any map or places provider.
 *
 * Latitude is north/south.
 * Longitude is east/west.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        /**
         * Creates a point using longitude first, latitude second.
         */
        fun fromLngLat(
            longitude: Double,
            latitude: Double
        ): GeoPoint {
            return GeoPoint(
                latitude = latitude,
                longitude = longitude
            )
        }
    }

    // Temporary compatibility helpers for existing code.
    fun latitude(): Double = latitude
    fun longitude(): Double = longitude
}