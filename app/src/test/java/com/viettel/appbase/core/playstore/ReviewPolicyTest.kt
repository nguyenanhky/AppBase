package com.viettel.appbase.core.playstore

import kotlin.time.Duration.Companion.days
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPolicyTest {
    private val firstLaunch = 1_000_000_000_000L
    private val engagedNow = firstLaunch + 10.days.inWholeMilliseconds

    @Test
    fun `engaged user who was never asked is eligible`() {
        assertTrue(ReviewPolicy.isEligible(ReviewPolicy.MIN_LAUNCHES, firstLaunch, 0, engagedNow))
    }

    @Test
    fun `too few launches is not eligible`() {
        assertFalse(ReviewPolicy.isEligible(ReviewPolicy.MIN_LAUNCHES - 1, firstLaunch, 0, engagedNow))
    }

    @Test
    fun `recent install is not eligible`() {
        val now = firstLaunch + 1.days.inWholeMilliseconds

        assertFalse(ReviewPolicy.isEligible(ReviewPolicy.MIN_LAUNCHES, firstLaunch, 0, now))
    }

    @Test
    fun `unknown first launch is not eligible`() {
        assertFalse(ReviewPolicy.isEligible(ReviewPolicy.MIN_LAUNCHES, 0, 0, engagedNow))
    }

    @Test
    fun `asked within cooldown is not eligible`() {
        val lastPrompt = engagedNow - 10.days.inWholeMilliseconds

        assertFalse(ReviewPolicy.isEligible(ReviewPolicy.MIN_LAUNCHES, firstLaunch, lastPrompt, engagedNow))
    }

    @Test
    fun `asked before cooldown ended is eligible again`() {
        val lastPrompt = engagedNow - ReviewPolicy.cooldown.inWholeMilliseconds

        assertTrue(ReviewPolicy.isEligible(ReviewPolicy.MIN_LAUNCHES, firstLaunch, lastPrompt, engagedNow))
    }
}
