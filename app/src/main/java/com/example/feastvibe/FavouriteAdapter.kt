package com.example.feastvibe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FavouriteAdapter(
    private var items: List<Place>,
    private val onClick: (Place) -> Unit,
    private val onRemove: () -> Unit
) : RecyclerView.Adapter<FavouriteAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val category: TextView = view.findViewById(R.id.tv_fav_category)
        val heart: TextView = view.findViewById(R.id.tv_fav_heart)
        val rating: TextView = view.findViewById(R.id.tv_fav_rating)
        val name: TextView = view.findViewById(R.id.tv_fav_name)
        val area: TextView = view.findViewById(R.id.tv_fav_area)
        val cuisine: TextView = view.findViewById(R.id.tv_fav_cuisine)
        val price: TextView = view.findViewById(R.id.tv_fav_price)
        val tag: TextView = view.findViewById(R.id.tv_fav_tag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favourite_card, parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.category.text = when (p.type) {
            PlaceType.RESTAURANT -> "🍽 RESTAURANT"
            PlaceType.FAST_FOOD -> "🍔 FAST FOOD"
            PlaceType.BAR -> "🍸 BAR"
            PlaceType.CLUB -> "🎧 CLUB"
        }
        holder.rating.text = if (p.rating != null) "★ ${p.rating}" else "New"
        holder.name.text = p.name
        holder.area.text = "📍 ${p.area}"
        holder.cuisine.text = p.cuisine
        holder.price.text = p.price ?: "Price N/A"
        holder.tag.text = if (p.generatorBackup) "⚡ Loadshedding Proof" else (p.tags.firstOrNull() ?: "")
        holder.tag.visibility = if (holder.tag.text.isBlank()) View.GONE else View.VISIBLE

        holder.heart.setOnClickListener {
            FavouritesStore.toggle(p.id)
            onRemove()
        }

        holder.itemView.setOnClickListener { onClick(p) }
    }

    override fun getItemCount() = items.size

    fun submit(newItems: List<Place>) {
        items = newItems
        notifyDataSetChanged()
    }
}