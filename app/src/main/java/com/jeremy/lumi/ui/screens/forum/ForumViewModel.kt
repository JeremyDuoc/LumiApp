package com.jeremy.lumi.ui.screens.forum

import android.content.Context
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

    private val _proposals = MutableStateFlow(readLocal())
    val proposals: StateFlow<List<VoiceProposal>> = _proposals.asStateFlow()

    // Sin identidad administrativa verificada por servidor NO existe modo admin.
    val isVerifiedAdmin: Boolean = false
    val isCommunityConnected: Boolean = false

    private fun readLocal(): List<VoiceProposal> {
        val json = prefs.getString("proposals", null) ?: return emptyList()
        return runCatching { gson.fromJson<List<VoiceProposal>>(json, type).orEmpty() }
            .getOrDefault(emptyList())
            .filter { !it.id.isNullOrBlank() && !it.title.isNullOrBlank() }
    }

    private fun update(block: (List<VoiceProposal>) -> List<VoiceProposal>) {
        _proposals.update { block(it) }
        prefs.edit().putString("proposals", gson.toJson(_proposals.value)).apply()
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
