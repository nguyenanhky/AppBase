package com.viettel.appbase.core.playstore

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.viettel.appbase.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Google Play in-app updates, driven by [UpdatePolicy]. Wired app-wide by InAppUpdateHandler.
 * An immediate update the user cancels is offered again on every resume (forced update);
 * a declined flexible update is not offered again until the next app start.
 */
class InAppUpdateController(private val appUpdateManager: AppUpdateManager) {
    private val mutableUpdateReady = MutableStateFlow(false)

    /** A flexible update finished downloading and waits for [completeUpdate]. */
    val updateReady: StateFlow<Boolean> = mutableUpdateReady.asStateFlow()

    private var flexibleDeclined = false
    private var pendingType: Int? = null

    private val installListener = InstallStateUpdatedListener { state ->
        mutableUpdateReady.value = state.installStatus() == InstallStatus.DOWNLOADED
    }

    init {
        appUpdateManager.registerListener(installListener)
    }

    suspend fun checkForUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        suspendRunCatching { startUpdateIfNeeded(launcher) }
            // Expected outside Play-installed builds (emulator, mock flavor, sideloaded APK).
            .onFailure { Timber.d(it, "In-app update check unavailable") }
    }

    fun onUpdateFlowResult(resultCode: Int) {
        if (pendingType == AppUpdateType.FLEXIBLE && resultCode != Activity.RESULT_OK) flexibleDeclined = true
        pendingType = null
    }

    /** Installs a downloaded flexible update; the app restarts. */
    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    private suspend fun startUpdateIfNeeded(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val info = appUpdateManager.appUpdateInfo.await()
        if (info.installStatus() == InstallStatus.DOWNLOADED) {
            mutableUpdateReady.value = true
            return
        }
        val type = UpdatePolicy.updateType(info.updatePriority(), info.clientVersionStalenessDays()) ?: return
        val shouldStart = when (info.updateAvailability()) {
            // Resume an immediate update interrupted by leaving the app.
            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> type == AppUpdateType.IMMEDIATE

            UpdateAvailability.UPDATE_AVAILABLE -> !(type == AppUpdateType.FLEXIBLE && flexibleDeclined)

            else -> false
        }
        if (shouldStart && info.isUpdateTypeAllowed(type)) {
            pendingType = type
            appUpdateManager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
        }
    }
}
