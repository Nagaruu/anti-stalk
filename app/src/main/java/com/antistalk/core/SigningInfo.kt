package com.antistalk.core

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import java.io.File
import java.security.MessageDigest

/**
 * Reads the signing certificate of (a) the installed app and (b) an APK file
 * we are about to install. Android refuses any update whose signer differs from
 * the installed one (INSTALL_FAILED_UPDATE_INCOMPATIBLE), so we detect that
 * BEFORE handing a file to the system installer and guide a one-time migration
 * instead of letting the install screen fail.
 *
 * [EXPECTED_CERT_SHA256] is pinned to keystore/debug.keystore — the same value
 * .github/workflows/build-apk.yml asserts after every build. Never rotate that
 * keystore without a migration flow: it would break every installed update path.
 */
object SigningInfo {

    /** SHA-256 of the certificate CI signs every release APK with. */
    const val EXPECTED_CERT_SHA256 =
        "162de3ba4d8a6c943e04080bbfe0cd5958d6482124fc2fd8610e2de3c62e8d82"

    fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun firstCertSha(info: PackageInfo?): String? {
        val sig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info?.signingInfo?.apkContentsSigners?.firstOrNull()
        } else {
            @Suppress("DEPRECATION")
            info?.signatures?.firstOrNull()
        } ?: return null
        return sha256Hex(sig.toByteArray())
    }

    /** SHA-256 of the certificate the running app was signed with. */
    fun installedCertSha256(ctx: Context): String? = try {
        val pm = ctx.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            firstCertSha(pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNING_CERTIFICATES))
        } else {
            @Suppress("DEPRECATION")
            firstCertSha(pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNATURES))
        }
    } catch (_: Exception) { null }

    /** Short display form, e.g. "162de3ba…62e8d82". */
    fun shortSha(sha: String?): String = when {
        sha.isNullOrBlank() -> "không đọc được"
        sha.length <= 16 -> sha
        else -> sha.take(8) + "…" + sha.takeLast(8)
    }

    /** SHA-256 of the certificate inside an APK referenced by [uri]. */
    fun archiveCertSha256(ctx: Context, uri: Uri): String? {
        val fromFile = uri.scheme == "file" && !uri.path.isNullOrEmpty()
        var file: File? = null
        return try {
            val target: File = if (fromFile) {
                File(uri.path!!)
            } else {
                val input = ctx.contentResolver.openInputStream(uri) ?: return null
                val tmp = File(ctx.cacheDir, "update-verify.apk")
                input.use { ins -> tmp.outputStream().use { out -> ins.copyTo(out) } }
                tmp
            }
            file = target
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            firstCertSha(ctx.packageManager.getPackageArchiveInfo(target.absolutePath, flags))
        } catch (_: Exception) {
            null
        } finally {
            if (!fromFile) file?.delete()
        }
    }

    /**
     * true  = safe to install over the current app
     * false = Android will reject it (different signer)
     * null  = couldn't read the archive, let the system decide
     */
    fun archiveMatchesInstalled(ctx: Context, uri: Uri): Boolean? {
        val archive = archiveCertSha256(ctx, uri) ?: return null
        val installed = installedCertSha256(ctx) ?: return null
        return archive.equals(installed, ignoreCase = true)
    }

    /** Installed app is signed with the same key CI releases with. */
    fun installedIsReleaseKey(ctx: Context): Boolean {
        val sha = installedCertSha256(ctx) ?: return false
        return sha.equals(EXPECTED_CERT_SHA256, ignoreCase = true)
    }
}
