package com.viettel.appbase.core.playstore

import kotlin.time.Duration.Companion.days

/**
 * When to ask for a review: only engaged users, never right after install, and not too often.
 * Play also applies its own hidden quota, so an eligible request may still show nothing.
 */
object ReviewPolicy {
    const val MIN_LAUNCHES = 5
    val minDaysSinceFirstLaunch = 3.days
    val cooldown = 60.days

    fun isEligible(
        launchCount: Int,
        firstLaunchEpochMillis: Long,
        lastPromptEpochMillis: Long,
        nowEpochMillis: Long,
    ): Boolean {
        val usedLongEnough = firstLaunchEpochMillis > 0 &&
            nowEpochMillis - firstLaunchEpochMillis >= minDaysSinceFirstLaunch.inWholeMilliseconds
        val cooledDown = lastPromptEpochMillis == 0L ||
            nowEpochMillis - lastPromptEpochMillis >= cooldown.inWholeMilliseconds
        return launchCount >= MIN_LAUNCHES && usedLongEnough && cooledDown
    }
}
