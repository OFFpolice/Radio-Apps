package com.offpolice.webradiobot.updater

import androidx.compose.runtime.Immutable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Immutable
@JsonClass(generateAdapter = true)
data class GithubRelease(
    val id: Long = 0L,
    val name: String? = null,
    @param:Json(name = "tag_name") val tagName: String = "",
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @param:Json(name = "published_at") val publishedAt: String? = null,
    @param:Json(name = "html_url") val htmlUrl: String? = null,
    val assets: List<GithubAsset> = emptyList()
)

@Immutable
@JsonClass(generateAdapter = true)
data class GithubAsset(
    val id: Long = 0L,
    val name: String = "",
    val size: Long = 0L,
    @param:Json(name = "content_type") val contentType: String? = null,
    @param:Json(name = "browser_download_url") val browserDownloadUrl: String = "",
    @param:Json(name = "download_count") val downloadCount: Int = 0
)
