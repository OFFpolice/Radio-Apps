package com.example.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.R
import com.example.data.ApiStation
import com.example.data.FavoriteStation

data class UIStationItem(
    val urlResolved: String,
    val name: String,
    val tags: String?,
    val favicon: String?,
    var isFavorite: Boolean = false,
    val originalApi: ApiStation? = null,
    val originalFav: FavoriteStation? = null
)

class StationAdapter(
    private val onSelect: (UIStationItem) -> Unit,
    private val onToggleFavorite: (UIStationItem) -> Unit
) : ListAdapter<UIStationItem, StationAdapter.ViewHolder>(DiffCallback) {

    private var activeUrl: String? = null

    fun setActiveUrl(url: String?) {
        val oldUrl = activeUrl
        activeUrl = url
        // Full bind update is fast enough for list of items; we notify state changes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.station_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, item.urlResolved == activeUrl, onSelect, onToggleFavorite)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rootCard = itemView.findViewById<View>(R.id.stationCardRoot)
        val coverImage = itemView.findViewById<ImageView>(R.id.stationCoverImage)
        val stationName = itemView.findViewById<TextView>(R.id.stationName)
        val compactEqualizer = itemView.findViewById<View>(R.id.compactEqualizer)
        val tagChip1 = itemView.findViewById<TextView>(R.id.tagChip1)
        val tagChip2 = itemView.findViewById<TextView>(R.id.tagChip2)
        val tagChip3 = itemView.findViewById<TextView>(R.id.tagChip3)
        val favoriteBtn = itemView.findViewById<ImageButton>(R.id.favoriteBtn)

        fun bind(
            item: UIStationItem,
            isActive: Boolean,
            onSelect: (UIStationItem) -> Unit,
            onToggleFavorite: (UIStationItem) -> Unit
        ) {
            // Set Card Active/Inactive Background & Equalizer Visibility
            if (isActive) {
                rootCard.setBackgroundResource(R.drawable.active_card_bg_rounded)
                compactEqualizer.visibility = View.VISIBLE
            } else {
                rootCard.setBackgroundResource(R.drawable.card_bg_rounded)
                compactEqualizer.visibility = View.GONE
            }

            // Bind Station Name
            stationName.text = item.name.trim().ifEmpty { "Без названия" }

            // Load favicon with Coil
            if (!item.favicon.isNullOrBlank()) {
                coverImage.load(item.favicon) {
                    crossfade(true)
                    placeholder(R.drawable.ic_radio)
                    error(R.drawable.ic_radio)
                }
            } else {
                coverImage.setImageResource(R.drawable.ic_radio)
            }

            // Apply consistent Cover icon tint only if it remains fallback
            if (item.favicon.isNullOrBlank()) {
                coverImage.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.context, R.color.secondary_pink)
                )
            } else {
                coverImage.imageTintList = null
            }

            // Parse and Bind Tags
            val parsedTags = item.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.distinct()?.take(3) ?: emptyList()
            
            // Set tag chips visibility
            val chips = listOf(tagChip1, tagChip2, tagChip3)
            chips.forEach { it.visibility = View.GONE }

            if (parsedTags.isNotEmpty()) {
                parsedTags.forEachIndexed { index, tag ->
                    if (index < chips.size) {
                        val chip = chips[index]
                        chip.text = tag
                        chip.visibility = View.VISIBLE

                        // Alternate style depending on active stream
                        if (isActive) {
                            chip.setBackgroundResource(R.drawable.active_tag_bg_rounded)
                            chip.setTextColor(Color.parseColor("#E0B0C0"))
                        } else {
                            chip.setBackgroundResource(R.drawable.tag_bg_rounded)
                            chip.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                        }
                    }
                }
            } else {
                tagChip1.text = "radio"
                tagChip1.visibility = View.VISIBLE
                if (isActive) {
                    tagChip1.setBackgroundResource(R.drawable.active_tag_bg_rounded)
                    tagChip1.setTextColor(Color.parseColor("#E0B0C0"))
                } else {
                    tagChip1.setBackgroundResource(R.drawable.tag_bg_rounded)
                    tagChip1.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                }
            }

            // Bind Favorite Icon Status
            if (item.isFavorite) {
                favoriteBtn.setImageResource(R.drawable.ic_favorite_filled)
                favoriteBtn.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.context, R.color.primary_pink)
                )
            } else {
                favoriteBtn.setImageResource(R.drawable.ic_favorite_border)
                favoriteBtn.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.context, R.color.text_secondary)
                )
            }

            // Click Actions
            rootCard.setOnClickListener { onSelect(item) }
            favoriteBtn.setOnClickListener { onToggleFavorite(item) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<UIStationItem>() {
            override fun areItemsTheSame(oldItem: UIStationItem, newItem: UIStationItem): Boolean {
                return oldItem.urlResolved == newItem.urlResolved
            }

            override fun areContentsTheSame(oldItem: UIStationItem, newItem: UIStationItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
