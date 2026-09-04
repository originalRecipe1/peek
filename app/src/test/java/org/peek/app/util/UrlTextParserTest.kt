package org.peek.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlTextParserTest {
    @Test
    fun `accepts a standalone public URL`() {
        assertEquals(
            "https://www.youtube.com/watch?v=abc",
            UrlTextParser.firstSupportedUrl("  https://www.youtube.com/watch?v=abc  "),
        )
    }

    @Test
    fun `extracts the first URL from shared prose`() {
        assertEquals(
            "https://www.reddit.com/r/videos/comments/abc/a_post",
            UrlTextParser.firstSupportedUrl(
                "Take a look: https://www.reddit.com/r/videos/comments/abc/a_post. Great clip!",
            ),
        )
    }

    @Test
    fun `removes punctuation wrapped around a shared URL`() {
        assertEquals(
            "https://x.com/example/status/123",
            UrlTextParser.firstSupportedUrl("(https://x.com/example/status/123)"),
        )
    }

    @Test
    fun `rejects unsupported and private URLs`() {
        assertNull(UrlTextParser.firstSupportedUrl("Open file:///sdcard/video.mp4"))
        assertNull(UrlTextParser.firstSupportedUrl("See http://127.0.0.1/private"))
        assertNull(UrlTextParser.firstSupportedUrl("There is no link here"))
    }
}
