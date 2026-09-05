package com.jeremy.lumi.data.backup.drive

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Error devuelto por la API de Drive, con su código HTTP para poder decidir. */
class DriveApiException(val httpCode: Int, message: String) : IOException(message)

/** Metadatos del archivo de respaldo tal como vive en el Drive de la usuaria. */
data class DriveBackupFile(
    val id: String,
    val name: String,
    val modifiedTimeMs: Long?,
    val sizeBytes: Long?
)

/** Cuenta de Google conectada (solo para mostrar el correo en Ajustes). */
data class DriveAccountInfo(
    val email: String?,
    val displayName: String?
)

/**
 * Cliente mínimo de la API REST de Google Drive v3, limitado al App Data Folder.
 *
 * Filosofía "cero responsabilidad": el archivo viaja del teléfono al Drive de la
 * usuaria y de vuelta. No hay servidores intermedios de Lumi, y con el scope
 * `drive.appdata` la app NI SIQUIERA puede ver el resto del Drive.
 *
 * Se usa HttpURLConnection a propósito: evita arrastrar el SDK completo de
 * Google API Client (pesado y con conflictos de empaquetado) para 5 endpoints.
 */
@Singleton
class DriveApiClient @Inject constructor() {

    companion object {
        private const val TAG = "DriveApiClient"

        /** Nombre fijo del archivo dentro del App Data Folder. */
        const val BACKUP_FILE_NAME = "lumi_respaldo.lbk"

        private const val API_BASE = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"
        private const val REVOKE_URL = "https://oauth2.googleapis.com/revoke"
        private const val TIMEOUT_MS = 30_000
        private const val BOUNDARY = "lumi_respaldo_multipart"
    }

    /** Consulta a quién pertenece la cuenta conectada (correo y nombre). */
    suspend fun getAccountInfo(token: String): DriveAccountInfo = withContext(Dispatchers.IO) {
        val json = executeForString(
            method = "GET",
            url = "$API_BASE/about?fields=user(displayName,emailAddress)",
            token = token
        )
        val user = JsonParser.parseString(json).asJsonObject.getAsJsonObject("user")
        DriveAccountInfo(
            email = user?.get("emailAddress")?.asString,
            displayName = user?.get("displayName")?.asString
        )
    }

    /** Busca el respaldo dentro del App Data Folder. Devuelve null si no existe. */
    suspend fun findBackupFile(token: String): DriveBackupFile? = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode("name = '$BACKUP_FILE_NAME'", "UTF-8")
        val fields = URLEncoder.encode("files(id,name,modifiedTime,size)", "UTF-8")
        val json = executeForString(
            method = "GET",
            url = "$API_BASE/files?spaces=appDataFolder&q=$q&fields=$fields&pageSize=10",
            token = token
        )
        val files = JsonParser.parseString(json).asJsonObject.getAsJsonArray("files")
        files?.firstOrNull()?.asJsonObject?.let { parseFile(it) }
    }

    /**
     * Sube el respaldo cifrado. Con [existingFileId] actualiza el archivo
     * existente (mantenemos una sola versión); con null lo crea dentro de
     * appDataFolder mediante subida multipart (metadatos + contenido).
     */
    suspend fun uploadBackup(
        token: String,
        bytes: ByteArray,
        existingFileId: String?
    ): DriveBackupFile = withContext(Dispatchers.IO) {
        val json = if (existingFileId == null) {
            val url = "$UPLOAD_BASE/files?uploadType=multipart&fields=id,name,modifiedTime,size"
            val conn = open("POST", url, token)
            conn.setRequestProperty("Content-Type", "multipart/related; boundary=$BOUNDARY")
            conn.doOutput = true
            try {
                conn.outputStream.use { writeMultipart(it, bytes) }
                readResponse(conn)
            } finally {
                conn.disconnect()
            }
        } else {
            // HttpURLConnection no soporta el verbo PATCH de forma nativa;
            // la API de Google acepta el override estándar sobre POST.
            val url = "$UPLOAD_BASE/files/$existingFileId?uploadType=media&fields=id,name,modifiedTime,size"
            val conn = open("POST", url, token)
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            conn.setRequestProperty("Content-Type", "application/octet-stream")
            conn.doOutput = true
            try {
                conn.outputStream.use { it.write(bytes) }
                readResponse(conn)
            } finally {
                conn.disconnect()
            }
        }
        parseFile(JsonParser.parseString(json).asJsonObject)
    }

    /** Descarga el contenido binario del respaldo. */
    suspend fun downloadBackup(token: String, fileId: String): ByteArray =
        withContext(Dispatchers.IO) {
            val conn = open("GET", "$API_BASE/files/$fileId?alt=media", token)
            try {
                val code = conn.responseCode
                if (code in 200..299) {
                    conn.inputStream.use { it.readBytes() }
                } else {
                    throw buildError(conn, code)
                }
            } finally {
                conn.disconnect()
            }
        }

    /** Borra el archivo de respaldo del Drive de la usuaria. */
    suspend fun deleteFile(token: String, fileId: String): Unit = withContext(Dispatchers.IO) {
        executeForString("DELETE", "$API_BASE/files/$fileId", token)
        Unit
    }

    /**
     * Revoca el permiso concedido a la app (al desconectar la cuenta).
     * Mejor esfuerzo: si falla, el token simplemente expira solo en ~1 hora.
     */
    suspend fun revokeAccess(token: String): Unit = withContext(Dispatchers.IO) {
        try {
            val conn = URL(REVOKE_URL).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.doOutput = true
            conn.outputStream.use { it.write("token=$token".toByteArray(Charsets.UTF_8)) }
            conn.responseCode // fuerza la ejecución de la petición
            conn.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo revocar el token (expirará solo): ${e.message}")
        }
    }

    // ── Internos ──────────────────────────────────────────────────────────────

    private fun writeMultipart(out: OutputStream, payload: ByteArray) {
        val metadata = """{"name":"$BACKUP_FILE_NAME","parents":["appDataFolder"]}"""
        // Se escribe a mano: cabecera de metadatos + contenido binario.
        // Ojo con el orden de los flush al mezclar writer y stream crudo.
        val writer = out.bufferedWriter(Charsets.UTF_8)
        writer.write("--$BOUNDARY\r\n")
        writer.write("Content-Type: application/json; charset=UTF-8\r\n\r\n")
        writer.write(metadata)
        writer.write("\r\n--$BOUNDARY\r\n")
        writer.write("Content-Type: application/octet-stream\r\n\r\n")
        writer.flush()
        out.write(payload)
        out.flush()
        writer.write("\r\n--$BOUNDARY--\r\n")
        writer.flush()
    }

    private fun parseFile(obj: JsonObject): DriveBackupFile = DriveBackupFile(
        id = obj.get("id").asString,
        name = obj.get("name")?.asString ?: BACKUP_FILE_NAME,
        modifiedTimeMs = obj.get("modifiedTime")?.asString?.let {
            try {
                Instant.parse(it).toEpochMilli()
            } catch (e: Exception) {
                null
            }
        },
        // Drive devuelve "size" como string JSON
        sizeBytes = obj.get("size")?.asString?.toLongOrNull()
    )

    private fun open(method: String, url: String, token: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        conn.setRequestProperty("Authorization", "Bearer $token")
        return conn
    }

    private fun executeForString(method: String, url: String, token: String): String {
        val conn = open(method, url, token)
        return try {
            readResponse(conn)
        } finally {
            conn.disconnect()
        }
    }

    private fun readResponse(conn: HttpURLConnection): String {
        val code = conn.responseCode
        return if (code in 200..299) {
            conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        } else {
            throw buildError(conn, code)
        }
    }

    private fun buildError(conn: HttpURLConnection, code: Int): DriveApiException {
        val body = try {
            conn.errorStream?.use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (e: Exception) {
            null
        }
        val message = try {
            body?.let {
                JsonParser.parseString(it).asJsonObject
                    .getAsJsonObject("error")?.get("message")?.asString
            }
        } catch (e: Exception) {
            null
        }
        Log.w(TAG, "Error HTTP $code de Drive: ${message ?: body?.take(200)}")
        return DriveApiException(code, message ?: "Error HTTP $code de la API de Drive")
    }
}
