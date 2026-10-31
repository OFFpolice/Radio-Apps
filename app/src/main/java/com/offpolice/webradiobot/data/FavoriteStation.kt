package com.offpolice.webradiobot.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_stations")
data class FavoriteStation(
    @PrimaryKey
    val urlResolved: String,
    val name: String,
    val tags: String? = null,
    val favicon: String? = null,
    val addedAt: Long = System.currentTimeMillis()
) {
    @Ignore
    private var _cachedTags: Triple<String?, String?, String?>? = null

    val topTags: Triple<String?, String?, String?>
        get() {
            var cached = _cachedTags
            if (cached == null) {
                cached = extractTopThreeTags(tags)
                _cachedTags = cached
            }
            return cached
        }
}
