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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

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
    // Written on Dispatchers.IO (refreshCacheNow), read on the main thread in
    // onAccessibilityEvent: @Volatile + immutable snapshots keep both sides safe.
    @Volatile
    private var keywords: List<Triple<Long, String, String>> = emptyList() // pid, pname, normalizedKw
    @Volatile
    private var enabledPkgs: Set<String> = emptySet()
    @Volatile
    private var suppressUntil: Long = 0L
    @Volatile
    private var lastLeftPerson: String = ""
    @Volatile
    private var lastLeftAt: Long = 0L
    private val suppressedPersonsUntil: MutableMap<String, Long> = ConcurrentHashMap()
    // Trigger maps are mutated from the main thread (processTyped via
    // onAccessibilityEvent) AND from IO coroutines (readFocusedText/processTitle),
    // so they must be concurrent — plain mutableMapOf risks CME/lost writes.
    private val lastTriggerAt: MutableMap<String, Long> = ConcurrentHashMap()
    private val lastProfileTriggerAt: MutableMap<String, Long> = ConcurrentHashMap()
    private val lastKeystrokeMatchAt: MutableMap<String, Long> = ConcurrentHashMap()
    private val lastSubmitAt: MutableMap<String, Long> = ConcurrentHashMap()
    // Normalized query text that fired the popup, per person+app. Used to tell
    // resume-replay (app redraws the same text when returning to foreground)
    // apart from genuinely new typing after "Đi ra" — pure passive filtering.
    private val lastFiredQuery: MutableMap<String, String> = ConcurrentHashMap()
    private var lastFocusReadAt: Long = 0L
    private var lastTitleCheckAt: Long = 0L
    private var lastRawLogAt: Long = 0L

    override fun onServiceConnected() {
        instance = this
        repo = AntiStalkRepository(this)
        refreshCache()

        // Continuously react to database changes: adding/deleting persons,
        // updating keywords, or toggling monitored apps immediately updates cache!
        scope.launch {
            try {
                combine(repo.persons, repo.keywords, repo.apps) { _, _, _ -> }
                    .collect {
                        refreshCacheNow()
                    }
            } catch (_: Exception) { }
        }
    }

    fun refreshCache() {
        scope.launch { refreshCacheNow() }
    }

    private suspend fun refreshCacheNow() {
        try {
            repo.seedIfNeeded()
            val allAppsCount = repo.appCountOnce()
            val loadedPkgs = repo.enabledPackagesOnce()
            enabledPkgs = if (loadedPkgs.isEmpty() && allAppsCount == 0) {
                MonitoredPackages.DEFAULTS.filter { it.defaultEnabled }.map { it.packageName }.toSet()
            } else {
                loadedPkgs
            }
            val persons = repo.personsOnce().associateBy { it.id }
            keywords = repo.keywordsOnce().mapNotNull { kw ->
                val p = persons[kw.personId] ?: return@mapNotNull null
                val pWords = com.antistalk.core.normalizeText(p.displayName).split("\\s+".toRegex()).filter { it.isNotBlank() }
                // Safety guard: for multi-word names, never load dangerous single sub-words (<= 4 chars or in name)
                if (pWords.size > 1 && !kw.normalized.contains(" ") && (kw.normalized.length <= 4 || pWords.contains(kw.normalized))) {
                    return@mapNotNull null
                }
                Triple(kw.personId, p.displayName, kw.normalized)
            }
        } catch (_: Exception) { }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (OverlayManager.isShowing()) return
        if (System.currentTimeMillis() < suppressUntil) return
        val pkg = event.packageName?.toString() ?: return
        if (!MonitoredPackages.isMonitored(pkg)) return
        if (pkg !in enabledPkgs) return

        // Snapshot synchronously: the framework may recycle the event
        // after this callback returns, so never touch it from a coroutine.
        val detector = DetectorRegistry.forPackage(pkg)
        val type = event.eventType
        val typed: CharSequence? = try { detector.searchTextFromEvent(event) } catch (_: Exception) { null }
        val isWindowChange = type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        val isContentChange = type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        val typeName = try { AccessibilityEvent.eventTypeToString(type) } catch (_: Exception) { type.toString() }

        // DEBUG: prove events arrive at all — including ones carrying no text.
        if (System.currentTimeMillis() - lastRawLogAt > RAW_LOG_THROTTLE_MS &&
            (typed == null || typed.length < 3)
        ) {
            lastRawLogAt = System.currentTimeMillis()
            DetectionLog.add(pkg, typeName, typed?.toString()?.take(24) ?: "—", "seen-no-text")
        }

        if (keywords.isEmpty()) {
            scope.launch {
                refreshCacheNow()
                if (typed != null) processTyped(pkg, typed, "event")
                else readFocusedText(pkg)
                if (isWindowChange || isContentChange) processTitle(pkg)
            }
            return
        }

        // Signal 1 — search input from event (HIGH confidence)
        if (typed != null) {
            processTyped(pkg, typed, "event")
        } else if (type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
            type == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            type == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED ||
            type == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            isContentChange
        ) {
            val now = System.currentTimeMillis()
            if (now - lastFocusReadAt > FOCUS_READ_THROTTLE_MS) {
                lastFocusReadAt = now
                scope.launch { readFocusedText(pkg) }
            }
        }

        // Signal 2+3 — window change or UI navigation update:
        // scans active search text (HIGH) or title candidates (profile/chat header).
        if (isWindowChange || isContentChange) {
            val now = System.currentTimeMillis()
            if (isWindowChange || (now - lastTitleCheckAt > TITLE_CHECK_THROTTLE_MS)) {
                lastTitleCheckAt = now
                scope.launch { processTitle(pkg) }
            }
        }
    }

    /** Read the focused input node's text directly (event.text may be empty). */
    private fun readFocusedText(pkg: String) {
        try {
            val root = rootInActiveWindow ?: return
            val focused = try {
                root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            } catch (_: Exception) { null }
            val text = focused?.text?.toString() ?: focused?.contentDescription?.toString()
            if (!text.isNullOrBlank() && text.length >= 3) {
                processTyped(pkg, text, "focused")
                return
            }
            // Many apps drop input focus while keyboard is open — scan every editable node instead.
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
        while (queue.isNotEmpty() && seen < 150) {
            val n = queue.removeFirst()
            seen++
            val editable = try { n.isEditable } catch (_: Exception) { false }
            if (editable || n.className?.contains("EditText") == true) {
                val t = n.text?.toString() ?: n.contentDescription?.toString()
                if (best == null && !t.isNullOrBlank() && t.length >= 3) best = t
            }
            for (i in 0 until n.childCount) {
                try { n.getChild(i)?.let { queue.add(it) } } catch (_: Exception) { }
            }
        }
        return best
    }

    private fun processTyped(pkg: String, typed: CharSequence, src: String): Matcher.Match? {
        if (OverlayManager.isShowing() || System.currentTimeMillis() < suppressUntil) return null
        val snippet = typed.toString().take(24)
        val m = Matcher.findMatch(typed, keywords)
        if (m == null) {
            if (typed.length >= 3) DetectionLog.add(pkg, src, snippet, "no-match")
            return null
        }
        val now = System.currentTimeMillis()
        val key = "${m.personName}|$pkg"
        if (isInLeaveQuiet(instance, m.personName, now)) {
            // Stale rescan (re-opened app still shows the old query): stay quiet.
            if (src == "active-search") {
                DetectionLog.add(pkg, src, snippet, "leave-quiet:${m.personName}")
                return null
            }
            // Resume-replay (Zalo redraws chat list / restores fields on return):
            // same text as the query that fired the popup -> quiet. A DIFFERENT
            // query still fires the repeat roast below. Fresh typing (event /
            // focused / editable) of new text always bypasses.
            val norm = try {
                com.antistalk.core.normalizeText(typed.toString())
            } catch (_: Exception) {
                typed.toString()
            }
            val fired = lastFiredQuery[key] ?: lastFiredQuery["${m.personName}|*"]
            if (fired != null && fired.isNotBlank() &&
                (norm == fired || norm.contains(fired) || fired.contains(norm))
            ) {
                DetectionLog.add(pkg, src, snippet, "resume-quiet:${m.personName}")
                return null
            }
        }
        if (now < (suppressedPersonsUntil[key] ?: 0L) || now < (suppressedPersonsUntil["${m.personName}|*"] ?: 0L)) {
            DetectionLog.add(pkg, src, snippet, "suppressed:${m.personName}")
            return null
        }
        // Arm the submit window even when the keystroke itself is cooling down:
        // the user is still typing this name, so a coming results page counts.
        lastKeystrokeMatchAt[key] = now
        DetectionLog.add(pkg, src, snippet, "match:${m.personName}")
        maybeTrigger(pkg, m, trigger = "SEARCH_INPUT", confidence = "HIGH", query = typed.toString())
        return m
    }

    private fun processTitle(pkg: String) {
        if (OverlayManager.isShowing() || System.currentTimeMillis() < suppressUntil) return
        try {
            val root = rootInActiveWindow ?: return
            val detector = DetectorRegistry.forPackage(pkg)

            // 1. Check if there is an active search bar with text on screen.
            // When re-opening the app or submitting, this catches the query instantly.
            val activeSearch = detector.findActiveSearchText(root)
            if (!activeSearch.isNullOrBlank()) {
                val sm = Matcher.findMatch(activeSearch, keywords)
                if (sm != null) {
                    processTyped(pkg, activeSearch, "active-search")
                    return
                } else {
                    // Search bar has an unrelated query (e.g. "tool") -> prevent background history false match
                    return
                }
            }

            // 2. Scan title candidates (profile header, chat header).
            // Search-history page (empty search box, history rows visible) stays
            // quiet: tapping a history row still fires via the click event path
            // (SEARCH_INPUT HIGH), but merely seeing it must not popup.
            val onSearchPage = try {
                detector.hasEmptySearchBox(root)
            } catch (_: Exception) {
                false
            }
            val candidates = try {
                detector.titleCandidatesFromRoot(root)
            } catch (_: Exception) {
                emptyList()
            }
            for (title in candidates) {
                val m = Matcher.findMatch(title, keywords) ?: continue
                val now = System.currentTimeMillis()
                // Same profile/title still on screen after "Đi ra" -> stay quiet.
                // Only a fresh keystroke inside SUBMIT_WINDOW_MS may promote to SEARCH_SUBMITTED.
                if (isInLeaveQuiet(instance, m.personName, now)) {
                    val key = "${m.personName}|$pkg"
                    if (now - (lastKeystrokeMatchAt[key] ?: 0L) > SUBMIT_WINDOW_MS) {
                        DetectionLog.add(pkg, "title", title.take(24), "leave-quiet:${m.personName}")
                        return
                    }
                }
                val key = "${m.personName}|$pkg"
                if (now < (suppressedPersonsUntil[key] ?: 0L) || now < (suppressedPersonsUntil["${m.personName}|*"] ?: 0L)) continue
                if (now - (lastKeystrokeMatchAt[key] ?: 0L) <= SUBMIT_WINDOW_MS) {
                    if (now - (lastSubmitAt[key] ?: 0L) < SUBMIT_COOLDOWN_MS) return
                    lastSubmitAt[key] = now
                    fireTrigger(pkg, m, trigger = "SEARCH_SUBMITTED", confidence = "HIGH", query = title)
                } else {
                    if (onSearchPage) {
                        DetectionLog.add(pkg, "title", title.take(24), "history-quiet:${m.personName}")
                        return
                    }
                    if (now - (lastProfileTriggerAt[key] ?: 0L) < PROFILE_COOLDOWN_MS) continue
                    lastProfileTriggerAt[key] = now
                    maybeTrigger(pkg, m, trigger = "PROFILE_TITLE", confidence = "MEDIUM", query = title)
                }
                return
            }
        } catch (_: Exception) { }
    }

    private fun maybeTrigger(pkg: String, m: Matcher.Match, trigger: String, confidence: String, query: String = "") {
        if (OverlayManager.isShowing() || System.currentTimeMillis() < suppressUntil) return
        val now = System.currentTimeMillis()
        val key = "${m.personName}|$pkg"
        if (now < (suppressedPersonsUntil[key] ?: 0L) || now < (suppressedPersonsUntil["${m.personName}|*"] ?: 0L)) {
            DetectionLog.add(pkg, trigger, m.personName.take(24), "suppressed")
            return
        }
        if (now - (lastTriggerAt[key] ?: 0L) < KEYSTROKE_COOLDOWN_MS) {
            DetectionLog.add(pkg, trigger, m.personName.take(24), "cooldown")
            return
        }
        lastTriggerAt[key] = now
        fireTrigger(pkg, m, trigger, confidence, query)
    }

    private fun fireTrigger(pkg: String, m: Matcher.Match, trigger: String, confidence: String, query: String = "") {
        if (OverlayManager.isShowing() || System.currentTimeMillis() < suppressUntil) return
        val now = System.currentTimeMillis()
        // Remember what fired so resume-replay of the same text stays quiet.
        if (query.isNotBlank()) {
            try {
                lastFiredQuery["${m.personName}|$pkg"] =
                    com.antistalk.core.normalizeText(query)
            } catch (_: Exception) { }
        }
        // Last defense: MEDIUM profile-title rescan must never consume the repeat
        // slot nor popup during leave-quiet — only HIGH (fresh typing/submit) may.
        if (confidence != "HIGH" && isInLeaveQuiet(instance, m.personName, now)) {
            DetectionLog.add(pkg, trigger, m.personName.take(24), "leave-quiet")
            return
        }
        val isRepeat = (now - lastLeftAt < 180_000L) && lastLeftPerson.equals(m.personName, ignoreCase = true)
        // Consume the repeat slot only on a fresh HIGH search; a stale MEDIUM
        // title must leave lastLeftAt intact for the next real typing.
        if (isRepeat && confidence == "HIGH") {
            lastLeftAt = 0L
        }
        scope.launch {
            try {
                val eventId = repo.logEvent(m.personName, m.personId, pkg, trigger, confidence)
                val countToday = repo.countTodayForPerson(m.personName)
                OverlayManager.show(
                    this@StalkAccessibilityService, eventId,
                    m.personName, pkg, trigger, confidence, countToday, isRepeat
                )
            } catch (_: Exception) { }
        }
    }

    override fun onInterrupt() { }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        OverlayManager.hide()
        return super.onUnbind(intent)
    }

    companion object {
        @Volatile
        private var instance: StalkAccessibilityService? = null

        /** Explicitly request the running service to re-read persons/keywords/apps immediately. */
        fun invalidateCache() {
            instance?.refreshCache()
        }

        /**
         * Disarms keystroke matches and handles suppression after overlay decision.
         * Play-safe + anti-double-popup: HOME only, no BACK automation.
         * - isLeave=true ("THÔI, TÔI ĐI RA"): 2s transition suppress for the
         *   Home animation + 60s LEAVE_QUIET for stale UI (same search text /
         *   same profile title still on screen when the user comes back).
         *   Stale rescan via processTitle/active-search is ignored during quiet,
         *   but a FRESH keystroke (processTyped src=event/focused/editable)
         *   still fires immediately with a repeat-attempt roast.
         * - isLeave=false ("Tôi vẫn muốn xem"): 60s peace for this session.
         */
        fun onUserDismiss(personName: String, pkg: String = "", isLeave: Boolean = true) {
            val svc = instance ?: return
            val now = System.currentTimeMillis()
            svc.suppressUntil = now + TRANSITION_SUPPRESS_MS
            if (personName.isNotBlank()) {
                val prefix = personName.trim()
                svc.lastKeystrokeMatchAt.keys.removeAll { it.startsWith(prefix, ignoreCase = true) }
                if (isLeave) {
                    svc.lastLeftPerson = personName
                    svc.lastLeftAt = now
                    // Do NOT wipe lastTriggerAt/lastSubmitAt/lastProfileTriggerAt here:
                    // wiping is what caused the 2nd popup on the unchanged screen.
                    // Cooldowns are kept so a stale rescan stays quiet; fresh typing
                    // bypasses via the submit-window / repeat path in processTyped.
                    svc.suppressedPersonsUntil.keys.removeAll { it.startsWith(prefix, ignoreCase = true) }
                } else {
                    // User chose to stay: give 60s peace period for this viewing session
                    val key = if (pkg.isNotBlank()) "$personName|$pkg" else personName
                    svc.suppressedPersonsUntil[key] = now + 60_000L
                    svc.suppressedPersonsUntil["$personName|*"] = now + 60_000L
                }
            }
        }

        /** True when [personName] left via "Đi ra" recently: stale UI must stay quiet. */
        fun isInLeaveQuiet(svc: StalkAccessibilityService?, personName: String, now: Long): Boolean {
            if (svc == null || personName.isBlank()) return false
            return now - svc.lastLeftAt < LEAVE_QUIET_MS &&
                svc.lastLeftPerson.equals(personName.trim(), ignoreCase = true)
        }

        /** Spam guard for repeat keystroke matches, per person+app: 2.5s is plenty while typing. */
        const val KEYSTROKE_COOLDOWN_MS = 2_500L
        /** Home-transition suppress after "Đi ra" (covers Home animation + FB resume). */
        const val TRANSITION_SUPPRESS_MS = 2_000L
        /** Stale-UI quiet after "Đi ra": same search/title rescan is ignored this long. */
        const val LEAVE_QUIET_MS = 60_000L
        /** Cooldown between profile title inspections: 15s so reading a profile doesn't spam. */
        const val PROFILE_COOLDOWN_MS = 15_000L
        /** A new window this soon after typing a matched name = user pressed search. */
        const val SUBMIT_WINDOW_MS = 5_000L
        /** Spam guard for submit triggers, per person+app. */
        const val SUBMIT_COOLDOWN_MS = 5_000L
        /** Min gap between focused-node reads (content-changed fires constantly). */
        const val FOCUS_READ_THROTTLE_MS = 300L
        /** Min gap between UI title/search checks during navigation/content updates. */
        const val TITLE_CHECK_THROTTLE_MS = 600L
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
