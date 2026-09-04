package org.peek.app.data.extractor.ytdlp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledYtDlpInstallerTest {
    @Test
    fun missingPrivateCopyRequiresRefresh() {
        assertTrue(bundledYtDlpNeedsRefresh(null, EXPECTED_HASH))
    }

    @Test
    fun stalePrivateCopyRequiresRefresh() {
        assertTrue(bundledYtDlpNeedsRefresh("old-hash", EXPECTED_HASH))
    }

    @Test
    fun matchingPrivateCopyDoesNotRequireRefresh() {
        assertFalse(bundledYtDlpNeedsRefresh(EXPECTED_HASH.uppercase(), EXPECTED_HASH))
    }

    private companion object {
        const val EXPECTED_HASH = "abcdef0123456789"
    }
}
