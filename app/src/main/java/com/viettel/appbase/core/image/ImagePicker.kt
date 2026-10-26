package com.viettel.appbase.core.image

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import timber.log.Timber

class ImagePicker(val pickFromGallery: () -> Unit, val takePhoto: () -> Unit)

/**
 * Gallery (system Photo Picker) and camera (system camera app) without any runtime permission.
 * Do not declare CAMERA in the manifest: TakePicture would then require granting it.
 */
@Composable
fun rememberImagePicker(onImagePick: (Uri) -> Unit): ImagePicker {
    val context = LocalContext.current
    val currentOnImagePick by rememberUpdatedState(onImagePick)
    var pendingCaptureUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(currentOnImagePick)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) pendingCaptureUri?.let(currentOnImagePick)
    }
    return remember(context, gallery, camera) {
        ImagePicker(
            pickFromGallery = {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            takePhoto = {
                val uri = newCaptureUri(context)
                pendingCaptureUri = uri
                try {
                    camera.launch(uri)
                } catch (exception: ActivityNotFoundException) {
                    Timber.w(exception, "No camera app")
                }
            },
        )
    }
}

/** Only the latest capture is kept in cache; older ones are deleted. */
private fun newCaptureUri(context: Context): Uri {
    val directory = File(context.cacheDir, CAPTURE_DIR).apply { mkdirs() }
    directory.listFiles()?.forEach { it.delete() }
    val file = File.createTempFile("capture_", ".jpg", directory)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private const val CAPTURE_DIR = "captures"
