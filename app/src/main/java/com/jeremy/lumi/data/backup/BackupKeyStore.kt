package com.jeremy.lumi.data.backup

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Envuelve (cifra) la clave de respaldo con una llave del Android Keystore
 * para poder cachearla en el dispositivo sin guardarla nunca en claro.
 *
 * ¿Por qué no usar el Keystore directamente para cifrar el respaldo?
 * Porque sus llaves NO SALEN del teléfono: un respaldo cifrado así sería
 * irrecuperable en un dispositivo nuevo — justo lo contrario de lo que
 * necesita una copia de seguridad. Por eso la clave real se deriva de la
 * contraseña de la usuaria (portable) y el Keystore solo protege su caché
 * local, que hace posible el respaldo automático sin volver a preguntarla.
 */
object BackupKeyStore {

    private const val TAG = "BackupKeyStore"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "lumi_backup_key_wrapper"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_BITS = 128
    private const val IV_SIZE = 12

    private fun obtainWrapperKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    /** Cifra la clave de respaldo. Devuelve base64(iv + ciphertext) para persistir. */
    fun wrap(keyBytes: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, obtainWrapperKey())
        val combined = cipher.iv + cipher.doFinal(keyBytes)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Descifra la clave envuelta. Devuelve null si el Keystore la invalidó
     * (p. ej. tras restaurar la app en otro teléfono) — en ese caso habrá
     * que volver a pedir la contraseña.
     */
    fun unwrap(wrapped: String): ByteArray? = try {
        val combined = Base64.decode(wrapped, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, IV_SIZE)
        val ciphertext = combined.copyOfRange(IV_SIZE, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, obtainWrapperKey(), GCMParameterSpec(TAG_BITS, iv))
        cipher.doFinal(ciphertext)
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo desenvolver la clave cacheada: ${e.message}")
        null
    }

    /** Elimina la llave envolvente (al desconectar la cuenta). */
    fun deleteWrapperKey() {
        try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(ALIAS)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo borrar la llave del Keystore: ${e.message}")
        }
    }
}
