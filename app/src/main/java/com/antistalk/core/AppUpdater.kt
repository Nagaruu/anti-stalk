package com.antistalk.core

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Self-update over GitHub Releases (public repo, no login needed).
 * Convention: tags are vN (v1, v2, ...) and the APK inside has versionCode == N.
 * Update available iff latest tag number > installed versionCode.
 *
 * Near-auto flow ("1 chạm duy nhất"): the app checks on start, downloads
 * in the background, then opens the system installer by itself. The user
 * only taps the system's Install button.
 *
 * No new dependencies: HttpURLConnection + org.json (bundled) + DownloadManager.
 * Android always requires ONE tap on the system install prompt — that part
 * cannot be skipped for sideloaded apps (OS anti-malware boundary; only
 * Play / device-owner / root can install silently).
 */
object AppUpdater {
    private const val REPO = "Nagaruu/anti-stalk"
    private const val API_LATEST = "https://api.github.com/repos/$REPO/releases/latest"
    private const val APK_NAME = "app-debug.apk"

    data class UpdateInfo(val tag: String, val tagNumber: Int, val notes: String, val apkUrl: String)

    /**
     * Honest result: [Unreachable] means the API itself failed (offline,
     * private repo, no releases yet) — NOT "up to date". Callers must show
     * this differently instead of claiming the newest version is installed.
     */
    sealed interface CheckResult {
        data class Update(val info: UpdateInfo) : CheckResult
        data object UpToDate : CheckResult
        data object Unreachable : CheckResult
    }

    // Separate prefs file so "xóa toàn bộ dữ liệu local" doesn't nuke it.
    private const val PREFS_UPDATE = "antistalk_update"
    private const val KEY_DOWNLOAD_ID = "download_id"
    private const val KEY_DOWNLOADED_TAG = "downloaded_tag"
    private const val KEY_PROMPTED_TAG = "prompted_install_tag"
    private const val FILE_NAME = "anti-stalk-update.apk"

    private fun updatePrefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS_UPDATE, Context.MODE_PRIVATE)

    /** Tag of the last enqueued download, or "" if none. Works offline. */
    fun savedTag(ctx: Context): String =
        try { updatePrefs(ctx).getString(KEY_DOWNLOADED_TAG, "") ?: "" }
        catch (_: Exception) { "" }

    fun wasPrompted(ctx: Context, tag: String): Boolean =
        try { updatePrefs(ctx).getString(KEY_PROMPTED_TAG, "") == tag }
        catch (_: Exception) { false }

    fun markPrompted(ctx: Context, tag: String) {
        try { updatePrefs(ctx).edit().putString(KEY_PROMPTED_TAG, tag).apply() }
        catch (_: Exception) { }
    }

    /** Fail-open: if we can't tell, don't block the update on a metered check. */
    fun isOnWifi(ctx: Context): Boolean {
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (_: Exception) { true }
    }

    /**
     * If [tag] was fully downloaded before (even in a previous process),
     * return its content Uri, else null. Survives app restarts: DownloadManager
     * keeps finished downloads until the file is removed.
     */
    fun downloadedUriIfReady(ctx: Context, tag: String): Uri? {
        return try {
            val p = updatePrefs(ctx)
            val id = p.getLong(KEY_DOWNLOAD_ID, -1L)
            if (id == -1L || p.getString(KEY_DOWNLOADED_TAG, "") != tag) return null
            val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.query(DownloadManager.Query().setFilterById(id)).use { c ->
                if (!c.moveToFirst()) return null
                val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                if (status == DownloadManager.STATUS_SUCCESSFUL) dm.getUriForDownloadedFile(id)
                else null
            }
        } catch (_: Exception) { null }
    }

    /**
     * Enqueue a background download (replacing any stale one) and invoke
     * [onComplete] on the main thread when it finishes. The receiver
     * unregisters itself; if the process dies mid-download, use
     * [downloadedUriIfReady] on next start to reconcile.
     */
    fun enqueueDownload(ctx: Context, url: String, tag: String, onComplete: (Uri) -> Unit): Long {
        val appCtx = ctx.applicationContext
        val dm = appCtx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val p = updatePrefs(appCtx)
        val old = p.getLong(KEY_DOWNLOAD_ID, -1L)
        if (old != -1L) { try { dm.remove(old) } catch (_: Exception) { } }
        val req = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("Anti-Stalk đang cập nhật…")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, FILE_NAME)
            setMimeType("application/vnd.android.package-archive")
        }
        val id = dm.enqueue(req)
        p.edit().putLong(KEY_DOWNLOAD_ID, id).putString(KEY_DOWNLOADED_TAG, tag).apply()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != id) return
                try { appCtx.unregisterReceiver(this) } catch (_: Exception) { }
                try {
                    val uri = dm.getUriForDownloadedFile(id) ?: return
                    onComplete(uri)
                } catch (_: Exception) {
                    Toast.makeText(c, "Tải xong nhưng không mở được file", Toast.LENGTH_LONG).show()
                }
            }
        }
        ContextCompat.registerReceiver(
            appCtx,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
        return id
    }

    /**
     * Open the system installer for an already-downloaded APK.
     *
     * Blocked with a toast when the APK's signer differs from the installed
     * app's — Android would fail that install anyway, and showing the system
     * install screen just to get "gói xung đột" wastes a tap.
     *
     * [verifySigner] = false lets callers that already compared signatures on
     * a background thread skip the (expensive) re-read.
     */
    fun openInstaller(ctx: Context, uri: Uri, verifySigner: Boolean = true): Boolean {
        if (verifySigner && SigningInfo.archiveMatchesInstalled(ctx, uri) == false) {
            Toast.makeText(
                ctx,
                "Bản tải về dùng khoá ký khác app đang cài — Android sẽ từ chối. Vào Cài đặt để làm bước chuyển đổi.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }
        return try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
            true
        } catch (_: Exception) {
            Toast.makeText(ctx, "Không mở được màn cài đặt", Toast.LENGTH_LONG).show()
            false
        }
    }

    suspend fun checkResult(currentVersionCode: Int): CheckResult = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(API_LATEST).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            if (conn.responseCode != 200) return@withContext CheckResult.Unreachable
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val tag = json.optString("tag_name", "")
            val num = tag.removePrefix("v").toIntOrNull() ?: return@withContext CheckResult.Unreachable
            if (num <= currentVersionCode) return@withContext CheckResult.UpToDate
            val assets = json.optJSONArray("assets") ?: return@withContext CheckResult.Unreachable
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name") == APK_NAME) {
                    apkUrl = a.optString("browser_download_url")
                    break
                }
            }
            if (apkUrl.isNullOrBlank()) return@withContext CheckResult.Unreachable
            CheckResult.Update(UpdateInfo(tag, num, json.optString("body", ""), apkUrl))
        } catch (_: Exception) {
            CheckResult.Unreachable // offline or API hiccup
        }
    }

    /** Nullable wrapper for callers that only care about available updates. */
    suspend fun check(currentVersionCode: Int): UpdateInfo? =
        (checkResult(currentVersionCode) as? CheckResult.Update)?.info

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

    /** Downloads the APK and opens the system installer on completion. */
    fun downloadAndInstall(ctx: Context, url: String, tag: String = "") {
        val appCtx = ctx.applicationContext
        if (!canInstall(appCtx)) {
            openInstallPermissionSettings(ctx)
            Toast.makeText(ctx, "Bật cho phép rồi nhấn Cập nhật lại nhé", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(ctx, "Đang tải bản mới…", Toast.LENGTH_SHORT).show()
        enqueueDownload(ctx, url, tag) { uri ->
            openInstaller(appCtx, uri)
        }
    }
}
