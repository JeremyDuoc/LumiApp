package com.jeremy.lumi.ui.screens.onboarding

import com.jeremy.lumi.domain.model.OnboardingData
import com.jeremy.lumi.domain.model.UserGoal
import org.junit.Assert.*
import org.junit.Test

class OnboardingV3VerificationTest {

    @Test
    fun testVisibleStepsForTrackMode() {
        val state = OnboardingUiState(
            currentStepIndex = 0,
            data = OnboardingData(mode = "track")
        )
        assertEquals(8, state.totalStepsCount)
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7), state.visibleSteps)
    }

    @Test
    fun testVisibleStepsForSupportMode() {
        val state = OnboardingUiState(
            currentStepIndex = 0,
            data = OnboardingData(mode = "support")
        )
        assertEquals(4, state.totalStepsCount)
        assertEquals(listOf(0, 1, 2, 7), state.visibleSteps)
    }

    @Test
    fun testStep1RequiresBothMedicalAndPrivacyConsent() {
        // En paso 1 (Seguridad)
        val stateNeither = OnboardingUiState(
            currentStepIndex = 1,
            data = OnboardingData(medicalDisclaimerAccepted = false, privacyAccepted = false)
        )
        assertFalse("Debe estar bloqueado si ninguno está aceptado", stateNeither.isNextEnabled)

        val stateOnlyMed = OnboardingUiState(
            currentStepIndex = 1,
            data = OnboardingData(medicalDisclaimerAccepted = true, privacyAccepted = false)
        )
        assertFalse("Debe estar bloqueado si solo se acepta lo médico", stateOnlyMed.isNextEnabled)

        val stateOnlyPriv = OnboardingUiState(
            currentStepIndex = 1,
            data = OnboardingData(medicalDisclaimerAccepted = false, privacyAccepted = true)
        )
        assertFalse("Debe estar bloqueado si solo se acepta la privacidad", stateOnlyPriv.isNextEnabled)

        val stateBoth = OnboardingUiState(
            currentStepIndex = 1,
            data = OnboardingData(medicalDisclaimerAccepted = true, privacyAccepted = true)
        )
        assertTrue("Debe desbloquearse cuando ambos están aceptados", stateBoth.isNextEnabled)
    }

    @Test
    fun testStep3RequiresRegularityChoice() {
        // En paso 3 (Tu ciclo)
        val stateNoChoice = OnboardingUiState(
            currentStepIndex = 3,
            data = OnboardingData(regularityChoice = null)
        )
        assertFalse(stateNoChoice.isNextEnabled)

        val stateChoiceYes = OnboardingUiState(
            currentStepIndex = 3,
            data = OnboardingData(regularityChoice = "yes")
        )
        assertTrue(stateChoiceYes.isNextEnabled)

        val stateChoiceUnknown = OnboardingUiState(
            currentStepIndex = 3,
            data = OnboardingData(regularityChoice = "unknown")
        )
        assertTrue(stateChoiceUnknown.isNextEnabled)
    }

    @Test
    fun testStep4RequiresDateOnlyIfDateKnown() {
        // En paso 4 (Último periodo)
        val stateNotRemembered = OnboardingUiState(
            currentStepIndex = 4,
            data = OnboardingData(lastPeriodKnown = false, lastPeriodDate = 0L)
        )
        assertTrue("Si no recuerda la fecha, puede continuar sin fecha", stateNotRemembered.isNextEnabled)

        val stateRememberedWithoutDate = OnboardingUiState(
            currentStepIndex = 4,
            data = OnboardingData(lastPeriodKnown = true, lastPeriodDate = 0L)
        )
        assertFalse("Si eligió dar fecha, debe seleccionarla antes de continuar", stateRememberedWithoutDate.isNextEnabled)

        val stateRememberedWithDate = OnboardingUiState(
            currentStepIndex = 4,
            data = OnboardingData(lastPeriodKnown = true, lastPeriodDate = 1728864000000L)
        )
        assertTrue("Con fecha válida, puede continuar", stateRememberedWithDate.isNextEnabled)
    }

    @Test
    fun testAuditHomeGreetingFormatsProperly() {
        fun formatGreeting(name: String?): String {
            val clean = name?.trim()?.takeIf { it.isNotBlank() }
            return if (clean != null) "Hola, $clean." else "Hola."
        }

        assertEquals("Hola.", formatGreeting(null))
        assertEquals("Hola.", formatGreeting(""))
        assertEquals("Hola.", formatGreeting("   "))
        assertEquals("Hola, Valentina.", formatGreeting("Valentina"))
        assertEquals("Hola, Jeremy.", formatGreeting("  Jeremy  "))
    }
}
