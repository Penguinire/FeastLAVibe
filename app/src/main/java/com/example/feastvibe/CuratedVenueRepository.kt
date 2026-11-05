
package com.example.feastvibe

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

object CuratedVenueRepository {

    private const val TAG = "CuratedVenueRepo"
    private const val COLLECTION = "curated_venues"

    fun fetchVerifiedVenues(
        originLat: Double,
        originLng: Double,
        onSuccess: (List<Place>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        FirebaseFirestore.getInstance()
            .collection(COLLECTION)
            .whereEqualTo("isVerified", true)
            .get()
            .addOnSuccessListener { documents ->

                val venues = documents.documents.mapNotNull { document ->
                    try {
                        val name = document.getString("name")
                            ?.takeIf { it.isNotBlank() }
                            ?: return@mapNotNull null

                        val latitude = document.getDouble("latitude")
                            ?: return@mapNotNull null

                        val longitude = document.getDouble("longitude")
                            ?: return@mapNotNull null

                        // Reject missing or invalid coordinates.
                        if (
                            latitude !in -90.0..90.0 ||
                            longitude !in -180.0..180.0 ||
                            (latitude == 0.0 && longitude == 0.0)
                        ) {
                            return@mapNotNull null
                        }

                        val category = document.getString("category")
                            ?.lowercase()
                            ?: return@mapNotNull null

                        val type = when (category) {
                            "club", "nightclub", "night_club" ->
                                PlaceType.CLUB

                            "bar", "lounge", "pub" ->
                                PlaceType.BAR

                            "fast_food", "fast food", "takeaway" ->
                                PlaceType.FAST_FOOD

                            "restaurant" ->
                                PlaceType.RESTAURANT

                            else -> return@mapNotNull null
                        }

                        val rating = document.getDouble("rating")
                        val reviewCount = document.getLong("reviewCount")
                            ?.takeIf { it >= 0L && it <= Int.MAX_VALUE }
                            ?.toInt()

                        val city = document.getString("city")
                        val address = document.getString("address")
                        val area = listOfNotNull(
                            address?.takeIf { it.isNotBlank() },
                            city?.takeIf {
                                it.isNotBlank() && it != "unverified"
                            }
                        ).joinToString(", ").ifBlank {
                            "Location not provided"
                        }

                        val tags = (document.get("tags") as? List<*>)
                            ?.filterIsInstance<String>()
                            ?: emptyList()

                        Place(
                            id = document.id,
                            name = name,
                            cuisine = document.getString("cuisine")
                                ?: category,
                            price = document.getString("price"),
                            rating = rating,
                            reviewCount = reviewCount,
                            distanceKm = distanceBetweenKm(
                                originLat,
                                originLng,
                                latitude,
                                longitude
                            ),
                            area = area,
                            isOpen = document.getBoolean("isOpen"),
                            type = type,
                            phone = document.getString("phone"),
                            website = document.getString("website"),
                            lat = latitude,
                            lng = longitude,
                            tags = tags,
                            generatorBackup = false
                        )
                    } catch (exception: Exception) {
                        Log.e(
                            TAG,
                            "Skipping invalid venue: ${document.id}",
                            exception
                        )
                        null
                    }
                }

                onSuccess(venues)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Could not load curated venues", exception)
                onError(exception)
            }
    }
}
