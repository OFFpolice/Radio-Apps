package com.offpolice.webradiobot.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.offpolice.webradiobot.BuildConfig
import com.squareup.moshi.Moshi
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
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

interface GitHubDataService {
    @GET("repos/OFFpolice/Radio-Apps/releases/latest")
    suspend fun getLatestRelease(): Response<GithubRelease>
}

interface DownloadService {
    @GET
    @Streaming
    fun downloadFile(@Url fileUrl: String): Call<ResponseBody>
}

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpdateAvailable(
        val release: GithubRelease,
        val apkAsset: GithubAsset?,
        val currentVersion: String,
        val newVersion: String,
        val isManualCheck: Boolean = false
    ) : UpdateState
    data class UpToDate(
        val currentVersion: String,
        val isManualCheck: Boolean = false
    ) : UpdateState
    data class Downloading(
        val percentage: Float, // 0.0f .. 1.0f
        val downloadedBytes: Long,
        val totalBytes: Long,
        val version: String
    ) : UpdateState
    data class ReadyToInstall(
        val apkFile: File,
        val version: String
    ) : UpdateState
    data class Error(
        val message: String,
        val isManualCheck: Boolean = false
    ) : UpdateState
}

class AppUpdateManager(private val context: Context) {

    private val moshi = Moshi.Builder().build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val gitHubDataService: GitHubDataService = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GitHubDataService::class.java)

    private val downloadService: DownloadService = Retrofit.Builder()
        .baseUrl("https://github.com/")
        .client(okHttpClient)
        .build()
        .create(DownloadService::class.java)

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _percentageFlow = MutableStateFlow(0f)
    val percentageFlow: StateFlow<Float> = _percentageFlow.asStateFlow()

    private val _isUpdateInProcessFlow = MutableStateFlow(false)
    val isUpdateInProcessFlow: StateFlow<Boolean> = _isUpdateInProcessFlow.asStateFlow()

    private var activeDownloadCall: Call<ResponseBody>? = null
    private var downloadJob: Job? = null

    val currentVersionName: String
        get() = try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: BuildConfig.VERSION_NAME
        } catch (e: Exception) {
            BuildConfig.VERSION_NAME
        }

    suspend fun checkForUpdates(isManualCheck: Boolean = false) = withContext(Dispatchers.IO) {
        if (_isUpdateInProcessFlow.value) return@withContext

        _updateState.value = UpdateState.Checking
        try {
            val response = gitHubDataService.getLatestRelease()
            if (response.isSuccessful && response.body() != null) {
                val release = response.body()!!
                val cleanRemoteTag = release.tagName.removePrefix("v").trim()
                val currentVersion = currentVersionName.removePrefix("v").trim()

                if (isNewerVersion(cleanRemoteTag, currentVersion)) {
                    val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                        ?: release.assets.firstOrNull()

                    _updateState.value = UpdateState.UpdateAvailable(
                        release = release,
                        apkAsset = apkAsset,
                        currentVersion = currentVersionName,
                        newVersion = release.tagName,
                        isManualCheck = isManualCheck
                    )
                } else {
                    _updateState.value = UpdateState.UpToDate(
                        currentVersion = currentVersionName,
                        isManualCheck = isManualCheck
                    )
                }
            } else {
                _updateState.value = UpdateState.Error(
                    message = "GitHub API response: ${response.code()}",
                    isManualCheck = isManualCheck
                )
            }
        } catch (e: UnknownHostException) {
            _updateState.value = UpdateState.Error(
                message = "Нет подключения к интернету",
                isManualCheck = isManualCheck
            )
        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Check update error", e)
            _updateState.value = UpdateState.Error(
                message = e.localizedMessage ?: "Ошибка проверки обновлений",
                isManualCheck = isManualCheck
            )
        }
    }

    fun startDownload(
        coroutineScope: CoroutineScope,
        release: GithubRelease,
        asset: GithubAsset
    ) {
        if (_isUpdateInProcessFlow.value) return

        downloadJob?.cancel()
        downloadJob = coroutineScope.launch(Dispatchers.IO) {
            val downloadUrl = asset.browserDownloadUrl.ifBlank {
                "https://github.com/OFFpolice/Radio-Apps/releases/download/${release.tagName}/${asset.name}"
            }
            val destinationFile = File(context.externalCacheDir ?: context.cacheDir, "app-release.apk")

            try {
                _isUpdateInProcessFlow.value = true
                _percentageFlow.value = 0f
                _updateState.value = UpdateState.Downloading(
                    percentage = 0f,
                    downloadedBytes = 0L,
                    totalBytes = asset.size,
                    version = release.tagName
                )

                if (destinationFile.exists()) {
                    destinationFile.delete()
                }

                val call = downloadService.downloadFile(downloadUrl)
                activeDownloadCall = call
                val response = call.execute()
                val body = response.body()

                if (response.isSuccessful && body != null) {
                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(destinationFile)
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    val contentLength = if (body.contentLength() > 0) body.contentLength() else asset.size
                    var lastPercentage = -1

                    try {
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            val currentPercentage = if (contentLength > 0) {
                                (totalBytesRead * 100 / contentLength).toInt().coerceIn(0, 100)
                            } else {
                                0
                            }

                            if (currentPercentage != lastPercentage) {
                                lastPercentage = currentPercentage
                                val progressFloat = currentPercentage / 100f
                                _percentageFlow.value = progressFloat
                                _updateState.value = UpdateState.Downloading(
                                    percentage = progressFloat,
                                    downloadedBytes = totalBytesRead,
                                    totalBytes = contentLength,
                                    version = release.tagName
                                )
                            }
                        }
                        outputStream.flush()
                    } finally {
                        outputStream.close()
                        inputStream.close()
                    }

                    _isUpdateInProcessFlow.value = false
                    _percentageFlow.value = 1f
                    _updateState.value = UpdateState.ReadyToInstall(
                        apkFile = destinationFile,
                        version = release.tagName
                    )

                    // Automatically prompt installation
                    withContext(Dispatchers.Main) {
                        installApk(destinationFile)
                    }
                } else {
                    throw IOException("HTTP error: ${response.code()}")
                }
            } catch (e: CancellationException) {
                _isUpdateInProcessFlow.value = false
                _percentageFlow.value = 0f
                _updateState.value = UpdateState.Idle
            } catch (e: Exception) {
                Log.e("AppUpdateManager", "Download error", e)
                _isUpdateInProcessFlow.value = false
                _percentageFlow.value = 0f
                _updateState.value = UpdateState.Error(
                    message = e.localizedMessage ?: "Ошибка скачивания обновления",
                    isManualCheck = true
                )
            } finally {
                activeDownloadCall = null
            }
        }
    }

    fun cancelDownload() {
        activeDownloadCall?.cancel()
        downloadJob?.cancel()
        _isUpdateInProcessFlow.value = false
        _percentageFlow.value = 0f
        _updateState.value = UpdateState.Idle
    }

    fun installApk(apkFile: File): Boolean {
        if (!apkFile.exists()) {
            Log.e("AppUpdateManager", "APK file does not exist: ${apkFile.absolutePath}")
            return false
        }

        // On Android 8.0+ (API 26), verify if app has permission to install unknown apps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                try {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                } catch (e: Exception) {
                    Log.e("AppUpdateManager", "Cannot open unknown sources settings", e)
                }
                return false
            }
        }

        return try {
            val authority = "${context.packageName}.provider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Failed to launch installer", e)
            false
        }
    }

    fun dismissUpdate() {
        if (!_isUpdateInProcessFlow.value) {
            _updateState.value = UpdateState.Idle
        }
    }

    companion object {
        @Volatile
        private var instance: AppUpdateManager? = null

        fun getInstance(context: Context): AppUpdateManager {
            return instance ?: synchronized(this) {
                instance ?: AppUpdateManager(context.applicationContext).also { instance = it }
            }
        }

        fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
            val cleanRemote = remoteVersion.removePrefix("v").trim()
            val cleanCurrent = currentVersion.removePrefix("v").trim()
            if (cleanRemote.isBlank() || cleanCurrent.isBlank()) return false
            if (cleanRemote == cleanCurrent) return false

            val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return cleanRemote != cleanCurrent
        }
    }
}
