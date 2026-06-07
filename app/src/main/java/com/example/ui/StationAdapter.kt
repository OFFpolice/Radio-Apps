package com.example.ui

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.R
import com.example.databinding.ItemStationBinding
import com.example.databinding.ItemTagChipBinding

data class DisplayStation(
    val name: String,
    val favicon: String?,
    val tags: String?,
    val urlResolved: String,
    val isFavorite: Boolean,
    val isActive: Boolean,
    val origin: Any
)

class StationAdapter(
    private val onSelect: (DisplayStation) -> Unit,
    private val onToggleFav: (DisplayStation) -> Unit
) : ListAdapter<DisplayStation, StationAdapter.StationViewHolder>(StationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val binding = ItemStationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StationViewHolder(
        private val binding: ItemStationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(station: DisplayStation) {
            val ctx = binding.root.context

            // Title
            binding.stationName.text = station.name.trim().ifEmpty { Loc.get("no_title", AppLanguageSetting.AUTO) }

            // Active/inactive state selection
            val cardBackgroundRes = if (station.isActive) {
                R.drawable.bg_station_card_active
            } else {
                R.drawable.bg_station_card
            }
            binding.stationCardContainer.setBackgroundResource(cardBackgroundRes)

            // Dynamic tags chips row: inflate at most 3 chips
            binding.tagsContainer.removeAllViews()
            val parsedTags = station.tags?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                ?.take(3) ?: emptyList()

            val tagsToDisplay = parsedTags.ifEmpty { listOf("radio") }
            tagsToDisplay.forEach { tagText ->
                val chipBinding = ItemTagChipBinding.inflate(
                    LayoutInflater.from(ctx),
                    binding.tagsContainer,
                    false
                )
                chipBinding.tagChipText.text = tagText

                // Set active/inactive background and text colors
                if (station.isActive) {
                    chipBinding.tagChipText.setBackgroundResource(R.drawable.bg_tag_chip_active)
                    chipBinding.tagChipText.setTextColor(ContextCompat.getColor(ctx, R.color.primary_pink))
                } else {
                    chipBinding.tagChipText.setBackgroundResource(R.drawable.bg_tag_chip)
                    chipBinding.tagChipText.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                }
                binding.tagsContainer.addView(chipBinding.root)
            }

            // Favorite Icon Toggle
            val favIcon = if (station.isFavorite) {
                R.drawable.ic_favorite
            } else {
                R.drawable.ic_favorite_border
            }
            binding.favToggleIcon.setImageResource(favIcon)

            val favTint = if (station.isFavorite) {
                R.color.primary_pink
            } else {
                R.color.text_secondary
            }
            binding.favToggleIcon.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(ctx, favTint)
            )

            // Favicon Loading with coil
            val fallbackTint = if (isSystemDark(ctx)) {
                R.color.secondary_pink
            } else {
                R.color.white
            }

            if (!station.favicon.isNullOrBlank()) {
                binding.faviconImg.imageTintList = null // Clear coloring tint
                binding.faviconImg.load(station.favicon) {
                    crossfade(true)
                    error(R.drawable.ic_radio)
                    listener(
                        onError = { _, _ ->
                            binding.faviconImg.setImageResource(R.drawable.ic_radio)
                            binding.faviconImg.imageTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(ctx, fallbackTint)
                            )
                        }
                    )
                }
            } else {
                binding.faviconImg.setImageResource(R.drawable.ic_radio)
                binding.faviconImg.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, fallbackTint)
                )
            }

            // Accent background for logo cover in light theme
            if (!isSystemDark(ctx)) {
                binding.faviconCard.setCardBackgroundColor(
                    ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.secondary_pink))
                )
                if (station.favicon.isNullOrBlank()) {
                    binding.faviconImg.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.white)
                    )
                }
            } else {
                binding.faviconCard.setCardBackgroundColor(
                    ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.search_bg))
                )
            }

            // Click listener
            binding.stationCardContainer.setOnClickListener {
                onSelect(station)
            }

            binding.favBtnContainer.setOnClickListener {
                onToggleFav(station)
            }
        }
    }

    private fun isSystemDark(context: Context): Boolean {
        val config = context.resources.configuration
        return (config.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}

class StationDiffCallback : DiffUtil.ItemCallback<DisplayStation>() {

    override fun areItemsTheSame(oldItem: DisplayStation, newItem: DisplayStation): Boolean {
        return oldItem.urlResolved == newItem.urlResolved
    }

    override fun areContentsTheSame(oldItem: DisplayStation, newItem: DisplayStation): Boolean {
        return oldItem.name == newItem.name &&
                oldItem.favicon == newItem.favicon &&
                oldItem.tags == newItem.tags &&
                oldItem.isActive == newItem.isActive &&
                oldItem.isFavorite == newItem.isFavorite
    }
}
