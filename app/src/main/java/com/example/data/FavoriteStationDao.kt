package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteStationDao {
    @Query("SELECT * FROM favorite_stations ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteStation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(station: FavoriteStation)

    @Query("DELETE FROM favorite_stations WHERE urlResolved = :urlResolved")
    suspend fun deleteFavoriteByUrl(urlResolved: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_stations WHERE urlResolved = :urlResolved LIMIT 1)")
    fun isFavorite(urlResolved: String): Flow<Boolean>
}
