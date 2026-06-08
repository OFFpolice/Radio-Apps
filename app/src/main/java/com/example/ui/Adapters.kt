package com.example.ui

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.R
import com.example.data.ApiStation
import com.example.data.FavoriteStation
import com.example.databinding.ItemStationCardBinding

class StationsAdapter(
    private var dataset: List<ApiStation> = emptyList(),
    private var favoriteUrls: Set<String> = emptySet(),
    private var activeUrl: String? = null,
    private val onStationSelect: (ApiStation) -> Unit,
    private val onToggleFavorite: (ApiStation) -> Unit
) : RecyclerView.Adapter<StationsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemStationCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStationCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val station = dataset[position]
        val context = holder.itemView.context
        val isFav = favoriteUrls.contains(station.url_resolved)
        val isActive = station.url_resolved == activeUrl

        // Update card themes
        if (isActive) {
            holder.binding.parentContainer.setBackgroundResource(R.drawable.bg_active_card)
        } else {
            holder.binding.parentContainer.setBackgroundResource(R.drawable.bg_rounded_card)
        }

        // Title and heart actions
        holder.binding.stationName.text = station.name
        holder.binding.btnFavorite.setImageResource(
            if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        )

        // Load Favicon using Coil
        val fallbackTint = if (isActive) context.getColor(R.color.primary_pink) else context.getColor(R.color.app_text_secondary)
        holder.binding.stationFavicon.imageTintList = android.content.res.ColorStateList.valueOf(fallbackTint)
        
        holder.binding.stationFavicon.load(station.favicon) {
            placeholder(R.drawable.ic_radio)
            error(R.drawable.ic_radio)
            listener(
                onSuccess = { _, _ ->
                    holder.binding.stationFavicon.imageTintList = null
                },
                onError = { _, _ ->
                    holder.binding.stationFavicon.imageTintList = android.content.res.ColorStateList.valueOf(fallbackTint)
                }
            )
        }

        // Render tags efficiently without re-inflation bottleneck
        bindTags(holder.binding.tagsContainer, station.tags, isActive, context)

        // Bind clicks
        holder.binding.root.setOnClickListener { onStationSelect(station) }
        holder.binding.btnFavorite.setOnClickListener { onToggleFavorite(station) }
    }

    override fun getItemCount(): Int = dataset.size

    fun submitData(newDataset: List<ApiStation>, newFavoriteUrls: Set<String>, newActiveUrl: String?) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = dataset.size
            override fun getNewListSize(): Int = newDataset.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return dataset[oldItemPosition].url_resolved == newDataset[newItemPosition].url_resolved
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = dataset[oldItemPosition]
                val new = newDataset[newItemPosition]
                val oldIsFav = favoriteUrls.contains(old.url_resolved)
                val newIsFav = newFavoriteUrls.contains(new.url_resolved)
                val oldIsActive = old.url_resolved == activeUrl
                val newIsActive = new.url_resolved == newActiveUrl
                
                return old.name == new.name &&
                        old.favicon == new.favicon &&
                        old.tags == new.tags &&
                        oldIsFav == newIsFav &&
                        oldIsActive == newIsActive
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.dataset = newDataset
        this.favoriteUrls = newFavoriteUrls
        this.activeUrl = newActiveUrl
        diffResult.dispatchUpdatesTo(this)
    }
}

class FavoritesAdapter(
    private var dataset: List<FavoriteStation> = emptyList(),
    private var activeUrl: String? = null,
    private val onStationSelect: (FavoriteStation) -> Unit,
    private val onToggleFavorite: (FavoriteStation) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemStationCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStationCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val station = dataset[position]
        val context = holder.itemView.context
        val isActive = station.urlResolved == activeUrl

        // Update card themes
        if (isActive) {
            holder.binding.parentContainer.setBackgroundResource(R.drawable.bg_active_card)
        } else {
            holder.binding.parentContainer.setBackgroundResource(R.drawable.bg_rounded_card)
        }

        // Settings
        holder.binding.stationName.text = station.name
        holder.binding.btnFavorite.setImageResource(R.drawable.ic_favorite)

        // Load Favicon using Coil
        val fallbackTint = if (isActive) context.getColor(R.color.primary_pink) else context.getColor(R.color.app_text_secondary)
        holder.binding.stationFavicon.imageTintList = android.content.res.ColorStateList.valueOf(fallbackTint)
        
        holder.binding.stationFavicon.load(station.favicon) {
            placeholder(R.drawable.ic_radio)
            error(R.drawable.ic_radio)
            listener(
                onSuccess = { _, _ ->
                    holder.binding.stationFavicon.imageTintList = null
                },
                onError = { _, _ ->
                    holder.binding.stationFavicon.imageTintList = android.content.res.ColorStateList.valueOf(fallbackTint)
                }
            )
        }

        // Render tags efficiently
        bindTags(holder.binding.tagsContainer, station.tags, isActive, context)

        // Click actions
        holder.binding.root.setOnClickListener { onStationSelect(station) }
        holder.binding.btnFavorite.setOnClickListener { onToggleFavorite(station) }
    }

    override fun getItemCount(): Int = dataset.size

    fun submitData(newDataset: List<FavoriteStation>, newActiveUrl: String?) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = dataset.size
            override fun getNewListSize(): Int = newDataset.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return dataset[oldItemPosition].urlResolved == newDataset[newItemPosition].urlResolved
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = dataset[oldItemPosition]
                val new = newDataset[newItemPosition]
                val oldIsActive = old.urlResolved == activeUrl
                val newIsActive = new.urlResolved == newActiveUrl
                
                return old.name == new.name &&
                        old.favicon == new.favicon &&
                        old.tags == new.tags &&
                        oldIsActive == newIsActive
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.dataset = newDataset
        this.activeUrl = newActiveUrl
        diffResult.dispatchUpdatesTo(this)
    }
}

// Global visual tag pool builder
private fun bindTags(container: LinearLayout, tagsString: String?, isActive: Boolean, context: Context) {
    container.removeAllViews()
    if (tagsString.isNullOrBlank()) {
        container.visibility = View.GONE
        return
    }
    val list = tagsString.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .take(3)

    if (list.isEmpty()) {
        container.visibility = View.GONE
        return
    }
    container.visibility = View.VISIBLE

    for (tag in list) {
        val tv = TextView(context).apply {
            text = tag
            textSize = 10f
            setTextColor(
                if (isActive) context.getColor(R.color.primary_pink)
                else context.getColor(R.color.app_text_secondary)
            )
            setPadding(dpToPx(6, context), dpToPx(2, context), dpToPx(6, context), dpToPx(2, context))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(6, context)
            }
            layoutParams = lp
            background = context.getDrawable(
                if (isActive) R.drawable.bg_tag_active
                else R.drawable.bg_tag_inactive
            )
        }
        container.addView(tv)
    }
}

private fun dpToPx(dp: Int, context: Context): Int {
    return (dp * context.resources.displayMetrics.density).toInt()
}
