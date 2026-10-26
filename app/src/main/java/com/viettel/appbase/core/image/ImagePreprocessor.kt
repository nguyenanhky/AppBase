package com.viettel.appbase.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.viettel.appbase.core.ai.AiImage
import com.viettel.appbase.domain.model.AppError
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Turns a picked/captured photo into an upload-ready JPEG: fixes rotation, downsizes to
 * [DEFAULT_MAX_SIDE] and re-encodes. Re-encoding drops all EXIF metadata, including GPS location.
 */
class ImagePreprocessor(private val context: Context) {
    suspend fun prepare(
        uri: Uri,
        maxSide: Int = DEFAULT_MAX_SIDE,
        quality: Int = DEFAULT_QUALITY,
    ): AiImage = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(uri) { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw AppError.InvalidImage()

        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageSizing.sampleSize(bounds.outWidth, bounds.outHeight, maxSide)
        }
        val decoded = open(uri) { BitmapFactory.decodeStream(it, null, options) } ?: throw AppError.InvalidImage()
        val rotation = open(uri) { ExifInterface(it).rotationDegrees }
        val prepared = decoded.fitAndRotate(maxSide, rotation)

        val bytes = ByteArrayOutputStream().use { out ->
            prepared.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }
        prepared.recycle()
        AiImage(bytes, MIME_JPEG)
    }

    // Only a stream that cannot be opened is an error: `read` may legitimately return null
    // (decodeStream always does in inJustDecodeBounds mode).
    private fun <T> open(uri: Uri, read: (java.io.InputStream) -> T): T {
        val stream = context.contentResolver.openInputStream(uri) ?: throw AppError.InvalidImage()
        return stream.use(read)
    }

    private fun Bitmap.fitAndRotate(maxSide: Int, rotation: Int): Bitmap {
        val (targetWidth, targetHeight) = ImageSizing.fit(width, height, maxSide)
        if (targetWidth == width && targetHeight == height && rotation == 0) return this
        val matrix = Matrix().apply {
            postScale(targetWidth.toFloat() / width, targetHeight.toFloat() / height)
            postRotate(rotation.toFloat())
        }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true).also { if (it !== this) recycle() }
    }

    companion object {
        const val DEFAULT_MAX_SIDE = 1024
        const val DEFAULT_QUALITY = 85
        const val MIME_JPEG = "image/jpeg"
    }
}
