package com.antistalk.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class SigningInfoTest {

    @Test
    fun sha256Hex_matchesKnownVector() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            SigningInfo.sha256Hex("abc".toByteArray())
        )
    }

    @Test
    fun expectedCert_is64HexLowercase() {
        val sha = SigningInfo.EXPECTED_CERT_SHA256
        assertEquals(64, sha.length)
        assertTrue(sha == sha.lowercase())
        assertTrue(sha.all { it in "0123456789abcdef" })
    }

    @Test
    fun shortSha_keepsEndsOnly() {
        val sha = SigningInfo.EXPECTED_CERT_SHA256
        assertEquals(sha.take(8) + "…" + sha.takeLast(8), SigningInfo.shortSha(sha))
        assertEquals("không đọc được", SigningInfo.shortSha(null))
    }

    /** Keeps SigningInfo.EXPECTED_CERT_SHA256 in sync with the CI assertion. */
    @Test
    fun expectedCert_pinnedToCiWorkflow() {
        val wf = File("../.github/workflows/build-apk.yml")
        assumeTrue("workflow not reachable from ${File(".").absolutePath}", wf.exists())
        assertTrue(
            "CI no longer asserts the same signer — update SigningInfo.EXPECTED_CERT_SHA256",
            wf.readText().contains(SigningInfo.EXPECTED_CERT_SHA256)
        )
    }
}
