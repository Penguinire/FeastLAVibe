package com.example.feastvibe

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment

/**
 * Hands off to the Google Maps app for turn-by-turn directions — the
 * "Option A" approach from the group's API plan. No Maps/Routes API key needed.
 */
object MapDirections {
    fun launch(fragment: Fragment, place: Place) {
        val uri = Uri.parse("google.navigation:q=${place.lat},${place.lng}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        val activity = fragment.requireActivity()
        if (intent.resolveActivity(activity.packageManager) != null) {
            fragment.startActivity(intent)
        } else {
            // Emulators often have no Maps app — fall back to the browser.
            fragment.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=${place.lat},${place.lng}")
                )
            )
        }
    }
}