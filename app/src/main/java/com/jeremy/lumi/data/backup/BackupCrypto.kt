package com.jeremy.lumi.data.backup

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** La contraseña introducida no descifra el respaldo (tag GCM inválido). */
class WrongPassphraseException : Exception("La contraseña de cifrado no es válida")

/** El archivo no tiene el formato de respaldo de Lumi o está dañado. */
class CorruptBackupException(message: String) : Exception(message)

/**
 * Cifrado de extremo a extremo del respaldo.
 *
 * Diseño "cero responsabilidad": la clave se deriva de una contraseña que solo
 * conoce la usuaria (PBKDF2-HMAC-SHA256) y el contenido se cifra con
 * AES-256-GCM. Ni Lumi ni Google pueden leer el archivo resultante.
 *
 * El archivo es AUTOCONTENIDO: la cabecera lleva sal, iteraciones e IV, de modo
 * que restaurar en un teléfono nuevo solo requiere el archivo + la contraseña.
 *
 * Formato binario (41 bytes de cabecera + ciphertext):
 * ```
 * [0..7]   magic "LUMIBKP1" (ASCII)
 * [8]      versión del formato de cifrado (1)
 * [9..24]  sal PBKDF2 (16 bytes)
 * [25..28] iteraciones PBKDF2 (Int32 big-endian)
 * [29..40] IV de GCM (12 bytes)
 * [41..]   ciphertext AES-256-GCM (tag de 128 bits incluido al final)
 * ```
 */
object BackupCrypto {

    private const val MAGIC = "LUMIBKP1"
    private const val FORMAT_VERSION: Byte = 1

    /**
     * Iteraciones de PBKDF2. Punto medio entre la recomendación OWASP (600k)
     * y el rendimiento de gama baja: es una operación poco frecuente (~1 s).
     */
    const val DEFAULT_ITERATIONS = 310_000

    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val KEY_BITS = 256
    private const val MAX_PBKDF2_ITERATIONS = 1_000_000
    private const val TAG_BITS = 128
    private const val HEADER_SIZE = 8 + 1 + SALT_SIZE + 4 + IV_SIZE
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    /** Clave derivada junto con los parámetros necesarios para re-derivarla. */
    class DerivedKey(
        val keyBytes: ByteArray,
        val salt: ByteArray,
        val iterations: Int
    ) {
        fun clear() {
            keyBytes.fill(0)
        }
    }

    /** Parámetros de derivación leídos de la cabecera de un archivo existente. */
    class Header(
        val salt: ByteArray,
        val iterations: Int,
        val iv: ByteArray
    )

    fun generateSalt(): ByteArray =
        ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }

    /**
     * Deriva la clave AES a partir de la contraseña. Operación costosa a
     * propósito (resistencia a fuerza bruta) — llamar desde Dispatchers.Default.
     */
    fun deriveKey(
        passphrase: CharArray,
        salt: ByteArray,
        iterations: Int = DEFAULT_ITERATIONS
    ): DerivedKey {
        require(salt.size == SALT_SIZE) { "El salt debe tener exactamente $SALT_SIZE bytes" }
        require(iterations in 50_000..MAX_PBKDF2_ITERATIONS) { "Iteraciones PBKDF2 fuera de los límites admitidos" }
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        // Borra la copia interna de la contraseña en cuanto deja de hacer falta
        spec.clearPassword()
        return DerivedKey(keyBytes, salt, iterations)
    }

    /** Cifra [plainBytes] y devuelve el archivo completo (cabecera + ciphertext). */
    fun encrypt(plainBytes: ByteArray, key: DerivedKey): ByteArray {
        val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key.keyBytes, "AES"),
            GCMParameterSpec(TAG_BITS, iv)
        )
        val ciphertext = cipher.doFinal(plainBytes)
        val magic = MAGIC.toByteArray(Charsets.US_ASCII)
        return ByteBuffer.allocate(HEADER_SIZE + ciphertext.size)
            .put(magic)
            .put(FORMAT_VERSION)
            .put(key.salt)
            .putInt(key.iterations)
            .put(iv)
            .put(ciphertext)
            .array()
    }

    /** Lee y valida la cabecera sin descifrar (para conocer sal/iteraciones). */
    fun readHeader(fileBytes: ByteArray): Header {
        if (fileBytes.size < HEADER_SIZE + (TAG_BITS / 8)) {
            throw CorruptBackupException("El archivo es demasiado corto para ser un respaldo")
        }
        val buffer = ByteBuffer.wrap(fileBytes)
        val magic = ByteArray(8).also { buffer.get(it) }
        if (!magic.contentEquals(MAGIC.toByteArray(Charsets.US_ASCII))) {
            throw CorruptBackupException("El archivo no es un respaldo de Lumi")
        }
        val version = buffer.get()
        if (version != FORMAT_VERSION) {
            throw CorruptBackupException("Versión de cifrado no soportada: $version")
        }
        val salt = ByteArray(SALT_SIZE).also { buffer.get(it) }
        val iterations = buffer.int
        if (iterations !in 50_000..MAX_PBKDF2_ITERATIONS) {
            throw CorruptBackupException("Cabecera de respaldo corrupta o iteraciones fuera de rango seguro")
        }
        val iv = ByteArray(IV_SIZE).also { buffer.get(it) }
        return Header(salt, iterations, iv)
    }

    /**
     * Descifra el archivo completo con la clave dada.
     * @throws WrongPassphraseException si la clave no corresponde (tag inválido).
     * @throws CorruptBackupException si la cabecera no es válida.
     */
    fun decrypt(fileBytes: ByteArray, keyBytes: ByteArray): ByteArray {
        val header = readHeader(fileBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(keyBytes, "AES"),
            GCMParameterSpec(TAG_BITS, header.iv)
        )
        return try {
            cipher.doFinal(fileBytes, HEADER_SIZE, fileBytes.size - HEADER_SIZE)
        } catch (e: AEADBadTagException) {
            throw WrongPassphraseException()
        } catch (e: GeneralSecurityException) {
            throw CorruptBackupException("Error criptográfico al descifrar respaldo: ${e.message}")
        }
    }
}
