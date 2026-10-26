package com.viettel.appbase.core.image

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import com.viettel.appbase.domain.model.AppError
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Runs the real decode → rotate → resize → JPEG pipeline on generated photos. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImagePreprocessorTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val preprocessor = ImagePreprocessor(context)

    @Test
    fun `normal photo is decoded and kept as JPEG`() = runTest {
        val image = preprocessor.prepare(photo(width = 800, height = 600))

        val (width, height) = decodedSize(image.bytes)
        assertEquals(800 to 600, width to height)
        assertEquals(ImagePreprocessor.MIME_JPEG, image.mimeType)
    }

    @Test
    fun `large photo is downsized to the max side`() = runTest {
        val image = preprocessor.prepare(photo(width = 3000, height = 2000))

        assertEquals(1024 to 683, decodedSize(image.bytes))
    }

    @Test
    fun `EXIF rotation is applied`() = runTest {
        val uri = photo(width = 400, height = 200, orientation = ExifInterface.ORIENTATION_ROTATE_90)

        assertEquals(200 to 400, decodedSize(preprocessor.prepare(uri).bytes))
    }

    @Test
    fun `GPS location is stripped`() = runTest {
        val uri = photo(width = 400, height = 300, withGps = true)

        val exif = ExifInterface(ByteArrayInputStream(preprocessor.prepare(uri).bytes))

        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
    }

    @Test
    fun `file that is not an image is rejected`() = runTest {
        val file = File(context.cacheDir, "not-an-image.jpg").apply { writeText("hello") }

        val error = runCatching { preprocessor.prepare(Uri.fromFile(file)) }.exceptionOrNull()

        assertTrue(error is AppError.InvalidImage)
    }

    private fun photo(
        width: Int,
        height: Int,
        orientation: Int? = null,
        withGps: Boolean = false,
    ): Uri {
        val file = File.createTempFile("photo", ".jpg", context.cacheDir)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        if (orientation != null || withGps) {
            ExifInterface(file).apply {
                orientation?.let { setAttribute(ExifInterface.TAG_ORIENTATION, it.toString()) }
                if (withGps) setLatLong(21.0285, 105.8542)
                saveAttributes()
            }
        }
        return Uri.fromFile(file)
    }

    private fun decodedSize(bytes: ByteArray): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return options.outWidth to options.outHeight
    }
}
