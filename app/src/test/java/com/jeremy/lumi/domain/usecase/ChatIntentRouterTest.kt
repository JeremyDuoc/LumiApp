package com.jeremy.lumi.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatIntentRouterTest {

    @Test fun emergencyBeatsRoutineSymptoms() {
        assertEquals(
            ChatIntent.URGENT_SYMPTOMS,
            ChatIntentRouter.classify("Tengo cólicos y dolor en el pecho")
        )
        assertEquals(
            ChatIntent.URGENT_SYMPTOMS,
            ChatIntentRouter.classify("Me estoy desmayando y tengo el periodo")
        )
    }

    @Test fun contraceptionFailureBeatsCycleTiming() {
        assertEquals(
            ChatIntent.EMERGENCY_CONTRACEPTION,
            ChatIntentRouter.classify("Se rompió el condón durante mis días fértiles")
        )
        assertEquals(
            ChatIntent.EMERGENCY_CONTRACEPTION,
            ChatIntentRouter.classify("No usamos preservativo y estoy atrasada")
        )
    }

    @Test fun recognizesCyclePhaseAndPeriodQueries() {
        assertEquals(
            ChatIntent.NEXT_PERIOD,
            ChatIntentRouter.classify("¿Cuándo llegará mi próximo período?")
        )
        assertEquals(
            ChatIntent.CURRENT_PHASE,
            ChatIntentRouter.classify("¿En qué fase del ciclo estoy?")
        )
        assertEquals(
            ChatIntent.LATE_PERIOD,
            ChatIntentRouter.classify("Mi regla está atrasada")
        )
    }

    @Test fun canAskForEvidenceRatherThanGenericTips() {
        assertEquals(
            ChatIntent.PERSONAL_PATTERNS,
            ChatIntentRouter.classify("¿Qué relación hay entre mis registros de sueño y dolor?")
        )
        assertEquals(
            ChatIntent.SLEEP,
            ChatIntentRouter.classify("Tengo insomnio")
        )
    }

    @Test fun exactWordsAvoidAccidentalGreetingMatches() {
        assertEquals(ChatIntent.UNKNOWN, ChatIntentRouter.classify("aluminio"))
        assertEquals(ChatIntent.GREETING, ChatIntentRouter.classify("Hola Lumi"))
    }

    @Test fun blankAndUnsupportedRequestsAreExplicitlyUnknown() {
        assertEquals(ChatIntent.UNKNOWN, ChatIntentRouter.classify("  "))
        assertEquals(ChatIntent.UNKNOWN, ChatIntentRouter.classify("Puedes arreglar mi computador"))
    }
}
