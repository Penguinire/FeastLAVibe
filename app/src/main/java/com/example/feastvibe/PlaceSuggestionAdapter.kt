package com.example.feastvibe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlaceSuggestionAdapter(
    private var items: List<String>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<PlaceSuggestionAdapter.SuggestionViewHolder>() {

    class SuggestionViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {

        val title: TextView =
            view.findViewById(R.id.tv_suggestion_title)

        val subtitle: TextView =
            view.findViewById(R.id.tv_suggestion_subtitle)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SuggestionViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_place_suggestion,
                parent,
                false
            )

        return SuggestionViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: SuggestionViewHolder,
        position: Int
    ) {

        val text = items[position]

        holder.title.text = text
        holder.subtitle.text = "Mapbox suggestion"

        holder.itemView.setOnClickListener {
            onClick(position)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }
}