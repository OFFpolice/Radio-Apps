package com.offpolice.webradiobot.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "cached_stations")
data class CachedStation(
    @PrimaryKey
    val urlResolved: String,
    val stationuuid: String? = null,
    val name: String,
    val tags: String? = null,
    val favicon: String? = null,
    val position: Int = 0
)
