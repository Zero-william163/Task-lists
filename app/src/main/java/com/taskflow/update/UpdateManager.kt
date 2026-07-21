package com.taskflow.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.taskflow.BuildConfig
import com.taskflow.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object UpdateManager {

    private const val GITHUB_API_URL = "https://api.github.com/repos/Zero-william163/Task-lists/releases/latest"
    private const val RELEASE_JSON_URL = "https://raw.githubusercontent.com/Zero-william163/Task-lists/main/release.json"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    data class ReleaseInfo(
        @SerializedName("tag_name") val tagName: String,
        @SerializedName("name") val name: String,
        @SerializedName("body") val body: String,
        @SerializedName("published_at") val publishedAt: String,
        @SerializedName("assets") val assets: List<ReleaseAsset>
    )

    data class ReleaseAsset(
        @SerializedName("name") val name: String,
        @SerializedName("browser_download_url") val downloadUrl: String,
        @SerializedName("size") val size: Long
    )

    data class ReleaseConfig(
        @SerializedName("versionCode") val versionCode: Int,
        @SerializedName("versionName") val versionName: String,
        @SerializedName("apkUrl") val apkUrl: String,
        @SerializedName("changelog") val changelog: String,
        @SerializedName("forceUpdate") val forceUpdate: Boolean,
        @SerializedName("apkHash") val apkHash: String
    )

    data class UpdateResult(
        val hasUpdate: Boolean,
        val versionName: String = "",
        val changelog: String = "",
        val downloadUrl: String = "",
        val apkHash: String = ""
    )

    suspend fun checkForUpdate(context: Context, showNotification: Boolean = false): UpdateResult {
        return withContext(Dispatchers.IO) {
            try {
                val config = fetchReleaseConfig()
                    ?: return@withContext checkGitHubRelease(showNotification)

                if (config.versionCode > BuildConfig.VERSION_CODE) {
                    if (showNotification) {
                        NotificationHelper.showUpdateNotification(
                            context,
                            config.versionName,
                            config.changelog
                        )
                    }
                    UpdateResult(
                        hasUpdate = true,
                        versionName = config.versionName,
                        changelog = config.changelog,
                        downloadUrl = config.apkUrl,
                        apkHash = config.apkHash
                    )
                } else {
                    UpdateResult(hasUpdate = false)
                }
            } catch (e: Exception) {
                UpdateResult(hasUpdate = false)
            }
        }
    }

    private fun fetchReleaseConfig(): ReleaseConfig? {
        val request = Request.Builder().url(RELEASE_JSON_URL).build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Gson().fromJson(response.body?.string(), ReleaseConfig::class.java)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun checkGitHubRelease(showNotification: Boolean): UpdateResult {
        val request = Request.Builder()
            .url(GITHUB_API_URL)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return UpdateResult(hasUpdate = false)

                val release = Gson().fromJson(response.body?.string(), ReleaseInfo::class.java)
                val latestVersion = release.tagName.removePrefix("v").removePrefix("V")
                val currentVersion = BuildConfig.VERSION_NAME

                if (isNewerVersion(latestVersion, currentVersion)) {
                    val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                    UpdateResult(
                        hasUpdate = true,
                        versionName = latestVersion,
                        changelog = release.body,
                        downloadUrl = apkAsset?.downloadUrl ?: "",
                        apkHash = ""
                    )
                } else {
                    UpdateResult(hasUpdate = false)
                }
            }
        } catch (e: Exception) {
            UpdateResult(hasUpdate = false)
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        return try {
            val latestParts = latest.split(".").map { it.toInt() }
            val currentParts = current.split(".").map { it.toInt() }
            for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            false
        } catch (e: Exception) {
            latest != current
        }
    }

    suspend fun downloadApk(context: Context, url: String, expectedHash: String = ""): File? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) return@withContext null

                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "taskflow_update.apk")
                response.body?.byteStream()?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                if (expectedHash.isNotBlank() && !verifyFileHash(file, expectedHash)) {
                    file.delete()
                    return@withContext null
                }

                file
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun verifyFileHash(file: File, expectedHash: String): Boolean {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } > 0) {
                    digest.update(buffer, 0, read)
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            hash.equals(expectedHash, ignoreCase = true)
        } catch (e: Exception) {
            true
        }
    }

    fun installApk(context: Context, file: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                Uri.fromFile(file)
            }
            setDataAndType(uri, "application/vnd.android.package-archive")
        }
        context.startActivity(intent)
    }
}
