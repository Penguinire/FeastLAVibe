package com.example.feastvibe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton

class EventDetailFragment : Fragment(R.layout.activity_event_detail) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val eventId = arguments?.getString("eventId")
        val event = eventId?.let { EventCache.get(it) }

        if (event == null) {
            // Shouldn't normally happen — EventCache is filled right before navigating
            // here — but if the process was killed and restored, fail safely.
            Toast.makeText(context, "Couldn't load that event.", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        view.findViewById<TextView>(R.id.tv_detail_title).text = event.name
        view.findViewById<TextView>(R.id.tv_detail_datetime).text =
            "📅 " + listOf(event.date, event.time).filter { it.isNotBlank() }.joinToString(" · ")
        view.findViewById<TextView>(R.id.tv_detail_venue_name).text = event.venue.ifBlank { "Venue TBC" }
        view.findViewById<TextView>(R.id.tv_detail_venue_address).text =
            listOf(event.address, event.city).filter { it.isNotBlank() }.joinToString(", ")
        view.findViewById<TextView>(R.id.tv_detail_about).text =
            event.description?.takeIf { it.isNotBlank() }
                ?: "No description was provided by the event organiser."

        if (event.imageUrl != null) {
            Glide.with(this).load(event.imageUrl).into(view.findViewById<ImageView>(R.id.iv_detail_hero))
        }

        view.findViewById<View>(R.id.iv_detail_back).setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<View>(R.id.iv_detail_share).setOnClickListener {
            val shareText = if (event.ticketUrl != null) {
                "Check out ${event.name} on Feast & Vibe: ${event.ticketUrl}"
            } else {
                "Check out ${event.name} on Feast & Vibe!"
            }
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    },
                    "Share event"
                )
            )
        }

        view.findViewById<MaterialButton>(R.id.btn_detail_tickets).setOnClickListener {
            val url = event.ticketUrl
            if (url != null) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } else {
                Toast.makeText(context, "No ticket link available for this event", Toast.LENGTH_SHORT).show()
            }
        }

        // Directions — same "always hand off to Google Maps" rule as everywhere else.
        view.findViewById<MaterialButton>(R.id.btn_detail_directions).setOnClickListener {
            val lat = event.latitude
            val lng = event.longitude
            val fallbackQuery = Uri.encode(
                listOf(event.venue, event.city).filter { it.isNotBlank() }.joinToString(", ")
            )
            val uri = if (lat != null && lng != null) {
                Uri.parse("google.navigation:q=$lat,$lng")
            } else {
                Uri.parse("geo:0,0?q=$fallbackQuery")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/?api=1&query=$fallbackQuery")
                    )
                )
            }
        }
    }
}