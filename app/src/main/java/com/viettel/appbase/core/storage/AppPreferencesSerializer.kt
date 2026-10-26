package com.viettel.appbase.core.storage

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
class AppPreferencesSerializer(private val cipher: DataCipher) : Serializer<AppPreferences> {
    override val defaultValue: AppPreferences = AppPreferences()

    override suspend fun readFrom(input: InputStream): AppPreferences = try {
        ProtoBuf.decodeFromByteArray(AppPreferences.serializer(), cipher.decrypt(input.readBytes()))
    } catch (exception: GeneralSecurityException) {
        throw CorruptionException("Cannot decrypt app preferences.", exception)
    } catch (exception: SerializationException) {
        throw CorruptionException("Cannot read app preferences.", exception)
    } catch (exception: IllegalArgumentException) {
        throw CorruptionException("Cannot read app preferences.", exception)
    }

    override suspend fun writeTo(t: AppPreferences, output: OutputStream) {
        output.write(cipher.encrypt(ProtoBuf.encodeToByteArray(AppPreferences.serializer(), t)))
    }
}
