package com.example.feastvibe

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.mapbox.geojson.Point
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchFragment : Fragment(R.layout.activity_search) {

    private val pretoriaCbd =
        Point.fromLngLat(28.1881, -25.7461)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        val input =
            view.findViewById<EditText>(R.id.et_search)

        val recentSection =
            view.findViewById<View>(R.id.recent_section)

        val suggestedSection =
            view.findViewById<View>(R.id.suggested_section)

        val resultsHeader =
            view.findViewById<View>(R.id.results_header)

        val results =
            view.findViewById<RecyclerView>(R.id.rv_results)

        val noResults =
            view.findViewById<View>(R.id.no_results_section)

        val resultsCount =
            view.findViewById<TextView>(R.id.tv_results_count)

        val noResultsMessage =
            view.findViewById<TextView>(R.id.tv_no_results_message)

        val suggestedList =
            view.findViewById<LinearLayout>(R.id.suggested_list)

        /*
         * ---------------------------------------------------------
         * BACK
         * ---------------------------------------------------------
         */

        view.findViewById<View>(R.id.iv_back)
            .setOnClickListener {

                findNavController().navigateUp()
            }

        /*
         * ---------------------------------------------------------
         * CLEAR
         * ---------------------------------------------------------
         */

        view.findViewById<View>(R.id.iv_clear)
            .setOnClickListener {

                input.setText("")
                showBrowseState(
                    recentSection,
                    suggestedSection,
                    resultsHeader,
                    results,
                    noResults
                )
            }

        /*
         * ---------------------------------------------------------
         * RESULT ADAPTER
         * ---------------------------------------------------------
         */

        val openDetail: (Place) -> Unit = { place ->

            PlaceCache.put(place)

            findNavController().navigate(
                R.id.restaurantDetailFragment,
                bundleOf(
                    "placeId" to place.id
                )
            )
        }

        val resultsAdapter =
            SearchResultAdapter(
                emptyList(),
                openDetail
            )

        results.layoutManager =
            LinearLayoutManager(requireContext())

        results.adapter =
            resultsAdapter

        /*
         * ---------------------------------------------------------
         * AUTOCOMPLETE SUGGESTIONS
         * ---------------------------------------------------------
         */

        var autocompleteJob: Job? = null

        fun clearSuggestions() {

            suggestedList.removeAllViews()
        }

        fun loadSuggestions(query: String) {

            autocompleteJob?.cancel()

            if (query.length < 3) {
                clearSuggestions()
                return
            }

            autocompleteJob =
                viewLifecycleOwner.lifecycleScope.launch {

                    delay(500)

                    val outcome =
                        try {
                            PlaceSearch.suggest(
                                query = query,
                                origin = pretoriaCbd,
                                limit = 5
                            )
                        } catch (e: Exception) {
                            null
                        }

                    if (!isAdded) {
                        return@launch
                    }

                    if (input.text.toString().trim() != query) {
                        return@launch
                    }

                    clearSuggestions()

                    val places =
                        outcome?.places.orEmpty()

                    if (places.isEmpty()) {
                        return@launch
                    }

                    PlaceCache.putAll(places)

                    places.forEach { place ->

                        val row =
                            LayoutInflater.from(requireContext())
                                .inflate(
                                    R.layout.item_place_suggestion,
                                    suggestedList,
                                    false
                                )

                        row.findViewById<TextView>(
                            R.id.tv_suggestion_title
                        ).text =
                            place.name

                        row.findViewById<TextView>(
                            R.id.tv_suggestion_subtitle
                        ).text =
                            "${place.cuisine} · ${place.area}"

                        row.setOnClickListener {

                            PlaceCache.put(place)

                            findNavController().navigate(
                                R.id.restaurantDetailFragment,
                                bundleOf(
                                    "placeId" to place.id
                                )
                            )
                        }

                        suggestedList.addView(row)
                    }
                }
        }

        /*
         * ---------------------------------------------------------
         * TEXT CHANGED
         * ---------------------------------------------------------
         */

        input.addTextChangedListener { text ->

            val query =
                text?.toString()?.trim().orEmpty()

            if (query.isBlank()) {

                showBrowseState(
                    recentSection,
                    suggestedSection,
                    resultsHeader,
                    results,
                    noResults
                )

                clearSuggestions()

            } else {

                /*
                 * Hide the old static browse content while
                 * the user is actively searching.
                 */
                recentSection.visibility =
                    View.GONE

                resultsHeader.visibility =
                    View.GONE

                results.visibility =
                    View.GONE

                noResults.visibility =
                    View.GONE

                suggestedSection.visibility =
                    View.VISIBLE

                loadSuggestions(query)
            }
        }

        /*
         * ---------------------------------------------------------
         * KEYBOARD SEARCH BUTTON
         * ---------------------------------------------------------
         */

        input.setOnEditorActionListener { _, actionId, event ->

            val pressedSearch =
                actionId == EditorInfo.IME_ACTION_SEARCH

            val pressedEnter =
                event?.keyCode == KeyEvent.KEYCODE_ENTER

            if (pressedSearch || pressedEnter) {

                val query =
                    input.text.toString().trim()

                if (query.isNotBlank()) {

                    clearSuggestions()

                    performFinalSearch(
                        query = query,
                        recentSection = recentSection,
                        suggestedSection = suggestedSection,
                        resultsHeader = resultsHeader,
                        results = results,
                        noResults = noResults,
                        resultsCount = resultsCount,
                        noResultsMessage = noResultsMessage,
                        adapter = resultsAdapter
                    )
                }

                true

            } else {

                false
            }
        }

        /*
         * ---------------------------------------------------------
         * RECENT SEARCHES
         * ---------------------------------------------------------
         */

        val chipRow =
            view.findViewById<LinearLayout>(
                R.id.recent_chip_row
            )

        DummyData.recentSearches
            .forEachIndexed { index, term ->

                val chip =
                    TextView(requireContext())

                chip.text = term

                chip.setBackgroundResource(
                    R.drawable.bg_chip_unselected
                )

                chip.setTextColor(
                    resources.getColor(
                        R.color.text_primary,
                        null
                    )
                )

                chip.textSize = 13f
                chip.gravity =
                    android.view.Gravity.CENTER

                chip.setPadding(
                    36,
                    0,
                    36,
                    0
                )

                val lp =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        dp(36)
                    )

                if (index > 0) {
                    lp.marginStart =
                        dp(8)
                }

                chip.layoutParams = lp

                chip.setOnClickListener {
                    input.setText(term)
                    input.setSelection(
                        input.text.length
                    )
                }

                chipRow.addView(chip)
            }

        /*
         * ---------------------------------------------------------
         * STATIC SUGGESTED ITEMS
         * ---------------------------------------------------------
         */

        DummyData.suggestions.forEach { label ->

            val row =
                LayoutInflater
                    .from(requireContext())
                    .inflate(
                        R.layout.item_suggested,
                        suggestedList,
                        false
                    )

            row.findViewById<TextView>(
                R.id.tv_suggested
            ).text = label

            row.setOnClickListener {

                input.setText(label)
                input.setSelection(
                    input.text.length
                )
            }

            suggestedList.addView(row)
        }

        /*
         * ---------------------------------------------------------
         * QUICK SUGGESTIONS
         * ---------------------------------------------------------
         */

        val quickWrap =
            view.findViewById<FlexboxLayout>(
                R.id.quick_suggestions_wrap
            )

        DummyData.quickSuggestions
            .forEach { term ->

                val chip =
                    TextView(requireContext())

                chip.text = term

                chip.setBackgroundResource(
                    R.drawable.bg_chip_unselected
                )

                chip.setTextColor(
                    resources.getColor(
                        R.color.text_primary,
                        null
                    )
                )

                chip.textSize = 12f

                chip.setPadding(
                    dp(14),
                    dp(8),
                    dp(14),
                    dp(8)
                )

                val lp =
                    FlexboxLayout.LayoutParams(
                        FlexboxLayout.LayoutParams.WRAP_CONTENT,
                        FlexboxLayout.LayoutParams.WRAP_CONTENT
                    )

                lp.setMargins(
                    dp(4),
                    dp(4),
                    dp(4),
                    dp(4)
                )

                chip.layoutParams = lp

                chip.setOnClickListener {

                    input.setText(term)

                    input.setSelection(
                        input.text.length
                    )
                }

                quickWrap.addView(chip)
            }

        /*
         * ---------------------------------------------------------
         * CLEAR RECENT
         * ---------------------------------------------------------
         */

        view.findViewById<View>(
            R.id.tv_clear_recent
        ).setOnClickListener {

            chipRow.removeAllViews()
        }

        /*
         * ---------------------------------------------------------
         * OTHER BUTTONS
         * ---------------------------------------------------------
         */

        view.findViewById<View>(
            R.id.btn_explore_hotspots
        ).setOnClickListener {

            findNavController().navigateUp()
        }

        view.findViewById<View>(
            R.id.btn_reset_filters
        ).setOnClickListener {

            input.setText("")
        }
    }

    /*
     * -------------------------------------------------------------
     * FINAL SEARCH
     * -------------------------------------------------------------
     */

    private fun performFinalSearch(
        query: String,
        recentSection: View,
        suggestedSection: View,
        resultsHeader: View,
        results: RecyclerView,
        noResults: View,
        resultsCount: TextView,
        noResultsMessage: TextView,
        adapter: SearchResultAdapter
    ) {

        recentSection.visibility =
            View.GONE

        suggestedSection.visibility =
            View.GONE

        resultsHeader.visibility =
            View.GONE

        results.visibility =
            View.GONE

        noResults.visibility =
            View.GONE

        lifecycleScope.launch {

            val outcome = try {

                PlaceSearch.search(
                    query = query,
                    origin = pretoriaCbd,
                    limit = 8
                )

            } catch (e: Exception) {

                android.util.Log.e(
                    "SearchFragment",
                    "Final search failed for '$query'",
                    e
                )

                null
            }

            if (outcome == null) {

                noResults.visibility =
                    View.VISIBLE

                noResultsMessage.text =
                    "Unable to load places. Check your internet connection and Mapbox access."

                return@launch
            }

            PlaceCache.putAll(
                outcome.places
            )

            if (outcome.places.isEmpty()) {

                noResults.visibility =
                    View.VISIBLE

                noResultsMessage.text =
                    "We couldn't find any restaurants, bars or places matching \"$query\"."

                return@launch
            }

            resultsHeader.visibility =
                View.VISIBLE

            results.visibility =
                View.VISIBLE

            resultsCount.text =
                "${outcome.places.size} Hotspots Found ●"

            adapter.submit(
                outcome.places
            )
        }
    }

    private fun showBrowseState(
        recentSection: View,
        suggestedSection: View,
        resultsHeader: View,
        results: RecyclerView,
        noResults: View
    ) {

        recentSection.visibility =
            View.VISIBLE

        suggestedSection.visibility =
            View.VISIBLE

        resultsHeader.visibility =
            View.GONE

        results.visibility =
            View.GONE

        noResults.visibility =
            View.GONE
    }

    private fun dp(value: Int): Int =
        (
                value *
                        resources.displayMetrics.density
                ).toInt()
}