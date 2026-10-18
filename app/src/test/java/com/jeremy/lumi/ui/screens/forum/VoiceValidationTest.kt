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
    @Test fun validatesComments() {
        assertFalse(VoiceValidation.validComment("  "))
        assertFalse(VoiceValidation.validComment("x".repeat(501)))
        assertTrue(VoiceValidation.validComment("Es una gran idea"))
    }
}
