package com.jeremy.lumi.domain.model

/**
 * ══════════════════════════════════════════════════════════════════════════
 *  MODELO DE DOMINIO DEL MODO PAREJA (v2)
 * ══════════════════════════════════════════════════════════════════════════
 *
 * Esquema Firestore (colección `partner_links/{linkId}`):
 *
 * ```
 * partner_links/{linkId}
 *   ├─ ownerUid            : String       — UID anónimo de quien creó el vínculo
 *   ├─ partnerUid          : String?      — UID de quien se unió con el código
 *   ├─ linkCode            : String       — código corto de 6 dígitos
 *   ├─ status              : "PENDING" | "ACTIVE" | "REVOKED"
 *   ├─ linkType            : "OBSERVER" | "CYCLE_SYNC"
 *   ├─ ownerDisplayName    : String?      — nombre visible de la titular
 *   ├─ partnerDisplayName  : String?      — nombre visible de la pareja
 *   ├─ ownerAvatarId       : String?      — id del avatar vectorial (AvatarCatalog)
 *   ├─ partnerAvatarId     : String?
 *   ├─ relationLabel       : String       — etiqueta libre ("Mi pareja", "Amiga"…)
 *   ├─ ownerFcmToken       : String?      — para notificaciones push
 *   ├─ partnerFcmToken     : String?
 *   ├─ ownerShare{Phase|Mood|Symptoms|Predictions}   : Boolean — privacidad titular
 *   ├─ partnerShare{Phase|Mood|Symptoms|Predictions} : Boolean — privacidad pareja
 *   ├─ ownerSnapshot       : CycleSnapshot? — estado del ciclo de la titular
 *   ├─ partnerSnapshot     : CycleSnapshot? — estado de la pareja (solo CYCLE_SYNC)
 *   ├─ lastOwnerCareAction   : { action, timestamp } — último detalle ENVIADO por la titular
 *   ├─ lastPartnerCareAction : { action, timestamp } — último detalle ENVIADO por la pareja
 *   └─ createdAtMs         : Long
 *
 * diaries/{linkId}/entries/{entryId}
 *   ├─ authorUid   : String
 *   ├─ authorName  : String
 *   ├─ text        : String (1..500)
 *   ├─ phase       : String (CyclePhase)
 *   └─ timestampMs : Long
 * ```
 *
 * Principio de privacidad: el snapshot ya viaja FILTRADO — quien publica
 * aplica sus toggles antes de escribir. Los flags share* del documento
 * permiten además que la UI y la Cloud Function respeten los cambios sin
 * esperar al siguiente snapshot.
 */
data class PartnerLink(
    val linkId: String = "",
    val ownerUid: String = "",
    val partnerUid: String? = null,
    val linkCode: String = "",
    val status: LinkStatus = LinkStatus.PENDING,
    val linkType: LinkType = LinkType.OBSERVER,

    val ownerDisplayName: String? = null,
    val partnerDisplayName: String? = null,
    val ownerAvatarId: String? = null,
    val partnerAvatarId: String? = null,
    val relationLabel: String = "",

    // Tokens FCM para notificaciones push (los envía una Cloud Function)
    val ownerFcmToken: String? = null,
    val partnerFcmToken: String? = null,

    // Privacidad granular de la titular (owner)
    val ownerSharePhase: Boolean = true,
    val ownerShareMood: Boolean = true,
    val ownerShareSymptoms: Boolean = false,
    val ownerSharePredictions: Boolean = true,

    // Privacidad granular de la pareja (solo relevante en CYCLE_SYNC)
    val partnerSharePhase: Boolean = true,
    val partnerShareMood: Boolean = true,
    val partnerShareSymptoms: Boolean = false,
    val partnerSharePredictions: Boolean = true,

    // Snapshots del ciclo (doble vía cuando el vínculo es CYCLE_SYNC)
    val ownerSnapshot: CycleSnapshot? = null,
    val partnerSnapshot: CycleSnapshot? = null,

    // Último "detalle" enviado por cada lado
    val lastOwnerCareAction: CareActionStatus? = null,
    val lastPartnerCareAction: CareActionStatus? = null,

    val createdAtMs: Long = 0L
)

enum class LinkStatus { PENDING, ACTIVE, REVOKED }

/**
 * OBSERVER  — la otra persona no registra su propio ciclo; solo acompaña.
 * CYCLE_SYNC — ambas registran su ciclo (amigas, parejas que menstrúan).
 */
enum class LinkType { OBSERVER, CYCLE_SYNC }

/**
 * Detalles que se pueden enviar. COFFEE se conserva por compatibilidad con
 * documentos antiguos, aunque la UI nueva ya no lo ofrece.
 */
enum class CareAction { HUG, TEA, COFFEE, CHOCOLATE, HEATING_PAD, PHARMACY }

data class CareActionStatus(
    val action: CareAction = CareAction.HUG,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String? = null
)

/**
 * Estado ligero del ciclo que se comparte con el vínculo. NUNCA contiene el
 * historial médico: es una foto del presente, ya filtrada por la privacidad
 * de quien la publica.
 *
 * Los campos de proyección (cycleStartDateMs, cycleLengthDays,
 * periodLengthDays) permiten al dispositivo de la pareja dibujar el
 * Calendario Compartido sin acceder a ningún registro histórico. Solo se
 * publican si sharePredictions está activo.
 */
data class CycleSnapshot(
    val currentPhase: CyclePhase = CyclePhase.UNKNOWN,
    val daysUntilNextPhase: Int = 0,
    val isLate: Boolean = false,
    val delayDays: Int = 0,

    val currentMood: String? = null,
    val topSymptoms: List<String> = emptyList(),

    // Proyección mínima para el Calendario Compartido (opt-in vía predicciones)
    val cycleStartDateMs: Long? = null,
    val cycleLengthDays: Int = 0,
    val periodLengthDays: Int = 0,

    val lastUpdatedAt: Long = System.currentTimeMillis()
)

/** Entrada del Diario Compartido (espacio íntimo del vínculo). */
data class DiaryEntry(
    val id: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val text: String = "",
    val phase: CyclePhase = CyclePhase.UNKNOWN,
    val timestampMs: Long = 0L
)

/** Toggles de privacidad granular de UN lado del vínculo. */
data class SharePreferences(
    val phase: Boolean = true,
    val mood: Boolean = true,
    val symptoms: Boolean = false,
    val predictions: Boolean = true
)

// ─────────────────────────────────────────────────────────────────────────────
//  Helpers de rol: centralizan el "¿quién soy yo dentro de este vínculo?"
//  para que la UI nunca repita la lógica owner/partner.
// ─────────────────────────────────────────────────────────────────────────────

/** ¿Soy la persona que creó el vínculo? */
fun PartnerLink.isOwner(myUid: String?): Boolean = myUid != null && ownerUid == myUid

/** Nombre visible de la OTRA persona del vínculo. */
fun PartnerLink.otherDisplayName(myUid: String?): String? =
    if (isOwner(myUid)) partnerDisplayName ?: relationLabel.takeIf { it.isNotBlank() }
    else ownerDisplayName

/** Avatar de la OTRA persona del vínculo. */
fun PartnerLink.otherAvatarId(myUid: String?): String? =
    if (isOwner(myUid)) partnerAvatarId else ownerAvatarId

/** Snapshot del ciclo de la OTRA persona (lo que me corresponde ver). */
fun PartnerLink.otherSnapshot(myUid: String?): CycleSnapshot? =
    if (isOwner(myUid)) partnerSnapshot else ownerSnapshot

/** Privacidad declarada por la OTRA persona (qué me deja ver). */
fun PartnerLink.otherSharePreferences(myUid: String?): SharePreferences =
    if (isOwner(myUid)) SharePreferences(
        phase = partnerSharePhase,
        mood = partnerShareMood,
        symptoms = partnerShareSymptoms,
        predictions = partnerSharePredictions
    ) else SharePreferences(
        phase = ownerSharePhase,
        mood = ownerShareMood,
        symptoms = ownerShareSymptoms,
        predictions = ownerSharePredictions
    )

/** Mi propia privacidad dentro de este vínculo. */
fun PartnerLink.mySharePreferences(myUid: String?): SharePreferences =
    if (isOwner(myUid)) SharePreferences(
        phase = ownerSharePhase,
        mood = ownerShareMood,
        symptoms = ownerShareSymptoms,
        predictions = ownerSharePredictions
    ) else SharePreferences(
        phase = partnerSharePhase,
        mood = partnerShareMood,
        symptoms = partnerShareSymptoms,
        predictions = partnerSharePredictions
    )

/** Último detalle que ME enviaron (lo mandó la otra persona). */
fun PartnerLink.careActionReceived(myUid: String?): CareActionStatus? =
    if (isOwner(myUid)) lastPartnerCareAction else lastOwnerCareAction
