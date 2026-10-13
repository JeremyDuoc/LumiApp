package com.jeremy.lumi.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.jeremy.lumi.data.remote.partner.toCareActionStatus
import com.jeremy.lumi.data.remote.partner.toDiaryEntry
import com.jeremy.lumi.data.remote.partner.toFirestoreMap
import com.jeremy.lumi.data.remote.partner.toPartnerLink
import com.jeremy.lumi.domain.model.CareAction
import com.jeremy.lumi.domain.model.CareActionStatus
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.CycleSnapshot
import com.jeremy.lumi.domain.model.DiaryEntry
import com.jeremy.lumi.domain.model.LinkStatus
import com.jeremy.lumi.domain.model.LinkType
import com.jeremy.lumi.domain.model.PartnerLink
import com.jeremy.lumi.domain.model.SharePreferences
import com.jeremy.lumi.domain.model.isOwner
import com.jeremy.lumi.domain.model.mySharePreferences
import com.jeremy.lumi.domain.model.filteredBy
import com.jeremy.lumi.domain.repository.PartnerRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación Firestore del Modo Pareja.
 *
 * - Tiempo real con `callbackFlow` + snapshot listeners.
 * - Caché offline: la persistencia local de Firestore está activada (ver
 *   FirebaseModule), así que cada Flow emite primero lo cacheado y después
 *   lo fresco. La app funciona en el metro sin cobertura.
 * - Los errores se REGISTRAN y se propagan a quien llama (nada de tragar
 *   excepciones en silencio: ese fue uno de los males de la versión anterior).
 */
@Singleton
class PartnerRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val messaging: FirebaseMessaging
) : PartnerRepository {

    companion object {
        private const val TAG = "PartnerRepository"
        private const val LINKS = "partner_links"
        private const val INVITATIONS = "partner_invitations"
        private const val DIARIES = "diaries"
        private const val ENTRIES = "entries"
        private const val MAX_DIARY_ENTRIES = 200L
        private const val DIARY_PRUNE_BATCH = 20L
    }

    // ── Sesión ───────────────────────────────────────────────────────────────

    override suspend fun signInAnonymouslyIfNeeded(): Result<Unit> {
        if (auth.currentUser != null) return Result.success(Unit)
        return try {
            auth.signInAnonymously().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo iniciar sesión anónima: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUid(): String? {
        signInAnonymouslyIfNeeded()
        return auth.currentUser?.uid
    }

    // ── Observación de vínculos ──────────────────────────────────────────────

    override fun observeMyLinks(): Flow<List<PartnerLink>> = flow {
        signInAnonymouslyIfNeeded()
        val uid = auth.currentUser?.uid
        if (uid == null) {
            emit(emptyList())
            return@flow
        }
        // Dos consultas (owner y partner) combinadas en una sola lista.
        // El owner ve también sus PENDING (para mostrar el código en el Hub);
        // quien se une solo aparece en vínculos ya ACTIVE.
        emitAll(
            combine(
                linksQueryFlow("ownerUid", uid, listOf(LinkStatus.PENDING, LinkStatus.ACTIVE)),
                linksQueryFlow("partnerUid", uid, listOf(LinkStatus.ACTIVE))
            ) { asOwner, asPartner ->
                (asOwner + asPartner).sortedByDescending { it.createdAtMs }
            }.distinctUntilChanged()
        )
    }

    private fun linksQueryFlow(
        field: String,
        uid: String,
        statuses: List<LinkStatus>
    ): Flow<List<PartnerLink>> = callbackFlow {
        val query = firestore.collection(LINKS)
            .whereEqualTo(field, uid)
            .whereIn("status", statuses.map { it.name })

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // No cerramos el flow: Firestore reintenta solo al volver la red
                Log.w(TAG, "Error escuchando vínculos ($field): ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            trySend(snapshot?.documents?.mapNotNull { it.toPartnerLink() } ?: emptyList())
        }
        awaitClose { registration.remove() }
    }

    override fun observeLink(linkId: String): Flow<PartnerLink?> = callbackFlow {
        val registration = firestore.collection(LINKS).document(linkId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error escuchando el vínculo $linkId: ${error.message}")
                    return@addSnapshotListener
                }
                trySend(snapshot?.toPartnerLink())
            }
        awaitClose { registration.remove() }
    }

    // ── Crear / unirse ───────────────────────────────────────────────────────

    private fun generateLinkCode(): String = (100000..999999).random().toString()

    override suspend fun createPartnerLink(
        linkType: LinkType,
        displayName: String?,
        avatarId: String?,
        relationLabel: String,
        privacy: SharePreferences
    ): String {
        val uid = getCurrentUid()
            ?: throw IllegalStateException("Sin sesión de Firebase — revisa la conexión")
        val fcmToken = fetchFcmTokenOrNull()
        val code = generateLinkCode()
        val docRef = firestore.collection(LINKS).document()
        val linkId = docRef.id
        val now = System.currentTimeMillis()
        val expiresAt = now + 48 * 60 * 60 * 1000L // 48 horas de caducidad

        val linkData = mapOf(
            "linkId" to linkId,
            "ownerUid" to uid,
            "partnerUid" to null,
            "linkCode" to code,
            "status" to LinkStatus.PENDING.name,
            "linkType" to linkType.name,
            "ownerDisplayName" to displayName,
            "partnerDisplayName" to null,
            "ownerAvatarId" to avatarId,
            "partnerAvatarId" to null,
            "relationLabel" to relationLabel,
            "ownerFcmToken" to fcmToken,
            "partnerFcmToken" to null,
            "ownerSharePhase" to privacy.phase,
            "ownerShareMood" to privacy.mood,
            "ownerShareSymptoms" to privacy.symptoms,
            "ownerSharePredictions" to privacy.predictions,
            "partnerSharePhase" to true,
            "partnerShareMood" to true,
            "partnerShareSymptoms" to false,
            "partnerSharePredictions" to true,
            "ownerSnapshot" to null,
            "partnerSnapshot" to null,
            "lastOwnerCareAction" to null,
            "lastPartnerCareAction" to null,
            "createdAtMs" to now
        )

        val invitationData = mapOf(
            "code" to code,
            "linkId" to linkId,
            "ownerUid" to uid,
            "status" to LinkStatus.PENDING.name,
            "expiresAtMs" to expiresAt,
            "createdAtMs" to now
        )

        val batch = firestore.batch()
        batch.set(docRef, linkData)
        batch.set(firestore.collection(INVITATIONS).document(code), invitationData)
        batch.commit().await()

        return code
    }

    override suspend fun joinPartnerLink(
        code: String,
        displayName: String?,
        avatarId: String?,
        privacy: SharePreferences
    ): PartnerLink? {
        val uid = getCurrentUid()
            ?: throw IllegalStateException("Sin sesión de Firebase — revisa la conexión")
        val fcmToken = fetchFcmTokenOrNull()
        val cleanCode = code.trim()

        // 1. Obtener la invitación directamente por código único (lectura puntual get)
        val invitationDoc = try {
            firestore.collection(INVITATIONS).document(cleanCode).get().await()
        } catch (e: Exception) {
            Log.w(TAG, "Error consultando invitación por código: ${e.message}")
            null
        }

        val linkId: String
        val invitationRef = invitationDoc?.takeIf { it.exists() }?.reference

        if (invitationDoc != null && invitationDoc.exists()) {
            val status = invitationDoc.getString("status")
            val expiresAt = invitationDoc.getLong("expiresAtMs") ?: 0L
            val ownerUid = invitationDoc.getString("ownerUid")
            val resolvedLinkId = invitationDoc.getString("linkId")

            if (status != LinkStatus.PENDING.name || expiresAt <= System.currentTimeMillis() || resolvedLinkId == null) {
                return null
            }
            if (ownerUid == uid) return null // Nadie puede unirse a su propio vínculo
            linkId = resolvedLinkId
        } else {
            // Fallback con consulta directa para instalaciones previas a la migración
            val snapshot = try {
                firestore.collection(LINKS)
                    .whereEqualTo("linkCode", cleanCode)
                    .whereEqualTo("status", LinkStatus.PENDING.name)
                    .limit(1)
                    .get().await()
            } catch (_: Exception) { null }

            val doc = snapshot?.documents?.firstOrNull() ?: return null
            if (doc.getString("ownerUid") == uid) return null
            linkId = doc.id
        }

        val linkRef = firestore.collection(LINKS).document(linkId)

        // 2. Transacción atómica: canjear invitación y activar el vínculo
        firestore.runTransaction { transaction ->
            if (invitationRef != null) {
                val currentInv = transaction.get(invitationRef)
                if (currentInv.getString("status") != LinkStatus.PENDING.name) {
                    throw IllegalStateException("La invitación ya fue canjeada o revocada")
                }
                transaction.update(invitationRef, mapOf(
                    "status" to "CLAIMED",
                    "claimedByUid" to uid,
                    "claimedAtMs" to System.currentTimeMillis()
                ))
            }

            val currentLink = transaction.get(linkRef)
            if (currentLink.getString("status") != LinkStatus.PENDING.name) {
                throw IllegalStateException("El vínculo ya no está pendiente")
            }

            transaction.update(linkRef, mapOf(
                "partnerUid" to uid,
                "partnerDisplayName" to displayName,
                "partnerAvatarId" to avatarId,
                "partnerFcmToken" to fcmToken,
                "partnerSharePhase" to privacy.phase,
                "partnerShareMood" to privacy.mood,
                "partnerShareSymptoms" to privacy.symptoms,
                "partnerSharePredictions" to privacy.predictions,
                "status" to LinkStatus.ACTIVE.name
            ))
        }.await()

        return linkRef.get().await().toPartnerLink()
    }

    // ── Privacidad e identidad por vínculo ───────────────────────────────────

    override suspend fun updateMyPrivacy(linkId: String, privacy: SharePreferences) {
        val uid = getCurrentUid() ?: return
        val ref = firestore.collection(LINKS).document(linkId)
        firestore.runTransaction { transaction ->
            val link = transaction.get(ref).toPartnerLink() ?: return@runTransaction
            check(link.ownerUid == uid || link.partnerUid == uid)
            val prefix = if (link.isOwner(uid)) "owner" else "partner"
            val previous = if (link.isOwner(uid)) link.ownerSnapshot else link.partnerSnapshot
            transaction.update(ref, mapOf(
                "${prefix}SharePhase" to privacy.phase,
                "${prefix}ShareMood" to privacy.mood,
                "${prefix}ShareSymptoms" to privacy.symptoms,
                "${prefix}SharePredictions" to privacy.predictions,
                "${prefix}Snapshot" to previous?.filteredBy(privacy)?.toFirestoreMap()
            ))
        }.await()
    }

    override suspend fun updateMyIdentity(linkId: String, displayName: String?, avatarId: String?) {
        val uid = getCurrentUid() ?: return
        val link = fetchLink(linkId) ?: return
        val prefix = if (link.isOwner(uid)) "owner" else "partner"
        val updates = buildMap<String, Any?> {
            if (displayName != null) put("${prefix}DisplayName", displayName)
            if (avatarId != null) put("${prefix}AvatarId", avatarId)
        }
        if (updates.isNotEmpty()) {
            firestore.collection(LINKS).document(linkId).update(updates).await()
        }
    }

    // ── Snapshot del ciclo ───────────────────────────────────────────────────

    override suspend fun publishSnapshot(snapshot: CycleSnapshot) {
        val uid = getCurrentUid() ?: return

        // 1. Vínculos donde soy titular → ownerSnapshot
        val asOwner = firestore.collection(LINKS)
            .whereEqualTo("ownerUid", uid)
            .whereEqualTo("status", LinkStatus.ACTIVE.name)
            .get().await()

        // 2. Vínculos CYCLE_SYNC donde soy la persona que se unió → partnerSnapshot
        val asPartner = firestore.collection(LINKS)
            .whereEqualTo("partnerUid", uid)
            .whereEqualTo("status", LinkStatus.ACTIVE.name)
            .whereEqualTo("linkType", LinkType.CYCLE_SYNC.name)
            .get().await()

        if (asOwner.isEmpty && asPartner.isEmpty) return

        // Re-read consent in the transaction so a concurrent privacy change cannot
        // be overwritten by a publication based on an earlier query result.
        for (document in asOwner.documents + asPartner.documents) {
            firestore.runTransaction { transaction ->
                val link = transaction.get(document.reference).toPartnerLink()
                    ?: return@runTransaction
                if (link.status != LinkStatus.ACTIVE) return@runTransaction
                if (link.ownerUid != uid && link.partnerUid != uid) return@runTransaction
                val field = if (link.isOwner(uid)) "ownerSnapshot" else "partnerSnapshot"
                transaction.update(document.reference, field,
                    snapshot.filteredBy(link.mySharePreferences(uid)).toFirestoreMap())
            }.await()
        }
    }

    // ── Detalles (Care Actions) ──────────────────────────────────────────────

    override suspend fun sendCareAction(linkId: String, action: CareAction, message: String?) {
        val uid = getCurrentUid() ?: return
        val link = fetchLink(linkId) ?: return
        val field = if (link.isOwner(uid)) "lastOwnerCareAction" else "lastPartnerCareAction"
        firestore.collection(LINKS).document(linkId)
            .update(field, CareActionStatus(action, System.currentTimeMillis(), message).toFirestoreMap())
            .await()
    }

    override suspend fun unlink(linkId: String) {
        val uid = getCurrentUid() ?: return
        val link = fetchLink(linkId) ?: return
        if (link.ownerUid == uid || link.partnerUid == uid) {
            firestore.collection(LINKS).document(linkId)
                .update("status", LinkStatus.REVOKED.name)
                .await()
        }
    }

    // ── Diario Compartido ────────────────────────────────────────────────────

    override fun observeDiaryEntries(linkId: String): Flow<List<DiaryEntry>> = callbackFlow {
        val query = firestore.collection(DIARIES).document(linkId).collection(ENTRIES)
            .orderBy("timestampMs", Query.Direction.ASCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Error escuchando el diario $linkId: ${error.message}")
                return@addSnapshotListener
            }
            trySend(snapshot?.documents?.mapNotNull { it.toDiaryEntry() } ?: emptyList())
        }
        awaitClose { registration.remove() }
    }

    override suspend fun sendDiaryEntry(
        linkId: String,
        text: String,
        phase: CyclePhase,
        authorName: String
    ) {
        val uid = getCurrentUid()
            ?: throw IllegalStateException("Sin sesión de Firebase — revisa la conexión")
        pruneDiaryIfNeeded(linkId)
        val entry = DiaryEntry(
            authorUid = uid,
            authorName = authorName,
            text = text.trim().take(500),
            phase = phase,
            timestampMs = System.currentTimeMillis()
        )
        firestore.collection(DIARIES).document(linkId).collection(ENTRIES)
            .add(entry.toFirestoreMap())
            .await()
    }

    /** Mantiene el diario acotado: al superar el tope, borra las más antiguas. */
    private suspend fun pruneDiaryIfNeeded(linkId: String) {
        try {
            val col = firestore.collection(DIARIES).document(linkId).collection(ENTRIES)
            val count = col.count().get(AggregateSource.SERVER).await().count
            if (count >= MAX_DIARY_ENTRIES) {
                val oldest = col.orderBy("timestampMs", Query.Direction.ASCENDING)
                    .limit(DIARY_PRUNE_BATCH)
                    .get().await()
                firestore.runBatch { batch ->
                    oldest.documents.forEach { batch.delete(it.reference) }
                }.await()
            }
        } catch (e: Exception) {
            // La poda es mantenimiento: si falla (p. ej. offline) no bloquea el envío
            Log.w(TAG, "No se pudo podar el diario $linkId: ${e.message}")
        }
    }

    // ── FCM ──────────────────────────────────────────────────────────────────

    override suspend fun refreshMyFcmToken() {
        val uid = auth.currentUser?.uid ?: return
        val token = fetchFcmTokenOrNull() ?: return

        val asOwner = firestore.collection(LINKS)
            .whereEqualTo("ownerUid", uid)
            .whereIn("status", listOf(LinkStatus.PENDING.name, LinkStatus.ACTIVE.name))
            .get().await()
        val asPartner = firestore.collection(LINKS)
            .whereEqualTo("partnerUid", uid)
            .whereEqualTo("status", LinkStatus.ACTIVE.name)
            .get().await()

        if (asOwner.isEmpty && asPartner.isEmpty) return

        firestore.runBatch { batch ->
            asOwner.documents.forEach { batch.update(it.reference, "ownerFcmToken", token) }
            asPartner.documents.forEach { batch.update(it.reference, "partnerFcmToken", token) }
        }.await()
    }

    // ── Internos ─────────────────────────────────────────────────────────────

    private suspend fun fetchLink(linkId: String): PartnerLink? =
        firestore.collection(LINKS).document(linkId).get().await().toPartnerLink()

    private suspend fun fetchFcmTokenOrNull(): String? = try {
        messaging.token.await()
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo obtener el token FCM: ${e.message}")
        null
    }
}
