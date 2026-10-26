package com.viettel.appbase.core.playstore

import com.google.android.play.core.install.model.AppUpdateType

/**
 * Picks the update flow. Priority (0-5) is set per release through the Play Developer API
 * (`inAppUpdatePriority`); without it, staleness alone triggers a flexible update.
 */
object UpdatePolicy {
    const val IMMEDIATE_PRIORITY = 4
    const val FLEXIBLE_PRIORITY = 2
    const val FLEXIBLE_STALENESS_DAYS = 3

    /** @return [AppUpdateType.IMMEDIATE], [AppUpdateType.FLEXIBLE], or null to not prompt. */
    fun updateType(priority: Int, stalenessDays: Int?): Int? = when {
        priority >= IMMEDIATE_PRIORITY -> AppUpdateType.IMMEDIATE
        priority >= FLEXIBLE_PRIORITY -> AppUpdateType.FLEXIBLE
        (stalenessDays ?: 0) >= FLEXIBLE_STALENESS_DAYS -> AppUpdateType.FLEXIBLE
        else -> null
    }
}
