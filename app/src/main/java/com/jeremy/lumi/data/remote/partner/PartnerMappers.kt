package com.jeremy.lumi.data.remote.partner

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.jeremy.lumi.domain.model.CareAction
import com.jeremy.lumi.domain.model.CareActionStatus
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.CycleSnapshot
import com.jeremy.lumi.domain.model.DiaryEntry
import com.jeremy.lumi.domain.model.LinkStatus
import com.jeremy.lumi.domain.model.LinkType
import com.jeremy.lumi.domain.model.PartnerLink

/**
 * Mapeo MANUAL entre Firestore y el dominio.
 *
 * Se evita `toObject()` (reflexión) a propósito: un campo renombrado o un tipo
 * inesperado en un documento antiguo rompería la deserialización completa.
 * Aquí cada campo se lee con tolerancia y valores por defecto, y los nombres
 * de campo son compatibles con los documentos creados por la versión anterior
 * de la app (p. ej. `late` para isLate).
 */
private const val TAG = "PartnerMappers"

private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
    name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

private fun Any?.asLongOrNull(): Long? = (this as? Number)?.toLong()
private fun Any?.asIntOr(default: Int): Int = (this as? Number)?.toInt() ?: default

fun DocumentSnapshot.toPartnerLink(): PartnerLink? {
    if (!exists()) return null
    return try {
        PartnerLink(
            linkId = id,
            ownerUid = getString("ownerUid") ?: return null,
            partnerUid = getString("partnerUid"),
            linkCode = getString("linkCode") ?: "",
            status = enumOrDefault(getString("status"), LinkStatus.PENDING),
            linkType = enumOrDefault(getString("linkType"), LinkType.OBSERVER),
            ownerDisplayName = getString("ownerDisplayName"),
            partnerDisplayName = getString("partnerDisplayName"),
            ownerAvatarId = getString("ownerAvatarId"),
            partnerAvatarId = getString("partnerAvatarId"),
            relationLabel = getString("relationLabel") ?: "",
            ownerFcmToken = getString("ownerFcmToken"),
            partnerFcmToken = getString("partnerFcmToken"),
            ownerSharePhase = getBoolean("ownerSharePhase") ?: true,
            ownerShareMood = getBoolean("ownerShareMood") ?: true,
            ownerShareSymptoms = getBoolean("ownerShareSymptoms") ?: false,
            ownerSharePredictions = getBoolean("ownerSharePredictions") ?: true,
            partnerSharePhase = getBoolean("partnerSharePhase") ?: true,
            partnerShareMood = getBoolean("partnerShareMood") ?: true,
            partnerShareSymptoms = getBoolean("partnerShareSymptoms") ?: false,
            partnerSharePredictions = getBoolean("partnerSharePredictions") ?: true,
            ownerSnapshot = (get("ownerSnapshot") as? Map<*, *>)?.toCycleSnapshot(),
            partnerSnapshot = (get("partnerSnapshot") as? Map<*, *>)?.toCycleSnapshot(),
            lastOwnerCareAction = (get("lastOwnerCareAction") as? Map<*, *>)?.toCareActionStatus(),
            lastPartnerCareAction = (get("lastPartnerCareAction") as? Map<*, *>)?.toCareActionStatus(),
            createdAtMs = getLong("createdAtMs") ?: 0L
        )
    } catch (e: Exception) {
        // Un documento corrupto no debe tumbar la lista completa de vínculos
        Log.w(TAG, "Documento de vínculo ilegible ($id): ${e.message}")
        null
    }
}

fun Map<*, *>.toCycleSnapshot(): CycleSnapshot = CycleSnapshot(
    currentPhase = enumOrDefault(this["currentPhase"] as? String, CyclePhase.UNKNOWN),
    daysUntilNextPhase = this["daysUntilNextPhase"].asIntOr(0),
    isLate = this["late"] as? Boolean ?: false,
    delayDays = this["delayDays"].asIntOr(0),
    currentMood = this["currentMood"] as? String,
    topSymptoms = (this["topSymptoms"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
    cycleStartDateMs = this["cycleStartDateMs"].asLongOrNull(),
    cycleLengthDays = this["cycleLengthDays"].asIntOr(0),
    periodLengthDays = this["periodLengthDays"].asIntOr(0),
    lastUpdatedAt = this["lastUpdatedAt"].asLongOrNull() ?: 0L
)

fun Map<*, *>.toCareActionStatus(): CareActionStatus? {
    val action = enumOrDefault(this["action"] as? String, CareAction.HUG)
    val timestamp = this["timestamp"].asLongOrNull() ?: return null
    val message = this["message"] as? String
    return CareActionStatus(action, timestamp, message)
}

fun CycleSnapshot.toFirestoreMap(): Map<String, Any?> = mapOf(
    "currentPhase" to currentPhase.name,
    "daysUntilNextPhase" to daysUntilNextPhase,
    "late" to isLate,   // nombre heredado de la versión anterior — no cambiar
    "delayDays" to delayDays,
    "currentMood" to currentMood,
    "topSymptoms" to topSymptoms,
    "cycleStartDateMs" to cycleStartDateMs,
    "cycleLengthDays" to cycleLengthDays,
    "periodLengthDays" to periodLengthDays,
    "lastUpdatedAt" to lastUpdatedAt
)

fun CareActionStatus.toFirestoreMap(): Map<String, Any?> = buildMap {
    put("action", action.name)
    put("timestamp", timestamp)
    if (!message.isNullOrBlank()) {
        put("message", message)
    }
}

fun DocumentSnapshot.toDiaryEntry(): DiaryEntry? {
    return try {
        DiaryEntry(
            id = id,
            authorUid = getString("authorUid") ?: return null,
            authorName = getString("authorName") ?: "",
            text = getString("text") ?: return null,
            phase = enumOrDefault(getString("phase"), CyclePhase.UNKNOWN),
            timestampMs = getLong("timestampMs") ?: 0L
        )
    } catch (e: Exception) {
        Log.w(TAG, "Entrada de diario ilegible ($id): ${e.message}")
        null
    }
}

fun DiaryEntry.toFirestoreMap(): Map<String, Any?> = mapOf(
    "authorUid" to authorUid,
    "authorName" to authorName,
    "text" to text,
    "phase" to phase.name,
    "timestampMs" to timestampMs
)
