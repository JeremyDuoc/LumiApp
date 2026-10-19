package com.jeremy.lumi.ui.screens.forum

import com.jeremy.lumi.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/** Modelos de UI estables. Nada de este repositorio accede a registros de salud. */
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
data class VoiceMine(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val status: String,
    val createdAtMs: Long,
    val public: Boolean
)

@Serializable
private data class VoiceIdeaRow(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val status: String,
    @SerialName("visible") val visible: Boolean = true,
    @SerialName("votes_count") val votesCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("created_at_ms") val createdAtMs: Long = 0L,
) {
    fun ui() = VoiceIdea(
        id = id, title = title, description = description, category = category,
        status = status, votes = votesCount, comments = commentsCount,
        createdAtMs = createdAtMs
    )
}
@Serializable
private data class VoiceSubmissionRow(
    val id: String, val title: String, val description: String,
    val category: String, val status: String,
    @SerialName("is_public") val isPublic: Boolean,
    @SerialName("created_at_ms") val createdAtMs: Long
) {
    fun ui() = VoiceMine(id,title,description,category,status,createdAtMs,isPublic)
}
@Serializable
private data class VoiceVoteRow(
    @SerialName("idea_id") val ideaId: String
)
@Serializable
private data class VoiceCommentRow(
    val id: String, val body: String,
    @SerialName("is_team") val isTeam: Boolean,
    @SerialName("idea_id") val ideaId: String,
    @SerialName("created_at_ms") val createdAtMs: Long
) {
    fun ui() = VoiceComment(id,body,isTeam)
}
@Serializable
private data class SubmitIdeaParams(
    @SerialName("p_title") val title: String,
    @SerialName("p_description") val description: String,
    @SerialName("p_category") val category: String
)
@Serializable
private data class VoteParams(@SerialName("p_idea_id") val ideaId: String)
@Serializable
private data class CommentParams(
    @SerialName("p_idea_id") val ideaId: String,
    @SerialName("p_body") val body: String
)
@Serializable
private data class ReportParams(
    @SerialName("p_idea_id") val ideaId: String,
    @SerialName("p_reason") val reason: String
)

/**
 * Supabase sólo para «Tu voz». No enviar fechas de período, síntomas,
 * preferencias ni otros datos privados a este cliente.
 *
 * Auth anónimo es independiente del Firebase Auth que se usa en otros módulos.
 * RLS y RPCs transaccionales controlan permisos y conteos en PostgreSQL.
 */
@Singleton
class VoiceRepository @Inject constructor() {

    private val client: SupabaseClient by lazy {
        require(BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()) {
            "Falta configurar LUMI_SUPABASE_PUBLISHABLE_KEY en ~/.gradle/gradle.properties."
        }
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }

    suspend fun connect(): String {
        client.auth.awaitInitialization()
        if (client.auth.currentUserOrNull() == null) {
            client.auth.signInAnonymously()
        }
        return client.auth.currentUserOrNull()?.id
            ?: error("No se pudo iniciar la sesión anónima de Tu voz.")
    }

    /** Ideas públicas: primera carga + actualizaciones PostgreSQL Realtime. */
    fun ideas(): Flow<List<VoiceIdea>> =
        client.from("voice_ideas").selectAsFlow(VoiceIdeaRow::id).map { rows ->
            rows.asSequence().filter { it.visible }
                .sortedByDescending { it.createdAtMs }.take(100).map { it.ui() }.toList()
        }

    /**
     * Los votos contienen UID privados. No publicamos la tabla en Realtime:
     * se consulta únicamente la sesión propietaria cada pocos segundos.
     */
    fun votes(uid: String): Flow<Set<String>> = flow {
        while (currentCoroutineContext().isActive) {
            val rows=client.from("voice_votes").select {
                filter { eq("owner_uid",uid) }
                limit(500)
            }.decodeList<VoiceVoteRow>()
            emit(rows.map { it.ideaId }.toSet())
            delay(7000)
        }
    }

    fun submissions(uid: String): Flow<List<VoiceMine>> = flow {
        while (currentCoroutineContext().isActive) {
            val rows=client.from("voice_submissions").select {
                filter { eq("owner_uid",uid) }
                limit(100)
            }.decodeList<VoiceSubmissionRow>()
            emit(rows.sortedByDescending { it.createdAtMs }.map { it.ui() })
            delay(7000)
        }
    }

    /** Los comentarios son públicos solo después de aprobación del equipo. */
    fun comments(ideaId: String): Flow<List<VoiceComment>> =
        client.from("voice_comments").selectAsFlow(VoiceCommentRow::id).map { rows ->
            rows.asSequence().filter { it.ideaId == ideaId }
                .sortedBy { it.createdAtMs }.take(100).map { it.ui() }.toList()
        }

    suspend fun suggest(title:String, description:String, category:String) {
        connect()
        client.postgrest.rpc(
            "voice_submit_idea",
            SubmitIdeaParams(title.trim(),description.trim(),category)
        )
    }

    suspend fun vote(ideaId:String) {
        connect()
        client.postgrest.rpc("voice_toggle_vote",VoteParams(ideaId))
    }

    suspend fun comment(ideaId:String,body:String) {
        connect()
        client.postgrest.rpc("voice_submit_comment",CommentParams(ideaId,body.trim()))
    }

    suspend fun report(ideaId:String,reason:String) {
        connect()
        client.postgrest.rpc("voice_report",ReportParams(ideaId,reason.trim()))
    }
}
