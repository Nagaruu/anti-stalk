package com.antistalk.detection

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Detector per app. MVP uses one GenericDetector for all 4 apps because
 * FB/IG/Zalo/Messenger have unstable resource-ids that change per release.
 * Add app-specific subclasses later WITHOUT touching the service.
 *
 * Two signals only:
 *  1. SEARCH_INPUT (HIGH confidence): text the user is typing in a search field.
 *  2. TITLE (MEDIUM confidence): chat/profile header title equals a keyword.
 */
interface AppDetector {
    val packageName: String
    fun searchTextFromEvent(event: AccessibilityEvent): CharSequence?
    fun titleFromRoot(root: AccessibilityNodeInfo?): CharSequence?
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

    override fun titleFromRoot(root: AccessibilityNodeInfo?): CharSequence? {
        if (root == null) return null
        // Best-effort: walk first ~40 nodes, prefer short title-like texts.
        // We deliberately do NOT dump message bodies — titles only.
        var seen = 0
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty() && seen < 40) {
            val n = queue.removeFirst()
            seen++
            val text = n.text?.toString()?.trim()
            if (!text.isNullOrEmpty() && text.length in 3..40 && !text.contains("\n")) {
                // Caller decides whether it matches a keyword.
                // Return candidate; service validates exact/contains match.
                if (n.isClickable || n.className?.contains("TextView") == true) {
                    // keep first candidate only if it matches — checked by service
                    queue.addAll(childrenOf(n))
                    // stash candidate via tag: simplest is to return first text and let service match
                    // To keep code small we return the first plausible title text.
                    recycleChildren(n, queue)
                    return text
                }
            }
            queue.addAll(childrenOf(n))
        }
        return null
    }

    private fun childrenOf(n: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val out = mutableListOf<AccessibilityNodeInfo>()
        for (i in 0 until n.childCount) {
            try { n.getChild(i)?.let { out.add(it) } } catch (_: Exception) { }
        }
        return out
    }

    private fun recycleChildren(n: AccessibilityNodeInfo, queue: ArrayDeque<AccessibilityNodeInfo>) {
        // no-op: nodes are recycled by framework; kept for clarity
    }
}

object DetectorRegistry {
    fun forPackage(pkg: String): AppDetector = GenericDetector(pkg)
}
