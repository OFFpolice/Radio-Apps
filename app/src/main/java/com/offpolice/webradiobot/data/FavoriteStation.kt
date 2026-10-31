package com.offpolice.webradiobot.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "favorite_stations")
data class FavoriteStation(
    @PrimaryKey
    val urlResolved: String,
    val name: String,
    val tags: String? = null,
    val favicon: String? = null,
    val addedAt: Long = System.currentTimeMillis()
) {
    @delegate:Ignore
    val topTags: Triple<String?, String?, String?> by lazy(LazyThreadSafetyMode.NONE) {
        extractTopThreeTags(tags)
    }
}
