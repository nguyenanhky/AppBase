package com.viettel.appbase.core.playstore

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.viettel.appbase.R
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Checks for updates on every resume and asks to restart once a flexible update is downloaded. */
@Composable
fun InAppUpdateHandler(controller: InAppUpdateController = koinInject()) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        controller.onUpdateFlowResult(result.resultCode)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, controller) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) { controller.checkForUpdate(launcher) }
    }

    val updateReady by controller.updateReady.collectAsStateWithLifecycle()
    var postponed by rememberSaveable { mutableStateOf(false) }
    if (updateReady && !postponed) {
        AlertDialog(
            onDismissRequest = { postponed = true },
            title = { Text(stringResource(R.string.update_ready_title)) },
            text = { Text(stringResource(R.string.update_ready_message)) },
            confirmButton = {
                TextButton(onClick = controller::completeUpdate) { Text(stringResource(R.string.update_restart)) }
            },
            dismissButton = {
                TextButton(onClick = { postponed = true }) { Text(stringResource(R.string.update_later)) }
            },
        )
    }
}

/**
 * Returns a callback that shows the Play review dialog when [ReviewPolicy] allows it.
 * Call it after a positive moment, e.g. from a ViewModel event once the user completes a task.
 */
@Composable
fun rememberInAppReview(controller: InAppReviewController = koinInject()): () -> Unit {
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    return remember(activity, controller, scope) {
        { activity?.let { scope.launch { controller.requestReviewIfEligible(it) } } }
    }
}
