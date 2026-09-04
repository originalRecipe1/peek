package org.peek.app.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoPlayerTest {
    @Test
    fun portraitVideoUsesPortraitAspectRatio() {
        assertEquals(
            9f / 16f,
            requireNotNull(calculateDisplayAspectRatio(1080, 1920, 1f)),
            0.0001f,
        )
    }

    @Test
    fun nonSquarePixelsAreIncludedInDisplayRatio() {
        assertEquals(
            16f / 9f,
            requireNotNull(calculateDisplayAspectRatio(720, 576, 1.4222222f)),
            0.0001f,
        )
    }

    @Test
    fun invalidVideoDimensionsHaveNoAspectRatio() {
        assertNull(calculateDisplayAspectRatio(0, 1920, 1f))
        assertNull(calculateDisplayAspectRatio(1080, 0, 1f))
        assertNull(calculateDisplayAspectRatio(1080, 1920, 0f))
    }
}
