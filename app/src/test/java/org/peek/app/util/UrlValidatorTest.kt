package org.peek.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlValidatorTest {
    @Test
    fun `accepts public http and https URLs`() {
        assertTrue(UrlValidator.isAllowed("https://www.youtube.com/watch?v=abc"))
        assertTrue(UrlValidator.isAllowed("http://example.com/media"))
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
