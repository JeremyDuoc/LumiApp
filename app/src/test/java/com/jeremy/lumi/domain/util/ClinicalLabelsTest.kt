package com.jeremy.lumi.domain.util

import com.jeremy.lumi.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalLabelsTest {

    @Test
    fun testFlowResolutionBothLanguages() {
        assertEquals(R.string.flow_light, ClinicalLabels.findFlowResId("Ligero"))
        assertEquals(R.string.flow_light, ClinicalLabels.findFlowResId("Light"))
        assertEquals(R.string.flow_medium, ClinicalLabels.findFlowResId("Medio"))
        assertEquals(R.string.flow_medium, ClinicalLabels.findFlowResId("Medium"))
        assertEquals(R.string.flow_heavy, ClinicalLabels.findFlowResId("Abundante"))
        assertEquals(R.string.flow_heavy, ClinicalLabels.findFlowResId("Heavy"))

        assertEquals("Ligero", ClinicalLabels.getCanonicalFlow(R.string.flow_light))
        assertEquals("Medio", ClinicalLabels.getCanonicalFlow(R.string.flow_medium))
        assertEquals("Abundante", ClinicalLabels.getCanonicalFlow(R.string.flow_heavy))

        assertNull(ClinicalLabels.findFlowResId("desconocido"))
        assertNull(ClinicalLabels.findFlowResId(null))
    }

    @Test
    fun testMoodResolutionAndMatching() {
        assertEquals(R.string.mood_happy, ClinicalLabels.findMoodResId("Feliz"))
        assertEquals(R.string.mood_happy, ClinicalLabels.findMoodResId("Happy"))
        assertEquals(R.string.mood_sad, ClinicalLabels.findMoodResId("Triste"))
        assertEquals(R.string.mood_sad, ClinicalLabels.findMoodResId("Sad"))
        assertEquals(R.string.mood_irritated, ClinicalLabels.findMoodResId("Irritable"))
        assertEquals(R.string.mood_irritated, ClinicalLabels.findMoodResId("Irritated"))

        assertTrue(ClinicalLabels.matchesMood("Sad", R.string.mood_sad, R.string.mood_happy))
        assertTrue(ClinicalLabels.matchesMood("Triste", R.string.mood_sad, R.string.mood_happy))
        assertFalse(ClinicalLabels.matchesMood("Calm", R.string.mood_sad))
        assertFalse(ClinicalLabels.matchesMood(null, R.string.mood_sad))
    }

    @Test
    fun testSymptomResolutionAndLanguageIndependence() {
        assertEquals(R.string.symp_cramps, ClinicalLabels.findSymptomResId("Cólicos"))
        assertEquals(R.string.symp_cramps, ClinicalLabels.findSymptomResId("Cramps"))
        assertEquals(R.string.symp_headache, ClinicalLabels.findSymptomResId("Dolor de cabeza"))
        assertEquals(R.string.symp_headache, ClinicalLabels.findSymptomResId("Headache"))
        assertEquals(R.string.symp_fatigue, ClinicalLabels.findSymptomResId("Fatiga"))
        assertEquals(R.string.symp_fatigue, ClinicalLabels.findSymptomResId("Fatigue"))
        assertEquals(R.string.symp_fatigue, ClinicalLabels.findSymptomResId("Cansancio"))

        assertTrue(ClinicalLabels.hasAnySymptom(listOf("Headache", "Acne"), R.string.symp_headache))
        assertTrue(ClinicalLabels.hasAnySymptom(listOf("Dolor de cabeza"), R.string.symp_headache))
        assertTrue(ClinicalLabels.hasAnySymptom(listOf("Fatigue"), R.string.symp_fatigue))
        assertTrue(ClinicalLabels.hasAnySymptom(listOf("Cansancio"), R.string.symp_fatigue))
        assertFalse(ClinicalLabels.hasAnySymptom(listOf("Acne"), R.string.symp_cramps))
    }

    @Test
    fun testMucusAndMethodResolution() {
        assertEquals(R.string.mucus_egg_white, ClinicalLabels.findMucusResId("Clara de huevo"))
        assertEquals(R.string.mucus_egg_white, ClinicalLabels.findMucusResId("Egg white"))

        assertEquals(R.string.method_condom, ClinicalLabels.findMethodResId("Condón"))
        assertEquals(R.string.method_condom, ClinicalLabels.findMethodResId("Condom"))
        assertEquals(R.string.method_condom, ClinicalLabels.findMethodResId("Preservativo"))
        assertEquals(R.string.method_pill, ClinicalLabels.findMethodResId("Píldora"))
        assertEquals(R.string.method_pill, ClinicalLabels.findMethodResId("Pill"))
    }
}
