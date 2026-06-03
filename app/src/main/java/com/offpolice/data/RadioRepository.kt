package com.offpolice.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class RadioRepository(context: Context) {
    private val favoriteStationDao = AppDatabase.getDatabase(context).favoriteStationDao()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://all.api.radio-browser.info/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(RadioBrowserApi::class.java)

    // Current resolved server for API operations
    private var resolvedServer: String = "https://de1.api.radio-browser.info"

    suspend fun resolveActiveServer() {
        try {
            val servers = api.getServers()
            if (servers.isNotEmpty()) {
                val randomServer = servers.random()
                resolvedServer = "https://${randomServer.name}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallbacks automatically to "https://de1.api.radio-browser.info"
        }
    }

    suspend fun searchStations(name: String?, limit: Int, offset: Int): List<ApiStation> {
        val pathUrl = "$resolvedServer/json/stations/search"
        return api.searchStations(
            url = pathUrl,
            name = name?.ifBlank { null },
            limit = limit,
            offset = offset
        )
    }

    // Favorites Operations
    fun getFavoritesFlow(): Flow<List<FavoriteStation>> = favoriteStationDao.getAllFavorites()

    suspend fun addFavorite(urlResolved: String, name: String, tags: String?, favicon: String?) {
        val favorite = FavoriteStation(
            urlResolved = urlResolved,
            name = name,
            tags = tags,
            favicon = favicon
        )
        favoriteStationDao.insertFavorite(favorite)
    }

    suspend fun removeFavoriteByUrl(urlResolved: String) {
        favoriteStationDao.deleteFavoriteByUrl(urlResolved)
    }

    fun isFavorite(urlResolved: String): Flow<Boolean> = favoriteStationDao.isFavorite(urlResolved)
}
