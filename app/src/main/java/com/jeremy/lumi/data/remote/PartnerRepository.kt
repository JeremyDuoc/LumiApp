package com.jeremy.lumi.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.jeremy.lumi.domain.model.LinkStatus
import com.jeremy.lumi.domain.model.LinkType
import com.jeremy.lumi.domain.model.PartnerLink
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose

@Singleton
class PartnerRepository @Inject constructor() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val messaging = FirebaseMessaging.getInstance()

    // Autenticación anónima silenciosa
    suspend fun signInAnonymouslyIfNeeded(): Result<Unit> {
        if (auth.currentUser == null) {
            return try {
                auth.signInAnonymously().await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        return Result.success(Unit)
    }

    suspend fun getCurrentUid(): String? {
        signInAnonymouslyIfNeeded()
        return auth.currentUser?.uid
    }

    suspend fun getFcmToken(): String? {
        return try {
            messaging.token.await()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Genera un código de 6 dígitos aleatorio para enlazar cuentas.
     */
    private fun generateLinkCode(): String {
        return (100000..999999).random().toString()
    }

    /**
     * La titular crea un enlace con sus preferencias de privacidad y obtiene el código.
     * BUG FIX: La privacidad ahora se persiste en Firestore al crear el link.
     */
    suspend fun createPartnerLink(
        sharePhase: Boolean = true,
        shareMood: Boolean = true,
        shareSymptoms: Boolean = false,
        sharePredictions: Boolean = true,
        ownerDisplayName: String? = null,
        linkType: LinkType = LinkType.OBSERVER
    ): String {
        val uid = getCurrentUid() ?: throw Exception("No se pudo autenticar. Verifica tu conexión a internet.")
        val fcmToken = getFcmToken()
        val linkCode = generateLinkCode()

        val docRef = firestore.collection("partner_links").document()
        val link = PartnerLink(
            linkId = docRef.id,
            ownerUid = uid,
            linkCode = linkCode,
            status = LinkStatus.PENDING,
            linkType = linkType,
            ownerFcmToken = fcmToken,
            ownerDisplayName = ownerDisplayName,
            ownerSharePhase = sharePhase,
            ownerShareMood = shareMood,
            ownerShareSymptoms = shareSymptoms,
            ownerSharePredictions = sharePredictions
        )

        docRef.set(link).await()
        return linkCode
    }

    /**
     * La pareja usa el código para vincularse.
     */
    suspend fun joinPartnerLink(code: String): PartnerLink? {
        val uid = getCurrentUid() ?: throw Exception("No se pudo autenticar. Verifica tu conexión a internet.")
        val fcmToken = getFcmToken()

        val snapshot = firestore.collection("partner_links")
            .whereEqualTo("linkCode", code)
            .whereEqualTo("status", LinkStatus.PENDING.name)
            .get().await()

        if (snapshot.isEmpty) return null

        val doc = snapshot.documents.first()
        val linkRef = firestore.collection("partner_links").document(doc.id)

        linkRef.update(
            mapOf(
                "partnerUid" to uid,
                "partnerFcmToken" to fcmToken,
                "status" to LinkStatus.ACTIVE.name
            )
        ).await()

        return doc.toObject(PartnerLink::class.java)
    }

    /**
     * Observa todos los enlaces de la usuaria actual (PENDING + ACTIVE).
     * BUG FIX: Ahora incluye links PENDING del owner para que aparezcan en el Hub.
     * BUG FIX: Usa combine() en lugar de variables locales para evitar race conditions.
     */
    fun observeMyLinks(): Flow<List<PartnerLink>> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(emptyList())

        // Buscar links donde sea owner (PENDING + ACTIVE)
        val ownerFlow: Flow<List<PartnerLink>> = callbackFlow {
            val query = firestore.collection("partner_links")
                .whereEqualTo("ownerUid", uid)
                .whereIn("status", listOf(LinkStatus.PENDING.name, LinkStatus.ACTIVE.name))

            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot.documents.mapNotNull { it.toObject(PartnerLink::class.java) })
            }
            awaitClose { listener.remove() }
        }

        // Buscar links donde sea partner (solo ACTIVE — el partner no tiene PENDING)
        val partnerFlow: Flow<List<PartnerLink>> = callbackFlow {
            val query = firestore.collection("partner_links")
                .whereEqualTo("partnerUid", uid)
                .whereEqualTo("status", LinkStatus.ACTIVE.name)

            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot.documents.mapNotNull { it.toObject(PartnerLink::class.java) })
            }
            awaitClose { listener.remove() }
        }

        return combine(ownerFlow, partnerFlow) { ownerLinks, partnerLinks ->
            ownerLinks + partnerLinks
        }
    }

    /**
     * Publica el snapshot del ciclo en Firestore.
     * Actualiza ownerSnapshot si somos owner, o partnerSnapshot si somos partner (y es CYCLE_SYNC).
     */
    suspend fun publishSnapshot(snapshot: com.jeremy.lumi.domain.model.CycleSnapshot) {
        try {
            val uid = getCurrentUid() ?: return

            // 1. Actualizar donde somos owner
            val ownerSnap = firestore.collection("partner_links")
                .whereEqualTo("ownerUid", uid)
                .whereEqualTo("status", LinkStatus.ACTIVE.name)
                .get().await()

            for (doc in ownerSnap.documents) {
                doc.reference.update("ownerSnapshot", snapshot).await()
            }

            // 2. Actualizar donde somos partner y el link es CYCLE_SYNC
            val partnerSnap = firestore.collection("partner_links")
                .whereEqualTo("partnerUid", uid)
                .whereEqualTo("status", LinkStatus.ACTIVE.name)
                .whereEqualTo("linkType", com.jeremy.lumi.domain.model.LinkType.CYCLE_SYNC.name)
                .get().await()

            for (doc in partnerSnap.documents) {
                doc.reference.update("partnerSnapshot", snapshot).await()
            }
        } catch (e: Exception) {
            // Ignore offline network exceptions
        }
    }

    /**
     * Envía una acción de cuidado (CareAction) a un link específico.
     */
    suspend fun sendCareAction(linkId: String, action: com.jeremy.lumi.domain.model.CareAction) {
        try {
            val uid = getCurrentUid() ?: return
            val docRef = firestore.collection("partner_links").document(linkId)
            val doc = docRef.get().await()
            if (!doc.exists()) return

            val link = doc.toObject(PartnerLink::class.java) ?: return
            val careActionStatus = com.jeremy.lumi.domain.model.CareActionStatus(
                action = action,
                timestamp = System.currentTimeMillis()
            )
            
            if (link.ownerUid == uid) {
                docRef.update("lastOwnerCareAction", careActionStatus).await()
            } else if (link.partnerUid == uid) {
                docRef.update("lastPartnerCareAction", careActionStatus).await()
            }
        } catch (e: Exception) {
            // Ignore offline network exceptions
        }
    }

    /**
     * Desvincula (revoca) un enlace específico.
     */
    suspend fun unlink(linkId: String) {
        try {
            val uid = getCurrentUid() ?: return
            val docRef = firestore.collection("partner_links").document(linkId)
            val doc = docRef.get().await()
            if (!doc.exists()) return

            val link = doc.toObject(PartnerLink::class.java) ?: return
            if (link.ownerUid == uid || link.partnerUid == uid) {
                docRef.update("status", LinkStatus.REVOKED.name).await()
            }
        } catch (e: Exception) {
            // Ignore offline network exceptions
        }
    }
}
