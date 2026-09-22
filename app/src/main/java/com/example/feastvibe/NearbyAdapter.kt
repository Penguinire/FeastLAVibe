package com.example.feastvibe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NearbyAdapter(
    private var items: List<Place>,
    private val fullWidth: Boolean = false,
    private val onClick: (Place) -> Unit
) : RecyclerView.Adapter<NearbyAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_card_name)
        val meta: TextView = view.findViewById(R.id.tv_card_meta)
        val rating: TextView = view.findViewById(R.id.tv_card_rating)
        val price: TextView = view.findViewById(R.id.tv_card_price)
        val openBadge: TextView = view.findViewById(R.id.tv_card_open_badge)
        val distance: TextView = view.findViewById(R.id.tv_card_distance)
        val favourite: TextView = view.findViewById(R.id.tv_card_favourite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nearby_card, parent, false)
        if (fullWidth) {
            view.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.name.text = p.name
        holder.meta.text = "${p.cuisine} · ${p.area}"
        holder.rating.text = if (p.rating != null) {
            "★ ${p.rating} (${p.reviewCount ?: 0})"
        } else {
            "New"
        }
        holder.price.text = p.price ?: ""
        holder.openBadge.visibility = if (p.isOpen) View.VISIBLE else View.GONE
        holder.distance.text = "↗ ${p.distanceKm} km away"

        // Local-only toggle — not persisted until Firebase favourites are connected.
        holder.favourite.text = if (FavouritesStore.isFavourite(p.id)) "❤" else "♡"
        holder.favourite.setOnClickListener {
            val nowFav = FavouritesStore.toggle(p.id)
            holder.favourite.text = if (nowFav) "❤" else "♡"
        }

        holder.itemView.setOnClickListener { onClick(p) }
    }

    override fun getItemCount() = items.size

    fun submit(newItems: List<Place>) {
        items = newItems
        notifyDataSetChanged()
    }
}