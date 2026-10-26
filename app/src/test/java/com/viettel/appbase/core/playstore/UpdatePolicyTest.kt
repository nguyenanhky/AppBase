package com.viettel.appbase.core.playstore

import com.google.android.play.core.install.model.AppUpdateType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdatePolicyTest {
    @Test
    fun `critical priority forces an immediate update`() {
        assertEquals(AppUpdateType.IMMEDIATE, UpdatePolicy.updateType(priority = 5, stalenessDays = 0))
    }

    @Test
    fun `medium priority offers a flexible update`() {
        assertEquals(AppUpdateType.FLEXIBLE, UpdatePolicy.updateType(priority = 2, stalenessDays = 0))
    }

    @Test
    fun `stale version offers a flexible update without priority`() {
        assertEquals(
            AppUpdateType.FLEXIBLE,
            UpdatePolicy.updateType(priority = 0, stalenessDays = UpdatePolicy.FLEXIBLE_STALENESS_DAYS),
        )
    }

    @Test
    fun `fresh low priority release is skipped`() {
        assertNull(UpdatePolicy.updateType(priority = 0, stalenessDays = null))
    }
}
