package com.jeremy.lumi.ui.screens.calendar

import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.data.local.entity.SymptomEntity
import org.junit.Assert.*
import org.junit.Test
import java.io.*

class DailyLogDraftTest {
    @Test fun editingPreservesHistoricalValuesAndIndependentIntensities() {
        val draft = DailyLogDraft.from(DailyLogWithSymptoms(
            DailyLogEntity(cycleId = 2, date = 0, flowIntensity = "Light", mood = "Calm",
                painLevel = 7, energyLevel = 8, stressLevel = 0, cervicalMucus = "Unusual historical value",
                basalBodyTemp = 36.65f, sleepHours = 7.5f),
            listOf(SymptomEntity(dailyLogId = 1, name = "Headache", intensity = 3),
                SymptomEntity(dailyLogId = 1, name = "Mi síntoma", intensity = 9))
        ))
        assertEquals("Ligero", draft.flow)
        assertEquals("Calmada", draft.mood)
        assertEquals(3, draft.symptoms["Dolor de cabeza"])
        assertEquals(9, draft.symptoms["Mi síntoma"])
        assertEquals(8, draft.energy)
        assertEquals(0, draft.stress)
        assertEquals("Unusual historical value", draft.mucus)
        assertEquals(36.65f, parseLogNumber(draft.temperature))
        assertFalse(draft.toggleSymptom("Mi síntoma").symptoms.containsKey("Mi síntoma"))
    }

    @Test fun decimalCommaIsAcceptedButInvalidOrNonFiniteMeasurementsAreNotSavedAsNull() {
        assertEquals(36.7f, parseLogNumber("36,7"))
        assertTrue(DailyLogDraft(sleep = "7,5", water = "1.5", temperature = "36,7").validMeasurements)
        listOf("abc", "NaN", "Infinity", "-2", "25").forEach {
            assertFalse("Invalid sleep: $it", DailyLogDraft(sleep = it).validMeasurements)
        }
        assertFalse(DailyLogDraft(temperature = "3").validMeasurements)
        assertTrue(DailyLogDraft().validMeasurements)
    }

    @Test fun draftRestorationKeepsCustomNamesAndAllFields() {
        val draft = DailyLogDraft(mood = "Feliz", symptoms = mapOf("Mi síntoma" to 8),
            notes = "Una nota", hadIntercourse = true, protection = false, showIntimacy = false,
            sleep = "7,5", temperature = "36,4")
        val bytes = ByteArrayOutputStream().also { output -> ObjectOutputStream(output).use { it.writeObject(draft) } }.toByteArray()
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
        assertEquals(draft, restored)
    }

    @Test fun searchIgnoresAccentsAndLocale() {
        assertEquals(normalizeSymptomSearch("  NÁUSEAS "), normalizeSymptomSearch("nauseas"))
        assertEquals("hinchazon", normalizeSymptomSearch("Hinchazón"))
    }

    @Test fun selectingSymptomDoesNotInventIntensityAndDeselectingReallyRemovesIt() {
        val selected = DailyLogDraft().toggleSymptom("Fatiga")
        assertEquals(0, selected.symptoms["Fatiga"])
        assertTrue(selected.hasContent)
        assertEquals(DailyLogDraft(), selected.toggleSymptom("Fatiga"))
    }

    @Test fun distinctCatalogEntriesDoNotCollapseIntoHistoricalAliases() {
        val draft = DailyLogDraft.from(DailyLogWithSymptoms(
            DailyLogEntity(cycleId = 0, date = 0),
            listOf(SymptomEntity(dailyLogId = 1, name = "Cólicos", intensity = 4),
                SymptomEntity(dailyLogId = 1, name = "Calambres", intensity = 8))
        ))
        assertEquals(mapOf("Cólicos" to 4, "Calambres" to 8), draft.symptoms)
    }
}
