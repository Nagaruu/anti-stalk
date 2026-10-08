package com.antistalk.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextNormalizeTest {

    @Test
    fun `strips diacritics, lowercases, keeps word spacing`() {
        assertEquals("nguyen van a", normalizeText("Nguyễn Văn A"))
        assertEquals("dang duc", normalizeText("Đặng  ĐỨC"))
    }

    @Test
    fun `replaces stroked d that NFD cannot decompose`() {
        assertEquals("d", normalizeText("Đ"))
        assertEquals("duc", normalizeText("Đức"))
    }

    @Test
    fun `collapses repeated whitespace so stored keywords can match real input`() {
        assertEquals("nguyen van a", normalizeText("  Nguyễn   Văn \t A  "))
        // Regression: a double-spaced display name used to store
        // "nguyen  van a", which never matches anything typed normally.
        assertEquals(
            normalizeText("Nguyễn Văn A"),
            normalizeText("Nguyễn  Văn A")
        )
    }

    @Test
    fun `suggests full name, compact handle and last two words`() {
        assertEquals(
            listOf("nguyen van duc", "nguyenvanduc", "van duc"),
            suggestKeywords("Nguyễn Văn Đức")
        )
        assertEquals(
            listOf("bao tram", "baotram", "tram"),
            suggestKeywords("Bảo Trâm")
        )
    }

    @Test
    fun `generic single-word names produce no keyword at all`() {
        // "anh"/"nguyen" alone would match almost every sentence.
        assertTrue(suggestKeywords("Anh").isEmpty())
        assertTrue(suggestKeywords("Minh").isEmpty())
        assertTrue(suggestKeywords("Nguyễn").isEmpty())
        assertTrue(suggestKeywords("   ").isEmpty())
    }

    @Test
    fun `generic given name is still excluded inside a full name`() {
        assertEquals(
            listOf("le hoang minh", "lehoangminh", "hoang minh"),
            suggestKeywords("Lê Hoàng Minh")
        )
        assertEquals(listOf("minh anh", "minhanh"), suggestKeywords("Minh Anh"))
    }

    @Test
    fun `double spaced display name yields identical suggestions`() {
        assertEquals(suggestKeywords("Nguyễn Văn A"), suggestKeywords("Nguyễn  Văn A"))
    }
}
