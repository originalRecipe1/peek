package org.peek.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlValidatorTest {
    @Test
    fun `accepts public http and https URLs`() {
        assertTrue(UrlValidator.isAllowed("https://www.youtube.com/watch?v=abc"))
        assertTrue(UrlValidator.isAllowed("http://example.com/media"))
    }

    @Test
    fun `upgrades public http input before network access`() {
        assertEquals(
            "https://example.com/media?item=1#preview",
            UrlValidator.toHttpsUrl("http://example.com/media?item=1#preview"),
        )
        assertEquals(
            "https://example.com/media",
            UrlValidator.toHttpsUrl("https://example.com/media"),
        )
        assertNull(UrlValidator.toHttpsUrl("http://127.0.0.1/private"))
    }

    @Test
    fun `only https URLs are allowed at network boundaries`() {
        assertTrue(UrlValidator.isAllowedHttps("https://example.com/media"))
        assertFalse(UrlValidator.isAllowedHttps("http://example.com/media"))
        assertFalse(UrlValidator.isAllowedHttps("https://192.168.1.20/media"))
    }

    @Test
    fun `rejects unsupported schemes and local literals`() {
        assertFalse(UrlValidator.isAllowed("file:///sdcard/private.txt"))
        assertFalse(UrlValidator.isAllowed("intent://example.com"))
        assertFalse(UrlValidator.isAllowed("https://localhost/media"))
        assertFalse(UrlValidator.isAllowed("https://127.0.0.1/media"))
        assertFalse(UrlValidator.isAllowed("https://192.168.1.20/media"))
        assertFalse(UrlValidator.isAllowed("https://[::1]/media"))
    }

    @Test
    fun `rejects unreasonably large URLs`() {
        assertFalse(UrlValidator.isAllowed("https://example.com/" + "a".repeat(9_000)))
    }
}
