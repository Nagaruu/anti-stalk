package com.antistalk.core

import java.text.Normalizer

// Common Vietnamese surnames and middle names that must NOT be standalone keywords
private val GENERIC_VIETNAMESE_WORDS = setOf(
    // Common Surnames
    "nguyen", "tran", "le", "pham", "hoang", "huynh", "phan", "vu", "vo",
    "dang", "bui", "do", "ho", "ngo", "duong", "ly", "dinh", "doan",
    "lam", "mai", "dao", "cao", "ha", "luu", "luong", "thai", "chau",
    "ta", "phung", "to", "vuong", "truong",
    // Common Middle names / ambiguous short words
    "van", "thi", "duc", "ngoc", "huu", "quang", "minh", "anh", "thanh",
    "xuan", "kim", "bao", "hai", "phuong", "tuan", "thao"
)

/**
 * Lowercase + strip Vietnamese diacritics so "Nguyễn Văn A" matches "nguyen van a".
 * Explicitly replaces 'đ' / 'Đ' because NFD does not decompose precomposed stroke characters.
 */
fun normalizeText(raw: String): String {
    val trimmed = raw.trim().lowercase()
    val nfd = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
    return nfd.replace("\\p{Mn}+".toRegex(), "")
        .replace('đ', 'd')
        .replace('Đ', 'd')
}

/**
 * Smartly suggests keywords from a display name:
 * - Full name (e.g. "nguyen van duc")
 * - Compact handle (e.g. "nguyenvanduc" for usernames/tags)
 * - Last 2 words if >= 3 words (e.g. "van duc")
 * - Excludes generic standalone surnames/middle names (e.g. "nguyen", "van") to prevent false positives.
 */
fun suggestKeywords(displayName: String): List<String> {
    val norm = normalizeText(displayName)
    if (norm.isBlank()) return emptyList()

    val words = norm.split("\\s+".toRegex()).filter { it.isNotBlank() }
    val out = mutableListOf<String>()

    // 1. Full name
    out.add(norm)

    // 2. Continuous username / handle (no spaces)
    if (words.size > 1) {
        out.add(norm.replace(" ", ""))
    }

    // 3. Last 2 words (e.g. "Văn Đức" from "Nguyễn Văn Đức")
    if (words.size >= 3) {
        val lastTwo = "${words[words.size - 2]} ${words.last()}"
        out.add(lastTwo)
    }

    // 4. Single given name if not a generic surname/middle name and len >= 3
    val givenName = words.lastOrNull()
    if (givenName != null && givenName.length >= 3 && !GENERIC_VIETNAMESE_WORDS.contains(givenName)) {
        out.add(givenName)
    }

    return out.distinct()
}

