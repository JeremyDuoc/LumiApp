package com.jeremy.lumi.data.backup.drive

import android.content.Context
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Resultado de pedir acceso al App Data Folder del Drive de la usuaria. */
sealed interface DriveAuthResult {
    /** Ya hay permiso: token OAuth listo para llamar a la API REST (~1 h de vida). */
    data class Authorized(val accessToken: String) : DriveAuthResult

    /** Google necesita mostrar su pantalla de consentimiento/selector de cuenta. */
    data class ConsentRequired(val intentSenderRequest: IntentSenderRequest) : DriveAuthResult

    /** Falló la autorización (sin Play Services, cancelación, red, etc.). */
    data class Failed(val cause: Exception) : DriveAuthResult
}

/**
 * Autorización con Google mediante Identity/AuthorizationClient.
 *
 * Pedimos ÚNICAMENTE el scope `drive.appdata`: la app solo puede tocar su
 * propia carpeta oculta dentro del Drive de la usuaria. No puede listar ni
 * leer archivos, fotos ni documentos personales — pilar del diseño
 * "cero responsabilidad".
 *
 * Si el permiso ya fue concedido antes, `authorize()` resuelve en silencio
 * (sin UI), lo que permite el respaldo automático en segundo plano.
 */
@Singleton
class DriveAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val SCOPE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
    }

    /**
     * Pide autorización. Silenciosa si ya fue concedida; en caso contrario
     * devuelve [DriveAuthResult.ConsentRequired] con el intent de Google
     * para lanzar desde la UI.
     */
    suspend fun requestAuthorization(): DriveAuthResult {
        return try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(SCOPE_APPDATA)))
                .build()
            val result = Identity.getAuthorizationClient(context)
                .authorize(request)
                .await()
            toAuthResult(result)
        } catch (e: Exception) {
            DriveAuthResult.Failed(e)
        }
    }

    /** Procesa el Intent devuelto por la pantalla de consentimiento de Google. */
    fun handleConsentResult(intent: Intent?): DriveAuthResult {
        return try {
            val result = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(intent)
            toAuthResult(result)
        } catch (e: ApiException) {
            // Incluye el caso "la usuaria cerró la pantalla de Google"
            DriveAuthResult.Failed(e)
        }
    }

    private fun toAuthResult(result: AuthorizationResult): DriveAuthResult {
        val pendingIntent = result.pendingIntent
        val token = result.accessToken
        return when {
            result.hasResolution() && pendingIntent != null ->
                DriveAuthResult.ConsentRequired(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )

            token != null -> DriveAuthResult.Authorized(token)

            else -> DriveAuthResult.Failed(
                IllegalStateException("Google no devolvió ni token ni pantalla de consentimiento")
            )
        }
    }
}
