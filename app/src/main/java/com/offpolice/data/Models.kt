package com.offpolice.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RadioServer(
    val name: String,
    val ip: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiStation(
    val changeid: String? = null,
    val stationuuid: String? = null,
    val name: String,
    val url: String? = null,
    val url_resolved: String, // Clean streamed URL
    val homepage: String? = null,
    val favicon: String? = null,
    val tags: String? = null,
    val country: String? = null,
    val language: String? = null,
    val votes: Int? = null,
    val codec: String? = null,
    val bitrate: Int? = null
)
