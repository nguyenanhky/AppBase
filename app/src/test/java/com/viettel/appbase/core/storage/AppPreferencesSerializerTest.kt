package com.viettel.appbase.core.storage

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AppPreferencesSerializerTest {
    @Test
    fun `preferences survive protobuf round trip`() = runTest {
        val expected = AppPreferences(
            accessToken = "token",
            refreshToken = "refresh",
            userId = "42",
            fcmToken = "fcm",
            themeMode = ThemeMode.DARK,
        )
        val output = ByteArrayOutputStream()

        AppPreferencesSerializer.writeTo(expected, output)
        val actual = AppPreferencesSerializer.readFrom(ByteArrayInputStream(output.toByteArray()))

        assertEquals(expected, actual)
    }
}
