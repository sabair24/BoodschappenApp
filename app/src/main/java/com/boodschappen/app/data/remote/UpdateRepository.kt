package com.boodschappen.app.data.remote

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

data class AppVersion(
    val versionCode: Long    = 0,
    val versionName: String  = "",
    val downloadUrl: String  = "",
    val releaseNotes: String = "",
    val mandatory: Boolean   = false
)

class UpdateRepository(private val context: Context) {

    private val db     = Firebase.firestore
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /** Haal de laatste versie op uit Firestore */
    suspend fun fetchLatestVersion(): AppVersion? = withContext(Dispatchers.IO) {
        try {
            val snap = db.collection("app_updates").document("latest").get().await()
            if (!snap.exists()) return@withContext null
            AppVersion(
                versionCode  = (snap.getLong("versionCode") ?: 0),
                versionName  = snap.getString("versionName") ?: "",
                downloadUrl  = snap.getString("downloadUrl") ?: "",
                releaseNotes = snap.getString("releaseNotes") ?: "",
                mandatory    = snap.getBoolean("mandatory") ?: false
            )
        } catch (e: Exception) {
            Log.w("UpdateRepo", "fetchLatestVersion failed: ${e.message}")
            null
        }
    }

    /** Download de APK en rapporteer voortgang via [onProgress] (0..100) */
    suspend fun downloadApk(
        url: String,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val request  = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body          = response.body ?: return@withContext null
            val contentLength = body.contentLength()
            val apkFile       = File(context.filesDir, "update.apk")

            body.byteStream().use { input ->
                apkFile.outputStream().use { output ->
                    val buffer    = ByteArray(8 * 1024)
                    var bytesRead = 0L
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        bytesRead += bytes
                        if (contentLength > 0) {
                            onProgress(((bytesRead * 100) / contentLength).toInt())
                        }
                    }
                }
            }
            onProgress(100)
            apkFile
        } catch (e: Exception) {
            Log.e("UpdateRepo", "downloadApk failed: ${e.message}")
            null
        }
    }

    /** Start de installatie van de gedownloade APK */
    fun installApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data  = uri
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
        }
        context.startActivity(intent)
    }

}
