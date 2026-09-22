package com.example.feastvibe

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.materialswitch.MaterialSwitch

class FiltersFragment : Fragment(R.layout.activity_filters) {

    private var selectedType: PlaceType? = null // null = "All"
    private val selectedCuisines = mutableSetOf<String>()
    private var selectedPrice: String? = "R150-R300"
    private var selectedMinRating: Double = 4.5
    private var loadsheddingOnly = false

    private lateinit var resultsButton: TextView
    private lateinit var openNowSwitch: MaterialSwitch
    private lateinit var within5kmSwitch: MaterialSwitch

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        resultsButton = view.findViewById(R.id.btn_show_results)
        openNowSwitch = view.findViewById(R.id.switch_open_now)
        within5kmSwitch = view.findViewById(R.id.switch_within_5km)

        view.findViewById<View>(R.id.iv_filters_close).setOnClickListener {
            findNavController().navigateUp()
        }
        view.findViewById<View>(R.id.btn_show_results).setOnClickListener {
            applyToExplore()
            findNavController().navigateUp()
        }
        view.findViewById<View>(R.id.tv_clear_all).setOnClickListener { resetAll(view) }
        view.findViewById<View>(R.id.tv_filters_reset).setOnClickListener { resetAll(view) }

        setupPlaceTypeChips(view)
        setupCuisineChips(view)
        setupSpendRow(view)
        setupRatingRow(view)
        setupLoadshedding(view)

        openNowSwitch.setOnCheckedChangeListener { _, _ -> updateResultCount() }
        within5kmSwitch.setOnCheckedChangeListener { _, _ -> updateResultCount() }

        updateResultCount()
    }

    private fun setupPlaceTypeChips(view: View) {
        val wrap = view.findViewById<FlexboxLayout>(R.id.type_chip_wrap)
        val options = listOf(
            "All" to null, "Restaurant" to PlaceType.RESTAURANT,
            "Fast Food" to PlaceType.FAST_FOOD, "Coffee" to PlaceType.FAST_FOOD,
            "Bars" to PlaceType.BAR, "Clubs" to PlaceType.CLUB
        )
        val chips = mutableListOf<TextView>()
        options.forEach { (label, type) ->
            val chip = makeChip(label, selected = (type == null))
            chips.add(chip)
            addToFlexbox(wrap, chip)
            chip.setOnClickListener {
                selectedType = type
                chips.forEach { paintChip(it, it == chip) }
                updateResultCount()
            }
        }
    }

    private fun setupCuisineChips(view: View) {
        val wrap = view.findViewById<FlexboxLayout>(R.id.cuisine_chip_wrap)
        val options = listOf("African", "Braai", "Italian", "Pizza", "Burgers", "Asian", "Café")
        options.forEach { label ->
            val isPreselected = label == "Braai"
            if (isPreselected) selectedCuisines.add(label)
            val chip = makeChip(label, selected = isPreselected)
            addToFlexbox(wrap, chip)
            chip.setOnClickListener {
                if (selectedCuisines.contains(label)) {
                    selectedCuisines.remove(label)
                    paintChip(chip, false)
                } else {
                    selectedCuisines.add(label)
                    paintChip(chip, true)
                }
                updateResultCount()
            }
        }
    }

    private fun setupSpendRow(view: View) {
        val row = view.findViewById<LinearLayout>(R.id.spend_row)
        val options = listOf("R", "R150-R300", "R300+")
        val segments = mutableListOf<TextView>()
        options.forEach { label ->
            val seg = makeSegment(label, selected = label == selectedPrice)
            segments.add(seg)
            row.addView(seg)
            seg.setOnClickListener {
                selectedPrice = label
                segments.forEach { paintSegment(it, it == seg) }
                updateResultCount()
            }
        }
    }

    private fun setupRatingRow(view: View) {
        val row = view.findViewById<LinearLayout>(R.id.rating_row)
        val options = listOf("Any" to 0.0, "4.0+" to 4.0, "4.5+" to 4.5)
        val segments = mutableListOf<TextView>()
        options.forEach { (label, value) ->
            val seg = makeSegment(label, selected = value == selectedMinRating)
            segments.add(seg)
            row.addView(seg)
            seg.setOnClickListener {
                selectedMinRating = value
                segments.forEach { paintSegment(it, it == seg) }
                updateResultCount()
            }
        }
    }

    private fun setupLoadshedding(view: View) {
        val row = view.findViewById<View>(R.id.loadshedding_row)
        val check = view.findViewById<TextView>(R.id.tv_loadshedding_check)
        row.setOnClickListener {
            loadsheddingOnly = !loadsheddingOnly
            check.text = if (loadsheddingOnly) "✓" else ""
            updateResultCount()
        }
    }

    private fun applyToExplore() {
        FilterState.type = selectedType
        FilterState.cuisines = selectedCuisines.toSet()
        FilterState.price = selectedPrice
        FilterState.minRating = selectedMinRating
        FilterState.openNowOnly = openNowSwitch.isChecked
        FilterState.within5kmOnly = within5kmSwitch.isChecked
        FilterState.loadsheddingOnly = loadsheddingOnly
        FilterState.isActive = true
    }

    private fun resetAll(view: View) {
        FilterState.reset()
        selectedType = null
        selectedCuisines.clear()
        selectedPrice = null
        selectedMinRating = 0.0
        loadsheddingOnly = false
        openNowSwitch.isChecked = false
        within5kmSwitch.isChecked = false
        view.findViewById<TextView>(R.id.tv_loadshedding_check).text = ""
        // Simplest reliable reset: rebuild every chip row from scratch.
        view.findViewById<FlexboxLayout>(R.id.type_chip_wrap).removeAllViews()
        view.findViewById<FlexboxLayout>(R.id.cuisine_chip_wrap).removeAllViews()
        view.findViewById<LinearLayout>(R.id.spend_row).removeAllViews()
        view.findViewById<LinearLayout>(R.id.rating_row).removeAllViews()
        setupPlaceTypeChips(view)
        setupCuisineChips(view)
        setupSpendRow(view)
        setupRatingRow(view)
        updateResultCount()
    }

    /** Filters the real dummy dataset, so the count is accurate today and stays
     *  accurate once DummyData is swapped for live Places API results. */
    private fun updateResultCount() {
        val matches = DummyData.places.filter { place ->
            (selectedType == null || place.type == selectedType) &&
                    (selectedCuisines.isEmpty() || selectedCuisines.any { c ->
                        place.tags.any { it.contains(c, ignoreCase = true) } ||
                                place.cuisine.contains(c, ignoreCase = true)
                    }) &&
                    (selectedPrice == null || place.price == selectedPrice) &&
                    (place.rating == null || place.rating >= selectedMinRating) &&
                    (!openNowSwitch.isChecked || place.isOpen) &&
                    (!within5kmSwitch.isChecked || place.distanceKm <= 5.0) &&
                    (!loadsheddingOnly || place.generatorBackup)
        }
        resultsButton.text = "Show ${matches.size} Results"
    }

    private fun makeChip(label: String, selected: Boolean): TextView {
        val chip = TextView(requireContext())
        chip.text = label
        chip.textSize = 13f
        chip.setPadding(dp(16), dp(9), dp(16), dp(9))
        paintChip(chip, selected)
        val lp = FlexboxLayout.LayoutParams(
            FlexboxLayout.LayoutParams.WRAP_CONTENT, FlexboxLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(0, 0, dp(8), dp(8))
        chip.layoutParams = lp
        return chip
    }

    private fun paintChip(chip: TextView, selected: Boolean) {
        chip.setBackgroundResource(if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
        chip.setTextColor(resources.getColor(if (selected) R.color.white else R.color.text_primary, null))
    }

    private fun makeSegment(label: String, selected: Boolean): TextView {
        val seg = TextView(requireContext())
        seg.text = label
        seg.textSize = 13f
        seg.gravity = android.view.Gravity.CENTER
        paintSegment(seg, selected)
        seg.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        return seg
    }

    private fun paintSegment(seg: TextView, selected: Boolean) {
        seg.setBackgroundResource(if (selected) R.drawable.bg_chip_selected else android.R.color.transparent)
        seg.setTextColor(resources.getColor(if (selected) R.color.white else R.color.text_secondary, null))
    }

    private fun addToFlexbox(wrap: FlexboxLayout, chip: TextView) = wrap.addView(chip)

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}