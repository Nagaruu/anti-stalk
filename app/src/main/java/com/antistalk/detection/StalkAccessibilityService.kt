package com.antistalk.detection

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.antistalk.core.MonitoredPackages
import com.antistalk.data.AntiStalkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Passive observer. NEVER performs gestures/clicks on behalf of the user
 * (that would violate Play's automation policy).
 *
 * Flow: event -> is monitored pkg? -> cooldown? -> match keyword? -> log + overlay.
 * - Throttle: notificationTimeout=300ms in XML + 30s cooldown per person here.
 * - No message bodies stored; only matched name + trigger type.
 */
class StalkAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repo: AntiStalkRepository
    private var keywords: List<Triple<Long, String, String>> = emptyList() // pid, pname, normalizedKw
    private var enabledPkgs: Set<String> = emptySet()
    private var lastTriggerAt: MutableMap<String, Long> = mutableMapOf()

    override fun onServiceConnected() {
        repo = AntiStalkRepository(this)
        refreshCache()
    }

    private fun refreshCache() {
        scope.launch {
            try {
                repo.seedIfNeeded()
                enabledPkgs = repo.enabledPackagesOnce()
                val persons = repo.personsOnce().associateBy { it.id }
                keywords = repo.keywordsOnce().mapNotNull { kw ->
                    val p = persons[kw.personId] ?: return@mapNotNull null
                    Triple(kw.personId, p.displayName, kw.normalized)
                }
            } catch (_: Exception) { }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (!MonitoredPackages.isMonitored(pkg)) return
        if (keywords.isEmpty() || !enabledPkgs.contains(pkg)) {
            // Refresh lazily: user may have just added a person.
            refreshCache()
            return
        }

        val detector = DetectorRegistry.forPackage(pkg)

        // Signal 1 — search input (HIGH confidence)
        val typed = try { detector.searchTextFromEvent(event) } catch (_: Exception) { null }
        if (typed != null) {
            val m = Matcher.findMatch(typed, keywords)
            if (m != null) {
                maybeTrigger(pkg, m, trigger = "SEARCH_INPUT", confidence = "HIGH")
                return
            }
        }

        // Signal 2 — window change: check title (MEDIUM confidence), throttled.
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // Don't run tree walk on main thread.
            scope.launch {
                try {
                    val root = rootInActiveWindow
                    val title = try { detector.titleFromRoot(root) } catch (_: Exception) { null }
                    if (title != null) {
                        val m = Matcher.findMatch(title, keywords)
                        if (m != null) maybeTrigger(pkg, m, trigger = "PROFILE_TITLE", confidence = "MEDIUM")
                    }
                } catch (_: Exception) { }
            }
        }
    }

    private fun maybeTrigger(pkg: String, m: Matcher.Match, trigger: String, confidence: String) {
        val now = System.currentTimeMillis()
        val key = "${m.personName}|$pkg"
        if (now - (lastTriggerAt[key] ?: 0L) < 30_000) return // 30s cooldown per person+app
        lastTriggerAt[key] = now
        scope.launch {
            try {
                val eventId = repo.logEvent(m.personName, m.personId, pkg, trigger, confidence)
                val countToday = repo.countTodayForPerson(m.personName)
                OverlayManager.show(
                    this@StalkAccessibilityService, eventId,
                    m.personName, pkg, trigger, confidence, countToday
                )
            } catch (_: Exception) { }
        }
    }

    override fun onInterrupt() { }

    override fun onUnbind(intent: Intent?): Boolean {
        OverlayManager.hide()
        return super.onUnbind(intent)
    }

    companion object {
        /**
         * The system stores enabled services as flattened components with the
         * FULL class name, e.g.
         * "com.antistalk.debug/com.antistalk.detection.StalkAccessibilityService".
         * Never match against the ".detection.…" shorthand — it never appears there.
         */
        fun isEnabled(ctx: android.content.Context): Boolean {
            val enabled = Settings.Secure.getString(
                ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val pkg = ctx.packageName
            val fullCls = StalkAccessibilityService::class.java.name
            return enabled.split(':').any { entry ->
                val slash = entry.indexOf('/')
                if (slash < 0) return@any false
                val ePkg = entry.substring(0, slash)
                val eCls = entry.substring(slash + 1)
                ePkg.equals(pkg, ignoreCase = true) &&
                    (eCls == fullCls || eCls.endsWith(".StalkAccessibilityService"))
            }
        }
    }
}
