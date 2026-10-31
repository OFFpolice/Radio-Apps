package com.offpolice.webradiobot.data

import androidx.compose.runtime.Immutable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

fun extractTopThreeTags(tags: String?): Triple<String?, String?, String?> {
    if (tags.isNullOrBlank()) return Triple(null, null, null)
    var t1: String? = null
    var t2: String? = null
    var t3: String? = null
    var start = 0
    var count = 0
    val len = tags.length
    for (i in 0..len) {
        if (i == len || tags[i] == ',') {
            if (i > start) {
                val part = tags.substring(start, i).trim()
                if (part.isNotEmpty()) {
                    if (count == 0) {
                        t1 = part
                        count = 1
                    } else if (count == 1 && part != t1) {
                        t2 = part
                        count = 2
                    } else if (count == 2 && part != t1 && part != t2) {
                        t3 = part
                        count = 3
                        break
                    }
                }
            }
            start = i + 1
        }
    }
    return Triple(t1, t2, t3)
}

fun isValidFaviconUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val trimmed = url.trim()
    if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
        return false
    }
    val host = trimmed.substringAfter("://")
    return host.length >= 4 && host.contains('.')
}

@Immutable
@JsonClass(generateAdapter = true)
data class RadioServer(
    val name: String,
    val ip: String? = null
)

@Immutable
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
) {
    val topTags: Triple<String?, String?, String?> by lazy(LazyThreadSafetyMode.NONE) {
        extractTopThreeTags(tags)
    }
}
