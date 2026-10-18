package com.jeremy.lumi.ui.screens.forum

/** Reglas puras compartidas por UI/ViewModel. No sustituyen validación de servidor. */
object VoiceValidation {
    val categories = setOf("Seguimiento", "Diseño", "Privacidad", "Accesibilidad", "Otros")

    fun validProposal(title: String, description: String, category: String): Boolean =
        title.trim().length in 8..100 &&
        description.trim().length in 15..900 &&
        category in categories

    fun validComment(text: String): Boolean = text.trim().length in 3..500
}
