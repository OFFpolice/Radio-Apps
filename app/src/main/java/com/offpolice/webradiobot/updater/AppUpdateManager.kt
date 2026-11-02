package com.offpolice.webradiobot.updater

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.offpolice.webradiobot.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class AppUpdateManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val prefs = context.getSharedPreferences("app_update_prefs", Context.MODE_PRIVATE)

    // Repository Target
    private val _repositoryInfo = run {
        val savedRepo = prefs.getString("github_repo", null)
        val actualRepo = if (savedRepo == null || savedRepo == "WebRadioBot") {
            prefs.edit().putString("github_repo", "Radio-Apps").apply()
            "Radio-Apps"
        } else {
            savedRepo
        }
        val savedOwner = prefs.getString("github_owner", "OFFpolice") ?: "OFFpolice"
        MutableStateFlow(savedOwner to actualRepo)
    }
    val repositoryInfo: StateFlow<Pair<String, String>> = _repositoryInfo.asStateFlow()

    var repoOwner: String
        get() = _repositoryInfo.value.first
        set(value) {
            val v = value.trim()
            prefs.edit().putString("github_owner", v).apply()
            _repositoryInfo.value = v to _repositoryInfo.value.second
        }

    var repoName: String
        get() = _repositoryInfo.value.second
        set(value) {
            val v = value.trim()
            prefs.edit().putString("github_repo", v).apply()
            _repositoryInfo.value = _repositoryInfo.value.first to v
        }

    fun setRepository(owner: String, repo: String) {
        val cleanOwner = owner.trim().removePrefix("https://github.com/").substringBefore("/").ifBlank { "OFFpolice" }
        val cleanRepo = repo.trim().removeSuffix(".git").substringAfterLast("/").ifBlank { "Radio-Apps" }
        prefs.edit()
            .putString("github_owner", cleanOwner)
            .putString("github_repo", cleanRepo)
            .apply()
        _repositoryInfo.value = cleanOwner to cleanRepo
    }

    // Auto-check on startup preference
    private val _autoCheckOnStartup = MutableStateFlow(prefs.getBoolean("auto_check_on_startup", true))
    val autoCheckOnStartup: StateFlow<Boolean> = _autoCheckOnStartup.asStateFlow()

    fun setAutoCheckOnStartup(enabled: Boolean) {
        _autoCheckOnStartup.value = enabled
        prefs.edit().putBoolean("auto_check_on_startup", enabled).apply()
    }

    // Update Channel (Stable vs Pre-release)
    private val _updateChannel = MutableStateFlow(
        try {
            UpdateChannel.valueOf(prefs.getString("update_channel", UpdateChannel.STABLE.name) ?: UpdateChannel.STABLE.name)
        } catch (e: Exception) {
            UpdateChannel.STABLE
        }
    )
    val updateChannel: StateFlow<UpdateChannel> = _updateChannel.asStateFlow()

    fun setUpdateChannel(channel: UpdateChannel) {
        _updateChannel.value = channel
        prefs.edit().putString("update_channel", channel.name).apply()
    }

    // Last checked timestamp
    private val _lastCheckedTimestamp = MutableStateFlow(prefs.getLong("last_checked_timestamp", 0L))
    val lastCheckedTimestamp: StateFlow<Long> = _lastCheckedTimestamp.asStateFlow()

    private fun markCheckedNow() {
        val now = System.currentTimeMillis()
        _lastCheckedTimestamp.value = now
        prefs.edit().putLong("last_checked_timestamp", now).apply()
    }

    private val _updateState = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val updateState: StateFlow<AppUpdateState> = _updateState.asStateFlow()

    private val _isUpdateDialogVisible = MutableStateFlow(false)
    val isUpdateDialogVisible: StateFlow<Boolean> = _isUpdateDialogVisible.asStateFlow()

    private var downloadJob: Job? = null

    private val moshi = Moshi.Builder().build()
    private val releaseAdapter = moshi.adapter(GithubRelease::class.java)
    private val releaseListAdapter = moshi.adapter<List<GithubRelease>>(
        Types.newParameterizedType(List::class.java, GithubRelease::class.java)
    )

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(180, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun checkForUpdates(isManual: Boolean = false) {
        if (_updateState.value is AppUpdateState.Checking || _updateState.value is AppUpdateState.Downloading) {
            return
        }

        scope.launch {
            _updateState.value = AppUpdateState.Checking

            try {
                val channel = _updateChannel.value
                val result = withContext(Dispatchers.IO) {
                    fetchLatestRelease(repoOwner, repoName, channel)
                }

                markCheckedNow()

                when (result) {
                    is FetchReleaseResult.Success -> {
                        val release = result.release
                        val remoteVersion = release.tagName.trim()
                        val currentVersion = BuildConfig.VERSION_NAME

                        if (isNewerVersion(remoteVersion, currentVersion)) {
                            val apkAsset = selectBestApkAsset(release.assets)
                            if (apkAsset != null) {
                                val updateInfo = AppUpdateInfo(
                                    versionName = cleanVersion(remoteVersion),
                                    releaseTitle = release.name?.ifBlank { null } ?: remoteVersion,
                                    changelog = AppUpdateInfo.sanitizeChangelog(release.body),
                                    apkUrl = apkAsset.browserDownloadUrl,
                                    apkSize = apkAsset.size,
                                    releaseUrl = release.htmlUrl ?: "https://github.com/$repoOwner/$repoName/releases",
                                    publishedAt = release.publishedAt,
                                    isPrerelease = release.prerelease == true
                                )

                                _updateState.value = AppUpdateState.UpdateAvailable(
                                    info = updateInfo,
                                    isManualCheck = isManual
                                )

                                val dismissedVersion = prefs.getString("dismissed_version", null)
                                if (isManual || dismissedVersion != updateInfo.versionName) {
                                    _isUpdateDialogVisible.value = true
                                }
                            } else {
                                // Release exists on GitHub but without an .apk asset
                                _updateState.value = AppUpdateState.NoApkInRelease(
                                    tagName = remoteVersion,
                                    repoOwner = repoOwner,
                                    repoName = repoName
                                )
                                if (isManual) {
                                    _isUpdateDialogVisible.value = true
                                }
                            }
                        } else {
                            _updateState.value = AppUpdateState.NoUpdateAvailable(
                                currentVersion = currentVersion,
                                latestRemoteTag = remoteVersion
                            )
                            if (isManual) {
                                _isUpdateDialogVisible.value = true
                            }
                        }
                    }

                    is FetchReleaseResult.NoReleases -> {
                        _updateState.value = AppUpdateState.NoReleasesOnGitHub(
                            repoOwner = result.repoOwner,
                            repoName = result.repoName,
                            currentVersion = BuildConfig.VERSION_NAME
                        )
                        if (isManual) {
                            _isUpdateDialogVisible.value = true
                        }
                    }

                    is FetchReleaseResult.RepoNotFound -> {
                        _updateState.value = AppUpdateState.Error(
                            message = "Репозиторий ${result.repoOwner}/${result.repoName} не найден на GitHub (404). Проверьте имя репозитория и статус Public.",
                            isManualCheck = isManual
                        )
                        if (isManual) {
                            _isUpdateDialogVisible.value = true
                        }
                    }

                    is FetchReleaseResult.RateLimited -> {
                        _updateState.value = AppUpdateState.Error(
                            message = "Превышен лимит запросов к GitHub API (60 в час). Попробуйте позже.",
                            isManualCheck = isManual
                        )
                        if (isManual) {
                            _isUpdateDialogVisible.value = true
                        }
                    }

                    is FetchReleaseResult.Error -> {
                        _updateState.value = AppUpdateState.Error(
                            message = result.message,
                            isManualCheck = isManual
                        )
                        if (isManual) {
                            _isUpdateDialogVisible.value = true
                        }
                    }
                }
            } catch (e: CancellationException) {
                // Cancelled
            } catch (e: Exception) {
                e.printStackTrace()
                _updateState.value = AppUpdateState.Error(
                    message = e.localizedMessage ?: "Network error",
                    isManualCheck = isManual
                )
                if (isManual) {
                    _isUpdateDialogVisible.value = true
                }
            }
        }
    }

    private sealed class FetchReleaseResult {
        data class Success(val release: GithubRelease) : FetchReleaseResult()
        data class NoReleases(val repoOwner: String, val repoName: String) : FetchReleaseResult()
        data class RepoNotFound(val repoOwner: String, val repoName: String) : FetchReleaseResult()
        data class RateLimited(val message: String) : FetchReleaseResult()
        data class Error(val message: String) : FetchReleaseResult()
    }

    private fun fetchLatestRelease(owner: String, repo: String, channel: UpdateChannel): FetchReleaseResult {
        val cachedEtagKey = "etag_${owner}_${repo}"
        val cachedJsonKey = "cached_release_${owner}_${repo}"

        // If user wants Pre-releases or if latest fails, query /releases endpoint
        if (channel == UpdateChannel.PRERELEASE) {
            val listUrl = "https://api.github.com/repos/$owner/$repo/releases?per_page=10"
            val req = Request.Builder()
                .url(listUrl)
                .header("User-Agent", "WebRadioBot-Android-App")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            try {
                val resp = okHttpClient.newCall(req).execute()
                resp.use { r ->
                    if (r.isSuccessful) {
                        val json = r.body?.string() ?: return FetchReleaseResult.NoReleases(owner, repo)
                        val list = releaseListAdapter.fromJson(json)
                        val best = list?.firstOrNull { it.draft != true }
                        return if (best != null) {
                            FetchReleaseResult.Success(best)
                        } else {
                            FetchReleaseResult.NoReleases(owner, repo)
                        }
                    }
                    if (r.code == 404) return FetchReleaseResult.RepoNotFound(owner, repo)
                    if (r.code == 403) return FetchReleaseResult.RateLimited("Rate limited")
                    return FetchReleaseResult.Error("HTTP ${r.code}: ${r.message}")
                }
            } catch (e: Exception) {
                return FetchReleaseResult.Error(e.localizedMessage ?: "Connection error")
            }
        }

        // Standard STABLE channel query with ETag conditional caching
        val latestUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
        val cachedEtag = prefs.getString(cachedEtagKey, null)

        val reqBuilder = Request.Builder()
            .url(latestUrl)
            .header("User-Agent", "WebRadioBot-Android-App")
            .header("Accept", "application/vnd.github.v3+json")

        if (cachedEtag != null) {
            reqBuilder.header("If-None-Match", cachedEtag)
        }

        try {
            val response = okHttpClient.newCall(reqBuilder.build()).execute()
            response.use { resp ->
                // 304 Not Modified: GitHub rate limit is saved (0 points consumed)
                if (resp.code == 304) {
                    val cachedJson = prefs.getString(cachedJsonKey, null)
                    if (cachedJson != null) {
                        val release = releaseAdapter.fromJson(cachedJson)
                        if (release != null) {
                            return FetchReleaseResult.Success(release)
                        }
                    }
                }

                if (resp.isSuccessful) {
                    val json = resp.body?.string() ?: return FetchReleaseResult.Error("Empty response body")
                    val release = releaseAdapter.fromJson(json)
                    return if (release != null) {
                        val newEtag = resp.header("ETag")
                        prefs.edit()
                            .putString(cachedEtagKey, newEtag)
                            .putString(cachedJsonKey, json)
                            .apply()
                        FetchReleaseResult.Success(release)
                    } else {
                        FetchReleaseResult.Error("Failed to parse release info")
                    }
                }

                if (resp.code == 403) {
                    return FetchReleaseResult.RateLimited("GitHub rate limit exceeded")
                }

                if (resp.code == 404) {
                    // Check list of releases (handles repos where latest isn't marked or only pre-releases exist)
                    val listUrl = "https://api.github.com/repos/$owner/$repo/releases?per_page=10"
                    val listRequest = Request.Builder()
                        .url(listUrl)
                        .header("User-Agent", "WebRadioBot-Android-App")
                        .header("Accept", "application/vnd.github.v3+json")
                        .build()

                    val listResp = okHttpClient.newCall(listRequest).execute()
                    listResp.use { lResp ->
                        if (lResp.isSuccessful) {
                            val listJson = lResp.body?.string() ?: return FetchReleaseResult.NoReleases(owner, repo)
                            val list = releaseListAdapter.fromJson(listJson)
                            val bestRelease = list?.firstOrNull { it.draft != true }
                            return if (bestRelease != null) {
                                FetchReleaseResult.Success(bestRelease)
                            } else {
                                FetchReleaseResult.NoReleases(owner, repo)
                            }
                        }

                        if (lResp.code == 404) {
                            return FetchReleaseResult.RepoNotFound(owner, repo)
                        }

                        if (lResp.code == 403) {
                            return FetchReleaseResult.RateLimited("GitHub rate limit exceeded")
                        }

                        return FetchReleaseResult.Error("HTTP ${lResp.code}: ${lResp.message}")
                    }
                }

                return FetchReleaseResult.Error("HTTP ${resp.code}: ${resp.message}")
            }
        } catch (e: Exception) {
            return FetchReleaseResult.Error(e.localizedMessage ?: "Connection error")
        }
    }

    /**
     * Professional ABI-aware APK asset matching.
     * Evaluates device CPU architecture (Build.SUPPORTED_ABIS), universal APKs, and release priority.
     */
    private fun selectBestApkAsset(assets: List<GithubAsset>): GithubAsset? {
        val apkAssets = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        if (apkAssets.isEmpty()) return null

        val deviceAbis = Build.SUPPORTED_ABIS.map { it.lowercase() }

        val scored = apkAssets.map { asset ->
            val name = asset.name.lowercase()
            var score = 0

            val matchesAbiIndex = deviceAbis.indexOfFirst { abi -> name.contains(abi) }
            if (matchesAbiIndex >= 0) {
                // Highest priority for primary device ABI
                score += 100 - (matchesAbiIndex * 10)
            } else if (name.contains("universal") || name.contains("all")) {
                score += 50
            } else {
                val knownAbis = listOf("arm64-v8a", "arm64", "armeabi-v7a", "armeabi", "x86_64", "x86")
                val isOtherAbi = knownAbis.any { abi -> name.contains(abi) }
                if (isOtherAbi) {
                    score -= 50
                } else {
                    score += 20 // neutral name like RadioApps-v1.0.1.apk
                }
            }

            if (name.contains("release")) score += 15
            if (name.contains("debug")) score -= 25

            asset to score
        }

        return scored.maxByOrNull { it.second }?.first ?: apkAssets.first()
    }

    fun startDownload(info: AppUpdateInfo) {
        downloadJob?.cancel()

        downloadJob = scope.launch {
            _updateState.value = AppUpdateState.Downloading(
                info = info,
                progress = 0f,
                downloadedBytes = 0L,
                totalBytes = info.apkSize
            )
            _isUpdateDialogVisible.value = true

            try {
                val updatesDir = File(context.cacheDir, "updates")
                if (!updatesDir.exists()) updatesDir.mkdirs()

                val apkFile = File(updatesDir, "WebRadioBot_${info.versionName}.apk")
                if (apkFile.exists()) apkFile.delete()

                val request = Request.Builder()
                    .url(info.apkUrl)
                    .header("User-Agent", "WebRadioBot-Android-App")
                    .header("Accept", "application/octet-stream")
                    .build()

                withContext(Dispatchers.IO) {
                    val response = okHttpClient.newCall(request).execute()
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code}: ${response.message}")
                    }

                    val body = response.body ?: throw IOException("Empty response body")
                    val totalLength = if (info.apkSize > 0) info.apkSize else body.contentLength()

                    body.byteStream().use { input ->
                        apkFile.outputStream().use { output ->
                            val buffer = ByteArray(16 * 1024)
                            var bytesRead: Int
                            var downloaded = 0L
                            var lastEmitTime = 0L

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloaded += bytesRead

                                val now = System.currentTimeMillis()
                                if (now - lastEmitTime >= 100L || downloaded == totalLength) {
                                    lastEmitTime = now
                                    val prog = if (totalLength > 0) {
                                        downloaded.toFloat() / totalLength.toFloat()
                                    } else {
                                        -1f
                                    }
                                    _updateState.value = AppUpdateState.Downloading(
                                        info = info,
                                        progress = prog.coerceIn(0f, 1f),
                                        downloadedBytes = downloaded,
                                        totalBytes = totalLength
                                    )
                                }
                            }
                        }
                    }
                }

                // Verify package archive integrity before signaling ready
                val isArchiveValid = validateApk(apkFile.absolutePath)
                if (!isArchiveValid) {
                    apkFile.delete()
                    _updateState.value = AppUpdateState.Error(
                        message = "Скачанный файл повреждён или не является верным APK. Попробуйте скачать снова.",
                        isManualCheck = true
                    )
                    return@launch
                }

                _updateState.value = AppUpdateState.ReadyToInstall(
                    info = info,
                    apkPath = apkFile.absolutePath
                )
            } catch (e: CancellationException) {
                // Cancelled
            } catch (e: Exception) {
                e.printStackTrace()
                _updateState.value = AppUpdateState.Error(
                    message = e.localizedMessage ?: "Download failed",
                    isManualCheck = true
                )
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        val current = _updateState.value
        if (current is AppUpdateState.Downloading) {
            _updateState.value = AppUpdateState.UpdateAvailable(current.info, isManualCheck = true)
        }
    }

    fun dismissDialog(rememberDismissed: Boolean = false) {
        _isUpdateDialogVisible.value = false
        val state = _updateState.value
        if (rememberDismissed && state is AppUpdateState.UpdateAvailable) {
            prefs.edit().putString("dismissed_version", state.info.versionName).apply()
        }
    }

    fun showDialog() {
        _isUpdateDialogVisible.value = true
    }

    fun canInstallApks(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun requestInstallPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                activity.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                activity.startActivity(intent)
            }
        }
    }

    fun validateApk(apkPath: String): Boolean {
        return try {
            val archiveInfo = context.packageManager.getPackageArchiveInfo(
                apkPath,
                PackageManager.GET_ACTIVITIES
            )
            archiveInfo != null && archiveInfo.packageName == context.packageName
        } catch (e: Exception) {
            false
        }
    }

    fun installApk(activity: Activity, apkPath: String) {
        val apkFile = File(apkPath)
        if (!apkFile.exists()) {
            _updateState.value = AppUpdateState.Error("Файл APK не найден")
            return
        }

        if (!validateApk(apkPath)) {
            apkFile.delete()
            _updateState.value = AppUpdateState.Error("Файл обновления повреждён. Попробуйте скачать снова.")
            return
        }

        if (!canInstallApks()) {
            requestInstallPermission(activity)
            return
        }

        try {
            val contentUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            // Grant read permission to package installers on device
            val resInfoList = activity.packageManager.queryIntentActivities(installIntent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                activity.grantUriPermission(resolveInfo.activityInfo.packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            activity.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            _updateState.value = AppUpdateState.Error("Ошибка запуска установщика: ${e.message}")
        }
    }

    companion object {
        fun cleanVersion(tag: String): String {
            return tag.trim().removePrefix("v").removePrefix("V").removePrefix("ver-").removePrefix("release-")
        }

        fun isNewerVersion(remoteTag: String, currentTag: String): Boolean {
            val remote = cleanVersion(remoteTag)
            val current = cleanVersion(currentTag)

            val remoteParts = remote.split(".", "-").mapNotNull { part ->
                part.takeWhile { it.isDigit() }.toIntOrNull()
            }
            val currentParts = current.split(".", "-").mapNotNull { part ->
                part.takeWhile { it.isDigit() }.toIntOrNull()
            }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }
    }
}
