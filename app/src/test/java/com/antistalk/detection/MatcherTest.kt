package com.antistalk.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatcherTest {

    private val kw = Triple(1L, "Nguyễn Văn A", "nguyen van a")

    @Test
    fun `matches full name with word boundaries`() {
        assertNotNull(Matcher.findMatch("đang tìm Nguyễn Văn A nè", listOf(kw)))
        assertNotNull(Matcher.findMatch("Nguyễn Văn A", listOf(kw)))
        assertNotNull(Matcher.findMatch("  NGUYỄN   VĂN   a  ", listOf(kw)))
    }

    @Test
    fun `does not match partial tokens`() {
        assertNull(Matcher.findMatch("nguyen van", listOf(kw)))
        assertNull(Matcher.findMatch("guyenvana", listOf(kw)))
        assertNull(Matcher.findMatch("ng", listOf(kw))) // < 3 chars
        assertNull(Matcher.findMatch("", listOf(kw)))
        assertNull(Matcher.findMatch(null, listOf(kw)))
        assertNull(Matcher.findMatch("   ", listOf(kw)))
    }

    @Test
    fun `short keywords are ignored even if present in the list`() {
        val short = listOf(Triple(1L, "X", "ab"))
        assertNull(Matcher.findMatch("ab cd", short))
    }

    @Test
    fun `boundary check rejects substrings`() {
        assertFalse(Matcher.containsKeyword("vantage plan", "van"))
        assertFalse(Matcher.containsKeyword("giaoduc", "duc"))
        assertFalse(Matcher.containsKeyword("advanced", "duc"))
        assertFalse(Matcher.containsKeyword("anha", "anh"))
    }

    @Test
    fun `boundary check accepts standalone tokens`() {
        assertTrue(Matcher.containsKeyword("tim anh ay", "anh"))
        assertTrue(Matcher.containsKeyword("(van)", "van"))
        assertTrue(Matcher.containsKeyword("nguyen,van", "van"))
    }

    @Test
    fun `empty keyword never matches`() {
        assertFalse(Matcher.containsKeyword("anything", ""))
        assertNull(Matcher.findMatch("anything", listOf(Triple(1L, "X", ""))))
    }

    @Test
    fun `returns the first matching person`() {
        val kws = listOf(
            kw,
            Triple(2L, "Trần Thị B", "tran thi b")
        )
        val m = Matcher.findMatch("gõ tên tran thi b", kws)
        assertNotNull(m)
        assertEquals(2L, m!!.personId)
        assertEquals("Trần Thị B", m.personName)
        assertEquals("tran thi b", m.keyword)
    }

    @Test
    fun `searching tool does not match Kang Too Jee`() {
        val kangKws = com.antistalk.core.suggestKeywords("Kang Too Jee").map {
            Triple(99L, "Kang Too Jee", it)
        }
        // "tool", "tool kit", "devtools" must NOT match
        assertNull(Matcher.findMatch("tool", kangKws))
        assertNull(Matcher.findMatch("tool kit", kangKws))
        assertNull(Matcher.findMatch("devtools", kangKws))
        assertNull(Matcher.findMatch("youtube", kangKws))

        // Legitimate matches for Kang Too Jee must still work
        assertNotNull(Matcher.findMatch("Kang Too Jee", kangKws))
        assertNotNull(Matcher.findMatch("tim too jee", kangKws))
        assertNotNull(Matcher.findMatch("kangtoojee", kangKws))
    }
}
