package com.viettel.appbase.core.storage

import androidx.datastore.core.CorruptionException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.GeneralSecurityException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalSerializationApi::class)
class AppPreferencesSerializerTest {
    private val serializer = AppPreferencesSerializer(XorCipher)

    @Test
    fun `preferences survive encrypted protobuf round trip`() = runTest {
        val expected = AppPreferences(
            accessToken = "token",
            refreshToken = "refresh",
            userId = "42",
            fcmToken = "fcm",
            themeMode = ThemeMode.DARK,
        )
        val output = ByteArrayOutputStream()

        serializer.writeTo(expected, output)
        val actual = serializer.readFrom(ByteArrayInputStream(output.toByteArray()))

        assertEquals(expected, actual)
        assertFalse(String(output.toByteArray(), Charsets.ISO_8859_1).contains("token"))
    }

    @Test
    fun `file written before new fields were added still reads`() = runTest {
        val oldBytes = ProtoBuf.encodeToByteArray(
            PreferencesV1.serializer(),
            PreferencesV1(accessToken = "token", themeMode = ThemeMode.DARK),
        )

        val actual = serializer.readFrom(ByteArrayInputStream(XorCipher.encrypt(oldBytes)))

        assertEquals(AppPreferences(accessToken = "token", themeMode = ThemeMode.DARK), actual)
    }

    @Test(expected = CorruptionException::class)
    fun `undecryptable file is reported as corruption`() = runTest {
        AppPreferencesSerializer(FailingCipher).readFrom(ByteArrayInputStream(byteArrayOf(1, 2, 3)))
    }

    /** AppPreferences as first shipped. Never edit: it stands for files already on users' devices. */
    @Serializable
    private data class PreferencesV1(
        val accessToken: String = "",
        val refreshToken: String = "",
        val userId: String = "",
        val fcmToken: String = "",
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
    )

    private object XorCipher : DataCipher {
        override fun encrypt(data: ByteArray) = ByteArray(data.size) { (data[it].toInt() xor 0x5A).toByte() }
        override fun decrypt(data: ByteArray) = encrypt(data)
    }

    private object FailingCipher : DataCipher {
        override fun encrypt(data: ByteArray) = data
        override fun decrypt(data: ByteArray): ByteArray = throw GeneralSecurityException("bad key")
    }
}
