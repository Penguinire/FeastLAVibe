package com.example.feastvibe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Adapted from Bear's Ticketmaster integration, wired into the Events layout. */
class EventsFragment : Fragment(R.layout.activity_events) {

    private enum class DateRange { WEEKEND, WEEK, MONTH }

    private var allEvents: List<Event> = emptyList()
    private var currentRange: DateRange = DateRange.WEEKEND
    private lateinit var rootView: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        rootView = view

        view.findViewById<View>(R.id.iv_events_profile).setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }

        setupChips(view)
        setupSearch(view)

        view.findViewById<View>(R.id.btn_submit_event).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://business.ticketmaster.com/")))
        }

        lifecycleScope.launch {
            val result = EventRepository.getEvents(city = "Pretoria")
            result.onSuccess { events ->
                allEvents = events
                EventCache.putAll(events)
                if (events.isEmpty()) {
                    view.findViewById<View>(R.id.tv_events_empty).visibility = View.VISIBLE
                } else {
                    bindFeatured(view, events.first())
                    renderList()
                }
            }.onFailure {
                view.findViewById<View>(R.id.tv_events_empty).visibility = View.VISIBLE
            }
        }
    }

    private fun setupChips(view: View) {
        val row = view.findViewById<LinearLayout>(R.id.events_chip_row)
        val ranges = listOf(DateRange.WEEKEND, DateRange.WEEK, DateRange.MONTH)
        for (i in 0 until row.childCount) {
            row.getChildAt(i).setOnClickListener {
                for (j in 0 until row.childCount) {
                    val chip = row.getChildAt(j) as TextView
                    if (j == i) {
                        chip.setBackgroundResource(R.drawable.bg_chip_selected)
                        chip.setTextColor(resources.getColor(R.color.white, null))
                    } else {
                        chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                        chip.setTextColor(resources.getColor(R.color.text_primary, null))
                    }
                }
                currentRange = ranges[i]
                view.findViewById<android.widget.EditText>(R.id.et_events_search).setText("")
                renderList()
            }
        }
    }

    private fun setupSearch(view: View) {
        view.findViewById<android.widget.EditText>(R.id.et_events_search).addTextChangedListener { text ->
            val query = text?.toString()?.trim().orEmpty()
            if (query.isEmpty()) {
                renderList()
            } else {
                val matches = allEvents.filter { it.name.contains(query, ignoreCase = true) }
                bindList(view, matches)
            }
        }
    }

    /** Filters allEvents by the selected date range. Falls back to showing everything
     *  if nothing matches, rather than leaving the screen looking empty/broken —
     *  the free Ticketmaster feed for one city won't always have something every weekend. */
    private fun renderList() {
        val filtered = allEvents.filter { matchesRange(it.date, currentRange) }
        bindList(rootView, filtered.ifEmpty { allEvents })
    }

    private fun matchesRange(isoDate: String, range: DateRange): Boolean {
        val date = parseDate(isoDate) ?: return false
        val eventCal = Calendar.getInstance().apply { time = date }
        val today = Calendar.getInstance()
        return when (range) {
            DateRange.WEEK -> {
                val end = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 7) }
                !date.before(today.time) && !date.after(end.time)
            }
            DateRange.WEEKEND -> {
                val sat = (today.clone() as Calendar)
                while (sat.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) sat.add(Calendar.DAY_OF_YEAR, 1)
                val sun = (sat.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
                isSameDay(eventCal, sat) || isSameDay(eventCal, sun)
            }
            DateRange.MONTH -> {
                eventCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        eventCal.get(Calendar.MONTH) == today.get(Calendar.MONTH)
            }
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun parseDate(isoDate: String): Date? = try {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(isoDate)
    } catch (e: Exception) {
        null
    }

    private fun bindFeatured(view: View, event: Event) {
        EventCache.put(event)
        view.findViewById<TextView>(R.id.tv_featured_title).text = event.name
        view.findViewById<TextView>(R.id.tv_featured_meta).text =
            listOf(event.date, event.venue).filter { it.isNotBlank() }.joinToString(" · ")

        if (event.imageUrl != null) {
            Glide.with(this).load(event.imageUrl).into(view.findViewById<ImageView>(R.id.iv_featured_image))
        }

        view.findViewById<View>(R.id.btn_featured_details).setOnClickListener { openDetails(event) }
        view.findViewById<View>(R.id.btn_featured_tickets).setOnClickListener { openTickets(event) }
    }

    private fun bindList(view: View, events: List<Event>) {
        val container = view.findViewById<LinearLayout>(R.id.events_list)
        container.removeAllViews()
        events.take(20).forEach { event ->
            EventCache.put(event)
            val row = layoutInflater.inflate(R.layout.item_event_card, container, false)
            row.findViewById<TextView>(R.id.tv_event_price).text =
                if (event.status?.equals("onsale", ignoreCase = true) == true) "On Sale" else "Tickets"
            row.findViewById<TextView>(R.id.tv_event_title).text = event.name
            row.findViewById<TextView>(R.id.tv_event_meta).text =
                listOf(event.date, event.time).filter { it.isNotBlank() }.joinToString(" · ")
            row.findViewById<TextView>(R.id.tv_event_venue).text =
                listOf(event.venue, event.city).filter { it.isNotBlank() }.joinToString(" · ")

            if (event.imageUrl != null) {
                Glide.with(this).load(event.imageUrl).into(row.findViewById<ImageView>(R.id.iv_event_thumb))
            }

            row.findViewById<View>(R.id.btn_event_details).setOnClickListener { openDetails(event) }
            row.findViewById<View>(R.id.btn_event_tickets).setOnClickListener { openTickets(event) }
            container.addView(row)
        }
    }

    private fun openDetails(event: Event) {
        findNavController().navigate(R.id.eventDetailFragment, bundleOf("eventId" to event.id))
    }

    private fun openTickets(event: Event) {
        val url = event.ticketUrl ?: return
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}