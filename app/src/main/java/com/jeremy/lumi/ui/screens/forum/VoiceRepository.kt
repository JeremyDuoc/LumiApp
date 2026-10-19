package com.jeremy.lumi.ui.screens.forum

import com.jeremy.lumi.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
    val public: Boolean = true,
    val estimatedAvailableOn: String? = null
)
data class VoiceStatusChange(
    val toStatus: String,
    val changedAtMs: Long
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
    @SerialName("estimated_available_on") val estimatedAvailableOn: String? = null,
) {
    fun ui() = VoiceIdea(
        id = id, title = title, description = description, category = category,
        status = status, votes = votesCount, comments = commentsCount,
        createdAtMs = createdAtMs, estimatedAvailableOn = estimatedAvailableOn
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
private data class VoiceHistoryRow(
    @SerialName("to_status") val toStatus: String,
    @SerialName("changed_at_ms") val changedAtMs: Long
){
    fun ui() = VoiceStatusChange(toStatus,changedAtMs)
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
@OptIn(SupabaseExperimental::class)
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

    @Volatile
    private var activeUserId: String? = null

    /**
     * El SDK 3.x usa la publishable key como Bearer cuando no hay sesión
     * (por ejemplo durante una renovación de JWT). Nunca consultar tablas
     * privadas con ese fallback: esperar a que Auth se recupere.
     */
    private suspend fun awaitRecoveredAuth() {
        client.auth.awaitInitialization()
        if (client.auth.sessionStatus.value is SessionStatus.RefreshFailure) {
            val recovered = withTimeoutOrNull(20_000L) {
                client.auth.sessionStatus.first { status ->
                    status is SessionStatus.Authenticated || status is SessionStatus.NotAuthenticated
                }
            }
            if (recovered == null) {
                error("No pudimos renovar la sesión de Tu voz. Revisa tu conexión e inténtalo nuevamente.")
            }
        }
    }

    suspend fun connect(): String {
        awaitRecoveredAuth()
        if (client.auth.currentSessionOrNull() == null) {
            if (activeUserId != null) {
                error("La sesión de Tu voz se cerró. Vuelve a abrir la sección para reconectar.")
            }
            client.auth.signInAnonymously()
        }
        val session = client.auth.currentSessionOrNull()
            ?: error("No se pudo iniciar la sesión anónima de Tu voz.")
        require(session.accessToken.count { it == '.' } == 2) {
            "La sesión de Tu voz no tiene un token de usuario válido."
        }
        val uid = session.user?.id ?: error("Falta la identidad anónima de Tu voz.")
        if (activeUserId != null && activeUserId != uid) {
            error("Tu sesión de Tu voz cambió. Vuelve a abrir la sección.")
        }
        activeUserId = uid
        return uid
    }

    private suspend fun requireSessionFor(uid: String) {
        awaitRecoveredAuth()
        val session = client.auth.currentSessionOrNull()
            ?: error("La sesión de Tu voz no está disponible. Vuelve a abrir la sección.")
        if (session.user?.id != uid || session.accessToken.count { it == '.' } != 2) {
            error("No pudimos confirmar tu sesión de Tu voz. Vuelve a abrir la sección.")
        }
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
        var consecutiveErrors = 0
        while (currentCoroutineContext().isActive) {
            try {
                requireSessionFor(uid)
                val rows = client.from("voice_votes").select {
                    filter { eq("owner_uid", uid) }
                    limit(500)
                }.decodeList<VoiceVoteRow>()
                emit(rows.map { it.ideaId }.toSet())
                consecutiveErrors = 0
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // La renovación de token y la conectividad pueden fallar
                // momentáneamente: preservar el último estado y reintentar.
                if (++consecutiveErrors >= 4) throw e
            }
            delay(7_000)
        }
    }

    fun submissions(uid: String): Flow<List<VoiceMine>> = flow {
        var consecutiveErrors = 0
        while (currentCoroutineContext().isActive) {
            try {
                requireSessionFor(uid)
                val rows = client.from("voice_submissions").select {
                    filter { eq("owner_uid", uid) }
                    limit(100)
                }.decodeList<VoiceSubmissionRow>()
                emit(rows.sortedByDescending { it.createdAtMs }.map { it.ui() })
                consecutiveErrors = 0
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (++consecutiveErrors >= 4) throw e
            }
            delay(7_000)
        }
    }

    /**
     * Únicamente estados comprobados desde la activación de la auditoría:
     * NO inferir fechas para hitos históricos que nunca se almacenaron.
     */
    fun history(ideaId: String): Flow<List<VoiceStatusChange>> = flow {
        var failures = 0
        while (currentCoroutineContext().isActive) {
            try {
                val uid = connect()
                requireSessionFor(uid)
                val rows=client.from("voice_status_history").select {
                    filter { eq("idea_id",ideaId) }
                    limit(200)
                }.decodeList<VoiceHistoryRow>()
                emit(rows.sortedBy { it.changedAtMs }.map { it.ui() })
                failures=0
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (++failures >= 4) throw e
            }
            delay(12_000)
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
        client.postgrest.rpc("voice_submit_idea", buildJsonObject {
            put("p_title", title.trim())
            put("p_description", description.trim())
            put("p_category", category)
        })
    }

    suspend fun vote(ideaId:String) {
        connect()
        client.postgrest.rpc("voice_toggle_vote", buildJsonObject {
            put("p_idea_id",ideaId)
        })
    }

    suspend fun comment(ideaId:String,body:String) {
        connect()
        client.postgrest.rpc("voice_submit_comment",buildJsonObject {
            put("p_idea_id",ideaId)
            put("p_body",body.trim())
        })
    }

    suspend fun report(ideaId:String,reason:String) {
        connect()
        client.postgrest.rpc("voice_report",buildJsonObject {
            put("p_idea_id",ideaId)
            put("p_reason",reason.trim())
        })
    }
}
