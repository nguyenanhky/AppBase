package com.viettel.appbase.core.playstore

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppUpdateControllerTest {
    private val appUpdateManager = mockk<AppUpdateManager>(relaxed = true)
    private val launcher = mockk<ActivityResultLauncher<IntentSenderRequest>>()
    private val options = slot<AppUpdateOptions>()

    private fun controller(): InAppUpdateController {
        every { appUpdateManager.startUpdateFlowForResult(any(), launcher, capture(options)) } returns true
        every { appUpdateManager.registerListener(any()) } just runs
        return InAppUpdateController(appUpdateManager)
    }

    @Test
    fun `high priority starts an immediate update`() = runTest {
        val controller = controller()
        givenUpdate(priority = 5)

        controller.checkForUpdate(launcher)

        assertEquals(AppUpdateType.IMMEDIATE, options.captured.appUpdateType())
    }

    @Test
    fun `stale version starts a flexible update`() = runTest {
        val controller = controller()
        givenUpdate(stalenessDays = UpdatePolicy.FLEXIBLE_STALENESS_DAYS)

        controller.checkForUpdate(launcher)

        assertEquals(AppUpdateType.FLEXIBLE, options.captured.appUpdateType())
    }

    @Test
    fun `fresh low priority update does not prompt`() = runTest {
        val controller = controller()
        givenUpdate(stalenessDays = 0)

        controller.checkForUpdate(launcher)

        verify(exactly = 0) { appUpdateManager.startUpdateFlowForResult(any(), launcher, any<AppUpdateOptions>()) }
    }

    @Test
    fun `declined flexible update is not offered again`() = runTest {
        val controller = controller()
        givenUpdate(stalenessDays = UpdatePolicy.FLEXIBLE_STALENESS_DAYS)

        controller.checkForUpdate(launcher)
        controller.onUpdateFlowResult(Activity.RESULT_CANCELED)
        controller.checkForUpdate(launcher)

        verify(exactly = 1) { appUpdateManager.startUpdateFlowForResult(any(), launcher, any<AppUpdateOptions>()) }
    }

    @Test
    fun `cancelled immediate update is offered again`() = runTest {
        val controller = controller()
        givenUpdate(priority = 5)

        controller.checkForUpdate(launcher)
        controller.onUpdateFlowResult(Activity.RESULT_CANCELED)
        controller.checkForUpdate(launcher)

        verify(exactly = 2) { appUpdateManager.startUpdateFlowForResult(any(), launcher, any<AppUpdateOptions>()) }
    }

    @Test
    fun `downloaded update asks for restart`() = runTest {
        val controller = controller()
        givenUpdate(installStatus = InstallStatus.DOWNLOADED)

        controller.checkForUpdate(launcher)

        assertTrue(controller.updateReady.value)
    }

    @Test
    fun `flexible download in progress is not restarted as immediate`() = runTest {
        val controller = controller()
        givenUpdate(
            availability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
            stalenessDays = UpdatePolicy.FLEXIBLE_STALENESS_DAYS,
        )

        controller.checkForUpdate(launcher)

        verify(exactly = 0) { appUpdateManager.startUpdateFlowForResult(any(), launcher, any<AppUpdateOptions>()) }
    }

    @Test
    fun `play unavailable is ignored`() = runTest {
        val controller = controller()
        every { appUpdateManager.appUpdateInfo } returns Tasks.forException(IllegalStateException("no Play"))

        controller.checkForUpdate(launcher)

        verify(exactly = 0) { appUpdateManager.startUpdateFlowForResult(any(), launcher, any<AppUpdateOptions>()) }
    }

    private fun givenUpdate(
        priority: Int = 0,
        stalenessDays: Int? = null,
        availability: Int = UpdateAvailability.UPDATE_AVAILABLE,
        installStatus: Int = InstallStatus.UNKNOWN,
    ) {
        val info = mockk<AppUpdateInfo> {
            every { updatePriority() } returns priority
            every { clientVersionStalenessDays() } returns stalenessDays
            every { updateAvailability() } returns availability
            every { installStatus() } returns installStatus
            every { isUpdateTypeAllowed(any<Int>()) } returns true
        }
        every { appUpdateManager.appUpdateInfo } returns Tasks.forResult(info)
    }
}
