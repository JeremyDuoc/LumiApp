package com.jeremy.lumi.data.backup

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.jeremy.lumi.domain.model.SecondaryGoal
import com.jeremy.lumi.domain.model.UserGoal

/**
 * Portable, allowlisted health profile. NEVER copy the raw lumi_prefs DataStore
 * because it also contains device state, consents and partner-sharing preferences.
 * Stored only INSIDE an encrypted backup (or in legacy explicitly unencrypted ZIPs).
 *
 * Nullable members are intentional: Gson can bypass Kotlin constructors.
 */
data class ProfileBackup(
    val formatVersion: Int? = 1,
    val userName: String? = null,
    val isRegular: Boolean? = null,
    val userGoal: String? = null,
    val defaultCycleLength: Int? = null,
    val defaultPeriodLength: Int? = null,
    val isPregnant: Boolean? = null,
    val isOnContraceptive: Boolean? = null,
    val userAge: Int? = null,
    val userHeightCm: Float? = null,
    val userWeightKg: Float? = null,
    val secondaryGoals: List<String>? = null
)

object ProfileBackupCodec {
    private const val FORMAT_VERSION = 1
    const val MAX_JSON_BYTES = 16 * 1024
    private val gson = GsonBuilder().disableHtmlEscaping().create()

    fun toJson(profile: ProfileBackup): String {
        val valid = validate(profile)
        return gson.toJson(valid)
    }

    fun fromJson(json: String): ProfileBackup {
        if (json.toByteArray(Charsets.UTF_8).size > MAX_JSON_BYTES) {
            throw CorruptBackupException("Perfil del respaldo demasiado grande")
        }
        val parsed = try {
            gson.fromJson(json, ProfileBackup::class.java)
        } catch (e: JsonSyntaxException) {
            throw CorruptBackupException("Perfil del respaldo ilegible")
        } ?: throw CorruptBackupException("Perfil de respaldo vacío")
        return validate(parsed)
    }

    /** Enforce strict bounds before touching DataStore or the Room database. */
    fun validate(profile: ProfileBackup): ProfileBackup {
        val version = profile.formatVersion ?: throw CorruptBackupException("Perfil sin versión")
        if (version > FORMAT_VERSION) throw BackupVersionTooNewException()
        if (version != FORMAT_VERSION) throw CorruptBackupException("Versión de perfil inválida")
        if (profile.userName != null && (profile.userName.length > 80 || profile.userName.any { it.isISOControl() })) {
            throw CorruptBackupException("Apodo inválido en el respaldo")
        }
        if (profile.defaultCycleLength?.let { it in 15..120 } != true ||
            profile.defaultPeriodLength?.let { it in 1..20 } != true ||
            profile.isPregnant == null || profile.isOnContraceptive == null) {
            throw CorruptBackupException("Parámetros del perfil faltantes o inválidos")
        }
        val goal = profile.userGoal ?: throw CorruptBackupException("Falta objetivo en perfil")
        if (runCatching { UserGoal.valueOf(goal) }.isFailure) {
            throw CorruptBackupException("Objetivo desconocido en perfil")
        }
        if (profile.userAge != null && profile.userAge !in 10..110) {
            throw CorruptBackupException("Edad de perfil inválida")
        }
        if (profile.userHeightCm != null &&
            (!profile.userHeightCm.isFinite() || profile.userHeightCm !in 100f..250f)) {
            throw CorruptBackupException("Estatura de perfil inválida")
        }
        if (profile.userWeightKg != null &&
            (!profile.userWeightKg.isFinite() || profile.userWeightKg !in 20f..400f)) {
            throw CorruptBackupException("Peso de perfil inválido")
        }
        val goals = profile.secondaryGoals ?: emptyList()
        if (goals.size > SecondaryGoal.entries.size || goals.any {
                runCatching { SecondaryGoal.valueOf(it) }.isFailure
            }) {
            throw CorruptBackupException("Objetivos secundarios inválidos")
        }
        return profile.copy(
            userName = profile.userName?.trim()?.takeIf { it.isNotEmpty() },
            secondaryGoals = goals.distinct()
        )
    }
}
