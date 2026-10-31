package com.offpolice.webradiobot.data

import android.content.Context
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class RadioRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val favoriteStationDao = database.favoriteStationDao()
    private val cachedStationDao = database.cachedStationDao()

    private val prefs = context.getSharedPreferences("radio_repo_prefs", Context.MODE_PRIVATE)

    // Moshi without reflection - models use codegen with @JsonClass(generateAdapter = true)
    private val moshi = Moshi.Builder().build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://all.api.radio-browser.info/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(RadioBrowserApi::class.java)

    // Current resolved server for API operations, persisted across app runs
    private var resolvedServer: String = prefs.getString("resolved_server", "https://de1.api.radio-browser.info")
        ?: "https://de1.api.radio-browser.info"

    suspend fun resolveActiveServer() {
        try {
            val servers = api.getServers()
            if (servers.isNotEmpty()) {
                val randomServer = servers.random()
                resolvedServer = "https://${randomServer.name}"
                prefs.edit().putString("resolved_server", resolvedServer).apply()
            }
        } catch (e: Exception) {
            // Fallback to currently resolved or default server
        }
    }

    suspend fun searchStations(name: String?, limit: Int, offset: Int): List<ApiStation> {
        val pathUrl = "$resolvedServer/json/stations/search"
        return try {
            api.searchStations(
                url = pathUrl,
                name = name?.ifBlank { null },
                limit = limit,
                offset = offset,
                hidebroken = true
            )
        } catch (e: Exception) {
            // Quick failover if the current server fails
            val fallbackUrl = "https://de1.api.radio-browser.info/json/stations/search"
            if (pathUrl != fallbackUrl) {
                resolvedServer = "https://de1.api.radio-browser.info"
                api.searchStations(
                    url = fallbackUrl,
                    name = name?.ifBlank { null },
                    limit = limit,
                    offset = offset,
                    hidebroken = true
                )
            } else {
                throw e
            }
        }
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

    suspend fun getCachedStations(): List<ApiStation> {
        return try {
            cachedStationDao.getCachedStations().map { cached ->
                ApiStation(
                    stationuuid = cached.stationuuid,
                    name = cached.name,
                    url_resolved = cached.urlResolved,
                    tags = cached.tags,
                    favicon = cached.favicon
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveCachedStations(stations: List<ApiStation>) {
        try {
            val entities = stations.mapIndexed { index, apiStation ->
                CachedStation(
                    urlResolved = apiStation.url_resolved,
                    stationuuid = apiStation.stationuuid,
                    name = apiStation.name,
                    tags = apiStation.tags,
                    favicon = apiStation.favicon,
                    position = index
                )
            }
            cachedStationDao.replaceCachedStations(entities)
        } catch (e: Exception) {
            // Ignore cache write errors
        }
    }
}
