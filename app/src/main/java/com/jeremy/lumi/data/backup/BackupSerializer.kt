package com.jeremy.lumi.data.backup

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.data.local.entity.SymptomEntity

/** El respaldo fue creado por una versión de Lumi más nueva que la instalada. */
class BackupVersionTooNewException :
    Exception("El respaldo proviene de una versión más nueva de la app")

// DTOs con TODOS los campos anulables a propósito: Gson ignora los valores por
// defecto de Kotlin al deserializar (instancia por reflexión sin constructor),
// así que un campo ausente en el JSON dejaría un tipo no-nulo en null y
// explotaría después. Validamos y aplicamos los defaults nosotros en el mapeo
// explícito hacia las entidades de Room.

data class BackupPayloadDto(
    val formatVersion: Int? = null,
    val dbSchemaVersion: Int? = null,
    val createdAtMs: Long? = null,
    val cycles: List<CycleDto?>? = null,
    val dailyLogs: List<DailyLogDto?>? = null,
    val symptoms: List<SymptomDto?>? = null,
    // Introducido de forma opcional en el formato v1 para compatibilidad.
    val profile: ProfileBackup? = null
)

data class CycleDto(
    val id: Int? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val predictedOvulationDate: Long? = null,
    val cycleLength: Int? = null,
    val periodLength: Int? = null
)

data class DailyLogDto(
    val id: Int? = null,
    val cycleId: Int? = null,
    val date: Long? = null,
    val flowIntensity: String? = null,
    val painLevel: Int? = null,
    val spotting: Boolean? = null,
    val mood: String? = null,
    val energyLevel: Int? = null,
    val sleepHours: Float? = null,
    val stressLevel: Int? = null,
    val waterIntake: Float? = null,
    val basalBodyTemp: Float? = null,
    val cervicalMucus: String? = null,
    val notes: String? = null,
    val hadIntercourse: Boolean? = null,
    val protectionUsed: Boolean? = null,
    val contraceptionMethod: String? = null,
    val intercourseNotes: String? = null,
    val showIntercourseOnCalendar: Boolean? = null
)

data class SymptomDto(
    val id: Int? = null,
    val dailyLogId: Int? = null,
    val name: String? = null,
    val intensity: Int? = null
)

/** Datos ya validados y convertidos a entidades de Room, listos para insertar. */
class RestoredData(
    val cycles: List<CycleEntity>,
    val dailyLogs: List<DailyLogEntity>,
    val symptoms: List<SymptomEntity>,
    val createdAtMs: Long?,
    val profile: ProfileBackup? = null
)

/**
 * Serialización del respaldo a JSON (lo que luego se comprime y se cifra).
 * Incluye ciclos, registros diarios, síntomas y (en copias nuevas) perfil menstrual.
 * Recordatorios y chat son locales del dispositivo y quedan fuera a propósito.
 */
object BackupSerializer {

    /** Versión del FORMATO del JSON (independiente del esquema de Room). */
    const val FORMAT_VERSION = 1

    /** Debe mantenerse sincronizada con la versión declarada en LumiDatabase. */
    const val DB_SCHEMA_VERSION = 9

    private val gson = GsonBuilder().disableHtmlEscaping().create()

    /** Serializa la instantánea local al JSON del respaldo. */
    fun toJson(
        cycles: List<CycleEntity>,
        logs: List<DailyLogEntity>,
        symptoms: List<SymptomEntity>,
        profile: ProfileBackup? = null
    ): String {
        val payload = BackupPayloadDto(
            formatVersion = FORMAT_VERSION,
            dbSchemaVersion = DB_SCHEMA_VERSION,
            createdAtMs = System.currentTimeMillis(),
            profile = profile?.let(ProfileBackupCodec::validate),
            cycles = cycles.map {
                CycleDto(
                    id = it.id,
                    startDate = it.startDate,
                    endDate = it.endDate,
                    predictedOvulationDate = it.predictedOvulationDate,
                    cycleLength = it.cycleLength,
                    periodLength = it.periodLength
                )
            },
            dailyLogs = logs.map {
                DailyLogDto(
                    id = it.id,
                    cycleId = it.cycleId,
                    date = it.date,
                    flowIntensity = it.flowIntensity,
                    painLevel = it.painLevel,
                    spotting = it.spotting,
                    mood = it.mood,
                    energyLevel = it.energyLevel,
                    sleepHours = it.sleepHours,
                    stressLevel = it.stressLevel,
                    waterIntake = it.waterIntake,
                    basalBodyTemp = it.basalBodyTemp,
                    cervicalMucus = it.cervicalMucus,
                    notes = it.notes,
                    hadIntercourse = it.hadIntercourse,
                    protectionUsed = it.protectionUsed,
                    contraceptionMethod = it.contraceptionMethod,
                    intercourseNotes = it.intercourseNotes,
                    showIntercourseOnCalendar = it.showIntercourseOnCalendar
                )
            },
            symptoms = symptoms.map {
                SymptomDto(
                    id = it.id,
                    dailyLogId = it.dailyLogId,
                    name = it.name,
                    intensity = it.intensity
                )
            }
        )
        return gson.toJson(payload)
    }

    /**
     * Parsea y valida el JSON del respaldo.
     * - Rechaza formatos de versiones futuras de la app.
     * - Rechaza filas incompletas, duplicadas o huérfanas ANTES de reemplazar datos.
     *   Una restauración parcial nunca debe borrar silenciosamente el historial.
     */
    fun fromJson(json: String): RestoredData {
        val dto = try {
            gson.fromJson(json, BackupPayloadDto::class.java)
        } catch (e: Exception) {
            when (e) {
                is JsonSyntaxException, is java.lang.IllegalStateException ->
                    throw CorruptBackupException("El contenido del respaldo no es JSON válido")
                else -> throw e
            }
        } ?: throw CorruptBackupException("El respaldo está vacío")

        val format = dto.formatVersion
            ?: throw CorruptBackupException("El respaldo no declara versión de formato")
        if (format > FORMAT_VERSION) throw BackupVersionTooNewException()
        if (format != FORMAT_VERSION) throw CorruptBackupException("Versión de formato inválida")
        val schema = dto.dbSchemaVersion ?: throw CorruptBackupException("Falta la versión del esquema")
        if (schema > DB_SCHEMA_VERSION) throw BackupVersionTooNewException()
        if (schema < 1) throw CorruptBackupException("Versión de esquema inválida")

        val cycles = (dto.cycles ?: throw CorruptBackupException("Faltan los ciclos")).map { c ->
            val id = c?.id?.takeIf { it > 0 } ?: throw CorruptBackupException("Ciclo sin identificador válido")
            val start = c.startDate ?: throw CorruptBackupException("Ciclo sin fecha")
            if (c.endDate != null && c.endDate < start) throw CorruptBackupException("Fechas de ciclo inválidas")
            CycleEntity(
                id = id,
                startDate = start,
                endDate = c.endDate,
                predictedOvulationDate = c.predictedOvulationDate,
                cycleLength = c.cycleLength?.takeIf { it in 15..120 } ?: 28,
                periodLength = c.periodLength?.takeIf { it in 1..20 } ?: 5
            )
        }

        val logs = (dto.dailyLogs ?: throw CorruptBackupException("Faltan los registros diarios")).map { l ->
            val id = l?.id?.takeIf { it > 0 } ?: throw CorruptBackupException("Registro sin identificador válido")
            val date = l.date ?: throw CorruptBackupException("Registro sin fecha")
            DailyLogEntity(
                id = id,
                cycleId = l.cycleId ?: 0,
                date = date,
                flowIntensity = l.flowIntensity,
                painLevel = l.painLevel?.coerceIn(0, 10) ?: 0,
                spotting = l.spotting ?: false,
                mood = l.mood,
                energyLevel = l.energyLevel,
                sleepHours = l.sleepHours,
                stressLevel = l.stressLevel,
                waterIntake = l.waterIntake,
                basalBodyTemp = l.basalBodyTemp,
                cervicalMucus = l.cervicalMucus,
                notes = l.notes,
                hadIntercourse = l.hadIntercourse ?: false,
                protectionUsed = l.protectionUsed,
                contraceptionMethod = l.contraceptionMethod,
                intercourseNotes = l.intercourseNotes,
                showIntercourseOnCalendar = l.showIntercourseOnCalendar ?: true
            )
        }

        val validLogIds = logs.mapTo(HashSet()) { it.id }
        val validCycleIds = cycles.mapTo(HashSet()) { it.id }
        if (validCycleIds.size != cycles.size || validLogIds.size != logs.size ||
            logs.map { it.date }.toSet().size != logs.size ||
            logs.any { it.cycleId != 0 && it.cycleId !in validCycleIds }) {
            throw CorruptBackupException("Registros duplicados o sin ciclo válido")
        }
        val symptoms = (dto.symptoms ?: throw CorruptBackupException("Faltan los síntomas")).map { s ->
            val logId = s?.dailyLogId ?: throw CorruptBackupException("Síntoma sin registro diario")
            val name = s.name ?: throw CorruptBackupException("Síntoma sin nombre")
            val id = s.id?.takeIf { it > 0 } ?: throw CorruptBackupException("Síntoma sin identificador válido")
            if (logId !in validLogIds) throw CorruptBackupException("Síntoma huérfano")
            SymptomEntity(
                id = id,
                dailyLogId = logId,
                name = name,
                intensity = s.intensity?.coerceIn(0, 5) ?: 0
            )
        }
        if (symptoms.map { it.id }.toSet().size != symptoms.size) {
            throw CorruptBackupException("Síntomas duplicados")
        }

        // Malformed optional profile must fail before replacing ANY health data.
        val profile = dto.profile?.let(ProfileBackupCodec::validate)
        return RestoredData(cycles, logs, symptoms, dto.createdAtMs, profile)
    }
}
