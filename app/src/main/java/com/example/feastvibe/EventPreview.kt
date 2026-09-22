package com.example.feastvibe

/**
 * Stand-in for event data shown as a preview on Explore.
 * The full Events screen (Ticketmaster integration, ticket links, full detail)
 * is Bear's responsibility — this only covers the small preview list here.
 */
data class EventPreview(
    val id: String,
    val title: String,
    val dateLabel: String,   // "SAT 18"
    val category: String,
    val venue: String,
    val area: String,
    val price: String        // "R180" or "Free Entry"
)

object EventPreviewData {
    val upcoming = listOf(
        EventPreview("capital_craft_fest", "Capital Craft Beer & Music Fest",
            "SAT 18", "Festival", "Pretoria National Botanical Garden", "Pretoria", "R180"),
        EventPreview("deep_soul_sessions", "Deep Soul Sessions Sunday",
            "SUN 19", "Chillout", "Various Venues", "Sunnyside, Pretoria", "Free Entry")
    )
}