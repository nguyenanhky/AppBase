package com.viettel.appbase.core.playstore

import android.app.Activity
import com.google.android.play.core.review.ReviewManager
import com.viettel.appbase.core.storage.AppDataStore
import com.viettel.appbase.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Google Play in-app review. Call [requestReviewIfEligible] right after a positive moment
 * (task completed, level cleared...), never from a button: use [openPlayStoreListing] for that.
 */
class InAppReviewController(private val reviewManager: ReviewManager, private val appDataStore: AppDataStore) {
    suspend fun onAppLaunched(nowEpochMillis: Long = System.currentTimeMillis()) {
        appDataStore.recordLaunch(nowEpochMillis)
    }

    suspend fun requestReviewIfEligible(activity: Activity, nowEpochMillis: Long = System.currentTimeMillis()) {
        val preferences = appDataStore.preferences.first()
        val eligible = ReviewPolicy.isEligible(
            launchCount = preferences.launchCount,
            firstLaunchEpochMillis = preferences.firstLaunchEpochMillis,
            lastPromptEpochMillis = preferences.lastReviewPromptEpochMillis,
            nowEpochMillis = nowEpochMillis,
        )
        if (!eligible) return

        suspendRunCatching {
            val reviewInfo = reviewManager.requestReviewFlow().await()
            // Play never reports whether the dialog appeared, so every attempt starts the cooldown.
            appDataStore.markReviewPrompted(nowEpochMillis)
            reviewManager.launchReviewFlow(activity, reviewInfo).await()
        }.onFailure { Timber.d(it, "In-app review unavailable") }
    }
}
