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

    /**
     * Finds active text inside an editable search field or input on screen.
     * Useful when re-opening an app or submitting search where text is already present.
     */
    fun findActiveSearchText(root: AccessibilityNodeInfo?): String?

    /**
     * True when the screen shows an EMPTY editable search box (search page with
     * no query). Used to tell "history list visible" apart from a real
     * profile/chat page: history rows must stay quiet, tapping one still fires
     * via the click event path (SEARCH_INPUT HIGH).
     */
    fun hasEmptySearchBox(root: AccessibilityNodeInfo?): Boolean
}

class GenericDetector(override val packageName: String) : AppDetector {

    override fun searchTextFromEvent(event: AccessibilityEvent): CharSequence? {
        // 1. Check event.text list (join if multiple tokens)
        val eventTexts = event.text
        if (!eventTexts.isNullOrEmpty()) {
            val joined = eventTexts.filter { !it.isNullOrBlank() }.joinToString(" ").trim()
            if (joined.isNotBlank()) return joined
        }
        // 2. Check event.source node directly (crucial for Zalo, custom EditTexts & IME callbacks)
        try {
            val src = event.source
            if (src != null) {
                val srcText = src.text?.toString()?.trim()
                if (!srcText.isNullOrBlank()) return srcText
                val desc = src.contentDescription?.toString()?.trim()
                if (!desc.isNullOrBlank()) return desc
            }
        } catch (_: Exception) { }
        // 3. Fallback: event contentDescription
        val directDesc = event.contentDescription?.toString()?.trim()
        if (!directDesc.isNullOrBlank()) return directDesc
        return null
    }

    override fun titleCandidatesFromRoot(root: AccessibilityNodeInfo?): List<String> {
        if (root == null) return emptyList()
        val out = mutableListOf<String>()
        var seen = 0
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty() && seen < 250 && out.size < 30) {
            val n = queue.removeFirst()
            seen++
            val text = n.text?.toString()?.trim() ?: n.contentDescription?.toString()?.trim()
            if (!text.isNullOrEmpty() && text.length in 3..50 && !text.contains("\n")) {
                if (n.isClickable || n.className?.contains("TextView") == true) {
                    if (out.none { it.equals(text, ignoreCase = true) }) out.add(text)
                }
            }
            queue.addAll(childrenOf(n))
        }
        return out
    }

    override fun findActiveSearchText(root: AccessibilityNodeInfo?): String? {
        if (root == null) return null
        var seen = 0
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty() && seen < 120) {
            val n = queue.removeFirst()
            seen++
            val isEdit = try { n.isEditable || n.className?.contains("EditText") == true } catch (_: Exception) { false }
            if (isEdit) {
                val t = n.text?.toString()?.trim()
                if (!t.isNullOrBlank() && t.length >= 3) return t
            }
            queue.addAll(childrenOf(n))
        }
        return null
    }

    override fun hasEmptySearchBox(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        var seen = 0
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty() && seen < 120) {
            val n = queue.removeFirst()
            seen++
            val isEdit = try { n.isEditable || n.className?.contains("EditText") == true } catch (_: Exception) { false }
            if (isEdit) {
                // Empty text (hint-only counts as empty): user hasn't typed anything.
                if (n.text?.toString()?.trim().isNullOrEmpty()) return true
            }
            queue.addAll(childrenOf(n))
        }
        return false
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
