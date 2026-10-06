package com.antistalk.core

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Self-update over GitHub Releases (public, no login needed).
 * Convention: tags are vN (v1, v2, ...) and the APK inside has versionCode == N.
 * Update available iff latest tag number > installed versionCode.
 *
 * No new dependencies: HttpURLConnection + org.json (bundled) + DownloadManager.
 * Android always requires ONE tap on the system install prompt — that part
 * cannot be skipped for sideloaded apps.
 */
object AppUpdater {
    private const val REPO = "Nagaruu/anti-stalk"
    private const val API_LATEST = "https://api.github.com/repos/$REPO/releases/latest"
    private const val APK_NAME = "app-debug.apk"

    data class UpdateInfo(val tag: String, val tagNumber: Int, val notes: String, val apkUrl: String)

    suspend fun check(currentVersionCode: Int): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(API_LATEST).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            if (conn.responseCode != 200) return@withContext null
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val tag = json.optString("tag_name", "")
            val num = tag.removePrefix("v").toIntOrNull() ?: return@withContext null
            if (num <= currentVersionCode) return@withContext null
            val assets = json.optJSONArray("assets") ?: return@withContext null
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name") == APK_NAME) {
                    apkUrl = a.optString("browser_download_url")
                    break
                }
            }
            if (apkUrl.isNullOrBlank()) return@withContext null
            UpdateInfo(tag, num, json.optString("body", ""), apkUrl)
        } catch (_: Exception) {
            null // offline or API hiccup: silently no update
        }
    }

    fun canInstall(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.packageManager.canRequestPackageInstalls()
        } else true

    fun openInstallPermissionSettings(ctx: Context) {
        try {
            ctx.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${ctx.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            Toast.makeText(ctx, "Vào Cài đặt > cho phép cài ứng dụng không xác định cho Anti-Stalk", Toast.LENGTH_LONG).show()
        }
    }

    /** Downloads the APK to Downloads/ and opens the system installer on completion. */
    fun downloadAndInstall(ctx: Context, url: String) {
        val appCtx = ctx.applicationContext
        if (!canInstall(appCtx)) {
            openInstallPermissionSettings(ctx)
            Toast.makeText(ctx, "Bật cho phép rồi nhấn Cập nhật lại nhé", Toast.LENGTH_LONG).show()
            return
        }
        val dm = appCtx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val req = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("Anti-Stalk đang cập nhật…")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "anti-stalk-update.apk")
            setMimeType("application/vnd.android.package-archive")
        }
        val id = dm.enqueue(req)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != id) return
                try { appCtx.unregisterReceiver(this) } catch (_: Exception) { }
                try {
                    val uri = dm.getUriForDownloadedFile(id) ?: return
                    c.startActivity(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    })
                } catch (_: Exception) {
                    Toast.makeText(c, "Tải xong nhưng không mở được màn cài đặt", Toast.LENGTH_LONG).show()
                }
            }
        }
        appCtx.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        Toast.makeText(ctx, "Đang tải bản mới…", Toast.LENGTH_SHORT).show()
    }
}
