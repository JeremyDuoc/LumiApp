package com.jeremy.lumi.ui.screens.onboarding

import com.jeremy.lumi.domain.model.OnboardingData
import com.jeremy.lumi.domain.model.SecondaryGoal
import com.jeremy.lumi.domain.model.UserGoal
import org.junit.Assert.*
import org.junit.Test

class OnboardingV7ViewModelTest {

    @Test
    fun testTrackModeHas8Steps() {
        val state = OnboardingUiState(
            currentStepIndex = 0,
            data = OnboardingData(mode = "track")
        )
        assertEquals("track", state.data.mode)
        assertEquals(8, state.totalStepsCount)
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7), state.visibleSteps)
        assertEquals(0, state.currentStepNumber)
        assertTrue(state.isNextEnabled)
    }

    @Test
    fun testSupportModeHas4Steps() {
        val state = OnboardingUiState(
            currentStepIndex = 0,
            data = OnboardingData(mode = "support")
        )
        assertEquals("support", state.data.mode)
        assertEquals(4, state.totalStepsCount)
        assertEquals(listOf(0, 1, 2, 7), state.visibleSteps)
        assertEquals(0, state.currentStepNumber)
    }

    @Test
    fun testStep1SafetyValidation() {
        // Ninguno aceptado -> Bloqueado
        val neither = OnboardingUiState(
            currentStepIndex = 1,
            data = OnboardingData(medicalDisclaimerAccepted = false, privacyAccepted = false)
        )
        assertFalse("Debe bloquearse si falta consentimiento", neither.isNextEnabled)

        // Solo médico -> Bloqueado
        val onlyMed = OnboardingUiState(
            currentStepIndex = 1,
            data = OnboardingData(medicalDisclaimerAccepted = true, privacyAccepted = false)
        )
        assertFalse("Debe bloquearse si falta privacidad", onlyMed.isNextEnabled)

        // Solo privacidad -> Bloqueado
        val onlyPriv = OnboardingUiState(
            currentStepIndex = 1,
            data = OnboardingData(medicalDisclaimerAccepted = false, privacyAccepted = true)
        )
        assertFalse("Debe bloquearse si falta descargo médico", onlyPriv.isNextEnabled)

        // Ambos aceptados -> Desbloqueado
        val both = OnboardingUiState(
            currentStepIndex = 1,
            data = OnboardingData(medicalDisclaimerAccepted = true, privacyAccepted = true)
        )
        assertTrue("Se desbloquea cuando ambos están aceptados", both.isNextEnabled)
    }

    @Test
    fun testStep2NicknameOptional() {
        val emptyName = OnboardingUiState(
            currentStepIndex = 2,
            data = OnboardingData(userName = null)
        )
        assertTrue("Apodo vacío es válido", emptyName.isNextEnabled)

        val filledName = OnboardingUiState(
            currentStepIndex = 2,
            data = OnboardingData(userName = "Valentina")
        )
        assertTrue("Apodo completado es válido", filledName.isNextEnabled)
    }

    @Test
    fun testStep3CycleRegularityRequired() {
        val noChoice = OnboardingUiState(
            currentStepIndex = 3,
            data = OnboardingData(regularityChoice = null)
        )
        assertFalse("Debe elegir regularidad antes de continuar", noChoice.isNextEnabled)

        val choiceYes = OnboardingUiState(
            currentStepIndex = 3,
            data = OnboardingData(regularityChoice = "yes", isRegular = true)
        )
        assertTrue(choiceYes.isNextEnabled)

        val choiceNo = OnboardingUiState(
            currentStepIndex = 3,
            data = OnboardingData(regularityChoice = "no", isRegular = false)
        )
        assertTrue(choiceNo.isNextEnabled)

        val choiceUnknown = OnboardingUiState(
            currentStepIndex = 3,
            data = OnboardingData(regularityChoice = "unknown", isRegular = null)
        )
        assertTrue(choiceUnknown.isNextEnabled)
    }

    @Test
    fun testStep4LastPeriodValidation() {
        val notRemembered = OnboardingUiState(
            currentStepIndex = 4,
            data = OnboardingData(lastPeriodKnown = false, lastPeriodDate = 0L)
        )
        assertTrue("Si no recuerda la fecha, puede continuar", notRemembered.isNextEnabled)

        val rememberedNoDate = OnboardingUiState(
            currentStepIndex = 4,
            data = OnboardingData(lastPeriodKnown = true, lastPeriodDate = 0L)
        )
        assertFalse("Si eligió dar fecha, debe ingresarla", rememberedNoDate.isNextEnabled)

        val rememberedWithDate = OnboardingUiState(
            currentStepIndex = 4,
            data = OnboardingData(lastPeriodKnown = true, lastPeriodDate = 1728864000000L)
        )
        assertTrue("Con fecha válida puede continuar", rememberedWithDate.isNextEnabled)
    }

    @Test
    fun testStep5PreferencesAlwaysEnabled() {
        val state = OnboardingUiState(
            currentStepIndex = 5,
            data = OnboardingData(userGoal = UserGoal.TRACK_CYCLE, contraceptionChoice = "yes")
        )
        assertTrue(state.isNextEnabled)
    }

    @Test
    fun testStep6CustomizationAlwaysEnabled() {
        val state = OnboardingUiState(
            currentStepIndex = 6,
            data = OnboardingData()
        )
        assertTrue(state.isNextEnabled)
    }

    @Test
    fun testStep7ReadyValidationRespectsIsCompleting() {
        val ready = OnboardingUiState(
            currentStepIndex = 7,
            isCompleting = false
        )
        assertTrue("En paso 7 con isCompleting false, botón habilitado", ready.isNextEnabled)

        val saving = OnboardingUiState(
            currentStepIndex = 7,
            isCompleting = true
        )
        assertFalse("Durante guardado (isCompleting true), botón deshabilitado", saving.isNextEnabled)
    }

    @Test
    fun testSaveErrorState() {
        val errorState = OnboardingUiState(
            currentStepIndex = 7,
            saveError = "No pudimos guardar los datos. Puedes intentarlo de nuevo."
        )
        assertNotNull(errorState.saveError)
        assertEquals("No pudimos guardar los datos. Puedes intentarlo de nuevo.", errorState.saveError)
    }

    @Test
    fun testGreetingFormatting() {
        fun formatGreeting(name: String?): String {
            val clean = name?.trim()?.takeIf { it.isNotBlank() }
            return if (clean != null) "Hola, $clean." else "Hola."
        }

        assertEquals("Hola.", formatGreeting(null))
        assertEquals("Hola.", formatGreeting(""))
        assertEquals("Hola.", formatGreeting("    "))
        assertEquals("Hola, Valentina.", formatGreeting("Valentina"))
        assertEquals("Hola, Jeremy.", formatGreeting("  Jeremy  "))
    }
}
