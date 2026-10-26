package com.viettel.appbase.core.image

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSizingTest {
    @Test
    fun `large photo is subsampled but stays above the target`() {
        // 4000 / 2 = 2000, / 4 = 1000 < 1024, so 2 is the largest safe subsample.
        assertEquals(2, ImageSizing.sampleSize(4000, 3000, 1024))
    }

    @Test
    fun `small photo is not subsampled`() {
        assertEquals(1, ImageSizing.sampleSize(800, 600, 1024))
    }

    @Test
    fun `fit keeps aspect ratio on the longest side`() {
        assertEquals(1024 to 768, ImageSizing.fit(2000, 1500, 1024))
        assertEquals(576 to 1024, ImageSizing.fit(1125, 2000, 1024))
    }

    @Test
    fun `fit never upscales`() {
        assertEquals(640 to 480, ImageSizing.fit(640, 480, 1024))
    }
}
