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
                    repository.ideas().catch { failure(it) }.collect { items ->
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
            } catch (e:Exception){ failure(e) }
        }
    }
    private fun failure(e:Throwable) {
        _state.update { it.copy(loading=false,busy=false,offlineMessage="No se pudo sincronizar Tu voz. Revisa tu conexión o la configuración de Firebase.",notice=e.localizedMessage) }
    }
    fun showComments(ideaId:String?) {
        commentsJob?.cancel()
        _state.update { it.copy(comments=emptyList()) }
        if(ideaId==null)return
        commentsJob=viewModelScope.launch {
            repository.comments(ideaId).catch { failure(it) }.collect{ comments ->
                _state.update { it.copy(comments=comments) }
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
                _state.update{it.copy(busy=false,notice=e.localizedMessage ?: "No se pudo completar la operación.")}
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
