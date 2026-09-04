package org.peek.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SafeLogTest {
    @Test
    fun `redacts URLs and secrets`() {
        val result = SafeLog.redact(
            "Failed https://example.com/watch?v=secret Cookie: session-value Authorization=bearer-value",
        )

        assertFalse(result.contains("secret"))
        assertFalse(result.contains("session-value"))
        assertFalse(result.contains("bearer-value"))
        assertEquals(
            "Failed <url> Cookie=<redacted> Authorization=<redacted>",
            result,
        )
    }
}
