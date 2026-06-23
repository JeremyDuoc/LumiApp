package com.jeremy.lumi.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.jeremy.lumi.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jeremy.lumi.domain.model.UserGoal
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
//  ONBOARDING SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(
    viewModel  : OnboardingViewModel = hiltViewModel(),
    onComplete : () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope   = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.fillMaxSize()) {

            // ── Indicador de progreso (oculto en página de celebración) ────────────────
            if (uiState.currentPage < 5) {
                Spacer(Modifier.height(52.dp))
                PageIndicator(
                    totalPages   = 5,
                    currentPage  = uiState.currentPage,
                    modifier     = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(32.dp))
            } else {
                Spacer(Modifier.height(52.dp))
            }

            // ── Contenido de la página activa ────────────────────────────────
            AnimatedContent(
                targetState  = uiState.currentPage,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { if (forward) it else -it } + fadeIn(tween(320)))
                        .togetherWith(slideOutHorizontally { if (forward) -it else it } + fadeOut(tween(220)))
                },
                modifier     = Modifier.weight(1f),
                label        = "onboarding_page"
            ) { page ->
                when (page) {
                    0 -> PageWelcome()
                    1 -> PageName(
                        name     = uiState.data.userName ?: "",
                        onChange = { viewModel.setUserName(it) }
                    )
                    2 -> PageLastPeriod(
                        selectedDate = uiState.data.lastPeriodDate,
                        onDatePicked = { viewModel.setLastPeriodDate(it) }
                    )
                    3 -> PageCycleLength(
                        days     = uiState.data.cycleLength,
                        onChange = { viewModel.setCycleLength(it) }
                    )
                    4 -> PagePeriodAndGoal(
                        periodLength = uiState.data.periodLength,
                        selectedGoal = uiState.data.userGoal,
                        onPeriodChange = { viewModel.setPeriodLength(it) },
                        onGoalChange   = { viewModel.setUserGoal(it) }
                    )
                    5 -> PageCelebration(
                        userName   = uiState.data.userName,
                        onComplete = { viewModel.completeOnboarding(onComplete) }
                    )
                }
            }

            // ── Botonera inferior (oculta en página de celebración) ────────────
            if (uiState.currentPage < 5) {
                BottomNavButtons(
                    currentPage  = uiState.currentPage,
                    isCompleting = uiState.isCompleting,
                    onBack       = { viewModel.prevPage() },
                    onNext       = {
                        if (uiState.currentPage < 4) viewModel.nextPage()
                        else viewModel.nextPage() // 4 → 5 (celebración)
                    },
                    onSkip       = { viewModel.nextPage() }
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  INDICADOR DE PÁGINAS — dots animados estilo iOS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PageIndicator(totalPages: Int, currentPage: Int, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(totalPages) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (selected) 24.dp else 8.dp,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                label = "dot_width_$index"
            )
            val alpha by animateFloatAsState(
                targetValue = if (selected) 1f else 0.35f,
                animationSpec = tween(300),
                label = "dot_alpha_$index"
            )
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .alpha(alpha)
                    .background(primary)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BOTONERA INFERIOR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BottomNavButtons(
    currentPage  : Int,
    isCompleting : Boolean,
    onBack       : () -> Unit,
    onNext       : () -> Unit,
    onSkip       : () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier            = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Botón principal
        Button(
            onClick  = onNext,
            enabled  = !isCompleting,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(18.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = primary)
        ) {
            if (isCompleting) {
                CircularProgressIndicator(
                    color    = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text       = if (currentPage < 4) "Continuar" else "Comenzar mi ciclo ✨",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 16.sp
                )
            }
        }

        // Fila secundaria: Atrás (izq) + Saltar nombre (der, solo en página 1)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            if (currentPage > 0) {
                TextButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Atrás", fontSize = 14.sp)
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }

            // "Saltar" solo en la página del nombre (es el único opcional)
            if (currentPage == 1) {
                TextButton(onClick = onSkip) {
                    Text(
                        "Saltar este paso",
                        fontSize = 14.sp,
                        color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PÁGINA 0 — BIENVENIDA
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PageWelcome() {
    val primary   = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    // Animaciones de entrada escalonadas
    val alpha1  = remember { Animatable(0f) }
    val scale1  = remember { Animatable(0.7f) }
    val alpha2  = remember { Animatable(0f) }
    val offset2 = remember { Animatable(20f) }
    val alpha3  = remember { Animatable(0f) }
    val offset3 = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        // Logo
        launch { alpha1.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
        launch { scale1.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
        // Título (delay 300ms)
        launch {
            kotlinx.coroutines.delay(300)
            launch { alpha2.animateTo(1f, tween(500)) }
            launch { offset2.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
        }
        // Subtítulo (delay 500ms)
        launch {
            kotlinx.coroutines.delay(500)
            launch { alpha3.animateTo(1f, tween(500)) }
            launch { offset3.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
        }
    }

    Column(
        modifier            = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo animado
        Box(
            Modifier
                .size(120.dp)
                .scale(scale1.value)
                .alpha(alpha1.value)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(primary, secondary.copy(alpha = 0.7f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Rounded.AutoAwesome,
                contentDescription = "Lumi logo",
                tint               = Color.White,
                modifier           = Modifier.size(56.dp)
            )
        }

        Spacer(Modifier.height(36.dp))

        // Título
        Text(
            text       = "Bienvenida a Lumi",
            fontSize   = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center,
            color      = MaterialTheme.colorScheme.onBackground,
            modifier   = Modifier
                .alpha(alpha2.value)
                .graphicsLayer { translationY = offset2.value.dp.toPx() }
        )

        Spacer(Modifier.height(16.dp))

        // Tagline
        Text(
            text      = "Tu ciclo, tu privacidad, tu control.",
            fontSize  = 16.sp,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            lineHeight = 24.sp,
            modifier  = Modifier
                .alpha(alpha3.value)
                .graphicsLayer { translationY = offset3.value.dp.toPx() }
        )

        Spacer(Modifier.height(12.dp))

        // Nota de privacidad
        Box(
            Modifier
                .alpha(alpha3.value)
                .graphicsLayer { translationY = offset3.value.dp.toPx() }
                .clip(RoundedCornerShape(14.dp))
                .background(primary.copy(alpha = 0.08f))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Security, null, Modifier.size(15.dp), primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text      = "Todo guardado en tu teléfono. Nunca en la nube.",
                    fontSize  = 12.sp,
                    color     = primary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PÁGINA 1 — NOMBRE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PageName(name: String, onChange: (String) -> Unit) {
    PageShell(
        icon      = Icons.Rounded.Person,
        title     = "¿Cómo te llamas?",
        subtitle  = "Lo usaremos solo para saludarte. Puedes saltarte este paso si prefieres."
    ) {
        OutlinedTextField(
            value         = name,
            onValueChange = onChange,
            placeholder   = { Text(stringResource(R.string.onboarding_name_hint)) },
            singleLine    = true,
            shape         = RoundedCornerShape(16.dp),
            modifier      = Modifier.fillMaxWidth(),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
            )
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text     = "Este dato nunca sale de tu dispositivo.",
            fontSize = 11.sp,
            color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PÁGINA 2 — FECHA DE ÚLTIMA REGLA (Wheel picker)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PageLastPeriod(selectedDate: Long, onDatePicked: (Long) -> Unit) {
    // Rango: últimos 90 días hasta hoy
    val today  = LocalDate.now()
    val dates  = remember { (0..89).map { today.minusDays(it.toLong()) } }

    // Índice activo: el que corresponde a la fecha guardada, o hoy (0)
    val initialIndex = remember(selectedDate) {
        if (selectedDate == 0L) 0
        else {
            val selected = Instant.ofEpochMilli(selectedDate)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            dates.indexOfFirst { it == selected }.coerceAtLeast(0)
        }
    }

    // Cuando el wheel picker hace su primer scroll a la posición inicial,
    // emite la fecha automáticamente via onDateSelected en DateWheelPicker.
    // Solo necesitamos pre-seleccionar "hoy" si el usuario llega sin fecha guardada.
    // El LaunchedEffect anterior sobreescribía silenciosamente sin confirmación del usuario —
    // ahora dejamos que el wheel emita la fecha al posicionarse (ver DateWheelPicker.LaunchedEffect).

    PageShell(
        icon     = Icons.Rounded.CalendarToday,
        title    = "¿Cuándo fue tu última regla?",
        subtitle = "Una fecha aproximada está bien. Podrás editarla después desde el calendario."
    ) {
        DateWheelPicker(
            dates        = dates,
            initialIndex = initialIndex,
            onDateSelected = { date ->
                val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                onDatePicked(millis)
            }
        )

        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick = {
                val todayMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                onDatePicked(todayMillis)
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text     = "No lo recuerdo — usar hoy",
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  WHEEL DATE PICKER — LazyColumn con snap y efecto de escala central
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DateWheelPicker(
    dates          : List<LocalDate>,
    initialIndex   : Int,
    onDateSelected : (LocalDate) -> Unit
) {
    val itemHeightDp    = 52.dp
    val visibleItems    = 5
    val density         = LocalDensity.current
    val itemHeightPx    = with(density) { itemHeightDp.toPx() }
    val listState       = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior   = rememberSnapFlingBehavior(listState)
    val formatter       = remember { DateTimeFormatter.ofPattern("d 'de' MMMM · yyyy", Locale("es")) }

    // Emitir la fecha central cada vez que el scroll se detiene
    val centerIndex by remember {
        derivedStateOf {
            val offset = listState.firstVisibleItemScrollOffset
            val first  = listState.firstVisibleItemIndex
            if (offset > itemHeightPx / 2) first + 1 else first
        }
    }

    LaunchedEffect(centerIndex) {
        if (centerIndex in dates.indices) onDateSelected(dates[centerIndex])
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(itemHeightDp * visibleItems)
    ) {
        // Líneas seleccionadoras
        repeat(2) { i ->
            val topOffset = (itemHeightDp * ((visibleItems / 2) + i))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .offset(y = topOffset)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            )
        }

        LazyColumn(
            state         = listState,
            flingBehavior = flingBehavior,
            modifier      = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeightDp * (visibleItems / 2))
        ) {
            items(dates.size) { index ->
                val distFromCenter = abs(centerIndex - index)
                val scale  by animateFloatAsState(
                    targetValue   = when (distFromCenter) { 0 -> 1f; 1 -> 0.88f; else -> 0.75f },
                    animationSpec = tween(150),
                    label         = "wheel_scale_$index"
                )
                val alpha by animateFloatAsState(
                    targetValue   = when (distFromCenter) { 0 -> 1f; 1 -> 0.6f; else -> 0.3f },
                    animationSpec = tween(150),
                    label         = "wheel_alpha_$index"
                )

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(itemHeightDp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .alpha(alpha),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = dates[index].format(formatter),
                        fontSize   = if (distFromCenter == 0) 17.sp else 15.sp,
                        fontWeight = if (distFromCenter == 0) FontWeight.SemiBold else FontWeight.Normal,
                        color      = if (distFromCenter == 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground,
                        textAlign  = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PÁGINA 3 — DURACIÓN DEL CICLO
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PageCycleLength(days: Int, onChange: (Int) -> Unit) {
    PageShell(
        icon     = Icons.Rounded.Loop,
        title    = "¿Cuánto dura tu ciclo?",
        subtitle = "Desde el primer día de tu regla hasta el día antes de la siguiente."
    ) {
        // Valor grande central
        Text(
            text       = "$days días",
            fontSize   = 48.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.primary,
            modifier   = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(4.dp))

        // Etiqueta contextual
        val label = when {
            days < 24 -> "Ciclo corto"
            days > 35 -> "Ciclo largo"
            else      -> "Ciclo típico ✓"
        }
        Text(
            text     = label,
            fontSize = 13.sp,
            color    = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(24.dp))

        Slider(
            value         = days.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange    = 21f..45f,
            steps         = 23,
            modifier      = Modifier.fillMaxWidth(),
            colors        = SliderDefaults.colors(
                thumbColor       = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("21", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
            Text("45", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
        }

        Spacer(Modifier.height(16.dp))

        TextButton(
            onClick  = { onChange(28) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                "No lo sé — usar 28 días",
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PÁGINA 4 — DURACIÓN DEL PERIODO + OBJETIVO
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PagePeriodAndGoal(
    periodLength   : Int,
    selectedGoal   : UserGoal,
    onPeriodChange : (Int) -> Unit,
    onGoalChange   : (UserGoal) -> Unit
) {
    PageShell(
        icon     = Icons.Rounded.WaterDrop,
        title    = "Últimos detalles",
        subtitle = "Casi lista. Solo dos cosas más."
    ) {
        // ── Duración del periodo ─────────────────────────────────────────────
        Text(
            "Duración de tu regla",
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))

        Text(
            text       = "$periodLength días",
            fontSize   = 36.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.primary,
            modifier   = Modifier.align(Alignment.CenterHorizontally)
        )

        Slider(
            value         = periodLength.toFloat(),
            onValueChange = { onPeriodChange(it.toInt()) },
            valueRange    = 1f..10f,
            steps         = 8,
            modifier      = Modifier.fillMaxWidth(),
            colors        = SliderDefaults.colors(
                thumbColor       = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
        Spacer(Modifier.height(20.dp))

        // ── Objetivo ─────────────────────────────────────────────────────────
        Text(
            "¿Para qué usarás Lumi?",
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))

        val goals = listOf(
            Triple(UserGoal.TRACK_CYCLE,       Icons.Rounded.Loop,         "Control de ciclo"),
            Triple(UserGoal.AVOID_PREGNANCY,   Icons.Rounded.Shield,       "Evitar embarazo"),
            Triple(UserGoal.SEEK_PREGNANCY,    Icons.Rounded.ChildCare,    "Buscar embarazo"),
            Triple(UserGoal.HEALTH_MONITORING, Icons.Rounded.MonitorHeart, "Salud general")
        )

        goals.chunked(2).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { (goal, icon, label) ->
                    GoalChip(
                        icon       = icon,
                        label      = label,
                        selected   = selectedGoal == goal,
                        onClick    = { onGoalChange(goal) },
                        modifier   = Modifier.weight(1f)
                    )
                }
                // Si la fila tiene solo 1 elemento, añadir spacer
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun GoalChip(
    icon     : ImageVector,
    label    : String,
    selected : Boolean,
    onClick  : () -> Unit,
    modifier : Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val bgColor by animateColorAsState(
        targetValue   = if (selected) primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(250),
        label         = "goal_chip_bg"
    )
    val borderColor by animateColorAsState(
        targetValue   = if (selected) primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
        animationSpec = tween(250),
        label         = "goal_chip_border"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(
                indication         = null,
                interactionSource  = remember { MutableInteractionSource() },
                onClick            = onClick
            )
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector  = icon,
            contentDescription = null,
            tint         = if (selected) primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            modifier     = Modifier.size(22.dp)
        )
        Text(
            text       = label,
            fontSize   = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign  = TextAlign.Center,
            color      = if (selected) primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            lineHeight = 16.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PAGE SHELL — estructura común a las páginas 1-4
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PageShell(
    icon     : ImageVector,
    title    : String,
    subtitle : String,
    content  : @Composable ColumnScope.() -> Unit
) {
    val alpha  = remember { Animatable(0f) }
    val offset = remember { Animatable(16f) }
    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing)) }
        launch { offset.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .alpha(alpha.value)
            .graphicsLayer { translationY = offset.value.dp.toPx() },
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Ícono de sección
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(26.dp), MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text       = title,
            fontSize   = 24.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground,
            lineHeight = 30.sp
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text      = subtitle,
            fontSize  = 14.sp,
            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(28.dp))

        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PÁGINA 5 — CELEBRACIÓN "¡Todo listo!"
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PageCelebration(
    userName  : String?,
    onComplete: () -> Unit
) {
    val primary   = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    // Animaciones de entrada escalonadas
    val alphaLogo  = remember { Animatable(0f) }
    val scaleLogo  = remember { Animatable(0.4f) }
    val alphaText  = remember { Animatable(0f) }
    val alphaBtn   = remember { Animatable(0f) }
    val offsetText = remember { Animatable(30f) }

    // Pulso continuo del icono
    val pulse = remember { Animatable(1f) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Entrada del logo
        launch { alphaLogo.animateTo(1f, tween(500)) }
        launch { scaleLogo.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
        // Texto (delay 400ms)
        launch {
            kotlinx.coroutines.delay(400)
            launch { alphaText.animateTo(1f, tween(500)) }
            launch { offsetText.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) }
        }
        // Botón (delay 700ms)
        launch {
            kotlinx.coroutines.delay(700)
            alphaBtn.animateTo(1f, tween(400))
        }
        // Pulso continuo del logo
        launch {
            kotlinx.coroutines.delay(600)
            while (true) {
                pulse.animateTo(1.08f, tween(900, easing = FastOutSlowInEasing))
                pulse.animateTo(1f,    tween(900, easing = FastOutSlowInEasing))
            }
        }
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icono animado con pulso
        Box(
            Modifier
                .size(120.dp)
                .scale(scaleLogo.value * pulse.value)
                .alpha(alphaLogo.value)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(primary, secondary.copy(alpha = 0.6f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("✨", fontSize = 52.sp)
        }

        Spacer(Modifier.height(36.dp))

        // Saludo personalizado
        Column(
            modifier            = Modifier
                .alpha(alphaText.value)
                .graphicsLayer { translationY = offsetText.value.dp.toPx() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text       = if (!userName.isNullOrBlank()) "¡Todo listo, $userName!" else "¡Todo listo!",
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text      = "Lumi ya conoce tu ciclo.\nTe acompañará cada día. 🌸",
                fontSize  = 16.sp,
                textAlign = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                lineHeight = 24.sp
            )
            Spacer(Modifier.height(8.dp))
            // Nota de privacidad breve
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(primary.copy(alpha = 0.08f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Security, null, Modifier.size(14.dp), primary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Todo guardado solo en tu teléfono.",
                        fontSize  = 11.sp,
                        color     = primary
                    )
                }
            }
        }

        Spacer(Modifier.height(44.dp))

        // Botón principal
        Button(
            onClick  = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .alpha(alphaBtn.value),
            shape  = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primary)
        ) {
            Text(
                text       = "Empezar con Lumi 🌙",
                fontWeight = FontWeight.SemiBold,
                fontSize   = 16.sp
            )
        }
    }
}
