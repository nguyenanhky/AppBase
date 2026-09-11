package com.viettel.appbase.core.storage

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
object AppPreferencesSerializer : Serializer<AppPreferences> {
    override val defaultValue: AppPreferences = AppPreferences()

    override suspend fun readFrom(input: InputStream): AppPreferences = try {
        ProtoBuf.decodeFromByteArray(AppPreferences.serializer(), input.readBytes())
    } catch (exception: SerializationException) {
        throw CorruptionException("Cannot read app preferences.", exception)
    }

    override suspend fun writeTo(t: AppPreferences, output: OutputStream) {
        output.write(ProtoBuf.encodeToByteArray(AppPreferences.serializer(), t))
    }
}
