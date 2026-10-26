package com.viettel.appbase.core.playstore

import android.app.Activity
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.viettel.appbase.core.storage.AppDataStore
import com.viettel.appbase.core.storage.AppPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class InAppReviewControllerTest {
    private val reviewManager = mockk<ReviewManager>()
    private val appDataStore = mockk<AppDataStore>()
    private val activity = mockk<Activity>()
    private val controller = InAppReviewController(reviewManager, appDataStore)

    private val firstLaunch = 1_000_000_000_000L
    private val now = firstLaunch + 10.days.inWholeMilliseconds

    @Test
    fun `eligible user sees the review flow and starts the cooldown`() = runTest {
        givenPreferences(AppPreferences(launchCount = ReviewPolicy.MIN_LAUNCHES, firstLaunchEpochMillis = firstLaunch))
        val reviewInfo = mockk<ReviewInfo>()
        every { reviewManager.requestReviewFlow() } returns Tasks.forResult(reviewInfo)
        every { reviewManager.launchReviewFlow(activity, reviewInfo) } returns Tasks.forResult(null)
        coEvery { appDataStore.markReviewPrompted(now) } returns Unit

        controller.requestReviewIfEligible(activity, now)

        verify(exactly = 1) { reviewManager.launchReviewFlow(activity, reviewInfo) }
        coVerify(exactly = 1) { appDataStore.markReviewPrompted(now) }
    }

    @Test
    fun `ineligible user is never asked`() = runTest {
        givenPreferences(AppPreferences(launchCount = 1, firstLaunchEpochMillis = firstLaunch))

        controller.requestReviewIfEligible(activity, now)

        verify(exactly = 0) { reviewManager.requestReviewFlow() }
    }

    @Test
    fun `play failure is swallowed and cooldown not started`() = runTest {
        givenPreferences(AppPreferences(launchCount = ReviewPolicy.MIN_LAUNCHES, firstLaunchEpochMillis = firstLaunch))
        every { reviewManager.requestReviewFlow() } returns Tasks.forException(IllegalStateException("no Play"))

        controller.requestReviewIfEligible(activity, now)

        coVerify(exactly = 0) { appDataStore.markReviewPrompted(any()) }
    }

    private fun givenPreferences(preferences: AppPreferences) {
        every { appDataStore.preferences } returns flowOf(preferences)
    }
}
