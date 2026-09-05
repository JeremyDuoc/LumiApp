package com.jeremy.lumi.ui.screens.settings.backup

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.R
import com.jeremy.lumi.data.backup.BackupVersionTooNewException
import com.jeremy.lumi.data.backup.CorruptBackupException
import com.jeremy.lumi.data.backup.DriveBackupManager
import com.jeremy.lumi.data.backup.WrongPassphraseException
import com.jeremy.lumi.data.backup.drive.DriveApiClient
import com.jeremy.lumi.data.backup.drive.DriveApiException
import com.jeremy.lumi.data.backup.drive.DriveAuthManager
import com.jeremy.lumi.data.backup.drive.DriveAuthResult
import com.jeremy.lumi.data.backup.drive.DriveBackupFile
import com.jeremy.lumi.data.preferences.BackupPreferenceManager
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import com.jeremy.lumi.data.worker.AutoBackupWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/** Estado de la operación en curso, para pintar banners sin bloquear la UI. */
sealed interface BackupOperation {
    data object Idle : BackupOperation
    data class Running(@StringRes val messageRes: Int) : BackupOperation
    data class Success(@StringRes val messageRes: Int) : BackupOperation
    data class Error(@StringRes val messageRes: Int, val detail: String? = null) : BackupOperation
}

/** Diálogos modales de la pantalla de respaldo (solo uno activo a la vez). */
sealed interface BackupDialog {
    /** Primera vez: crear la contraseña de cifrado. */
    data object CreatePassphrase : BackupDialog

    /** Restaurar sin clave local (teléfono nuevo) o con clave incorrecta. */
    data object AskPassphraseForRestore : BackupDialog

    /** Confirmación antes de reemplazar los datos locales. */
    data object ConfirmRestore : BackupDialog

    /** Confirmación antes de desconectar la cuenta. */
    data object ConfirmDisconnect : BackupDialog

    /** Confirmación antes de borrar el archivo remoto. */
    data object ConfirmDeleteRemote : BackupDialog

    /** Restauración exitosa: la app debe reiniciarse para recargar todo. */
    data object RestartAfterRestore : BackupDialog
}

data class BackupUiState(
    val isConnected: Boolean = false,
    val accountEmail: String? = null,
    val remoteBackup: DriveBackupFile? = null,
    val autoBackupEnabled: Boolean = false,
    val hasLocalKey: Boolean = false,
    val isLoadingInfo: Boolean = false,
    val operation: BackupOperation = BackupOperation.Idle,
    val activeDialog: BackupDialog? = null
)

/** Eventos de un solo disparo hacia la pantalla. */
sealed interface BackupEvent {
    /** Lanzar la pantalla de consentimiento de Google. */
    data class LaunchConsent(val request: IntentSenderRequest) : BackupEvent
}

/**
 * ViewModel de la pantalla de respaldo privado en Google Drive.
 *
 * Toda operación de red corre en viewModelScope y se comunica por estado
 * observable: la UI nunca se bloquea. El baile del consentimiento de Google
 * se resuelve con [pendingAfterConsent]: la acción interrumpida se reanuda
 * sola cuando la usuaria acepta.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val authManager: DriveAuthManager,
    private val backupManager: DriveBackupManager,
    private val api: DriveApiClient,
    private val prefs: BackupPreferenceManager,
    private val onboardingPrefs: OnboardingPreferenceManager
) : ViewModel() {

    companion object {
        private const val TAG = "BackupViewModel"
    }

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BackupEvent>()
    val events: SharedFlow<BackupEvent> = _events.asSharedFlow()

    /** Token vigente en memoria (Google los emite por ~1 h y renueva en silencio). */
    private var accessToken: String? = null

    /** Acción a reanudar cuando la usuaria acepte la pantalla de Google. */
    private var pendingAfterConsent: (suspend () -> Unit)? = null

    init {
        viewModelScope.launch {
            val email = prefs.accountEmailFlow.first()
            val auto = prefs.autoBackupFlow.first()
            val hasKey = backupManager.hasLocalKey()
            _uiState.update {
                it.copy(
                    isConnected = email != null,
                    accountEmail = email,
                    autoBackupEnabled = auto,
                    hasLocalKey = hasKey
                )
            }
            if (email != null) refreshRemoteInfoSilently()
        }
    }

    // ── Conexión ─────────────────────────────────────────────────────────────

    /** Conecta con Google Drive (puede abrir la pantalla de consentimiento). */
    fun connect() {
        viewModelScope.launch {
            setRunning(R.string.backup_status_connecting)
            val token = obtainToken(interactive = true, resume = { finishConnection() }) ?: return@launch
            finishConnection(token)
        }
    }

    private suspend fun finishConnection(tokenOverride: String? = null) {
        try {
            setRunning(R.string.backup_status_connecting)
            val token = tokenOverride ?: accessToken ?: return
            val info = api.getAccountInfo(token)
            prefs.setAccountEmail(info.email)
            val remote = backupManager.fetchRemoteInfo(token)
            _uiState.update {
                it.copy(
                    isConnected = true,
                    accountEmail = info.email,
                    remoteBackup = remote,
                    hasLocalKey = backupManager.hasLocalKey(),
                    operation = BackupOperation.Idle
                )
            }
        } catch (e: Exception) {
            setError(e)
        }
    }

    /** Resultado de la pantalla de consentimiento de Google. */
    fun onConsentResult(intent: Intent?) {
        viewModelScope.launch {
            when (val result = authManager.handleConsentResult(intent)) {
                is DriveAuthResult.Authorized -> {
                    accessToken = result.accessToken
                    val action = pendingAfterConsent
                    pendingAfterConsent = null
                    if (action != null) action() else finishConnection(result.accessToken)
                }

                is DriveAuthResult.Failed -> {
                    // Normalmente la usuaria cerró la pantalla de Google:
                    // volvemos a reposo sin mostrar un error alarmante.
                    Log.i(TAG, "Consentimiento cancelado o fallido: ${result.cause.message}")
                    pendingAfterConsent = null
                    _uiState.update { it.copy(operation = BackupOperation.Idle) }
                }

                is DriveAuthResult.ConsentRequired -> {
                    // No debería encadenarse un segundo consentimiento; tratamos como cancelación
                    pendingAfterConsent = null
                    _uiState.update { it.copy(operation = BackupOperation.Idle) }
                }
            }
        }
    }

    // ── Respaldar ────────────────────────────────────────────────────────────

    /** Crea un respaldo ahora. Si aún no hay clave de cifrado, pide crearla. */
    fun backupNow() {
        viewModelScope.launch {
            if (!backupManager.hasLocalKey()) {
                _uiState.update { it.copy(activeDialog = BackupDialog.CreatePassphrase) }
                return@launch
            }
            runBackup(newPassphrase = null)
        }
    }

    /** Callback del diálogo de creación de contraseña. */
    fun onPassphraseCreated(passphrase: String) {
        _uiState.update { it.copy(activeDialog = null) }
        viewModelScope.launch { runBackup(newPassphrase = passphrase.toCharArray()) }
    }

    private suspend fun runBackup(newPassphrase: CharArray?) {
        setRunning(R.string.backup_status_preparing)
        try {
            val token = obtainToken(interactive = true, resume = { runBackup(newPassphrase) })
                ?: return
            val key = if (newPassphrase != null) {
                backupManager.createAndCacheKey(newPassphrase)
            } else {
                backupManager.getCachedKey()
            }
            if (key == null) {
                // El Keystore invalidó la clave cacheada: hay que crear una nueva
                _uiState.update {
                    it.copy(operation = BackupOperation.Idle, activeDialog = BackupDialog.CreatePassphrase)
                }
                return
            }
            setRunning(R.string.backup_status_uploading)
            val uploaded = backupManager.createBackup(token, key)
            newPassphrase?.fill(' ')
            _uiState.update {
                it.copy(
                    remoteBackup = uploaded,
                    hasLocalKey = true,
                    operation = BackupOperation.Success(R.string.backup_success_created)
                )
            }
        } catch (e: Exception) {
            setError(e)
        }
    }

    // ── Restaurar ────────────────────────────────────────────────────────────

    fun requestRestore() {
        _uiState.update { it.copy(activeDialog = BackupDialog.ConfirmRestore) }
    }

    fun confirmRestore() {
        _uiState.update { it.copy(activeDialog = null) }
        viewModelScope.launch { runRestore(passphrase = null) }
    }

    /** Callback del diálogo que pide la contraseña para restaurar. */
    fun onRestorePassphraseEntered(passphrase: String) {
        _uiState.update { it.copy(activeDialog = null) }
        viewModelScope.launch { runRestore(passphrase.toCharArray()) }
    }

    private suspend fun runRestore(passphrase: CharArray?) {
        setRunning(R.string.backup_status_downloading)
        try {
            val token = obtainToken(interactive = true, resume = { runRestore(passphrase) })
                ?: return
            setRunning(R.string.backup_status_restoring)
            backupManager.restoreBackup(token, passphrase)
            passphrase?.fill(' ')
            // Si venimos de una instalación limpia, saltamos el onboarding:
            // la usuaria ya tiene sus datos reales restaurados.
            onboardingPrefs.markOnboardingCompleted()
            _uiState.update {
                it.copy(
                    hasLocalKey = true,
                    operation = BackupOperation.Idle,
                    activeDialog = BackupDialog.RestartAfterRestore
                )
            }
        } catch (e: DriveBackupManager.PassphraseRequiredException) {
            _uiState.update {
                it.copy(operation = BackupOperation.Idle, activeDialog = BackupDialog.AskPassphraseForRestore)
            }
        } catch (e: WrongPassphraseException) {
            _uiState.update {
                it.copy(
                    operation = BackupOperation.Error(R.string.backup_error_wrong_pass),
                    activeDialog = BackupDialog.AskPassphraseForRestore
                )
            }
        } catch (e: Exception) {
            setError(e)
        }
    }

    /** Reinicio limpio tras restaurar — mismo patrón que el reset de Ajustes. */
    fun restartApp() {
        val intent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        if (intent != null) {
            appContext.startActivity(intent)
        }
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    // ── Respaldo automático ──────────────────────────────────────────────────

    fun setAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setAutoBackup(enabled)
            if (enabled) AutoBackupWorker.schedule(appContext) else AutoBackupWorker.cancel(appContext)
            _uiState.update { it.copy(autoBackupEnabled = enabled) }
        }
    }

    // ── Desconectar / eliminar ───────────────────────────────────────────────

    fun requestDisconnect() {
        _uiState.update { it.copy(activeDialog = BackupDialog.ConfirmDisconnect) }
    }

    fun confirmDisconnect() {
        _uiState.update { it.copy(activeDialog = null) }
        viewModelScope.launch {
            setRunning(R.string.backup_status_disconnecting)
            try {
                accessToken?.let { api.revokeAccess(it) }
                backupManager.clearLocalState()
                AutoBackupWorker.cancel(appContext)
                accessToken = null
                // Estado virgen: como si nunca se hubiera conectado
                _uiState.value = BackupUiState()
            } catch (e: Exception) {
                setError(e)
            }
        }
    }

    fun requestDeleteRemote() {
        _uiState.update { it.copy(activeDialog = BackupDialog.ConfirmDeleteRemote) }
    }

    fun confirmDeleteRemote() {
        _uiState.update { it.copy(activeDialog = null) }
        viewModelScope.launch { runDeleteRemote() }
    }

    private suspend fun runDeleteRemote() {
        setRunning(R.string.backup_status_deleting)
        try {
            val token = obtainToken(interactive = true, resume = { runDeleteRemote() }) ?: return
            backupManager.deleteRemoteBackup(token)
            _uiState.update {
                it.copy(
                    remoteBackup = null,
                    operation = BackupOperation.Success(R.string.backup_success_deleted)
                )
            }
        } catch (e: Exception) {
            setError(e)
        }
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    fun dismissDialog() {
        _uiState.update { it.copy(activeDialog = null) }
    }

    fun clearOperation() {
        _uiState.update { it.copy(operation = BackupOperation.Idle) }
    }

    /**
     * Consigue un token de acceso. Si Google exige consentimiento e
     * [interactive] es true, lanza la pantalla de Google, deja [resume] en
     * espera y devuelve null (la operación continuará sola tras aceptar).
     */
    private suspend fun obtainToken(
        interactive: Boolean,
        resume: (suspend () -> Unit)? = null
    ): String? {
        return when (val result = authManager.requestAuthorization()) {
            is DriveAuthResult.Authorized -> {
                accessToken = result.accessToken
                result.accessToken
            }

            is DriveAuthResult.ConsentRequired -> {
                if (interactive) {
                    pendingAfterConsent = resume
                    // La pantalla de Google es el feedback: no dejamos un spinner colgado
                    _uiState.update { it.copy(operation = BackupOperation.Idle) }
                    _events.emit(BackupEvent.LaunchConsent(result.intentSenderRequest))
                }
                null
            }

            is DriveAuthResult.Failed -> {
                if (interactive) setError(result.cause)
                null
            }
        }
    }

    private suspend fun refreshRemoteInfoSilently() {
        _uiState.update { it.copy(isLoadingInfo = true) }
        val token = obtainToken(interactive = false)
        if (token != null) {
            try {
                val remote = backupManager.fetchRemoteInfo(token)
                _uiState.update { it.copy(remoteBackup = remote) }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo leer la info del respaldo remoto: ${e.message}")
            }
        }
        _uiState.update { it.copy(isLoadingInfo = false) }
    }

    private fun setRunning(@StringRes messageRes: Int) {
        _uiState.update { it.copy(operation = BackupOperation.Running(messageRes)) }
    }

    private fun setError(e: Throwable) {
        Log.w(TAG, "Operación de respaldo fallida", e)
        val operation = when (e) {
            is DriveBackupManager.NoBackupFoundException ->
                BackupOperation.Error(R.string.backup_error_no_backup)

            is BackupVersionTooNewException ->
                BackupOperation.Error(R.string.backup_error_newer_version)

            is CorruptBackupException ->
                BackupOperation.Error(R.string.backup_error_corrupt)

            // Ojo: DriveApiException ES un IOException — debe evaluarse antes
            is DriveApiException -> when (e.httpCode) {
                401, 403 -> BackupOperation.Error(R.string.backup_error_auth, e.message)
                else -> BackupOperation.Error(R.string.backup_error_generic, e.message)
            }

            is IOException ->
                BackupOperation.Error(R.string.backup_error_network)

            else ->
                BackupOperation.Error(R.string.backup_error_generic, e.message)
        }
        _uiState.update { it.copy(operation = operation) }
    }
}
