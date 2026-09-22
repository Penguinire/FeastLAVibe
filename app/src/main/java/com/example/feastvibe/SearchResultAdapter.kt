package com.example.feastvibe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SearchResultAdapter(
    private var items: List<Place>,
    private val onClick: (Place) -> Unit,
) : RecyclerView.Adapter<SearchResultAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_result_name)
        val rating: TextView = view.findViewById(R.id.tv_result_rating)
        val meta: TextView = view.findViewById(R.id.tv_result_meta)
        val price: TextView = view.findViewById(R.id.tv_result_price)
        val distance: TextView = view.findViewById(R.id.tv_result_distance)
        val open: TextView = view.findViewById(R.id.tv_result_open)
        val generator: TextView = view.findViewById(R.id.tv_result_generator)
        val favourite: TextView = view.findViewById(R.id.tv_result_favourite)
        val tagRow: LinearLayout = view.findViewById(R.id.result_tag_row)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.name.text = p.name
        holder.rating.text = if (p.rating != null) {
            "★ ${p.rating} (${p.reviewCount ?: 0})"
        } else {
            "New"
        }
        holder.meta.text = "${p.area} · ${p.cuisine}"
        holder.price.text = p.price ?: "Price N/A"
        holder.distance.text = "${p.distanceKm} km"
        holder.open.visibility = if (p.isOpen) View.VISIBLE else View.GONE
        holder.generator.visibility = if (p.generatorBackup) View.VISIBLE else View.GONE

        holder.tagRow.removeAllViews()
        p.tags.forEach { tag ->
            val tagView = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.item_search_tag, holder.tagRow, false) as TextView
            tagView.text = tag
            holder.tagRow.addView(tagView)
        }

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