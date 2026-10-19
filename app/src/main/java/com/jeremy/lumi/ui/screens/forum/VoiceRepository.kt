package com.jeremy.lumi.ui.screens.forum

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
    private val db: FirebaseFirestore
) {
    suspend fun connect(): String {
        if (auth.currentUser == null) auth.signInAnonymously().await()
        return auth.currentUser?.uid ?: error("No se pudo establecer la sesión anónima.")
    }

    fun ideas(): Flow<List<VoiceIdea>> = callbackFlow {
        // Filtrar visible en la consulta: las reglas no funcionan como filtros.
        val registration = db.collection("voice_ideas")
            .whereEqualTo("visible", true)
            .orderBy("createdAtMs", Query.Direction.DESCENDING)
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
                trySend(snapshot?.documents.orEmpty().filter{it.getBoolean("active")==true}.map{it.id}.toSet())
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

    /**
     * Spark: la aprobación es manual desde consola/CLI local.
     * Escribir ambas copias en un batch evita envíos incompletos.
     */
    suspend fun suggest(title:String,description:String,category:String) {
        val uid=connect()
        val ref=db.collection("voice_pending_ideas").document()
        val own=db.collection("voice_my_submissions").document(uid).collection("items").document(ref.id)
        val whenMs=System.currentTimeMillis()
        db.batch()
            .set(ref,mapOf(
                "title" to title,"description" to description,"category" to category,
                "ownerUid" to uid,"createdAtMs" to whenMs
            ))
            .set(own,mapOf(
                "title" to title,"description" to description,"category" to category,
                "status" to "Pendiente de revisión","public" to false,"createdAtMs" to whenMs
            )).commit().await()
    }
    /**
     * Voto único por UID: el cambio de active y votesCount son atómicos.
     * Las reglas Firestore comprueban ambos lados antes de aceptar el commit.
     */
    suspend fun vote(ideaId:String) {
        val uid=connect()
        val idea=db.collection("voice_ideas").document(ideaId)
        val vote=db.collection("voice_votes").document(uid).collection("items").document(ideaId)
        db.runTransaction { tx ->
            val post=tx.get(idea)
            if (!post.exists() || post.getBoolean("visible") != true) {
                throw IllegalStateException("Esta propuesta ya no está disponible.")
            }
            val prior=tx.get(vote)
            val previous=prior.getBoolean("active")==true
            val previousCount=post.getLong("votesCount") ?: 0
            val nextCount=(previousCount + if(previous) -1 else 1)
            if(nextCount<0)throw IllegalStateException("Conteo inconsistente; vuelve a intentarlo.")
            if(prior.exists())tx.update(vote,"active",!previous)
            else tx.set(vote,mapOf("active" to true))
            tx.update(idea,"votesCount",nextCount)
            nextCount
        }.await()
    }
    suspend fun comment(ideaId:String,body:String) {
        connect()
        val idea=db.collection("voice_ideas").document(ideaId).get().await()
        if(idea.getBoolean("visible")!=true)error("La idea ya no está disponible.")
        db.collection("voice_pending_comments").document().set(mapOf(
            "ideaId" to ideaId,"body" to body,
            "ownerUid" to (auth.currentUser?.uid ?: error("Sesión no disponible.")),
            "createdAtMs" to System.currentTimeMillis()
        )).await()
    }
    suspend fun report(ideaId:String,reason:String) {
        val uid=connect()
        db.collection("voice_reports").document(uid+"_"+ideaId).set(mapOf(
            "ideaId" to ideaId,"reporterUid" to uid,
            "reason" to reason,"createdAtMs" to System.currentTimeMillis()
        )).await()
    }
}
