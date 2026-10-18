package com.jeremy.lumi.ui.screens.forum

import android.content.Context
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** Content displayed publicly, without a personal identifier or health records. */
data class VoiceComment(
    val id: String,
    val text: String,
    val createdAt: Long,
    val team: Boolean = false
)

data class VoiceProposal(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val status: String = "Recibida",
    val createdAt: Long = 0L,
    val votes: Int = 0,
    val votedByMe: Boolean = false,
    val createdByMe: Boolean = false,
    val comments: List<VoiceComment> = emptyList(),
    val reportedByMe: Boolean = false,
    val hiddenByMe: Boolean = false,
    val commentsCount: Int = 0
)

/**
 * Tu voz uses only voice_* Firestore collections and secure Cloud Functions.
 * Firebase Auth is the technical anti-duplicate identity, never publicly displayed.
 * Does NOT read/send period, health, Drive, Bonds or on-device tracking data.
 */
@HiltViewModel
class ForumViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : ViewModel() {
    private val local = context.getSharedPreferences("lumi_voice_ui_v2", Context.MODE_PRIVATE)
    private val hiddenIds = local.getStringSet("hidden_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
    private val reportedIds = local.getStringSet("reported_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
    private val ownIds = local.getStringSet("own_ids", emptySet())?.toMutableSet() ?: mutableSetOf()

    private val _proposals = MutableStateFlow<List<VoiceProposal>>(emptyList())
    val proposals: StateFlow<List<VoiceProposal>> = _proposals.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _online = MutableStateFlow(false)
    val online: StateFlow<Boolean> = _online.asStateFlow()
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private var rawIdeas: List<VoiceProposal> = emptyList()
    private var votedIds = emptySet<String>()
    private val commentCache = mutableMapOf<String, List<VoiceComment>>()
    private var ideasListener: ListenerRegistration? = null
    private var votesListener: ListenerRegistration? = null
    private var commentsListener: ListenerRegistration? = null
    private var activeCommentId: String? = null

    val isVerifiedAdmin = false // Not inferred client-side. Server checks custom claim.
    val isCommunityConnected: Boolean get() = _online.value

    init {
        val user = auth.currentUser
        if (user != null) subscribe(user.uid)
        else auth.signInAnonymously()
            .addOnSuccessListener { subscribe(it.user?.uid.orEmpty()) }
            .addOnFailureListener { fail(it) }
    }

    private fun subscribe(uid: String) {
        if (uid.isBlank()) {
            _error.value = "No fue posible abrir una identidad anónima para Tu voz."
            _loading.value = false
            return
        }
        ideasListener?.remove()
        votesListener?.remove()
        ideasListener = db.collection("voice_ideas")
            .whereEqualTo("visible", true)
            .limit(80)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, e ->
                if (e != null) {
                    fail(e)
                    _online.value = false
                    return@addSnapshotListener
                }
                _loading.value = false
                if (snapshot != null) {
                    _online.value = !snapshot.metadata.isFromCache
                    rawIdeas = snapshot.documents.mapNotNull(::mapIdea)
                    publishCombined()
                }
            }
        votesListener = db.collection("voice_user_votes").document(uid)
            .collection("ideas").limit(200)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    fail(e)
                    return@addSnapshotListener
                }
                votedIds = snapshot?.documents?.map { it.id }?.toSet().orEmpty()
                publishCombined()
            }
    }

    private fun mapIdea(doc: DocumentSnapshot): VoiceProposal? {
        val title = doc.getString("title") ?: return null
        if (title.isBlank()) return null
        return VoiceProposal(
            id = doc.id,
            title = title,
            description = doc.getString("description").orEmpty(),
            category = doc.getString("category").orEmpty(),
            status = doc.getString("status") ?: "Recibida",
            createdAt = doc.getLong("createdAtMs") ?: 0L,
            votes = (doc.getLong("votes") ?: 0L).coerceAtLeast(0).toInt(),
            commentsCount = (doc.getLong("commentsCount") ?: 0L).coerceAtLeast(0).toInt()
        )
    }

    private fun publishCombined() {
        _proposals.value = rawIdeas.map { item ->
            item.copy(
                votedByMe = item.id in votedIds,
                createdByMe = item.id in ownIds,
                reportedByMe = item.id in reportedIds,
                hiddenByMe = item.id in hiddenIds,
                comments = commentCache[item.id].orEmpty()
            )
        }
    }

    /** Observe only the proposal whose detail sheet is visible. */
    fun observeComments(id: String?) {
        if (activeCommentId == id) return
        commentsListener?.remove()
        commentsListener = null
        activeCommentId = id
        if (id == null || id !in rawIdeas.map { it.id }) return
        commentsListener = db.collection("voice_ideas").document(id)
            .collection("comments").orderBy("createdAtMs").limit(80)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    fail(e)
                    return@addSnapshotListener
                }
                commentCache[id] = snapshot?.documents?.mapNotNull { doc ->
                    val text = doc.getString("text") ?: return@mapNotNull null
                    VoiceComment(doc.id, text, doc.getLong("createdAtMs") ?: 0L,
                        doc.getBoolean("team") ?: false)
                }.orEmpty()
                publishCombined()
            }
    }

    private fun fail(throwable: Exception) {
        _loading.value = false
        val firebaseError = throwable as? FirebaseFunctionsException
        _error.value = when (firebaseError?.code) {
            FirebaseFunctionsException.Code.UNAVAILABLE ->
                "Servicio temporalmente sin conexión. Verifica tu red."
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                "Has realizado demasiadas acciones. Espera un momento."
            FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                "No se pudo autenticar Tu voz. Reabre la pantalla."
            FirebaseFunctionsException.Code.NOT_FOUND ->
                "El servidor de Tu voz aún no está desplegado o la propuesta ya no existe."
            else -> throwable.localizedMessage?.take(180)
                ?: "No fue posible conectar con Tu voz."
        }
    }

    fun clearError() { _error.value = null }

    private fun call(name: String, data: Map<String, Any>, done: (Boolean, Any?) -> Unit) {
        if (auth.currentUser == null) {
            _error.value = "Esperando autenticación. Inténtalo nuevamente."
            done(false, null)
            return
        }
        functions.getHttpsCallable(name).call(data)
            .addOnSuccessListener { done(true, it.data) }
            .addOnFailureListener {
                fail(it)
                done(false, null)
            }
    }

    /** The sheet closes only after the server accepts the new idea. */
    fun publish(title: String, description: String, category: String, done: (Boolean) -> Unit) {
        if (!VoiceValidation.validProposal(title, description, category)) {
            _error.value = "Revisa el título, la descripción y la categoría."
            done(false)
            return
        }
        call("voiceCreateIdea", mapOf(
            "title" to title.trim(), "description" to description.trim(), "category" to category
        )) { success, result ->
            if (success) {
                val id = (result as? Map<*, *>)?.get("id") as? String
                if (id != null) {
                    ownIds.add(id)
                    local.edit().putStringSet("own_ids", ownIds.toSet()).apply()
                    publishCombined()
                }
            }
            done(success)
        }
    }

    fun toggleVote(id: String) {
        if (id !in rawIdeas.map { it.id }) return
        call("voiceToggleVote", mapOf("id" to id)) { _, _ -> }
    }

    fun comment(id: String, text: String, done: (Boolean) -> Unit) {
        if (!VoiceValidation.validComment(text)) {
            _error.value = "El comentario debe tener entre 3 y 500 caracteres."
            done(false)
            return
        }
        call("voiceAddComment", mapOf("id" to id, "text" to text.trim())) { success, _ ->
            done(success)
        }
    }

    fun report(id: String) {
        call("voiceReportIdea", mapOf("id" to id, "reason" to "Contenido inapropiado")) { success, _ ->
            if (success) {
                reportedIds.add(id)
                local.edit().putStringSet("reported_ids", reportedIds.toSet()).apply()
                publishCombined()
            }
        }
    }

    /** Hiding is per-device only, never deletes another user's content. */
    fun hide(id: String) {
        hiddenIds.add(id)
        local.edit().putStringSet("hidden_ids", hiddenIds.toSet()).apply()
        publishCombined()
    }

    override fun onCleared() {
        ideasListener?.remove()
        votesListener?.remove()
        commentsListener?.remove()
        super.onCleared()
    }

    companion object {
        val statuses = listOf("Recibida", "En revisión", "Aceptada", "En desarrollo",
            "Disponible", "No prevista")
        val developmentStates = setOf("En revisión", "Aceptada", "En desarrollo", "Disponible")
    }
}
