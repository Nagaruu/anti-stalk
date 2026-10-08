package com.antistalk.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoastBankTest {

    @Test
    fun `pick interpolates name and count correctly`() {
        val roast = RoastBank.pick(2, 5, "Kang Too Jee", vi = true)
        assertFalse(roast.contains("{name}"))
        assertFalse(roast.contains("{count}"))
        assertTrue(roast.isNotBlank())
    }

    @Test
    fun `pick does not return the exact same roast consecutively`() {
        val r1 = RoastBank.pick(2, 1, "Crush", vi = true)
        val r2 = RoastBank.pick(2, 1, "Crush", vi = true)
        assertNotEquals(r1, r2)
    }

    @Test
    fun `repeat attempt picks repeat-specific roast`() {
        val r = RoastBank.pick(2, 2, "Kang Too Jee", vi = true, isRepeatAttempt = true)
        assertTrue(
            r.contains("Tôi đi ra") ||
            r.contains("quay xe") ||
            r.contains("Bắt quả tang") ||
            r.contains("mấy chục giây") ||
            r.contains("kìm lòng") ||
            r.contains("tắt đi")
        )
    }
}
