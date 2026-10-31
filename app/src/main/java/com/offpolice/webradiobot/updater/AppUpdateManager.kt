package com.offpolice.webradiobot.updater

import android.app.Activity
import android.content.Context
import android.content.Intent
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

    var repoOwner: String
        get() = prefs.getString("github_owner", "OFFpolice") ?: "OFFpolice"
        set(value) = prefs.edit().putString("github_owner", value.trim()).apply()

    var repoName: String
        get() = prefs.getString("github_repo", "WebRadioBot") ?: "WebRadioBot"
        set(value) = prefs.edit().putString("github_repo", value.trim()).apply()

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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    fun checkForUpdates(isManual: Boolean = false) {
        if (_updateState.value is AppUpdateState.Checking || _updateState.value is AppUpdateState.Downloading) {
            return
        }

        scope.launch {
            _updateState.value = AppUpdateState.Checking

            try {
                val release = withContext(Dispatchers.IO) {
                    fetchLatestRelease(repoOwner, repoName)
                }

                if (release == null) {
                    _updateState.value = AppUpdateState.NoUpdateAvailable(
                        currentVersion = BuildConfig.VERSION_NAME
                    )
                    if (isManual) {
                        _isUpdateDialogVisible.value = true
                    }
                    return@launch
                }

                val remoteVersion = release.tagName.trim()
                val currentVersion = BuildConfig.VERSION_NAME

                if (isNewerVersion(remoteVersion, currentVersion)) {
                    val apkAsset = selectBestApkAsset(release.assets)
                    if (apkAsset != null) {
                        val updateInfo = AppUpdateInfo(
                            versionName = cleanVersion(remoteVersion),
                            releaseTitle = release.name?.ifBlank { null } ?: remoteVersion,
                            changelog = release.body?.trim().orEmpty(),
                            apkUrl = apkAsset.browserDownloadUrl,
                            apkSize = apkAsset.size,
                            releaseUrl = release.htmlUrl ?: "https://github.com/$repoOwner/$repoName/releases",
                            publishedAt = release.publishedAt
                        )

                        _updateState.value = AppUpdateState.UpdateAvailable(
                            info = updateInfo,
                            isManualCheck = isManual
                        )

                        // If auto-check, check if this version was previously dismissed
                        val dismissedVersion = prefs.getString("dismissed_version", null)
                        if (isManual || dismissedVersion != updateInfo.versionName) {
                            _isUpdateDialogVisible.value = true
                        }
                    } else {
                        // Release exists but no APK asset attached
                        _updateState.value = AppUpdateState.NoUpdateAvailable(currentVersion)
                        if (isManual) {
                            _isUpdateDialogVisible.value = true
                        }
                    }
                } else {
                    _updateState.value = AppUpdateState.NoUpdateAvailable(currentVersion)
                    if (isManual) {
                        _isUpdateDialogVisible.value = true
                    }
                }
            } catch (e: CancellationException) {
                // Task cancelled
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

    private fun fetchLatestRelease(owner: String, repo: String): GithubRelease? {
        val latestUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
        val request = Request.Builder()
            .url(latestUrl)
            .header("User-Agent", "WebRadioBot-Android-App")
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string() ?: return null
                return releaseAdapter.fromJson(json)
            } else if (response.code == 404) {
                // If latest endpoint returns 404, check releases list
                val listUrl = "https://api.github.com/repos/$owner/$repo/releases?per_page=5"
                val listRequest = Request.Builder()
                    .url(listUrl)
                    .header("User-Agent", "WebRadioBot-Android-App")
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()
                val listResponse = okHttpClient.newCall(listRequest).execute()
                if (listResponse.isSuccessful) {
                    val listJson = listResponse.body?.string() ?: return null
                    val list = releaseListAdapter.fromJson(listJson)
                    return list?.firstOrNull { it.draft != true }
                }
            }
        } catch (e: Exception) {
            throw e
        }
        return null
    }

    private fun selectBestApkAsset(assets: List<GithubAsset>): GithubAsset? {
        val apkAssets = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        if (apkAssets.isEmpty()) return null

        // Priority: 1) arm64-v8a apk, 2) release apk, 3) first available apk
        return apkAssets.firstOrNull { it.name.contains("arm64", ignoreCase = true) }
            ?: apkAssets.firstOrNull { it.name.contains("release", ignoreCase = true) }
            ?: apkAssets.first()
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
                                if (now - lastEmitTime >= 120L || downloaded == totalLength) {
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

    fun installApk(activity: Activity, apkPath: String) {
        val apkFile = File(apkPath)
        if (!apkFile.exists()) {
            _updateState.value = AppUpdateState.Error("APK file not found")
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
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            activity.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            _updateState.value = AppUpdateState.Error("Error launching installer: ${e.message}")
        }
    }

    companion object {
        fun cleanVersion(tag: String): String {
            return tag.trim().removePrefix("v").removePrefix("V")
        }

        fun isNewerVersion(remoteTag: String, currentTag: String): Boolean {
            val remote = cleanVersion(remoteTag)
            val current = cleanVersion(currentTag)

            val remoteParts = remote.split(".").mapNotNull { part ->
                part.takeWhile { it.isDigit() }.toIntOrNull()
            }
            val currentParts = current.split(".").mapNotNull { part ->
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
