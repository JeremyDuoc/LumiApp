package com.jeremy.lumi.ui.screens.onboarding

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  ONBOARDING SCREEN
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun OnboardingScreen(
    viewModel  : OnboardingViewModel = hiltViewModel(),
    onComplete : () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope   = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.fillMaxSize()) {

            // â”€â”€ Indicador de progreso (oculto en página de celebración) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (uiState.currentPage < 6) {
                Spacer(Modifier.height(52.dp))
                PageIndicator(
                    totalPages   = 6,
                    currentPage  = uiState.currentPage,
                    modifier     = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(32.dp))
            } else {
                Spacer(Modifier.height(52.dp))
            }

            // â”€â”€ Contenido de la página activa â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
                    0 -> PageWelcome(
                        onTrackCycleClick = { viewModel.nextPage() },
                        onObserverClick   = { viewModel.completeObserverOnboarding(onComplete) }
                    )
                    1 -> PageName(
                        name     = uiState.data.userName ?: "",
                        onChange = { viewModel.setUserName(it) }
                    )
                    2 -> PageRegularity(
                        isRegular = uiState.data.isRegular,
                        onChange  = { viewModel.setRegularity(it) }
                    )
                    3 -> PageCycleLength(
                        days      = uiState.data.cycleLength,
                        isRegular = uiState.data.isRegular,
                        onChange  = { viewModel.setCycleLength(it) }
                    )
                    4 -> PageLastPeriod(
                        selectedDate    = uiState.data.lastPeriodDate,
                        lastPeriodKnown = uiState.data.lastPeriodKnown,
                        onDatePicked    = { known, date -> 
                            viewModel.setLastPeriodKnown(known)
                            if (known && date != null) viewModel.setLastPeriodDate(date)
                        }
                    )
                    5 -> PagePeriodAndGoal(
                        periodLength = uiState.data.periodLength,
                        selectedGoal = uiState.data.userGoal,
                        onPeriodChange = { viewModel.setPeriodLength(it) },
                        onGoalChange   = { viewModel.setUserGoal(it) }
                    )
                    6 -> PageCelebration(
                        userName   = uiState.data.userName,
                        onComplete = { viewModel.completeOnboarding(onComplete) }
                    )
                }
            }

            if (uiState.currentPage in 1..5) {
                BottomNavButtons(
                    currentPage  = uiState.currentPage,
                    isCompleting = uiState.isCompleting,
                    isNextEnabled = uiState.isNextEnabled,
                    onBack       = { viewModel.prevPage() },
                    onNext       = {
                        if (uiState.currentPage < 5) viewModel.nextPage()
                        else viewModel.nextPage() // 5 â†’ 6 (celebración)
                    },
                    onSkip       = { viewModel.nextPage() }
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  INDICADOR DE PÁGINAS â€” dots animados estilo iOS
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  BOTONERA INFERIOR
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun BottomNavButtons(
    currentPage  : Int,
    isCompleting : Boolean,
    isNextEnabled: Boolean = true,
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
            enabled  = !isCompleting && isNextEnabled,
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
                    text       = if (currentPage < 5) "Continuar" else "Comenzar mi ciclo ✨",
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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  PÁGINA 0 â€” BIENVENIDA
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun PageWelcome(
    onTrackCycleClick: () -> Unit,
    onObserverClick: () -> Unit
) {
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
        Image(
            painter = painterResource(id = R.drawable.lumi_logo),
            contentDescription = "Lumi logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(120.dp)
                .scale(scale1.value)
                .alpha(alpha1.value)
        )

        Spacer(Modifier.height(36.dp))

        // Título
        Text(
            text       = "¿Qué te trae a Lumi?",
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
            text      = "Elige cómo quieres empezar tu experiencia.",
            fontSize  = 16.sp,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            lineHeight = 24.sp,
            modifier  = Modifier
                .alpha(alpha3.value)
                .graphicsLayer { translationY = offset3.value.dp.toPx() }
        )

        Spacer(Modifier.height(48.dp))

        // Botones de selección
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(alpha3.value)
                .graphicsLayer { translationY = offset3.value.dp.toPx() },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Opción 1: Track Cycle
            Surface(
                onClick = onTrackCycleClick,
                shape = RoundedCornerShape(20.dp),
                color = primary,
                modifier = Modifier.fillMaxWidth().height(80.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = Color.White)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Seguimiento de mi ciclo",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Aprende más sobre tu cuerpo.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Opción 2: Observer
            Surface(
                onClick = onObserverClick,
                shape = RoundedCornerShape(20.dp),
                color = primary.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, primary.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth().height(80.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.People, contentDescription = null, tint = primary)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Acompañar a alguien",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Ingresar código de vínculo.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  PÁGINA 1 â€” NOMBRE
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  PÁGINA 2 â€” REGULARIDAD
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun PageRegularity(isRegular: Boolean?, onChange: (Boolean?) -> Unit) {
    PageShell(
        icon     = Icons.Rounded.Sync,
        title    = "¿Tus periodos son regulares?",
        subtitle = "Esto nos ayuda a saber cómo hacer tus predicciones."
    ) {
        val options = listOf(
            Pair(true, "Sí, son regulares"),
            Pair(false, "No, son irregulares"),
            Pair(null, "No estoy segura")
        )

        options.forEach { (value, label) ->
            GoalChip(
                icon     = if (value == true) Icons.Rounded.CheckCircle else if (value == false) Icons.Rounded.Timeline else Icons.Rounded.QuestionMark,
                label    = label,
                selected = isRegular == value && (isRegular != null || value == null && isRegular == null),
                onClick  = { onChange(value) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  PÁGINA 4 — FECHA DE ÚLTIMA REGLA (Wheel picker)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun PageLastPeriod(selectedDate: Long, lastPeriodKnown: Boolean, onDatePicked: (Boolean, Long?) -> Unit) {
    // Rango: últimos 90 días hasta hoy
    val today  = LocalDate.now()
    val dates  = remember { (0..89).map { today.minusDays(it.toLong()) } }

    // Índice activo: el que corresponde a la fecha guardada, o hoy (0)
    val initialIndex = remember(selectedDate) {
        if (selectedDate == 0L) 0
        else {
            val selected = Instant.ofEpochMilli(selectedDate)
                .atZone(ZoneId.of("UTC")).toLocalDate()
            dates.indexOfFirst { it == selected }.coerceAtLeast(0)
        }
    }

    PageShell(
        icon     = Icons.Rounded.CalendarToday,
        title    = "¿Cuándo empezó tu último periodo?",
        subtitle = "Una fecha aproximada está bien."
    ) {
        if (!lastPeriodKnown) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No te preocupes. Lumi esperará a que registres tu próximo periodo para comenzar las predicciones.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(24.dp))
            TextButton(
                onClick = { onDatePicked(true, today.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Ingresar una fecha")
            }
        } else {
            DateWheelPicker(
                dates        = dates,
                initialIndex = initialIndex,
                onDateSelected = { date ->
                    val millis = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                    onDatePicked(true, millis)
                }
            )

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = {
                    onDatePicked(false, null)
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text     = "No lo recuerdo",
                    fontSize = 13.sp,
                    color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  WHEEL DATE PICKER â€” LazyColumn con snap y efecto de escala central
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  PÁGINA 3 — DURACIÓN DEL CICLO
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun PageCycleLength(days: Int, isRegular: Boolean?, onChange: (Int) -> Unit) {
    PageShell(
        icon     = Icons.Rounded.Loop,
        title    = "¿Cuánto dura tu ciclo?",
        subtitle = if (isRegular == false) "Ya que eres irregular, usaremos un promedio inicial para aprender."
                   else "Desde el primer día de tu regla hasta el día antes de la siguiente."
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
            else      -> "Ciclo típico âœ“"
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
                "No lo sé â€” usar 28 días",
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  PÁGINA 4 — DURACIÓN DEL PERIODO + OBJETIVO
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
        // â”€â”€ Duración del periodo â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

        // â”€â”€ Objetivo â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  PAGE SHELL â€” estructura común a las páginas 1-4
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  PÁGINA 5 — CELEBRACIÓN "¡Todo listo!"
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
        Image(
            painter = painterResource(id = R.drawable.lumi_logo),
            contentDescription = "Lumi logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(120.dp)
                .scale(scaleLogo.value * pulse.value)
                .alpha(alphaLogo.value)
        )

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

