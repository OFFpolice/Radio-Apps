package com.offpolice.webradiobot.updater

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GithubRelease(
    @Json(name = "tag_name")
    val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @Json(name = "html_url")
    val htmlUrl: String? = null,
    @Json(name = "published_at")
    val publishedAt: String? = null,
    val prerelease: Boolean? = false,
    val draft: Boolean? = false,
    val assets: List<GithubAsset> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GithubAsset(
    val name: String,
    val size: Long = 0L,
    @Json(name = "browser_download_url")
    val browserDownloadUrl: String,
    @Json(name = "content_type")
    val contentType: String? = null
)

data class AppUpdateInfo(
    val versionName: String,
    val releaseTitle: String,
    val changelog: String,
    val apkUrl: String,
    val apkSize: Long,
    val releaseUrl: String,
    val publishedAt: String?
)

sealed class AppUpdateState {
    object Idle : AppUpdateState()
    object Checking : AppUpdateState()
    data class NoUpdateAvailable(
        val currentVersion: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : AppUpdateState()
    data class UpdateAvailable(
        val info: AppUpdateInfo,
        val isManualCheck: Boolean = false
    ) : AppUpdateState()
    data class Downloading(
        val info: AppUpdateInfo,
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : AppUpdateState()
    data class ReadyToInstall(
        val info: AppUpdateInfo,
        val apkPath: String
    ) : AppUpdateState()
    data class Error(
        val message: String,
        val isManualCheck: Boolean = false
    ) : AppUpdateState()
}
