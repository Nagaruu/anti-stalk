package com.antistalk.core

import java.text.Normalizer

/** Lowercase + strip Vietnamese diacritics so "Nguyễn Văn A" matches "nguyen van a". */
fun normalizeText(raw: String): String {
    val nfd = Normalizer.normalize(raw.trim().lowercase(), Normalizer.Form.NFD)
    return nfd.replace("\\p{Mn}+".toRegex(), "")
}

/** Auto-suggest keywords from a display name: full name + each word (len>=2). */
fun suggestKeywords(displayName: String): List<String> {
    val norm = normalizeText(displayName)
    val parts = norm.split("\\s+".toRegex()).filter { it.length >= 2 }
    return (listOf(norm) + parts).distinct()
}
