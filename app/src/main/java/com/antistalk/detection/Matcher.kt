package com.antistalk.detection

import com.antistalk.core.normalizeText

/**
 * Pure matching logic — no Android dependencies, easy to unit test.
 * Only minimal text is inspected: search input or screen title.
 */
object Matcher {
    data class Match(val personId: Long?, val personName: String, val keyword: String)

    /**
     * Returns a match if [text] contains any keyword (normalized).
     * Requires keyword length >= 3 to avoid noise from single chars.
     */
    fun findMatch(text: CharSequence?, keywords: List<Triple<Long, String, String>>): Match? {
        if (text.isNullOrBlank()) return null
        val norm = normalizeText(text.toString())
        if (norm.length < 3) return null
        for ((pid, pname, kw) in keywords) {
            if (kw.length >= 3 && norm.contains(kw)) return Match(pid, pname, kw)
        }
        return null
    }
}
