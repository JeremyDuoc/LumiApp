package com.jeremy.lumi.ui.screens.forum

import androidx.lifecycle.ViewModel
import com.jeremy.lumi.domain.model.ForumPost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ForumViewModel @Inject constructor() : ViewModel() {

    private val _posts = MutableStateFlow<List<ForumPost>>(emptyList())
    val posts: StateFlow<List<ForumPost>> = _posts.asStateFlow()

    init {
        loadMockData()
    }

    private fun loadMockData() {
        // Datos simulados para probar la UI antes de conectar Firebase
        _posts.value = listOf(
            ForumPost("1", "¿Algún consejo para los cólicos en la madrugada? El ibuprofeno no me hace nada.", 45, 12),
            ForumPost("2", "¿Es normal que mi fase folicular dure casi 20 días?", 12, 3),
            ForumPost("3", "Chicas, descubrí que el té de canela con jengibre es la salvación para la inflamación.", 120, 45, isUpvotedByMe = true),
            ForumPost("4", "Primera vez usando la copa menstrual y tengo dudas, ¿cómo sé si está bien puesta?", 89, 22)
        )
    }

    fun toggleUpvote(postId: String) {
        _posts.update { currentList ->
            currentList.map { post ->
                if (post.id == postId) {
                    val voteChange = if (post.isUpvotedByMe) -1 else 1
                    post.copy(
                        upvotes = post.upvotes + voteChange,
                        isUpvotedByMe = !post.isUpvotedByMe
                    )
                } else {
                    post
                }
            }
        }
    }

    fun addQuestion(text: String) {
        if (text.isBlank()) return

        val newPost = ForumPost(
            id = UUID.randomUUID().toString(),
            question = text,
            upvotes = 0,
            repliesCount = 0
        )
        // Lo añadimos al principio de la lista
        _posts.update { listOf(newPost) + it }
    }
}