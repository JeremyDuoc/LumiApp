package com.jeremy.lumi.ui.screens.forum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VoiceUiState(
    val loading:Boolean=true,
    val offlineMessage:String?=null,
    val ideas:List<VoiceIdea> = emptyList(),
    val voted:Set<String> = emptySet(),
    val mine:List<VoiceMine> = emptyList(),
    val comments:List<VoiceComment> = emptyList(),
    val history:List<VoiceStatusChange> = emptyList(),
    val busy:Boolean=false,
    val notice:String?=null
)
@HiltViewModel
class ForumViewModel @Inject constructor(private val repository:VoiceRepository): ViewModel() {
    private val _state=MutableStateFlow(VoiceUiState())
    val state:StateFlow<VoiceUiState> = _state.asStateFlow()
    private var commentsJob:Job?=null
    init {
        viewModelScope.launch {
            try {
                val uid=repository.connect()
                launch {
                    repository.ideas().catch { failure(it, publicFeed = true) }.collect { items ->
                        _state.update { it.copy(ideas=items,loading=false,offlineMessage=null) }
                    }
                }
                launch {
                    repository.votes(uid).catch { failure(it) }.collect { votes ->
                        _state.update { it.copy(voted=votes) }
                    }
                }
                launch {
                    repository.submissions(uid).catch { failure(it) }.collect { mine ->
                        _state.update { it.copy(mine=mine) }
                    }
                }
            } catch (e:Exception){ failure(e, publicFeed = true) }
        }
    }

    /**
     * Nunca mostrar el mensaje técnico original de PostgREST: puede incluir
     * URL, cabeceras Authorization y la clave publishable del cliente.
     */
    private fun safeMessage(e:Throwable):String {
        val raw=e.localizedMessage.orEmpty()
        val lower=raw.lowercase()
        return when {
            lower.contains("permission denied") || lower.contains("42501") ||
                lower.contains("jwt") || lower.contains("sesión") ||
                lower.contains("session") ->
                "No pudimos confirmar tu sesión de Tu voz. Comprueba la conexión y vuelve a abrir la sección."
            lower.contains("timeout") || lower.contains("unable to resolve") ||
                lower.contains("network") || lower.contains("connect") ->
                "No pudimos conectar con Tu voz. Comprueba internet e inténtalo de nuevo."
            lower.contains("propuesta inválida") ->
                "Revisa el título, la descripción y la categoría de la propuesta."
            lower.contains("espera cinco minutos") ->
                "Espera cinco minutos antes de enviar otra propuesta."
            lower.contains("espera un minuto") ->
                "Espera un minuto antes de enviar otro comentario."
            lower.contains("no incluyas enlaces") ->
                "Por privacidad, evita publicar enlaces o datos de contacto."
            lower.contains("comentario inválido") ->
                "El comentario debe tener entre 4 y 420 caracteres y no incluir datos de contacto."
            else -> "No se pudo completar la operación. Inténtalo nuevamente."
        }
    }
    private fun failure(e:Throwable, publicFeed:Boolean=false) {
        _state.update {
            it.copy(
                loading=false,busy=false,
                // Errores de votos o sugerencias propias no deben ocultar
                // el listado de ideas públicas que sí está funcionando.
                offlineMessage=if(publicFeed)
                    "No se pudo actualizar el listado de ideas. Comprueba tu conexión."
                    else it.offlineMessage,
                notice=safeMessage(e)
            )
        }
    }
    fun showComments(ideaId:String?) {
        commentsJob?.cancel()
        _state.update { it.copy(comments=emptyList(),history=emptyList()) }
        if(ideaId==null)return
        commentsJob=viewModelScope.launch {
            launch {
                repository.comments(ideaId).catch { failure(it) }.collect { comments ->
                    _state.update { it.copy(comments=comments) }
                }
            }
            launch {
                repository.history(ideaId).catch { failure(it) }.collect { records ->
                    _state.update { it.copy(history=records) }
                }
            }
        }
    }
    fun dismissNotice(){_state.update{it.copy(notice=null)}}
    private fun perform(success:String, action:suspend ()->Unit) {
        if(_state.value.busy)return
        viewModelScope.launch {
            _state.update{it.copy(busy=true,notice=null)}
            try {
                action()
                _state.update{it.copy(busy=false,notice=success)}
            } catch(e:Exception) {
                _state.update{it.copy(busy=false,notice=safeMessage(e))}
            }
        }
    }
    fun suggest(title:String,description:String,category:String)=perform("Idea enviada. Aparecerá públicamente cuando el equipo la revise.") {
        repository.suggest(title.trim(),description.trim(),category)
    }
    fun vote(ideaId:String)=perform("Apoyo actualizado."){repository.vote(ideaId)}
    fun comment(ideaId:String,body:String)=perform("Comentario enviado a revisión."){repository.comment(ideaId,body.trim())}
    fun report(ideaId:String,reason:String)=perform("Reporte recibido. Gracias por ayudar a cuidar este espacio."){repository.report(ideaId,reason.trim())}
}
