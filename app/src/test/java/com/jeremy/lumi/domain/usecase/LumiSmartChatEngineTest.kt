package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.BioVariable
import com.jeremy.lumi.domain.model.CyclePhase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LumiSmartChatEngineTest {

    private lateinit var chatEngine: LumiSmartChatEngine
    private lateinit var correlationEngine: AdvancedBioCorrelationMatrixEngine
    private lateinit var clusterEngine: SymptomClusterMiningEngine
    private lateinit var medicalDetector: MedicalPatternDetector
    private lateinit var stringResolver: ChatStringResolver

    @Before
    fun setup() {
        correlationEngine = AdvancedBioCorrelationMatrixEngine()
        clusterEngine = SymptomClusterMiningEngine()
        medicalDetector = MedicalPatternDetector()
        stringResolver = TestChatStringResolver()
        chatEngine = LumiSmartChatEngine(
            correlationMatrixEngine = correlationEngine,
            clusterMiningEngine = clusterEngine,
            medicalPatternDetector = medicalDetector,
            stringResolver = stringResolver
        )
    }

    class TestChatStringResolver : ChatStringResolver {
        private val stringMap = mutableMapOf<String, String>()
        private val idToNameMap = mutableMapOf<Int, String>()

        init {
            try {
                val rStringClass = Class.forName("com.jeremy.lumi.R\$string")
                rStringClass.fields.forEach { field ->
                    try {
                        val id = field.getInt(null)
                        idToNameMap[id] = field.name
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}

            val possibleFiles = listOf(
                File("src/main/res/values/strings_chat.xml"),
                File("app/src/main/res/values/strings_chat.xml"),
                File("../app/src/main/res/values/strings_chat.xml")
            )
            val file = possibleFiles.firstOrNull { it.exists() }
            if (file != null) {
                val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
                val stringNodes = doc.getElementsByTagName("string")
                for (i in 0 until stringNodes.length) {
                    val node = stringNodes.item(i)
                    val name = node.attributes?.getNamedItem("name")?.nodeValue
                    val text = node.textContent
                    if (name != null && text != null) {
                        stringMap[name] = text.replace("\\n", "\n")
                    }
                }
            }
        }

        override fun getString(resId: Int): String {
            val name = idToNameMap[resId] ?: return ""
            return stringMap[name] ?: ""
        }

        override fun getString(resId: Int, vararg formatArgs: Any): String {
            val raw = getString(resId)
            return if (raw.isNotEmpty()) {
                try {
                    String.format(raw, *formatArgs)
                } catch (e: Exception) {
                    raw
                }
            } else ""
        }

        override fun getStringArray(resId: Int): Array<String> = emptyArray()

        override fun getRandomStringFromArray(resId: Int): String = ""

        override fun formatPhaseName(phase: CyclePhase): String = when (phase) {
            CyclePhase.MENSTRUAL  -> getString(R.string.phase_menstrual_name).ifEmpty { "Fase Menstrual" }
            CyclePhase.FOLLICULAR -> getString(R.string.phase_follicular_name).ifEmpty { "Fase Folicular" }
            CyclePhase.OVULATION  -> getString(R.string.phase_ovulation_name).ifEmpty { "Fase Ovulatoria" }
            CyclePhase.LUTEAL     -> getString(R.string.phase_luteal_name).ifEmpty { "Fase Lútea" }
            CyclePhase.PREGNANCY  -> getString(R.string.phase_pregnancy_name).ifEmpty { "Embarazo" }
            CyclePhase.UNKNOWN    -> getString(R.string.phase_unknown_name).ifEmpty { "Ciclo" }
        }

        override fun formatBioVarName(variable: BioVariable): String = when (variable) {
            BioVariable.SLEEP_HOURS     -> getString(R.string.bio_var_sleep).ifEmpty { "Tus horas de sueño" }
            BioVariable.PAIN_LEVEL      -> getString(R.string.bio_var_pain).ifEmpty { "Tu nivel de dolor" }
            BioVariable.STRESS_LEVEL    -> getString(R.string.bio_var_stress).ifEmpty { "Tu nivel de estrés" }
            BioVariable.ENERGY_LEVEL    -> getString(R.string.bio_var_energy).ifEmpty { "Tu nivel de energía" }
            BioVariable.WATER_INTAKE    -> getString(R.string.bio_var_water).ifEmpty { "Tu consumo de agua" }
            BioVariable.BASAL_BODY_TEMP -> getString(R.string.bio_var_bbt).ifEmpty { "Tu temperatura basal" }
        }
    }

    private fun mockCycleContext(
        phase: CyclePhase = CyclePhase.FOLLICULAR,
        dayOfCycle: Int = 10,
        cycleLength: Int = 28,
        daysUntilPeriod: Int = 18,
        daysUntilOvulation: Int = 4,
        isLate: Boolean = false,
        delayDays: Int = 0,
        painLevel: Int = 0,
        sleepHours: Float? = 7.5f,
        isOnContraceptive: Boolean = false
    ): CycleContext {
        return CycleContext(
            phase = phase,
            dayOfCycle = dayOfCycle,
            dayOfPhase = 5,
            daysUntilPeriod = daysUntilPeriod,
            daysUntilOvulation = daysUntilOvulation,
            cycleLength = cycleLength,
            isLate = isLate,
            delayDays = delayDays,
            todayPainLevel = painLevel,
            todaySleepHours = sleepHours,
            isOnContraceptive = isOnContraceptive
        )
    }

    @Test
    fun `test unprotected sex response provides emergency contraception and test guidance`() = runBlocking {
        val query = "Tuve relaciones sin protección, ¿qué debo hacer?"
        val ctx = mockCycleContext(phase = CyclePhase.OVULATION, dayOfCycle = 14)

        val response = chatEngine.generateResponse(query, ctx)

        // Verifies emergency contraception advice and timings
        assertTrue(response.contains("Levonorgestrel", ignoreCase = true))
        assertTrue(response.contains("72 horas", ignoreCase = true))
        assertTrue(response.contains("Ulipristal", ignoreCase = true))
        assertTrue(response.contains("120 horas", ignoreCase = true))
        assertTrue(response.contains("prueba de embarazo", ignoreCase = true))
        assertTrue(response.contains("14", ignoreCase = true))
    }

    @Test
    fun `test structured question answering for ChatQuestion enum`() = runBlocking {
        val ctx = mockCycleContext(phase = CyclePhase.MENSTRUAL, dayOfCycle = 2, painLevel = 6)
        val response = chatEngine.generateAnswerForQuestion(ChatQuestion.WHY_CRAMPS, ctx)

        assertTrue(response.contains("prostaglandinas", ignoreCase = true))
        assertTrue(response.contains("calor local", ignoreCase = true))
    }

    @Test
    fun `test menstrual cramps advice includes evidence based recommendations`() = runBlocking {
        val query = "Tengo cólicos muy fuertes, me duele el vientre"
        val ctx = mockCycleContext(phase = CyclePhase.MENSTRUAL, dayOfCycle = 2, painLevel = 7)

        val response = chatEngine.generateResponse(query, ctx)

        assertTrue(response.contains("prostaglandinas", ignoreCase = true))
        assertTrue(response.contains("calor", ignoreCase = true))
        assertTrue(response.contains("magnesio", ignoreCase = true))
        assertTrue(response.contains("7/10"))
    }

    @Test
    fun `test headache and migraine response explains estrogen drop`() = runBlocking {
        val query = "Tengo una migraña horrible"
        val ctx = mockCycleContext(phase = CyclePhase.LUTEAL, dayOfCycle = 25, daysUntilPeriod = 3)

        val response = chatEngine.generateResponse(query, ctx)

        assertTrue(response.contains("migraña", ignoreCase = true) || response.contains("migrana", ignoreCase = true))
        assertTrue(response.contains("estrógeno", ignoreCase = true) || response.contains("estrogeno", ignoreCase = true))
        assertTrue(response.contains("vasos", ignoreCase = true))
    }

    @Test
    fun `test late period query analyzes delay reasons and pregnancy test guidance`() = runBlocking {
        val query = "¿Por qué se retrasó mi periodo?"
        val ctx = mockCycleContext(isLate = true, delayDays = 5, dayOfCycle = 33)

        val response = chatEngine.generateResponse(query, ctx)

        assertTrue(response.contains("5 días") || response.contains("5 dias"))
        assertTrue(response.contains("ovulación", ignoreCase = true) || response.contains("ovulacion", ignoreCase = true))
        assertTrue(response.contains("cortisol", ignoreCase = true) || response.contains("estrés", ignoreCase = true) || response.contains("estres", ignoreCase = true))
        assertTrue(response.contains("embarazo", ignoreCase = true))
    }

    @Test
    fun `test fertility query respects contraceptive mode`() = runBlocking {
        val query = "¿Puedo quedar embarazada hoy?"
        val ctx = mockCycleContext(dayOfCycle = 14, isOnContraceptive = true)

        val response = chatEngine.generateResponse(query, ctx)

        assertTrue(response.contains("anticonceptivo", ignoreCase = true))
        assertTrue(response.contains("inferior al 1%", ignoreCase = true) || response.contains("inhibir la ovulación", ignoreCase = true) || response.contains("inhibir la ovulacion", ignoreCase = true))
    }

    @Test
    fun `test fertility query identifies peak ovulation in natural cycle`() = runBlocking {
        val query = "¿Estoy en mis días fértiles?"
        val ctx = mockCycleContext(dayOfCycle = 14, cycleLength = 28, isOnContraceptive = false)

        val response = chatEngine.generateResponse(query, ctx)

        assertTrue(response.contains("Ovulación", ignoreCase = true) || response.contains("Ovulacion", ignoreCase = true))
        assertTrue(response.contains("Ventana Fértil", ignoreCase = true) || response.contains("Ventana Fertil", ignoreCase = true))
        assertTrue(response.contains("clara de huevo", ignoreCase = true))
    }

    @Test
    fun `test mood response explains neurotransmitters and PMDD`() = runBlocking {
        val query = "¿Por qué estoy tan irritable y con ganas de llorar?"
        val ctx = mockCycleContext(phase = CyclePhase.LUTEAL, dayOfCycle = 26)

        val response = chatEngine.generateResponse(query, ctx)

        assertTrue(response.contains("progesterona", ignoreCase = true))
        assertTrue(response.contains("TDPM", ignoreCase = true) || response.contains("serotonina", ignoreCase = true) || response.contains("GABA", ignoreCase = true))
    }

    @Test
    fun `test sleep response explains luteal body temperature connection`() = runBlocking {
        val query = "Tengo insomnio y no puedo dormir nada"
        val ctx = mockCycleContext(phase = CyclePhase.LUTEAL, dayOfCycle = 24, sleepHours = 4.5f)

        val response = chatEngine.generateResponse(query, ctx)

        assertTrue(response.contains("temperatura", ignoreCase = true))
        assertTrue(response.contains("progesterona", ignoreCase = true))
        assertTrue(response.contains("melatonina", ignoreCase = true))
    }

    @Test
    fun `test phase specific nutrition advice`() = runBlocking {
        val query = "¿Qué debo comer hoy?"
        val ctx = mockCycleContext(phase = CyclePhase.MENSTRUAL)

        val response = chatEngine.generateResponse(query, ctx)

        assertTrue(response.contains("hierro", ignoreCase = true))
        assertTrue(response.contains("espinacas", ignoreCase = true) || response.contains("lentejas", ignoreCase = true))
    }

    @Test
    fun `test phase specific exercise recommendation`() = runBlocking {
        val query = "¿Qué ejercicio puedo hacer hoy?"
        val ctx = mockCycleContext(phase = CyclePhase.OVULATION)

        val response = chatEngine.generateResponse(query, ctx)

        assertTrue(response.contains("HIIT", ignoreCase = true) || response.contains("potencia", ignoreCase = true) || response.contains("intensidad", ignoreCase = true))
    }

    @Test
    fun `test contextual fallback handles miscellaneous queries gracefully`() = runBlocking {
        val query = "Hola Lumi, me siento rara hoy"
        val ctx = mockCycleContext(dayOfCycle = 12)

        val response = chatEngine.generateResponse(query, ctx)

        assertTrue(response.isNotEmpty())
        assertTrue(response.contains("día 12") || response.contains("dia 12"))
    }
}
