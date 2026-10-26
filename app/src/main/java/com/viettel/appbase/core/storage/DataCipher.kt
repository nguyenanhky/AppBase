package com.viettel.appbase.core.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface DataCipher {
    fun encrypt(data: ByteArray): ByteArray

    /** @throws GeneralSecurityException when [data] was not produced by [encrypt] with the same key. */
    fun decrypt(data: ByteArray): ByteArray
}

/** AES-256-GCM with a non-exportable key held by Android Keystore. Output layout: IV + ciphertext. */
class KeystoreDataCipher(private val keyAlias: String) : DataCipher {
    private val keyStore: KeyStore by lazy { KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) } }

    override fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        return cipher.iv + cipher.doFinal(data)
    }

    override fun decrypt(data: ByteArray): ByteArray {
        if (data.size < IV_SIZE) throw GeneralSecurityException("Encrypted payload is too short.")
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_SIZE_BITS, data, 0, IV_SIZE))
        }
        return cipher.doFinal(data, IV_SIZE, data.size - IV_SIZE)
    }

    @Synchronized
    private fun secretKey(): SecretKey = keyStore.getKey(keyAlias, null) as? SecretKey ?: generateKey()

    private fun generateKey(): SecretKey = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        .apply {
            init(
                KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    .build(),
            )
        }
        .generateKey()

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_SIZE_BITS = 128
        const val KEY_SIZE_BITS = 256
    }
}
