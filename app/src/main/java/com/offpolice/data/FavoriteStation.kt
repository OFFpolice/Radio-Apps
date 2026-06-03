package com.offpolice.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_stations")
data class FavoriteStation(
    @PrimaryKey
    val urlResolved: String,
    val name: String,
    val tags: String? = null,
    val favicon: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
