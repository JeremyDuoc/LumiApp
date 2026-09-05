package com.jeremy.lumi.ui.screens.partner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jeremy.lumi.R
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.otherDisplayName
import com.jeremy.lumi.domain.model.otherSharePreferences
import com.jeremy.lumi.domain.model.otherSnapshot
import com.jeremy.lumi.domain.repository.LumiRepository
import com.jeremy.lumi.domain.repository.PartnerRepository
import com.jeremy.lumi.domain.usecase.CyclePredictor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────────────────────────

/** Un día del mes con la fase de cada persona (null si no hay proyección). */
data class DualDay(
    val date: LocalDate,
    val myPhase: CyclePhase?,
    val otherPhase: CyclePhase?,
    val isToday: Boolean
)

data class DualCalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val days: List<DualDay> = emptyList(),
    val myName: String = "",
    val otherName: String? = null,
    val hasOtherData: Boolean = false,
    val menstrualOverlapDays: Int = 0,
    val isLoading: Boolean = true
)

/** Parámetros mínimos para proyectar las fases de un ciclo sobre un mes. */
private data class CycleProjection(
    val startDate: LocalDate,
    val cycleLength: Int,
    val periodLength: Int
) {
    fun phaseFor(date: LocalDate): CyclePhase {
        val day = CyclePredictor.dayInCycleWrapped(startDate, cycleLength, date)
        return CyclePredictor.phaseForDay(day, cycleLength, periodLength)
    }
}

/**
 * Combina MI ciclo (Room + predictor local) con el de la otra persona
 * (proyección mínima publicada en su snapshot de Firestore) para pintar
 * el Calendario Compartido. Nadie comparte historial: solo tres números.
 */
@HiltViewModel
class DualCalendarViewModel @Inject constructor(
    private val lumiRepository: LumiRepository,
    private val partnerRepository: PartnerRepository,
    private val prefsManager: OnboardingPreferenceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DualCalendarUiState())
    val uiState: StateFlow<DualCalendarUiState> = _uiState.asStateFlow()

    private var myProjection: CycleProjection? = null
    private var otherProjection: CycleProjection? = null
    private var observeJob: Job? = null

    fun load(linkId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.update { it.copy(myName = prefsManager.userNameFlow.first().orEmpty()) }

            // Mi proyección local (si registro ciclo; una observadora no tiene)
            myProjection = buildMyProjection()

            val myUid = partnerRepository.getCurrentUid()
            partnerRepository.observeLink(linkId).collect { link ->
                if (link == null) return@collect
                val snapshot = link.otherSnapshot(myUid)
                val sharesPredictions = link.otherSharePreferences(myUid).predictions
                otherProjection = snapshot
                    ?.takeIf { sharesPredictions }
                    ?.takeIf { (it.cycleStartDateMs ?: 0L) > 0L && it.cycleLengthDays >= 15 }
                    ?.let {
                        CycleProjection(
                            startDate = Instant.ofEpochMilli(it.cycleStartDateMs!!)
                                .atZone(ZoneOffset.UTC).toLocalDate(),
                            cycleLength = it.cycleLengthDays,
                            periodLength = it.periodLengthDays.coerceIn(1, 10)
                        )
                    }
                _uiState.update {
                    it.copy(
                        otherName = link.otherDisplayName(myUid),
                        hasOtherData = otherProjection != null,
                        isLoading = false
                    )
                }
                rebuildMonth()
            }
        }
    }

    fun previousMonth() {
        _uiState.update { it.copy(month = it.month.minusMonths(1)) }
        rebuildMonth()
    }

    fun nextMonth() {
        _uiState.update { it.copy(month = it.month.plusMonths(1)) }
        rebuildMonth()
    }

    private suspend fun buildMyProjection(): CycleProjection? {
        val active = lumiRepository.getCurrentActiveCycle() ?: return null
        val closed = lumiRepository.getClosedCycles()
        val length = if (closed.size >= 3) {
            CyclePredictor.weightedAverageCycleLength(closed).toInt().coerceIn(15, 60)
        } else {
            active.cycleLength.coerceAtLeast(15)
        }
        return CycleProjection(
            startDate = Instant.ofEpochMilli(active.startDate).atZone(ZoneOffset.UTC).toLocalDate(),
            cycleLength = length,
            periodLength = active.periodLength.coerceIn(1, 10)
        )
    }

    private fun rebuildMonth() {
        val state = _uiState.value
        val today = com.jeremy.lumi.utils.LumiClock.today()
        val days = (1..state.month.lengthOfMonth()).map { dayOfMonth ->
            val date = state.month.atDay(dayOfMonth)
            DualDay(
                date = date,
                myPhase = myProjection?.phaseFor(date),
                otherPhase = otherProjection?.phaseFor(date),
                isToday = date == today
            )
        }
        val overlap = days.count {
            it.myPhase == CyclePhase.MENSTRUAL && it.otherPhase == CyclePhase.MENSTRUAL
        }
        _uiState.update { it.copy(days = days, menstrualOverlapDays = overlap) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Pantalla
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualCalendarScreen(
    linkId: String,
    onBack: () -> Unit,
    viewModel: DualCalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(linkId) { viewModel.load(linkId) }

    val otherLabel = uiState.otherName ?: stringResource(R.string.pm_dual_partner_fallback)
    val myLabel = uiState.myName.ifBlank { stringResource(R.string.pm_dual_me) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.pm_dual_title),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.pm_dual_subtitle, myLabel, otherLabel),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.pm_back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(2.dp))

            // ── Navegación de mes ──
            val locale = LocalConfiguration.current.locales[0]
            val monthTitle = uiState.month
                .format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
                .replaceFirstChar { it.uppercase(locale) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::previousMonth) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = stringResource(R.string.pm_dual_month_prev),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = monthTitle,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = viewModel::nextMonth) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = stringResource(R.string.pm_dual_month_next),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ── Banner de coincidencias ──
            if (uiState.hasOtherData) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.menstrualOverlapDays > 0)
                            phaseColor(CyclePhase.MENSTRUAL).copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = if (uiState.menstrualOverlapDays > 0)
                            stringResource(R.string.pm_dual_overlap_banner, uiState.menstrualOverlapDays)
                        else
                            stringResource(R.string.pm_dual_overlap_none),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            } else if (!uiState.isLoading) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.pm_dual_no_partner_data, otherLabel),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.pm_dual_no_partner_data_desc),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // ── Cabecera de días de la semana (lunes primero) ──
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf(
                    R.string.cal_mon, R.string.cal_tue, R.string.cal_wed, R.string.cal_thu,
                    R.string.cal_fri, R.string.cal_sat, R.string.cal_sun
                ).forEach { res ->
                    Text(
                        text = stringResource(res),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                    )
                }
            }

            // ── Rejilla del mes ──
            val offset = uiState.month.atDay(1).dayOfWeek.value - 1  // lunes = 0
            val cells: List<DualDay?> = List(offset) { null } + uiState.days
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(modifier = Modifier.weight(1f)) {
                            if (day != null) DualDayCell(day)
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            // ── Leyenda ──
            DualLegend(myLabel = myLabel, otherLabel = otherLabel)
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Celda de día con círculo dividido: mitad izquierda = mi fase,
 * mitad derecha = la fase de la otra persona.
 */
@Composable
private fun DualDayCell(day: DualDay) {
    val myColor = day.myPhase?.let { phaseColor(it) }
    val otherColor = day.otherPhase?.let { phaseColor(it) }
    val todayRing = MaterialTheme.colorScheme.primary
    val hasFill = myColor != null || otherColor != null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(3.dp)
            .then(
                if (day.isToday)
                    Modifier.border(2.dp, todayRing, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = size.minDimension * 0.86f
            val strokeWidth = 8f
            val topLeft = Offset(
                (size.width - diameter) / 2f + strokeWidth / 2f,
                (size.height - diameter) / 2f + strokeWidth / 2f
            )
            val arcSize = Size(diameter - strokeWidth, diameter - strokeWidth)
            
            // Si ninguno tiene datos, pintamos un anillo de fondo muy sutil
            if (!hasFill) {
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.15f),
                    radius = (diameter - strokeWidth) / 2f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )
            }

            // Mitad izquierda: yo (stroke)
            myColor?.let {
                drawArc(
                    color = it.copy(alpha = 0.9f),
                    startAngle = 90f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
            // Mitad derecha: la otra persona (stroke)
            otherColor?.let {
                drawArc(
                    color = it.copy(alpha = 0.9f),
                    startAngle = 270f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
        }
        Text(
            text = day.date.dayOfMonth.toString(),
            fontSize = 12.sp,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (hasFill) 0.95f else 0.5f)
        )
    }
}

@Composable
private fun DualLegend(myLabel: String, otherLabel: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Quién es cada mitad
            Row(verticalAlignment = Alignment.CenterVertically) {
                HalfCircleSample(left = true)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = myLabel,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(20.dp))
                HalfCircleSample(left = false)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = otherLabel,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            // Colores por fase
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    CyclePhase.MENSTRUAL, CyclePhase.FOLLICULAR,
                    CyclePhase.OVULATION, CyclePhase.LUTEAL
                ).forEach { phase ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(phaseColor(phase), CircleShape)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = stringResource(phaseLabelRes(phase)),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HalfCircleSample(left: Boolean) {
    Canvas(modifier = Modifier.size(14.dp)) {
        drawArc(
            color = Color(0xFF9E9E9E),
            startAngle = if (left) 90f else 270f,
            sweepAngle = 180f,
            useCenter = true
        )
    }
}
