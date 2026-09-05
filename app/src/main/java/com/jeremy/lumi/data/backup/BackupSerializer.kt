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
    val cycles: List<CycleDto>? = null,
    val dailyLogs: List<DailyLogDto>? = null,
    val symptoms: List<SymptomDto>? = null
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
    val createdAtMs: Long?
)

/**
 * Serialización del respaldo a JSON (lo que luego se comprime y se cifra).
 * Solo incluye datos de salud: ciclos, registros diarios y síntomas.
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
        symptoms: List<SymptomEntity>
    ): String {
        val payload = BackupPayloadDto(
            formatVersion = FORMAT_VERSION,
            dbSchemaVersion = DB_SCHEMA_VERSION,
            createdAtMs = System.currentTimeMillis(),
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
     * - Descarta filas sin campos esenciales (id/fecha) en lugar de romper toda
     *   la restauración por una fila dañada.
     * - Descarta síntomas huérfanos cuyo día no vino en el respaldo.
     */
    fun fromJson(json: String): RestoredData {
        val dto = try {
            gson.fromJson(json, BackupPayloadDto::class.java)
        } catch (e: JsonSyntaxException) {
            throw CorruptBackupException("El contenido del respaldo no es JSON válido")
        } ?: throw CorruptBackupException("El respaldo está vacío")

        val format = dto.formatVersion
            ?: throw CorruptBackupException("El respaldo no declara versión de formato")
        if (format > FORMAT_VERSION) throw BackupVersionTooNewException()

        val cycles = dto.cycles.orEmpty().mapNotNull { c ->
            val id = c.id ?: return@mapNotNull null
            val start = c.startDate ?: return@mapNotNull null
            CycleEntity(
                id = id,
                startDate = start,
                endDate = c.endDate,
                predictedOvulationDate = c.predictedOvulationDate,
                cycleLength = c.cycleLength ?: 28,
                periodLength = c.periodLength ?: 5
            )
        }

        val logs = dto.dailyLogs.orEmpty().mapNotNull { l ->
            val id = l.id ?: return@mapNotNull null
            val date = l.date ?: return@mapNotNull null
            DailyLogEntity(
                id = id,
                cycleId = l.cycleId ?: 0,
                date = date,
                flowIntensity = l.flowIntensity,
                painLevel = l.painLevel ?: 0,
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
        val symptoms = dto.symptoms.orEmpty().mapNotNull { s ->
            val logId = s.dailyLogId ?: return@mapNotNull null
            val name = s.name ?: return@mapNotNull null
            if (logId !in validLogIds) return@mapNotNull null
            SymptomEntity(
                id = s.id ?: 0,
                dailyLogId = logId,
                name = name,
                intensity = s.intensity ?: 0
            )
        }

        return RestoredData(cycles, logs, symptoms, dto.createdAtMs)
    }
}
