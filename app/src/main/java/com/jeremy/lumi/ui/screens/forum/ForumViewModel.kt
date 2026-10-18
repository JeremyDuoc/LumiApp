package com.jeremy.lumi.ui.screens.forum

import android.content.Context
import com.jeremy.lumi.BuildConfig
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject

/**
 * Datos comunitarios NO médicos. Almacén local aislado: no lee el perfil,
 * calendario, síntomas ni backups de Lumi. No es un backend compartido.
 */
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
    val createdAt: Long = System.currentTimeMillis(),
    val votes: Int = 0,
    val votedByMe: Boolean = false,
    val createdByMe: Boolean = true,
    val comments: List<VoiceComment> = emptyList(),
    val reportedByMe: Boolean = false,
    val hiddenByMe: Boolean = false
)

@HiltViewModel
class ForumViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {
    private val prefs = context.getSharedPreferences("lumi_voice_local_v1", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val type = object : TypeToken<List<VoiceProposal>>() {}.type

    private val _proposals = MutableStateFlow(if (BuildConfig.DEBUG) examples() + readLocal() else readLocal())
    val proposals: StateFlow<List<VoiceProposal>> = _proposals.asStateFlow()

    // Sin identidad administrativa verificada por servidor NO existe modo admin.
    val isVerifiedAdmin: Boolean = false
    val isCommunityConnected: Boolean = false

    /**
     * The five examples below come from the APPROVED HTML v3 and are visible in
     * DEBUG builds ONLY. They are clearly labeled previews, never official data.
     * Sample votes are in-memory so they cannot pollute the private local store.
     */
    private fun examples(): List<VoiceProposal> {
        val now = System.currentTimeMillis()
        fun sample(id: String, title: String, desc: String, category: String,
                   status: String, votes: Int, comments: List<VoiceComment> = emptyList()) =
            VoiceProposal("voice-demo-$id", title, desc, category, status,
                now - 86_400_000L * (id.first().code - 'a'.code + 1),
                votes, comments = comments, createdByMe = false)
        return listOf(
            sample("a", "Un calendario que se pueda deslizar por semanas",
                "Me ayudaría ver los días siguientes sin entrar al calendario completo. Que también marque claramente el día de hoy.",
                "Calendario", "En desarrollo", 96,
                listOf(VoiceComment("example-a1",
                    "Estamos trabajando en un calendario más fácil de recorrer, sin perder el contexto del día actual.", now, true),
                    VoiceComment("example-a2", "Sería genial que recordara la semana en la que quedé.", now))),
            sample("b", "Elegir qué síntomas aparecen primero en mi registro",
                "Uso siempre los mismos tres síntomas. Quiero poder organizarlos para registrar mi día sin tanto desplazamiento.",
                "Registro diario", "Aceptada", 74,
                listOf(VoiceComment("example-b1", "Sí, y que respete nuestro orden al abrir el registro.", now))),
            sample("c", "Modo negro OLED en todas las pantallas",
                "Tengo un teléfono AMOLED y me gustaría usar un tema con negro verdadero, sin destellos claros al cambiar de pantalla.",
                "Apariencia", "En revisión", 58,
                listOf(VoiceComment("example-c1", "¡También lo usaría para las noches!", now))),
            sample("d", "Exportar un resumen de mis registros sin compartirlos",
                "Un archivo que se genere en el dispositivo para guardarlo o mostrarlo durante una consulta, sin enviarlo automáticamente a ningún servidor.",
                "Privacidad", "Recibida", 42),
            sample("e", "Recordatorios que permitan elegir varios días",
                "Quiero seleccionar lunes, miércoles y viernes en un mismo recordatorio, sin tener que crear tres distintos.",
                "Recordatorios", "Disponible", 31,
                listOf(VoiceComment("example-e1",
                    "Esta opción ya se encuentra disponible en esta demostración de hoja de ruta.", now, true)))
        )
    }

    private fun readLocal(): List<VoiceProposal> {
        val json = prefs.getString("proposals", null) ?: return emptyList()
        return runCatching { gson.fromJson<List<VoiceProposal>>(json, type).orEmpty() }
            .getOrDefault(emptyList())
            .filter { !it.id.isNullOrBlank() && !it.title.isNullOrBlank() }
    }

    private fun update(block: (List<VoiceProposal>) -> List<VoiceProposal>) {
        _proposals.update { block(it) }
        // Never persist demo fixtures as if they were community data.
        prefs.edit().putString("proposals", gson.toJson(_proposals.value.filterNot { it.id.startsWith("voice-demo-") })).apply()
    }

    fun publish(title: String, description: String, category: String): Boolean {
        val cleanTitle = title.trim()
        val cleanDescription = description.trim()
        if (!VoiceValidation.validProposal(cleanTitle, cleanDescription, category)) return false
        // Los datos se guardan exclusivamente en este dispositivo hasta habilitar backend.
        val idea = VoiceProposal(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            description = cleanDescription,
            category = category
        )
        update { listOf(idea) + it }
        return true
    }

    fun toggleVote(id: String) {
        update { list -> list.map { p ->
            if (p.id != id) p
            else p.copy(
                votedByMe = !p.votedByMe,
                votes = (p.votes + if (p.votedByMe) -1 else 1).coerceAtLeast(0)
            )
        } }
    }

    fun comment(id: String, text: String): Boolean {
        val clean = text.trim()
        if (!VoiceValidation.validComment(clean) || _proposals.value.none { it.id == id }) return false
        update { list -> list.map { p ->
            if (p.id == id) p.copy(comments = p.comments.orEmpty() +
                VoiceComment(UUID.randomUUID().toString(), clean, System.currentTimeMillis()))
            else p
        } }
        return true
    }

    fun report(id: String) {
        update { list -> list.map {
            if (it.id == id) it.copy(reportedByMe = true) else it
        } }
    }

    fun hide(id: String) {
        update { list -> list.map {
            if (it.id == id) it.copy(hiddenByMe = true) else it
        } }
    }

    companion object {
        val statuses = listOf("Recibida", "En revisión", "Aceptada", "En desarrollo", "Disponible", "No prevista")
        val developmentStates = setOf("En revisión", "Aceptada", "En desarrollo", "Disponible")
    }
}
