package com.jeremy.lumi.data.backup

import android.util.Base64
import com.jeremy.lumi.data.backup.drive.DriveApiClient
import com.jeremy.lumi.data.backup.drive.DriveApiException
import com.jeremy.lumi.data.backup.drive.DriveBackupFile
import com.jeremy.lumi.data.local.dao.LumiDao
import com.jeremy.lumi.data.preferences.BackupPreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orquestador del respaldo privado en Google Drive.
 *
 * Flujo de subida:   Room → JSON → gzip → AES-256-GCM → App Data Folder.
 * Flujo de bajada:   App Data Folder → descifrar → gunzip → validar → Room.
 *
 * La autenticación NO vive aquí: el ViewModel (o el worker) consigue el token
 * con DriveAuthManager y lo pasa a cada operación. Así este manager queda sin
 * estado de sesión y es trivial de probar.
 */
@Singleton
class DriveBackupManager @Inject constructor(
    private val dao: LumiDao,
    private val api: DriveApiClient,
    private val prefs: BackupPreferenceManager
) {

    /** No existe ningún respaldo en el Drive de esta cuenta. */
    class NoBackupFoundException : Exception("No hay respaldo en Drive")

    /** No hay clave local utilizable: hay que pedir la contraseña a la usuaria. */
    class PassphraseRequiredException : Exception("Se necesita la contraseña de cifrado")

    // ── Gestión de la clave de cifrado ───────────────────────────────────────

    /** ¿Existe material de clave guardado en este teléfono? */
    suspend fun hasLocalKey(): Boolean = prefs.getEncryptionKeyMaterial() != null

    /**
     * Clave local lista para usar, o null si no hay ninguna o el Keystore
     * la invalidó (en ese caso se volverá a pedir la contraseña).
     */
    suspend fun getCachedKey(): BackupCrypto.DerivedKey? {
        val material = prefs.getEncryptionKeyMaterial() ?: return null
        val keyBytes = BackupKeyStore.unwrap(material.wrappedKey) ?: return null
        val salt = try {
            Base64.decode(material.saltB64, Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            return null
        }
        return BackupCrypto.DerivedKey(keyBytes, salt, material.iterations)
    }

    /**
     * Deriva una clave NUEVA a partir de la contraseña elegida y la deja
     * cacheada (envuelta por el Keystore) para respaldos futuros.
     * PBKDF2 es costoso a propósito → Dispatchers.Default.
     */
    suspend fun createAndCacheKey(passphrase: CharArray): BackupCrypto.DerivedKey =
        withContext(Dispatchers.Default) {
            val key = BackupCrypto.deriveKey(passphrase, BackupCrypto.generateSalt())
            prefs.saveEncryptionKey(
                wrappedKey = BackupKeyStore.wrap(key.keyBytes),
                saltB64 = Base64.encodeToString(key.salt, Base64.NO_WRAP),
                iterations = key.iterations
            )
            key
        }

    // ── Operaciones contra Drive ─────────────────────────────────────────────

    /** Consulta los metadatos del respaldo remoto (fecha, tamaño) para la UI. */
    suspend fun fetchRemoteInfo(token: String): DriveBackupFile? {
        val file = api.findBackupFile(token)
        prefs.setFileId(file?.id)
        return file
    }

    /** Crea (o reemplaza) el respaldo cifrado en el Drive de la usuaria. */
    suspend fun createBackup(token: String, key: BackupCrypto.DerivedKey): DriveBackupFile {
        val snapshot = dao.getHealthDataSnapshot()
        val json = BackupSerializer.toJson(snapshot.cycles, snapshot.logs, snapshot.symptoms)
        val encrypted = withContext(Dispatchers.Default) {
            BackupCrypto.encrypt(gzip(json.toByteArray(Charsets.UTF_8)), key)
        }

        val existingId = prefs.getFileId() ?: api.findBackupFile(token)?.id
        val uploaded = try {
            api.uploadBackup(token, encrypted, existingId)
        } catch (e: DriveApiException) {
            // El id cacheado puede estar obsoleto (archivo borrado desde otro
            // teléfono): si Drive dice 404, reintentamos creando desde cero.
            if (e.httpCode == 404 && existingId != null) {
                prefs.setFileId(null)
                api.uploadBackup(token, encrypted, null)
            } else {
                throw e
            }
        }

        prefs.setFileId(uploaded.id)
        prefs.setLastBackupMs(System.currentTimeMillis())
        return uploaded
    }

    /**
     * Descarga, descifra y restaura el respaldo dentro de una transacción.
     *
     * Con [passphrase] deriva la clave desde la cabecera del archivo (camino
     * "teléfono nuevo"); con null usa la clave local cacheada.
     *
     * @throws NoBackupFoundException   si no hay archivo en Drive.
     * @throws PassphraseRequiredException si no hay clave local y no se dio contraseña.
     * @throws WrongPassphraseException si la contraseña/clave no descifra el archivo.
     */
    suspend fun restoreBackup(token: String, passphrase: CharArray?): RestoredData {
        val file = api.findBackupFile(token) ?: throw NoBackupFoundException()
        val bytes = api.downloadBackup(token, file.id)

        val keyBytes: ByteArray = if (passphrase != null) {
            val header = BackupCrypto.readHeader(bytes)
            withContext(Dispatchers.Default) {
                BackupCrypto.deriveKey(passphrase, header.salt, header.iterations).keyBytes
            }
        } else {
            getCachedKey()?.keyBytes ?: throw PassphraseRequiredException()
        }

        val json = withContext(Dispatchers.Default) {
            gunzip(BackupCrypto.decrypt(bytes, keyBytes))
        }.toString(Charsets.UTF_8)

        val restored = BackupSerializer.fromJson(json)
        dao.restoreHealthDataSnapshot(restored.cycles, restored.dailyLogs, restored.symptoms)
        prefs.setFileId(file.id)

        if (passphrase != null) {
            // La contraseña escrita demostró ser correcta: cacheamos la clave
            // para que el respaldo automático funcione también en este teléfono.
            val header = BackupCrypto.readHeader(bytes)
            prefs.saveEncryptionKey(
                wrappedKey = BackupKeyStore.wrap(keyBytes),
                saltB64 = Base64.encodeToString(header.salt, Base64.NO_WRAP),
                iterations = header.iterations
            )
        }
        return restored
    }

    /** Borra el respaldo del Drive (los datos locales del teléfono no se tocan). */
    suspend fun deleteRemoteBackup(token: String) {
        val id = prefs.getFileId() ?: api.findBackupFile(token)?.id ?: return
        try {
            api.deleteFile(token, id)
        } catch (e: DriveApiException) {
            // Si ya no existía, el objetivo está cumplido igualmente
            if (e.httpCode != 404) throw e
        }
        prefs.setFileId(null)
    }

    /** Limpieza local al desconectar la cuenta. El archivo remoto NO se toca. */
    suspend fun clearLocalState() {
        prefs.clearAll()
        BackupKeyStore.deleteWrapperKey()
    }

    // ── Compresión ───────────────────────────────────────────────────────────

    companion object {
        private const val MAX_UNCOMPRESSED_BACKUP_BYTES = 25 * 1024 * 1024 // 25 MB
    }

    private fun gzip(data: ByteArray): ByteArray =
        ByteArrayOutputStream().use { bos ->
            GZIPOutputStream(bos).use { it.write(data) }
            bos.toByteArray()
        }

    private fun gunzip(data: ByteArray): ByteArray {
        val buffer = ByteArray(8192)
        val out = ByteArrayOutputStream()
        var totalBytesRead = 0

        GZIPInputStream(ByteArrayInputStream(data)).use { gis ->
            var bytesRead = gis.read(buffer)
            while (bytesRead != -1) {
                totalBytesRead += bytesRead
                if (totalBytesRead > MAX_UNCOMPRESSED_BACKUP_BYTES) {
                    throw CorruptBackupException("El respaldo descomprimido supera el límite de seguridad (25 MB)")
                }
                out.write(buffer, 0, bytesRead)
                bytesRead = gis.read(buffer)
            }
        }
        return out.toByteArray()
    }
}
