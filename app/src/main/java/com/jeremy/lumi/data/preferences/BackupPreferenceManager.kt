package com.jeremy.lumi.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.backupDataStore by preferencesDataStore(name = "backup_prefs")

/** Material de clave persistido: clave envuelta por el Keystore + parámetros PBKDF2. */
data class EncryptionKeyMaterial(
    val wrappedKey: String,
    val saltB64: String,
    val iterations: Int
)

/**
 * Preferencias del respaldo privado en Google Drive.
 *
 * Aquí NUNCA se guarda nada sensible en claro: la clave de cifrado se persiste
 * envuelta por el Android Keystore (ver BackupKeyStore) y lo demás son
 * metadatos (correo mostrado en Ajustes, fecha del último respaldo, etc.).
 */
@Singleton
class BackupPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val ACCOUNT_EMAIL = stringPreferencesKey("cuenta_google_email")
        private val LAST_BACKUP_MS = longPreferencesKey("ultimo_respaldo_ms")
        private val AUTO_BACKUP = booleanPreferencesKey("respaldo_automatico")
        private val WRAPPED_KEY = stringPreferencesKey("clave_cifrado_envuelta")
        private val KEY_SALT_B64 = stringPreferencesKey("clave_salt_b64")
        private val KEY_ITERATIONS = intPreferencesKey("clave_iteraciones")
        private val DRIVE_FILE_ID = stringPreferencesKey("drive_file_id")
    }

    private val safeData: Flow<androidx.datastore.preferences.core.Preferences> =
        context.backupDataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val accountEmailFlow: Flow<String?> =
        safeData.map { it[ACCOUNT_EMAIL] }

    val lastBackupMsFlow: Flow<Long?> =
        safeData.map { it[LAST_BACKUP_MS] }

    val autoBackupFlow: Flow<Boolean> =
        safeData.map { it[AUTO_BACKUP] ?: false }

    suspend fun setAccountEmail(email: String?) {
        context.backupDataStore.edit { prefs ->
            if (email == null) prefs.remove(ACCOUNT_EMAIL) else prefs[ACCOUNT_EMAIL] = email
        }
    }

    suspend fun setLastBackupMs(timestampMs: Long) {
        context.backupDataStore.edit { it[LAST_BACKUP_MS] = timestampMs }
    }

    suspend fun setAutoBackup(enabled: Boolean) {
        context.backupDataStore.edit { it[AUTO_BACKUP] = enabled }
    }

    suspend fun saveEncryptionKey(wrappedKey: String, saltB64: String, iterations: Int) {
        context.backupDataStore.edit {
            it[WRAPPED_KEY] = wrappedKey
            it[KEY_SALT_B64] = saltB64
            it[KEY_ITERATIONS] = iterations
        }
    }

    /** Lectura puntual del material de clave; null si nunca se creó o falla I/O. */
    suspend fun getEncryptionKeyMaterial(): EncryptionKeyMaterial? {
        val prefs = safeData.firstOrNull() ?: return null
        val wrapped = prefs[WRAPPED_KEY] ?: return null
        val salt = prefs[KEY_SALT_B64] ?: return null
        val iterations = prefs[KEY_ITERATIONS] ?: return null
        return EncryptionKeyMaterial(wrapped, salt, iterations)
    }

    suspend fun getFileId(): String? =
        safeData.firstOrNull()?.get(DRIVE_FILE_ID)

    suspend fun setFileId(fileId: String?) {
        context.backupDataStore.edit { prefs ->
            if (fileId == null) prefs.remove(DRIVE_FILE_ID) else prefs[DRIVE_FILE_ID] = fileId
        }
    }

    /** Borra TODO el estado local del respaldo (al desconectar la cuenta). */
    suspend fun clearAll() {
        context.backupDataStore.edit { it.clear() }
    }
}
