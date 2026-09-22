package com.example.feastvibe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.flexbox.FlexboxLayout

class RestaurantDetailFragment : Fragment(R.layout.activity_restaurant_detail) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val placeId = arguments?.getString("placeId") ?: "central_012"
        val place = DummyData.byId(placeId)

        view.findViewById<TextView>(R.id.tv_detail_name).text = place.name
        view.findViewById<TextView>(R.id.tv_detail_categories).text = place.cuisine
        view.findViewById<TextView>(R.id.tv_detail_location).text =
            "${place.area.uppercase()} · ${place.distanceKm} km away · ${place.price ?: "Price N/A"}"

        view.findViewById<TextView>(R.id.tv_detail_rating).text =
            if (place.rating != null) "★ ${place.rating}" else "New"
        view.findViewById<TextView>(R.id.tv_detail_review_count).text =
            if (place.reviewCount != null) "(${place.reviewCount} authentic reviews)" else "(No reviews yet)"

        view.findViewById<TextView>(R.id.tv_open_badge).text =
            if (place.isOpen) "● Open Now" else "● Closed"
        view.findViewById<TextView>(R.id.tv_open_badge).setTextColor(
            resources.getColor(if (place.isOpen) R.color.open_green else R.color.text_secondary, null)
        )

        view.findViewById<View>(R.id.tv_detail_generator).visibility =
            if (place.generatorBackup) View.VISIBLE else View.GONE

        view.findViewById<TextView>(R.id.tv_detail_about).text =
            "Sample description for ${place.name}, a fictional Feast & Vibe venue in ${place.area}. " +
                    "Real venue descriptions will come from the Google Places API once connected."

        view.findViewById<TextView>(R.id.tv_detail_address).text = "${place.area}, South Africa"

        bindHighlights(view, place)
        bindOpeningHours(view)
        bindRatingBars(view.findViewById(R.id.rating_bars))
        bindReviewPreviews(view)
        bindActions(view, place)

        view.findViewById<View>(R.id.iv_detail_back).setOnClickListener {
            findNavController().navigateUp()
        }
        val favIcon = view.findViewById<ImageView>(R.id.iv_detail_favourite)
        favIcon.setImageResource(
            if (FavouritesStore.isFavourite(place.id)) R.drawable.ic_nav_favourite else android.R.drawable.btn_star_big_on
        )
        favIcon.setOnClickListener {
            val nowFav = FavouritesStore.toggle(place.id)
            Toast.makeText(
                context,
                if (nowFav) "Saved to favourites" else "Removed from favourites",
                Toast.LENGTH_SHORT
            ).show()
        }
        view.findViewById<View>(R.id.map_snippet).setOnClickListener {
            findNavController().navigate(R.id.mapFragment, bundleOf("placeId" to place.id))
        }
        view.findViewById<View>(R.id.address_card).setOnClickListener {
            findNavController().navigate(R.id.mapFragment, bundleOf("placeId" to place.id))
        }
        view.findViewById<View>(R.id.tv_open_in_maps).setOnClickListener {
            findNavController().navigate(R.id.mapFragment, bundleOf("placeId" to place.id))
        }

        val openReviews = {
            findNavController().navigate(
                R.id.reviewsFragment,
                bundleOf("placeId" to place.id)
            )
        }
        view.findViewById<View>(R.id.tv_detail_review_count).setOnClickListener { openReviews() }
        view.findViewById<View>(R.id.btn_view_all_reviews).setOnClickListener { openReviews() }
    }

    private fun bindActions(view: View, place: Place) {
        val directions = view.findViewById<View>(R.id.action_directions)
        directions.findViewById<TextView>(R.id.tv_action_icon).text = "↗"
        directions.findViewById<TextView>(R.id.tv_action_label).text = "Directions"
        directions.setOnClickListener { MapDirections.launch(this, place) }

        val call = view.findViewById<View>(R.id.action_call)
        call.findViewById<TextView>(R.id.tv_action_icon).text = "📞"
        call.findViewById<TextView>(R.id.tv_action_label).text = "Call"
        call.setOnClickListener {
            if (place.phone != null) {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${place.phone}")))
            } else {
                Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
            }
        }

        val website = view.findViewById<View>(R.id.action_website)
        website.findViewById<TextView>(R.id.tv_action_icon).text = "🌐"
        website.findViewById<TextView>(R.id.tv_action_label).text = "Website"
        website.setOnClickListener {
            if (place.website != null) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://${place.website}")))
            } else {
                Toast.makeText(context, "No website available", Toast.LENGTH_SHORT).show()
            }
        }

        val menu = view.findViewById<View>(R.id.action_menu)
        menu.findViewById<TextView>(R.id.tv_action_icon).text = "🍽"
        menu.findViewById<TextView>(R.id.tv_action_label).text = "Menu"
        menu.setOnClickListener {
            Toast.makeText(context, "Menu coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindHighlights(view: View, place: Place) {
        val wrap = view.findViewById<FlexboxLayout>(R.id.highlight_wrap)
        wrap.removeAllViews()
        val tags = place.tags.ifEmpty { listOf("No highlights listed yet") }
        tags.forEach { tag ->
            val chip = TextView(requireContext())
            chip.text = tag
            chip.setBackgroundResource(R.drawable.bg_tag_outline)
            chip.setTextColor(resources.getColor(R.color.text_primary, null))
            chip.textSize = 12f
            chip.setPadding(dp(12), dp(8), dp(12), dp(8))
            val lp = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT, FlexboxLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, dp(8), dp(8))
            chip.layoutParams = lp
            wrap.addView(chip)
        }
    }

    /** Same sample hours shown for every place until Places API opening_hours is connected. */
    private fun bindOpeningHours(view: View) {
        val rows = listOf(
            Triple(R.id.hours_1, "Mon - Thu", "11:00 - 23:00"),
            Triple(R.id.hours_2, "Fri - Sat (Peak Vibe)", "11:00 - 02:00"),
            Triple(R.id.hours_3, "Sunday Chillout", "12:00 - 22:00")
        )
        rows.forEach { (id, day, time) ->
            val row = view.findViewById<View>(id)
            row.findViewById<TextView>(R.id.tv_hours_day).text = day
            row.findViewById<TextView>(R.id.tv_hours_time).text = time
            if (id == R.id.hours_2) {
                row.setBackgroundResource(R.drawable.bg_rating_bar_partial)
            }
        }
    }

    private fun bindRatingBars(container: LinearLayout) {
        DummyData.ratingBreakdown().take(3).forEach { (star, percent) ->
            val row = layoutInflater.inflate(R.layout.item_rating_bar_row, container, false)
            row.findViewById<TextView>(R.id.tv_bar_star).text = star.toString()
            row.findViewById<TextView>(R.id.tv_bar_percent).text = "$percent%"

            val fill = row.findViewById<View>(R.id.bar_fill)
            val fillParams = fill.layoutParams as LinearLayout.LayoutParams
            fillParams.weight = percent.toFloat()
            fill.layoutParams = fillParams

            val spacer = (row.findViewById<LinearLayout>(R.id.bar_track)).getChildAt(1)
            val spacerParams = spacer.layoutParams as LinearLayout.LayoutParams
            spacerParams.weight = (100 - percent).toFloat()
            spacer.layoutParams = spacerParams

            container.addView(row)
        }
    }

    private fun bindReviewPreviews(view: View) {
        val reviews = DummyData.reviewsFor(arguments?.getString("placeId") ?: "central_012")
        val ids = listOf(R.id.review_1, R.id.review_2)
        ids.forEachIndexed { index, id ->
            val root = view.findViewById<View>(id)
            val review = reviews.getOrNull(index)
            if (review == null) {
                root.visibility = View.GONE
            } else {
                root.findViewById<TextView>(R.id.tv_review_initials).text = review.initials
                root.findViewById<TextView>(R.id.tv_review_author).text = review.author
                root.findViewById<TextView>(R.id.tv_review_meta).text =
                    "${review.verifiedTag ?: "Guest"} · ${review.timeAgo}"
                root.findViewById<TextView>(R.id.tv_review_rating).text =
                    "★".repeat(review.rating.toInt()) + "☆".repeat(5 - review.rating.toInt())
                root.findViewById<TextView>(R.id.tv_review_body).text = review.body
            }
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}