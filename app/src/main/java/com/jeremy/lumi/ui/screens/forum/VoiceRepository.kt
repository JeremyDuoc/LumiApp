package com.jeremy.lumi.ui.screens.forum

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class VoiceIdea(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val status: String,
    val votes: Int,
    val comments: Int,
    val createdAtMs: Long,
    val public: Boolean = true
)
data class VoiceComment(val id: String, val body: String, val team: Boolean)
data class VoiceMine(val id: String, val title: String, val description: String, val category: String, val status: String, val createdAtMs: Long, val public: Boolean)

@Singleton
class VoiceRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions
) {
    suspend fun connect(): String {
        if (auth.currentUser == null) auth.signInAnonymously().await()
        return auth.currentUser?.uid ?: error("No se pudo establecer la sesión anónima.")
    }

    fun ideas(): Flow<List<VoiceIdea>> = callbackFlow {
        // Filtrar visible en la consulta: las reglas no funcionan como filtros.
        val registration = db.collection("voice_ideas")
            .whereEqualTo("visible", true)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents.orEmpty().mapNotNull { doc ->
                    if (doc.getBoolean("visible") != true) return@mapNotNull null
                    VoiceIdea(
                        id=doc.id,title=doc.getString("title").orEmpty(),
                        description=doc.getString("description").orEmpty(),
                        category=doc.getString("category").orEmpty(),
                        status=doc.getString("status").orEmpty(),
                        votes=(doc.getLong("votesCount") ?: 0).toInt(),
                        comments=(doc.getLong("commentsCount") ?: 0).toInt(),
                        createdAtMs=doc.getLong("createdAtMs") ?: 0
                    )
                })
            }
        awaitClose { registration.remove() }
    }
    fun votes(uid: String): Flow<Set<String>> = callbackFlow {
        val registration=db.collection("voice_votes").document(uid).collection("items")
            .addSnapshotListener { snapshot,error ->
                if(error!=null){ close(error); return@addSnapshotListener }
                trySend(snapshot?.documents.orEmpty().map{it.id}.toSet())
            }
        awaitClose { registration.remove() }
    }
    fun submissions(uid: String): Flow<List<VoiceMine>> = callbackFlow {
        val registration=db.collection("voice_my_submissions").document(uid).collection("items")
            .addSnapshotListener { snapshot,error ->
                if(error!=null){ close(error); return@addSnapshotListener }
                trySend(snapshot?.documents.orEmpty().map{ doc -> VoiceMine(
                    doc.id,doc.getString("title").orEmpty(),doc.getString("description").orEmpty(),
                    doc.getString("category").orEmpty(),doc.getString("status").orEmpty(),
                    doc.getLong("createdAtMs") ?: 0,doc.getBoolean("public")==true
                ) })
            }
        awaitClose { registration.remove() }
    }
    fun comments(ideaId:String): Flow<List<VoiceComment>> = callbackFlow {
        val registration=db.collection("voice_ideas").document(ideaId).collection("comments")
            .orderBy("createdAtMs",Query.Direction.ASCENDING).limit(100)
            .addSnapshotListener { snapshot,error ->
                if(error!=null){ close(error); return@addSnapshotListener }
                trySend(snapshot?.documents.orEmpty().map{doc ->
                    VoiceComment(doc.id,doc.getString("body").orEmpty(),doc.getBoolean("team")==true)
                })
            }
        awaitClose { registration.remove() }
    }
    private suspend fun invoke(name:String, arguments:Map<String,Any>) {
        functions.getHttpsCallable(name).call(arguments).await()
    }
    suspend fun suggest(title:String,description:String,category:String) =
        invoke("submitVoiceIdea",mapOf("title" to title,"description" to description,"category" to category))
    suspend fun vote(ideaId:String) = invoke("voteVoiceIdea",mapOf("ideaId" to ideaId))
    suspend fun comment(ideaId:String,body:String) = invoke("submitVoiceComment",mapOf("ideaId" to ideaId,"body" to body))
    suspend fun report(ideaId:String,reason:String) = invoke("reportVoiceIdea",mapOf("ideaId" to ideaId,"reason" to reason))
}
