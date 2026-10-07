package com.antistalk.detection

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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
 * - Keystroke matches also arm a short "submit window": if a new window opens
 *   within a few seconds (user pressed search / opened results), a
 *   SEARCH_SUBMITTED trigger fires even inside the 30s cooldown.
 * - No message bodies stored; only matched name + trigger type.
 */
class StalkAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repo: AntiStalkRepository
    private var keywords: List<Triple<Long, String, String>> = emptyList() // pid, pname, normalizedKw
    private var enabledPkgs: Set<String> = emptySet()
    private var lastTriggerAt: MutableMap<String, Long> = mutableMapOf()
    private var lastKeystrokeMatchAt: MutableMap<String, Long> = mutableMapOf()
    private var lastSubmitAt: MutableMap<String, Long> = mutableMapOf()
    private var lastFocusReadAt: Long = 0L
    private var lastRawLogAt: Long = 0L

    override fun onServiceConnected() {
        repo = AntiStalkRepository(this)
        refreshCache()
    }

    private fun refreshCache() {
        scope.launch { refreshCacheNow() }
    }

    private suspend fun refreshCacheNow() {
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (!MonitoredPackages.isMonitored(pkg)) return

        // Snapshot synchronously: the framework may recycle the event
        // after this callback returns, so never touch it from a coroutine.
        val detector = DetectorRegistry.forPackage(pkg)
        val type = event.eventType
        val typed: CharSequence? = try { detector.searchTextFromEvent(event) } catch (_: Exception) { null }
        val isWindowChange = type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        val typeName = try { AccessibilityEvent.eventTypeToString(type) } catch (_: Exception) { type.toString() }

        // DEBUG: prove events arrive at all — including ones carrying no text.
        // Without this line, "Facebook sends nothing" and "text extraction
        // fails" look identical in the log. Throttled: content-change storms.
        if (System.currentTimeMillis() - lastRawLogAt > RAW_LOG_THROTTLE_MS &&
            (typed == null || typed.length < 3)
        ) {
            lastRawLogAt = System.currentTimeMillis()
            DetectionLog.add(pkg, typeName, typed?.toString()?.take(24) ?: "—", "seen-no-text")
        }

        if (keywords.isEmpty() || !enabledPkgs.contains(pkg)) {
            // Cold cache (e.g. person just added): refresh, then process
            // this same event with fresh data instead of dropping it.
            if (type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                DetectionLog.add(pkg, typeName, typed?.toString()?.take(24) ?: "—", "cache-refresh")
            }
            scope.launch {
                refreshCacheNow()
                if (!enabledPkgs.contains(pkg)) return@launch
                if (typed != null) processTyped(pkg, typed, "event")
                else readFocusedText(pkg)
                if (isWindowChange) processTitle(pkg)
            }
            return
        }

        // Signal 1 — search input (HIGH confidence)
        if (typed != null) {
            processTyped(pkg, typed, "event")
        } else if (type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
            type == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            // Some search fields (e.g. newer Compose text fields) fire events
            // WITHOUT putting text in event.text. Fall back to reading the
            // currently focused input node, throttled.
            val now = System.currentTimeMillis()
            if (now - lastFocusReadAt > FOCUS_READ_THROTTLE_MS) {
                lastFocusReadAt = now
                scope.launch { readFocusedText(pkg) }
            }
        }

        // Signal 2+3 — window change: match every title candidate (MEDIUM),
        // or fire SEARCH_SUBMITTED (HIGH) if it follows a keystroke match.
        if (isWindowChange) {
            // Don't run tree walk on main thread.
            scope.launch { processTitle(pkg) }
        }
    }

    /** Read the focused input node's text directly (event.text may be empty). */
    private fun readFocusedText(pkg: String) {
        try {
            val root = rootInActiveWindow ?: return
            val focused = try {
                root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            } catch (_: Exception) { null }
            val text = focused?.text?.toString()
            if (!text.isNullOrBlank() && text.length >= 3) {
                processTyped(pkg, text, "focused")
                return
            }
            // Facebook can drop input focus while the keyboard/autocomplete is
            // open — scan every editable node instead of giving up.
            val editable = firstEditableText(root)
            if (editable != null) processTyped(pkg, editable, "editable")
        } catch (_: Exception) { }
    }

    /** BFS the first editable node with usable text. Bounded: trees are huge. */
    private fun firstEditableText(root: AccessibilityNodeInfo): String? {
        var best: String? = null
        var seen = 0
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty() && seen < 80) {
            val n = queue.removeFirst()
            seen++
            val editable = try { n.isEditable } catch (_: Exception) { false }
            if (editable || n.className?.contains("EditText") == true) {
                val t = n.text?.toString()
                if (best == null && !t.isNullOrBlank() && t.length >= 3) best = t
            }
            for (i in 0 until n.childCount) {
                try { n.getChild(i)?.let { queue.add(it) } } catch (_: Exception) { }
            }
        }
        return best
    }

    private fun processTyped(pkg: String, typed: CharSequence, src: String): Matcher.Match? {
        val snippet = typed.toString().take(24)
        val m = Matcher.findMatch(typed, keywords)
        if (m == null) {
            if (typed.length >= 3) DetectionLog.add(pkg, src, snippet, "no-match")
            return null
        }
        // Arm the submit window even when the keystroke itself is cooling down:
        // the user is still typing this name, so a coming results page counts.
        lastKeystrokeMatchAt["${m.personName}|$pkg"] = System.currentTimeMillis()
        DetectionLog.add(pkg, src, snippet, "match:${m.personName}")
        maybeTrigger(pkg, m, trigger = "SEARCH_INPUT", confidence = "HIGH")
        return m
    }

    private fun processTitle(pkg: String) {
        try {
            val root = rootInActiveWindow
            val detector = DetectorRegistry.forPackage(pkg)
            val candidates = try {
                detector.titleCandidatesFromRoot(root)
            } catch (_: Exception) {
                emptyList()
            }
            for (title in candidates) {
                val m = Matcher.findMatch(title, keywords) ?: continue
                val now = System.currentTimeMillis()
                val key = "${m.personName}|$pkg"
                if (now - (lastKeystrokeMatchAt[key] ?: 0L) <= SUBMIT_WINDOW_MS) {
                    // User pressed search / opened results right after typing
                    // the name: roast again even inside the 30s cooldown.
                    if (now - (lastSubmitAt[key] ?: 0L) < SUBMIT_COOLDOWN_MS) return
                    lastSubmitAt[key] = now
                    fireTrigger(pkg, m, trigger = "SEARCH_SUBMITTED", confidence = "HIGH")
                } else {
                    maybeTrigger(pkg, m, trigger = "PROFILE_TITLE", confidence = "MEDIUM")
                }
                return
            }
        } catch (_: Exception) { }
    }

    private fun maybeTrigger(pkg: String, m: Matcher.Match, trigger: String, confidence: String) {
        val now = System.currentTimeMillis()
        val key = "${m.personName}|$pkg"
        if (now - (lastTriggerAt[key] ?: 0L) < KEYSTROKE_COOLDOWN_MS) {
            DetectionLog.add(pkg, trigger, m.personName.take(24), "cooldown")
            return // 30s cooldown per person+app
        }
        lastTriggerAt[key] = now
        fireTrigger(pkg, m, trigger, confidence)
    }

    private fun fireTrigger(pkg: String, m: Matcher.Match, trigger: String, confidence: String) {
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
        /** Spam guard for repeat keystroke/title matches, per person+app. */
        const val KEYSTROKE_COOLDOWN_MS = 30_000L
        /** A new window this soon after typing a matched name = user pressed search. */
        const val SUBMIT_WINDOW_MS = 5_000L
        /** Spam guard for submit triggers, per person+app. */
        const val SUBMIT_COOLDOWN_MS = 5_000L
        /** Min gap between focused-node reads (content-changed fires constantly). */
        const val FOCUS_READ_THROTTLE_MS = 400L
        /** Min gap between "event seen but no text" debug lines. */
        const val RAW_LOG_THROTTLE_MS = 500L

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
