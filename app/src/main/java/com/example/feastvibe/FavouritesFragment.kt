package com.example.feastvibe

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FavouritesFragment : Fragment(R.layout.activity_favourites) {

    private lateinit var adapter: FavouriteAdapter
    private var currentFilter: (Place) -> Boolean = { true }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val openDetail: (Place) -> Unit = { place ->
            findNavController().navigate(
                R.id.restaurantDetailFragment,
                bundleOf("placeId" to place.id)
            )
        }

        adapter = FavouriteAdapter(emptyList(), openDetail) { refresh() }

        val screenWidthDp = resources.displayMetrics.widthPixels /
                resources.displayMetrics.density

        val spanCount = when {
            screenWidthDp >= 900 -> 4
            screenWidthDp >= 600 -> 3
            screenWidthDp >= 360 -> 2
            else -> 1
        }

        view.findViewById<RecyclerView>(R.id.recycler_favourites).apply {
            layoutManager = GridLayoutManager(requireContext(), spanCount)
            adapter = this@FavouritesFragment.adapter
            setHasFixedSize(false)
        }

        setupChips(view)

        view.findViewById<View>(R.id.btn_explore_places).setOnClickListener {
            findNavController().navigate(R.id.exploreFragment)
        }

        bindEmptySuggestions(view, openDetail)
    }

    override fun onResume() {
        super.onResume()
        // Favourites can change on any other screen, so refresh every time this tab is shown.
        refresh()
    }

    private fun setupChips(view: View) {
        val row = view.findViewById<LinearLayout>(R.id.fav_chip_row)
        val filters: List<(Place) -> Boolean> = listOf(
            { true },
            { it.type == PlaceType.RESTAURANT || it.type == PlaceType.FAST_FOOD },
            { it.type == PlaceType.BAR || it.type == PlaceType.CLUB }
        )
        for (i in 0 until minOf(row.childCount, filters.size)) {
            row.getChildAt(i).setOnClickListener {
                for (j in 0 until minOf(row.childCount, filters.size)) {
                    val chip = row.getChildAt(j) as TextView
                    if (j == i) {
                        chip.setBackgroundResource(R.drawable.bg_chip_selected)
                        chip.setTextColor(resources.getColor(R.color.white, null))
                    } else {
                        chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                        chip.setTextColor(resources.getColor(R.color.text_primary, null))
                    }
                }
                currentFilter = filters[i]
                refresh()
            }
        }
    }

    private fun refresh() {
        val allFavourites = FavouritesStore.all()
        val favourites = allFavourites.filter(currentFilter)

        val root = requireView()
        val recycler = root.findViewById<View>(R.id.recycler_favourites)
        val emptyState = root.findViewById<View>(R.id.fav_empty_state)

        val emptyTitle = root.findViewById<TextView>(R.id.tv_fav_empty_title)
        val emptyMessage = root.findViewById<TextView>(R.id.tv_fav_empty_message)
        val suggestions = root.findViewById<View>(R.id.empty_suggestions_list)
        val suggestionsTitle = root.findViewById<View>(R.id.tv_empty_suggestions_title)
        val exploreButton = root.findViewById<View>(R.id.btn_explore_places)

        root.findViewById<TextView>(R.id.tv_fav_saved_count).text =
            "${allFavourites.size} Saved"

        if (favourites.isNotEmpty()) {
            emptyState.visibility = View.GONE
            recycler.visibility = View.VISIBLE
            adapter.submit(favourites)
        } else {
            recycler.visibility = View.GONE
            emptyState.visibility = View.VISIBLE

            if (allFavourites.isEmpty()) {
                emptyTitle.text = "Your favourite places will appear here"
                emptyMessage.text =
                    "Save restaurants, cafes, and nightlife spots while exploring Feast & Vibe."

                suggestions.visibility = View.VISIBLE
                suggestionsTitle.visibility = View.VISIBLE
                exploreButton.visibility = View.VISIBLE

            } else {
                emptyTitle.text = "No matching favourites"
                emptyMessage.text =
                    "You haven't saved any places in this category yet."

                suggestions.visibility = View.GONE
                suggestionsTitle.visibility = View.GONE
                exploreButton.visibility = View.GONE
            }
        }
    }

    /** Quick-add list shown only in the empty state — lets you populate Favourites without leaving the tab. */
    private fun bindEmptySuggestions(view: View, openDetail: (Place) -> Unit) {
        val container = view.findViewById<LinearLayout>(R.id.empty_suggestions_list)
        DummyData.places.take(4).forEach { place ->
            val row = layoutInflater.inflate(R.layout.item_restaurant_list, container, false)
            row.findViewById<TextView>(R.id.tv_row_name).text = place.name
            row.findViewById<TextView>(R.id.tv_row_rating).text =
                if (place.rating != null) "★ ${place.rating}" else "New"
            row.findViewById<TextView>(R.id.tv_row_meta).text = place.cuisine
            row.findViewById<TextView>(R.id.tv_row_area).text = "📍 ${place.area}"
            val statusView = row.findViewById<TextView>(R.id.tv_row_status)
            statusView.visibility =
                if (place.isOpen == null) View.GONE else View.VISIBLE
            statusView.text = when (place.isOpen) {
                true -> "Open Now"
                false -> "Closed"
                null -> ""
            }
            row.findViewById<TextView>(R.id.tv_row_reviews).text =
                if (place.reviewCount != null) "${place.reviewCount} reviews" else "No reviews yet"
            row.setOnClickListener {
                FavouritesStore.toggle(place.id)
                refresh()
            }
            container.addView(row)
        }
    }
}