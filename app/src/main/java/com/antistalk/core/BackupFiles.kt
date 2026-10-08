package com.antistalk.core

import android.content.Context
import android.net.Uri

/**
 * Stream helpers for the Storage Access Framework URIs the backup flow hands us
 * (SAF URIs can point into other providers, so everything is copied through
 * streams — never assumed to live in getExternalFilesDir, which Android wipes
 * on uninstall anyway).
 */
object BackupFiles {

    fun readText(ctx: Context, uri: Uri): String? {
        return try {
            val input = ctx.contentResolver.openInputStream(uri) ?: return null
            input.use { it.readBytes().decodeToString() }
        } catch (_: Exception) {
            null
        }
    }

    fun writeText(ctx: Context, uri: Uri, text: String): Boolean {
        return try {
            val out = ctx.contentResolver.openOutputStream(uri, "wt") ?: return false
            out.use { it.write(text.toByteArray()) }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun copy(ctx: Context, src: Uri, dst: Uri): Boolean {
        return try {
            val input = ctx.contentResolver.openInputStream(src) ?: return false
            val output = ctx.contentResolver.openOutputStream(dst, "wt") ?: return false
            input.use { ins -> output.use { out -> ins.copyTo(out) } }
            true
        } catch (_: Exception) {
            false
        }
    }
}
