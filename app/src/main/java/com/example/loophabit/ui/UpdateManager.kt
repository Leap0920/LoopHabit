package com.example.loophabit.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String
)

object UpdateManager {
    private const val UPDATE_JSON_URL = "https://loop-habit-website.vercel.app/version.json"

    fun getCurrentVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    fun isNewerVersion(current: String, latest: String): Boolean {
        val cleanCurrent = current.removePrefix("v").split(".")
        val cleanLatest = latest.removePrefix("v").split(".")
        
        val maxLength = maxOf(cleanCurrent.size, cleanLatest.size)
        for (i in 0 until maxLength) {
            val currVal = cleanCurrent.getOrNull(i)?.toIntOrNull() ?: 0
            val latVal = cleanLatest.getOrNull(i)?.toIntOrNull() ?: 0
            if (latVal > currVal) return true
            if (currVal > latVal) return false
        }
        return false
    }

    suspend fun checkForUpdates(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(UPDATE_JSON_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "LoopHabitApp")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                return@withContext UpdateInfo(
                    versionName = json.getString("versionName"),
                    downloadUrl = json.getString("downloadUrl"),
                    releaseNotes = json.optString("releaseNotes", "")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 30000
            conn.setRequestProperty("User-Agent", "LoopHabitApp")
            conn.connect()

            val contentLength = conn.contentLength
            val cacheFile = File(context.cacheDir, "update.apk")
            if (cacheFile.exists()) {
                cacheFile.delete()
            }

            conn.inputStream.use { input ->
                cacheFile.outputStream().use { output ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    var totalBytesWritten = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesWritten += bytesRead
                        if (contentLength > 0) {
                            onProgress(totalBytesWritten.toFloat() / contentLength.toFloat())
                        }
                    }
                }
            }
            return@withContext cacheFile
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to start installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
