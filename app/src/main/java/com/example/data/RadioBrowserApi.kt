package com.example.data

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface RadioBrowserApi {
    @GET
    suspend fun getServers(
        @Url url: String = "https://all.api.radio-browser.info/json/servers"
    ): List<RadioServer>

    @GET
    suspend fun searchStations(
        @Url url: String,
        @Query("name") name: String?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: Boolean = true
    ): List<ApiStation>
}
