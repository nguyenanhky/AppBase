package com.viettel.appbase.core.monetization.ads

import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrequencyCapTest {
    private val start = 1_000_000L
    private val interval = 3.minutes.inWholeMilliseconds
    private val cap = FrequencyCap(3.minutes, sessionStartMillis = start)

    @Test
    fun `no ad right after the app starts`() {
        assertFalse(cap.canShow(start + interval - 1))
    }

    @Test
    fun `first ad allowed once the interval passed since start`() {
        assertTrue(cap.canShow(start + interval))
    }

    @Test
    fun `next ad waits a full interval after the previous one`() {
        val shownAt = start + interval
        cap.recordShown(shownAt)

        assertFalse(cap.canShow(shownAt + interval - 1))
        assertTrue(cap.canShow(shownAt + interval))
    }
}
