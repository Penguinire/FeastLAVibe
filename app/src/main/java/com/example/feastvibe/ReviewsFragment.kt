package com.example.feastvibe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class ReviewsFragment : Fragment(R.layout.activity_reviews) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val placeId = arguments?.getString("placeId") ?: "central_012"
        val place = DummyData.byId(placeId)
        val reviews = DummyData.reviewsFor(place.id).toMutableList()

        view.findViewById<TextView>(R.id.tv_reviews_place_name).text = place.name
        view.findViewById<TextView>(R.id.tv_verified_count).text =
            "${place.reviewCount ?: 0} verified vibes"
        view.findViewById<TextView>(R.id.tv_big_rating).text =
            place.rating?.toString() ?: "New"

        view.findViewById<View>(R.id.iv_reviews_back).setOnClickListener {
            findNavController().navigateUp()
        }

        bindRatingBars(view.findViewById(R.id.reviews_rating_bars))
        bindStarInput(view)
        bindReviewList(view, reviews)

        view.findViewById<View>(R.id.btn_submit_review).setOnClickListener {
            // Visual only — no submission target until Firebase reviews are connected.
            view.findViewById<android.widget.EditText>(R.id.et_review_body).setText("")
        }

        view.findViewById<View>(R.id.btn_load_more).setOnClickListener {
            // Sample data only has a handful of reviews — nothing further to load yet.
            it.visibility = View.GONE
        }
    }

    private fun bindRatingBars(container: LinearLayout) {
        DummyData.ratingBreakdown().forEach { (star, percent) ->
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

    private fun bindStarInput(view: View) {
        val row = view.findViewById<LinearLayout>(R.id.star_input_row)
        val stars = (1..5).map { index ->
            TextView(requireContext()).apply {
                text = "☆"
                textSize = 26f
                setTextColor(resources.getColor(R.color.text_secondary, null))
                setPadding(dp(4), 0, dp(4), 0)
                tag = index
            }
        }
        stars.forEach { star ->
            row.addView(star)
            star.setOnClickListener {
                val selected = star.tag as Int
                stars.forEach { s ->
                    val filled = (s.tag as Int) <= selected
                    s.text = if (filled) "★" else "☆"
                    s.setTextColor(
                        resources.getColor(
                            if (filled) R.color.star_yellow else R.color.text_secondary, null
                        )
                    )
                }
            }
        }
    }

    private fun bindReviewList(view: View, reviews: MutableList<Review>) {
        val container = view.findViewById<LinearLayout>(R.id.review_list)
        reviews.forEach { review ->
            val row = layoutInflater.inflate(R.layout.item_review_full, container, false)
            row.findViewById<TextView>(R.id.tv_full_initials).text = review.initials
            row.findViewById<TextView>(R.id.tv_full_author).text = review.author
            row.findViewById<TextView>(R.id.tv_full_time).text = review.timeAgo
            row.findViewById<TextView>(R.id.tv_full_rating).text =
                "★".repeat(review.rating.toInt()) + "☆".repeat(5 - review.rating.toInt())
            row.findViewById<TextView>(R.id.tv_full_body).text = review.body

            val vibeTagView = row.findViewById<TextView>(R.id.tv_full_vibe_tag)
            if (review.vibeTag != null) {
                vibeTagView.text = review.vibeTag
            } else {
                vibeTagView.visibility = View.GONE
            }

            val helpfulView = row.findViewById<TextView>(R.id.tv_full_helpful)
            var count = review.helpfulCount
            helpfulView.text = "👍 Helpful ($count)"
            helpfulView.setOnClickListener {
                count += 1
                helpfulView.text = "👍 Helpful ($count)"
            }

            container.addView(row)
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}