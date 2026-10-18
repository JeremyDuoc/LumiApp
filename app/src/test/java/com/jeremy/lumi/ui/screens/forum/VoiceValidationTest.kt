package com.jeremy.lumi.ui.screens.forum

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceValidationTest {
    @Test fun rejectsEmptyAndOversizedProposals() {
        assertFalse(VoiceValidation.validProposal("", "Esta descripción es suficiente", "Diseño"))
        assertFalse(VoiceValidation.validProposal("Un título válido", "Muy corto", "Diseño"))
        assertFalse(VoiceValidation.validProposal("Un título válido", "Esta descripción es suficiente", "No válida"))
        assertFalse(VoiceValidation.validProposal("a".repeat(101), "Esta descripción es suficiente", "Diseño"))
        assertFalse(VoiceValidation.validProposal("Un título válido", "x".repeat(901), "Diseño"))
    }
    @Test fun acceptsValidProposal() {
        assertTrue(VoiceValidation.validProposal("Calendario más claro", "Me gustaría distinguir mejor los días de periodo.", "Diseño"))
    }
    @Test fun acceptsApprovedV3Categories() {
        val categories = listOf("Calendario", "Registro diario", "Apariencia",
            "Privacidad", "Recordatorios", "Otro")
        categories.forEach { category ->
            assertTrue(VoiceValidation.validProposal(
                "Una mejora para Lumi", "Esta mejora podría hacer más cómodo el registro diario.", category))
        }
    }
    @Test fun validatesComments() {
        assertFalse(VoiceValidation.validComment("  "))
        assertFalse(VoiceValidation.validComment("x".repeat(501)))
        assertTrue(VoiceValidation.validComment("Es una gran idea"))
    }
}
