package com.offpolice.webradiobot.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface CachedStationDao {
    @Query("SELECT * FROM cached_stations ORDER BY position ASC")
    suspend fun getCachedStations(): List<CachedStation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStations(stations: List<CachedStation>)

    @Query("DELETE FROM cached_stations")
    suspend fun clearCachedStations()

    @Transaction
    suspend fun replaceCachedStations(stations: List<CachedStation>) {
        clearCachedStations()
        insertStations(stations)
    }
}
