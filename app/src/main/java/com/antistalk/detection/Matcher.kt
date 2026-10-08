package com.antistalk.detection

import com.antistalk.core.normalizeText

/**
 * Pure matching logic — no Android dependencies, easy to unit test.
 * Only minimal text is inspected: search input or screen title.
 */
object Matcher {
    data class Match(val personId: Long?, val personName: String, val keyword: String)

    /**
     * Checks if [text] contains [kw] respecting word/token boundaries.
     * Prevents false positives where "van" matches "vantage" or "advanced",
     * or "duc" matches "giaoduc".
     */
    fun containsKeyword(text: String, kw: String): Boolean {
        if (kw.isEmpty() || text.length < kw.length) return false
        var start = 0
        while (true) {
            val idx = text.indexOf(kw, start)
            if (idx == -1) return false
            val end = idx + kw.length
            val validStart = (idx == 0) || !text[idx - 1].isLetterOrDigit()
            val validEnd = (end == text.length) || !text[end].isLetterOrDigit()
            if (validStart && validEnd) return true
            start = idx + 1
        }
    }

    /**
     * Returns a match if [text] contains any keyword (normalized) with word boundaries.
     * Requires keyword length >= 3 to avoid noise from single chars.
     */
    fun findMatch(text: CharSequence?, keywords: List<Triple<Long, String, String>>): Match? {
        if (text.isNullOrBlank()) return null
        val norm = normalizeText(text.toString())
        if (norm.length < 3) return null
        for ((pid, pname, kw) in keywords) {
            if (kw.length >= 3 && containsKeyword(norm, kw)) return Match(pid, pname, kw)
        }
        return null
    }
}

