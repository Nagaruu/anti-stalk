package com.antistalk.detection

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Detector per app. MVP uses one GenericDetector for all 4 apps because
 * FB/IG/Zalo/Messenger have unstable resource-ids that change per release.
 * Add app-specific subclasses later WITHOUT touching the service.
 *
 * Three signals:
 *  1. SEARCH_INPUT (HIGH confidence): text the user is typing in a search field.
 *  2. SEARCH_SUBMITTED (HIGH confidence): a new window opens shortly after
 *     typing a matched name (user pressed search / opened results).
 *  3. TITLE (MEDIUM confidence): chat/profile header title equals a keyword.
 */
interface AppDetector {
    val packageName: String
    fun searchTextFromEvent(event: AccessibilityEvent): CharSequence?
    /**
     * All plausible title-like texts on screen (short, single-line).
     * The service matches each against keywords — never return just
     * the first text, or a profile name further down the tree is missed.
     */
    fun titleCandidatesFromRoot(root: AccessibilityNodeInfo?): List<String>
}

class GenericDetector(override val packageName: String) : AppDetector {

    override fun searchTextFromEvent(event: AccessibilityEvent): CharSequence? {
        // TYPE_VIEW_TEXT_CHANGED carries the typed text for EditText search boxes.
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val t = event.text?.firstOrNull()
            if (!t.isNullOrBlank()) return t
        }
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            val t = event.text?.firstOrNull()
            if (!t.isNullOrBlank()) return t
        }
        return null
    }

    override fun titleCandidatesFromRoot(root: AccessibilityNodeInfo?): List<String> {
        if (root == null) return emptyList()
        // Best-effort: walk first ~60 nodes, collect short title-like texts.
        // We deliberately do NOT dump message bodies — titles only.
        val out = mutableListOf<String>()
        var seen = 0
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty() && seen < 60 && out.size < 20) {
            val n = queue.removeFirst()
            seen++
            val text = n.text?.toString()?.trim()
            if (!text.isNullOrEmpty() && text.length in 3..40 && !text.contains("\n")) {
                if (n.isClickable || n.className?.contains("TextView") == true) {
                    if (out.none { it.equals(text, ignoreCase = true) }) out.add(text)
                }
            }
            queue.addAll(childrenOf(n))
        }
        return out
    }

    private fun childrenOf(n: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val out = mutableListOf<AccessibilityNodeInfo>()
        for (i in 0 until n.childCount) {
            try { n.getChild(i)?.let { out.add(it) } } catch (_: Exception) { }
        }
        return out
    }
}

object DetectorRegistry {
    fun forPackage(pkg: String): AppDetector = GenericDetector(pkg)
}
