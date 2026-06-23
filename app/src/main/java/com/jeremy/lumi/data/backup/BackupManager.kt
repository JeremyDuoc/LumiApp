package com.jeremy.lumi.data.backup

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Gestiona la exportación e importación de la base de datos de Room
 * comprimiéndola en un archivo ZIP para que la usuaria sea dueña de sus datos.
 */
class BackupManager(private val context: Context) {

    private val dbName = "lumi_db"
    
    // Room utiliza 3 archivos en modo WAL. Hay que respaldar los 3.
    private val dbFiles = listOf(
        dbName,
        "$dbName-shm",
        "$dbName-wal"
    )

    /**
     * Comprime los archivos de la base de datos y los escribe en el [outputStream]
     * (típicamente provisto por Intent.ACTION_CREATE_DOCUMENT).
     */
    suspend fun exportDatabaseToZip(outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Es buena práctica hacer checkpoint antes de exportar, pero Room lo hace 
            // automáticamente al cerrarse o podemos simplemente copiar el WAL también.
            // Copiando los 3 archivos es seguro.

            ZipOutputStream(outputStream).use { zos ->
                for (fileName in dbFiles) {
                    val dbFile = context.getDatabasePath(fileName)
                    if (dbFile.exists()) {
                        FileInputStream(dbFile).use { fis ->
                            val zipEntry = ZipEntry(fileName)
                            zos.putNextEntry(zipEntry)
                            fis.copyTo(zos)
                            zos.closeEntry()
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Lee un ZIP desde el [inputStream] (provisto por Intent.ACTION_OPEN_DOCUMENT)
     * y sobrescribe los archivos actuales de la base de datos.
     * ADVERTENCIA: La base de datos debe estar cerrada antes de hacer esto, 
     * o la app debe reiniciarse inmediatamente después.
     */
    suspend fun importDatabaseFromZip(inputStream: InputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    // Validar que el archivo dentro del zip sea uno de los nuestros
                    if (dbFiles.contains(entry.name)) {
                        val targetFile = context.getDatabasePath(entry.name)
                        // Asegurar que el directorio databases existe
                        targetFile.parentFile?.mkdirs()
                        
                        FileOutputStream(targetFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
