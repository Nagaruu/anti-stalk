package com.antistalk.core

/** Monitored social apps. Add new apps here only — detectors are generic. */
object MonitoredPackages {
    const val FACEBOOK = "com.facebook.katana"
    const val MESSENGER = "com.facebook.orca"
    const val INSTAGRAM = "com.instagram.android"
    const val ZALO = "com.zing.zalo"

    val DEFAULTS = listOf(
        MonitoredApp(FACEBOOK, "Facebook", true),
        MonitoredApp(MESSENGER, "Messenger", true),
        MonitoredApp(INSTAGRAM, "Instagram", true),
        MonitoredApp(ZALO, "Zalo", true),
    )

    data class MonitoredApp(val packageName: String, val label: String, val defaultEnabled: Boolean)

    fun isMonitored(pkg: String?): Boolean =
        pkg != null && DEFAULTS.any { it.packageName == pkg }
}

// Free tier limits (monetization stub for later)
object FreeLimits {
    const val MAX_PERSONS = 1
    const val MAX_APPS = 2
}
