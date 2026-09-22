package com.example.feastvibe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RestaurantListAdapter(
    private var items: List<Place>,
    private val onClick: (Place) -> Unit
) : RecyclerView.Adapter<RestaurantListAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_row_name)
        val rating: TextView = view.findViewById(R.id.tv_row_rating)
        val meta: TextView = view.findViewById(R.id.tv_row_meta)
        val area: TextView = view.findViewById(R.id.tv_row_area)
        val status: TextView = view.findViewById(R.id.tv_row_status)
        val reviews: TextView = view.findViewById(R.id.tv_row_reviews)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_restaurant_list, parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.name.text = p.name
        holder.rating.text = if (p.rating != null) "★ ${p.rating}" else "New"
        holder.meta.text = p.cuisine
        holder.area.text = "📍 ${p.area}"
        holder.status.text = if (p.isOpen) "Open Now" else "Closed"
        holder.status.setTextColor(
            holder.itemView.resources.getColor(
                if (p.isOpen) R.color.open_green else R.color.text_secondary, null
            )
        )
        holder.reviews.text =
            if (p.reviewCount != null) "${p.reviewCount} reviews" else "No reviews yet"
        holder.itemView.setOnClickListener { onClick(p) }
    }

    override fun getItemCount() = items.size

    fun submit(newItems: List<Place>) {
        items = newItems
        notifyDataSetChanged()
    }
}