package com.offpolice.webradiobot.updater

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Immutable
@JsonClass(generateAdapter = true)
data class GithubRelease(
    @param:Json(name = "tag_name")
    val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @param:Json(name = "html_url")
    val htmlUrl: String? = null,
    @param:Json(name = "published_at")
    val publishedAt: String? = null,
    val prerelease: Boolean? = false,
    val draft: Boolean? = false,
    val assets: List<GithubAsset> = emptyList()
)

@Immutable
@JsonClass(generateAdapter = true)
data class GithubAsset(
    val name: String,
    val size: Long = 0L,
    @param:Json(name = "browser_download_url")
    val browserDownloadUrl: String,
    @param:Json(name = "content_type")
    val contentType: String? = null
)

enum class UpdateChannel {
    STABLE,
    PRERELEASE
}

@Immutable
data class AppUpdateInfo(
    val versionName: String,
    val releaseTitle: String,
    val changelog: String,
    val apkUrl: String,
    val apkSize: Long,
    val releaseUrl: String,
    val publishedAt: String?,
    val isPrerelease: Boolean = false
) {
    companion object {
        fun sanitizeChangelog(raw: String?): String {
            if (raw.isNullOrBlank()) return ""
            return raw
                .lines()
                .filterNot { line ->
                    val trimmed = line.trim()
                    trimmed.startsWith("**Full Changelog**", ignoreCase = true) ||
                    trimmed.startsWith("Full Changelog:", ignoreCase = true) ||
                    trimmed.startsWith("<!--") ||
                    trimmed.endsWith("-->")
                }
                .joinToString("\n") { line ->
                    // Replace markdown PR links like "in https://github.com/.../pull/123" with clean text
                    line.replace(Regex("in https://github.com/[^/]+/[^/]+/pull/(\\d+)"), "(#$1)")
                }
                .trim()
        }
    }
}

@Stable
sealed class AppUpdateState {
    @Immutable
    object Idle : AppUpdateState()

    @Immutable
    object Checking : AppUpdateState()

    @Immutable
    data class NoUpdateAvailable(
        val currentVersion: String,
        val latestRemoteTag: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : AppUpdateState()

    @Immutable
    data class NoReleasesOnGitHub(
        val repoOwner: String,
        val repoName: String,
        val currentVersion: String
    ) : AppUpdateState()

    @Immutable
    data class NoApkInRelease(
        val tagName: String,
        val repoOwner: String,
        val repoName: String
    ) : AppUpdateState()

    @Immutable
    data class UpdateAvailable(
        val info: AppUpdateInfo,
        val isManualCheck: Boolean = false
    ) : AppUpdateState()

    @Immutable
    data class Downloading(
        val info: AppUpdateInfo,
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : AppUpdateState()

    @Immutable
    data class ReadyToInstall(
        val info: AppUpdateInfo,
        val apkPath: String
    ) : AppUpdateState()

    @Immutable
    data class Error(
        val message: String,
        val isManualCheck: Boolean = false
    ) : AppUpdateState()
}
