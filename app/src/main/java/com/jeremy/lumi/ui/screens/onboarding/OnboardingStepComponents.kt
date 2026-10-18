package com.jeremy.lumi.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import kotlinx.coroutines.isActive
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.OnboardingData
import com.jeremy.lumi.domain.model.SecondaryGoal
import com.jeremy.lumi.domain.model.UserGoal
import com.jeremy.lumi.ui.components.LumiBrandLockup
import com.jeremy.lumi.ui.components.LumiEditorialHeading
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENTES AUXILIARES REUSABLES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LumiOnboardingThemeTokens.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(tokens.panel)
            .border(1.dp, tokens.line, RoundedCornerShape(17.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun OptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiOnboardingThemeTokens.current
    val borderAnimColor = if (selected) tokens.accent else tokens.line
    val bgAnimColor = if (selected) tokens.accentSoft else tokens.panel

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 74.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bgAnimColor)
            .border(if (selected) 1.5.dp else 1.dp, borderAnimColor, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(tokens.panel2),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tokens.accent,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(13.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.text,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = tokens.muted,
                lineHeight = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Indicador circular tipo radio
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) tokens.accent else Color.Transparent)
                .border(if (selected) 1.5.dp else 1.2.dp, if (selected) tokens.accent else tokens.line, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(tokens.bg)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PASO 0: BIENVENIDA
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun Step0Welcome(
    mode: String,
    onSelectMode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            LumiBrandLockup(
                flowerSize = 46,
                fontSize = 42.sp,
                starSize = 16.sp,
                letterSpacing = (-2.8).sp,
                animated = true
            )
        }

        LumiEditorialHeading(
            titleNormal = stringResource(R.string.onboarding_welcome_normal),
            titleSerifItalic = stringResource(R.string.onboarding_welcome_italic),
            description = stringResource(R.string.onboarding_welcome_desc),
            titleSize = 27.sp,
            serifSize = 28.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            OptionCard(
                title = stringResource(R.string.onboarding_mode_track_title),
                subtitle = stringResource(R.string.onboarding_mode_track_subtitle),
                icon = DefaultOnboardingIconProvider.getVector(OnboardingIconKey.Calendar),
                selected = mode == "track",
                onClick = { onSelectMode("track") }
            )
            OptionCard(
                title = stringResource(R.string.onboarding_mode_support_title),
                subtitle = stringResource(R.string.onboarding_mode_support_subtitle),
                icon = DefaultOnboardingIconProvider.getVector(OnboardingIconKey.People),
                selected = mode == "support",
                onClick = { onSelectMode("support") }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PASO 1: SEGURIDAD Y PRIVACIDAD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun Step1Safety(
    accepted: Boolean,
    onToggleConsent: (Boolean) -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiOnboardingThemeTokens.current

    Column(modifier = modifier.fillMaxWidth()) {
        LumiEditorialHeading(
            titleNormal = stringResource(R.string.onboarding_safety_normal),
            titleSerifItalic = stringResource(R.string.onboarding_safety_italic),
            description = stringResource(R.string.onboarding_safety_desc),
            titleSize = 27.sp,
            serifSize = 28.sp
        )

        Spacer(modifier = Modifier.height(19.dp))

        PaperCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("⬟", fontSize = 18.sp, color = tokens.accent)
                Text(
                    text = stringResource(R.string.onboarding_safety_card_title),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
            }
            Spacer(modifier = Modifier.height(11.dp))
            Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(start = 4.dp)) {
                BulletItem(text = stringResource(R.string.onboarding_safety_bullet_1))
                BulletItem(text = stringResource(R.string.onboarding_safety_bullet_2))
                BulletItem(text = stringResource(R.string.onboarding_safety_bullet_3))
            }
        }

        Spacer(modifier = Modifier.height(13.dp))

        // Tarjeta de Consentimiento con checkbox trazado
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(17.dp))
                .background(tokens.panel)
                .border(if (accepted) 1.5.dp else 1.dp, if (accepted) tokens.accent else tokens.line, RoundedCornerShape(17.dp))
                .clickable { onToggleConsent(!accepted) }
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            LumiConsentCheckbox(
                checked = accepted,
                onCheckedChange = { onToggleConsent(it) },
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.onboarding_safety_understand_bold),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                Text(
                    text = stringResource(R.string.onboarding_safety_change_later_note),
                    fontSize = 11.sp,
                    color = tokens.muted,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                Text(
                    text = stringResource(R.string.onboarding_safety_privacy_bold),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                Text(
                    text = stringResource(R.string.onboarding_safety_privacy_sub),
                    fontSize = 11.sp,
                    color = tokens.muted,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                Text(
                    text = stringResource(R.string.onboarding_safety_consult_policy),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.accent,
                    modifier = Modifier.clickable { onOpenPrivacy() }
                )
            }
        }
    }
}

@Composable
private fun BulletItem(text: String) {
    val tokens = LumiOnboardingThemeTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("•", color = tokens.muted, fontSize = 12.sp)
        Text(text = text, color = tokens.muted, fontSize = 11.5.sp, lineHeight = 16.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PASO 2: APODO / TU NOMBRE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun Step2Nickname(
    name: String,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiOnboardingThemeTokens.current
    var isFocused by remember { mutableStateOf(false) }

    val easedFocus = CubicBezierEasing(0.22f, 0.85f, 0.25f, 1f)
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) tokens.accent else tokens.line,
        animationSpec = tween(360, easing = easedFocus), label = "nickname_border"
    )
    val focusLift by animateDpAsState(
        targetValue = if (isFocused) (-2).dp else 0.dp,
        animationSpec = tween(360, easing = easedFocus), label = "nickname_lift"
    )
    val underlineScale by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(550, easing = CubicBezierEasing(.25f, .8f, .25f, 1f)),
        label = "nickname_underline"
    )
    val rippleProgress by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(600, easing = easedFocus), label = "nickname_ripple"
    )
    val ornamentScale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = tween(420, easing = CubicBezierEasing(.23f, 1f, .36f, 1f)),
        label = "nickname_sparkle_scale"
    )
    val ornamentAngle by animateFloatAsState(
        targetValue = if (isFocused) 13f else 0f,
        animationSpec = tween(420, easing = easedFocus), label = "nickname_sparkle_rotation"
    )
    // CSS: cuatro puntos con retrasos 0/.25/.49/.72s, comenzando al obtener foco.
    val dotProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    var dotCycles by remember { mutableIntStateOf(0) }
    LaunchedEffect(isFocused) {
        dotCycles = 0
        dotProgress.snapTo(0f)
        if (isFocused) {
            while (isActive) {
                dotProgress.animateTo(1f, tween(2300, easing = LinearEasing))
                dotProgress.snapTo(0f)
                dotCycles++
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        LumiEditorialHeading(
            titleNormal = stringResource(R.string.onboarding_name_normal),
            titleSerifItalic = stringResource(R.string.onboarding_name_italic),
            description = stringResource(R.string.onboarding_name_desc),
            titleSize = 27.sp,
            serifSize = 28.sp
        )

        Spacer(modifier = Modifier.height(19.dp))

        Text(
            text = stringResource(R.string.onboarding_name_field_label),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = tokens.muted
        )

        Spacer(modifier = Modifier.height(9.dp))

        // Campo idéntico al HTML: radio 15px, elevación -2, halo, ripple,
        // subrayado 550ms, sparkle con giro y partículas desfasadas 2300ms.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = focusLift)
                .shadow(
                    elevation = if (isFocused) 10.dp else 0.dp,
                    shape = RoundedCornerShape(15.dp),
                    clip = false,
                    ambientColor = tokens.glow,
                    spotColor = tokens.glow
                )
                .height(55.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(tokens.panel)
                .border(1.dp, borderColor, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            // CSS .name-ripple — luz interna que se expande suavemente a 250px.
            if (rippleProgress > 0f) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val radiusPx = 125.dp.toPx() * rippleProgress
                    if (radiusPx > 0f) {
                        val c = Offset(size.width * .78f, size.height * .5f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(tokens.glow.copy(alpha = .30f), Color.Transparent),
                                center = c, radius = radiusPx
                            ),
                            center = c, radius = radiusPx
                        )
                    }
                }
            }

            // CSS .name-dots i — movimiento escalonado, no ciclos aleatorios.
            if (isFocused) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val offsets = listOf(
                        Triple(.82f, .70f, 0f),
                        Triple(.90f, .20f, 250f),
                        Triple(.72f, .55f, 490f),
                        Triple(.96f, .56f, 720f)
                    )
                    val elapsed = dotCycles * 2300f + dotProgress.value * 2300f
                    offsets.forEach { (rx, ry, delayMs) ->
                        val millis = elapsed - delayMs
                        if (millis >= 0f) {
                            val p = ((millis % 2300f) / 2300f).coerceIn(0f, 1f)
                            val eased = if (p < .5f) 2 * p * p else 1f - (-2 * p + 2) * (-2 * p + 2) / 2f
                            val alpha = if (p < .4f) .55f * p / .4f else .55f * (1f - p) / .6f
                            drawCircle(
                                tokens.accent.copy(alpha = alpha.coerceIn(0f, .55f)),
                                radius = 1.5.dp.toPx(),
                                center = Offset(size.width * rx, size.height * ry + (8f - 29f * eased).dp.toPx())
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxSize().padding(start = 15.dp, end = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = name,
                    onValueChange = { if (it.length <= 32) onNameChange(it) },
                    singleLine = true,
                    textStyle = TextStyle(color = tokens.text, fontSize = 14.sp, fontWeight = FontWeight.Normal),
                    cursorBrush = SolidColor(tokens.accent),
                    modifier = Modifier.weight(1f).onFocusChanged { isFocused = it.isFocused },
                    decorationBox = { innerTextField ->
                        Box {
                            if (name.isEmpty()) Text(stringResource(R.string.onboarding_name_placeholder), color = tokens.muted, fontSize = 14.sp)
                            innerTextField()
                        }
                    }
                )
                // Path del SVG #ico-sparkle, no el icono Material aproximado.
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .padding(start = 11.dp)
                        .size(19.dp)
                        .graphicsLayer {
                            rotationZ = ornamentAngle
                            scaleX = ornamentScale
                            scaleY = ornamentScale
                        }
                ) {
                    val sx = size.width / 24f
                    val sy = size.height / 24f
                    val star = Path().apply {
                        moveTo(12*sx, 2*sy)
                        lineTo(14.1f*sx, 9.9f*sy)
                        lineTo(22*sx, 12*sy)
                        lineTo(14.1f*sx, 14.1f*sy)
                        lineTo(12*sx, 22*sy)
                        lineTo(9.9f*sx, 14.1f*sy)
                        lineTo(2*sx, 12*sy)
                        lineTo(9.9f*sx, 9.9f*sy)
                        close()
                    }
                    drawPath(star, color = tokens.accent, style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round))
                    drawLine(tokens.accent, Offset(19*sx,2*sy), Offset(19*sx,6*sy), strokeWidth = 1.6.dp.toPx())
                    drawLine(tokens.accent, Offset(17*sx,4*sy), Offset(21*sx,4*sy), strokeWidth = 1.6.dp.toPx())
                }
            }

            if (underlineScale > 0f) {
                Box(
                    Modifier.align(Alignment.BottomStart)
                        .fillMaxWidth(underlineScale).height(2.dp)
                        .background(Brush.horizontalGradient(listOf(tokens.rose, tokens.accent)))
                )
            }
        }

        Spacer(modifier = Modifier.height(17.dp))

        // Preview del saludo
        PaperCard {
            Text(
                text = stringResource(R.string.onboarding_name_preview_label),
                fontSize = 11.sp,
                color = tokens.muted
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.onboarding_hello_simple),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                if (name.isNotBlank()) {
                    Text(
                        text = ", ",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.text
                    )
                    Text(
                        text = name.trim(),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Normal,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Serif,
                        color = tokens.accent
                    )
                }
                Text(
                    text = ".",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
            }
        }

        Spacer(modifier = Modifier.height(17.dp))

        Text(
            text = "Puedes cambiarlo más adelante.",
            fontSize = 11.sp,
            color = tokens.muted
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PASO 3: TU CICLO
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun Step3Cycle(
    regularityChoice: String?,
    onSelectRegularity: (String) -> Unit,
    cycleKnown: Boolean,
    cycleLength: Int,
    onToggleCycleKnown: () -> Unit,
    onAdjustCycleLength: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiOnboardingThemeTokens.current

    Column(modifier = modifier.fillMaxWidth()) {
        LumiEditorialHeading(
            titleNormal = stringResource(R.string.onboarding_cycle_title_normal),
            titleSerifItalic = stringResource(R.string.onboarding_cycle_title_italic),
            description = stringResource(R.string.onboarding_cycle_subtitle),
            titleSize = 27.sp,
            serifSize = 28.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            OptionCard(
                title = stringResource(R.string.onboarding_regular_yes),
                subtitle = stringResource(R.string.onboarding_reg_regular_subtitle),
                icon = DefaultOnboardingIconProvider.getVector(OnboardingIconKey.Calendar),
                selected = regularityChoice == "yes",
                onClick = { onSelectRegularity("yes") }
            )
            OptionCard(
                title = stringResource(R.string.onboarding_regular_no),
                subtitle = stringResource(R.string.onboarding_reg_irregular_subtitle),
                icon = DefaultOnboardingIconProvider.getVector(OnboardingIconKey.Calendar),
                selected = regularityChoice == "no",
                onClick = { onSelectRegularity("no") }
            )
            OptionCard(
                title = stringResource(R.string.onboarding_reg_unsure_title),
                subtitle = stringResource(R.string.onboarding_reg_unsure_subtitle),
                icon = DefaultOnboardingIconProvider.getVector(OnboardingIconKey.Calendar),
                selected = regularityChoice == "unknown",
                onClick = { onSelectRegularity("unknown") }
            )
        }

        Spacer(modifier = Modifier.height(17.dp))

        PaperCard {
            Text(
                text = stringResource(R.string.onboarding_cycle_approx_duration),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.text
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.onboarding_cycle_approx_desc),
                fontSize = 11.sp,
                color = tokens.muted
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (cycleKnown) tokens.accentSoft else tokens.panel2)
                    .border(1.dp, if (cycleKnown) tokens.accent else tokens.line, CircleShape)
                    .clickable { onToggleCycleKnown() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (cycleKnown) stringResource(R.string.onboarding_cycle_btn_dont_know) else stringResource(R.string.onboarding_cycle_btn_estimate),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (cycleKnown) tokens.accent else tokens.muted
                )
            }

            if (cycleKnown) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundStepperButton(
                        text = "−",
                        enabled = cycleLength > 21,
                        onClick = { onAdjustCycleLength(cycleLength - 1) }
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "$cycleLength",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tokens.accent
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    RoundStepperButton(
                        text = "+",
                        enabled = cycleLength < 45,
                        onClick = { onAdjustCycleLength(cycleLength + 1) }
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.onboarding_cycle_dont_worry),
                    fontSize = 11.sp,
                    color = tokens.muted
                )
            }
        }
    }
}

@Composable
fun RoundStepperButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiOnboardingThemeTokens.current
    Box(
        modifier = modifier
            .size(43.dp)
            .clip(CircleShape)
            .background(tokens.panel2)
            .border(1.dp, tokens.line, CircleShape)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 23.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) tokens.text else tokens.dim
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PASO 4: ÚLTIMO PERIODO
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun Step4LastPeriod(
    dateKnown: Boolean,
    lastPeriodDate: Long,
    onSelectDateKnown: (Boolean) -> Unit,
    onOpenDatePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiOnboardingThemeTokens.current

    Column(modifier = modifier.fillMaxWidth()) {
        LumiEditorialHeading(
            titleNormal = stringResource(R.string.onboarding_last_period_q_title_normal),
            titleSerifItalic = stringResource(R.string.onboarding_last_period_q_title_italic),
            description = stringResource(R.string.onboarding_last_period_remember_q),
            titleSize = 27.sp,
            serifSize = 28.sp
        )

        Spacer(modifier = Modifier.height(19.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipButton(
                label = stringResource(R.string.onboarding_last_period_choose_date),
                selected = dateKnown,
                onClick = { onSelectDateKnown(true) }
            )
            ChipButton(
                label = stringResource(R.string.onboarding_last_period_dont_remember),
                selected = !dateKnown,
                onClick = { onSelectDateKnown(false) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        PaperCard {
            if (dateKnown) {
                Text(
                    text = stringResource(R.string.onboarding_first_day_bleeding),
                    fontSize = 11.sp,
                    color = tokens.muted
                )
                Spacer(modifier = Modifier.height(8.dp))

                val formattedDate = if (lastPeriodDate > 0L) {
                    val date = Instant.ofEpochMilli(lastPeriodDate).atZone(ZoneId.systemDefault()).toLocalDate()
                    date.format(DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy", Locale.getDefault()))
                } else {
                    stringResource(R.string.onboarding_tap_select_date)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.panel2)
                        .border(1.dp, tokens.line, RoundedCornerShape(12.dp))
                        .clickable { onOpenDatePicker() }
                        .padding(14.dp)
                ) {
                    Text(
                        text = formattedDate,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (lastPeriodDate > 0L) tokens.text else tokens.muted
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_last_period_calendar_note),
                    fontSize = 11.sp,
                    color = tokens.muted
                )
            } else {
                Text(
                    text = stringResource(R.string.onboarding_last_period_no_need_now),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.onboarding_last_period_log_later),
                    fontSize = 11.sp,
                    color = tokens.muted
                )
            }
        }
    }
}

@Composable
fun ChipButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiOnboardingThemeTokens.current
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) tokens.accentSoft else tokens.panel)
            .border(1.dp, if (selected) tokens.accent else tokens.line, CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) tokens.text else tokens.muted
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PASO 5: PREFERENCIAS Y ANTICONCEPCIÓN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun Step5Preferences(
    currentGoal: UserGoal?,
    onSelectGoal: (UserGoal) -> Unit,
    contraceptionChoice: String,
    onSelectContraception: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiOnboardingThemeTokens.current

    Column(modifier = modifier.fillMaxWidth()) {
        LumiEditorialHeading(
            titleNormal = stringResource(R.string.onboarding_goals_normal),
            titleSerifItalic = stringResource(R.string.onboarding_goals_italic),
            description = stringResource(R.string.onboarding_goals_desc),
            titleSize = 27.sp,
            serifSize = 28.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Grid 2x2 de objetivos
        val goals = listOf(
            Triple(stringResource(R.string.onboarding_goal_understand_cycle), UserGoal.TRACK_CYCLE, OnboardingIconKey.Calendar),
            Triple(stringResource(R.string.onboarding_goal_track_wellness), UserGoal.HEALTH_MONITORING, OnboardingIconKey.Heart),
            Triple(stringResource(R.string.onboarding_goal_seek_pregnancy), UserGoal.SEEK_PREGNANCY, OnboardingIconKey.Leaf),
            Triple(stringResource(R.string.onboarding_goal_avoid_pregnancy), UserGoal.AVOID_PREGNANCY, OnboardingIconKey.Shield)
        )

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            for (row in goals.chunked(2)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    for ((title, goal, iconKey) in row) {
                        val isSelected = currentGoal == goal
                        GoalCard(
                            title = title,
                            icon = DefaultOnboardingIconProvider.getVector(iconKey),
                            selected = isSelected,
                            onClick = { onSelectGoal(goal) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bloque de anticoncepción hormonal
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = tokens.line,
                    shape = RoundedCornerShape(17.dp)
                )
                .background(tokens.panel, RoundedCornerShape(17.dp))
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_contraception_title),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.text
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = stringResource(R.string.onboarding_contraception_desc),
                fontSize = 11.sp,
                color = tokens.muted
            )

            Spacer(modifier = Modifier.height(13.dp))

            // Selector segmentado deslizante (Glider)
            ContraceptionGlider(
                selectedChoice = contraceptionChoice,
                onSelectChoice = onSelectContraception
            )
        }
    }
}

@Composable
fun GoalCard(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiOnboardingThemeTokens.current

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) tokens.accentSoft else tokens.panel)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) tokens.accent else tokens.line, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(29.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(tokens.panel2),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tokens.accent,
                modifier = Modifier.size(17.dp)
            )
        }
        Text(
            text = title,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = tokens.text,
            lineHeight = 15.sp,
            maxLines = 2
        )
    }
}

@Composable
fun ContraceptionGlider(
    selectedChoice: String,
    onSelectChoice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiOnboardingThemeTokens.current
    val options = listOf(
        "yes" to stringResource(R.string.common_yes),
        "no" to stringResource(R.string.common_no),
        "unsure" to stringResource(R.string.onboarding_contraception_prefer_not_say)
    )
    val selectedIndex = options.indexOfFirst { it.first == selectedChoice }

    val weights = listOf(1f, 1f, 1.68f)
    val totalWeight = 3.68f
    val gap = 4.dp
    val padding = 5.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(45.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(tokens.panel)
            .border(1.dp, tokens.line, RoundedCornerShape(999.dp))
            .padding(padding)
    ) {
        val totalAvailableWidth = maxWidth - gap * 2
        val width0 = totalAvailableWidth * (weights[0] / totalWeight)
        val width1 = totalAvailableWidth * (weights[1] / totalWeight)
        val width2 = totalAvailableWidth * (weights[2] / totalWeight)

        val targetWidth = when (selectedIndex) {
            0 -> width0
            1 -> width1
            2 -> width2
            else -> 0.dp
        }

        val targetOffset = when (selectedIndex) {
            0 -> 0.dp
            1 -> width0 + gap
            2 -> width0 + width1 + gap * 2
            else -> 0.dp
        }

        val animWidth by animateDpAsState(
            targetValue = targetWidth,
            animationSpec = tween(
                durationMillis = 370,
                easing = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f)
            ),
            label = "glider_width"
        )

        val animOffset by animateDpAsState(
            targetValue = targetOffset,
            animationSpec = tween(
                durationMillis = 370,
                easing = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f)
            ),
            label = "glider_offset"
        )

        val gliderAlpha by animateFloatAsState(
            targetValue = if (selectedIndex >= 0) 1f else 0f,
            animationSpec = tween(durationMillis = 200),
            label = "glider_alpha"
        )

        // Pastilla deslizante (Glider)
        if (gliderAlpha > 0f) {
            Box(
                modifier = Modifier
                    .offset(x = animOffset)
                    .width(animWidth)
                    .fillMaxHeight()
                    .alpha(gliderAlpha)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(tokens.btnPink, tokens.btnViolet)
                        )
                    )
            )
        }

        // Fila de opciones clickeables por encima
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, (key, label) ->
                val isSelected = selectedChoice == key
                Box(
                    modifier = Modifier
                        .weight(weights[index])
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onSelectChoice(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isSelected) Color(0xFF21172D) else tokens.muted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PASO 6: PERSONALIZACIÓN / ÚLTIMOS DETALLES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun Step6Customization(
    periodKnown: Boolean,
    periodLength: Int,
    onTogglePeriodKnown: () -> Unit,
    onAdjustPeriodLength: (Int) -> Unit,
    secondaryGoals: Set<SecondaryGoal>,
    onToggleSecondaryGoal: (SecondaryGoal) -> Unit,
    age: Int?,
    height: Float?,
    weight: Float?,
    onPhysicalProfileChange: (Int?, Float?, Float?) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiOnboardingThemeTokens.current
    var profileExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        LumiEditorialHeading(
            titleNormal = stringResource(R.string.onboarding_customization_title_normal),
            titleSerifItalic = stringResource(R.string.onboarding_customization_title_italic),
            description = stringResource(R.string.onboarding_final_details_optional_desc),
            titleSize = 27.sp,
            serifSize = 28.sp
        )

        Spacer(modifier = Modifier.height(17.dp))

        // Duración del sangrado
        PaperCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.onboarding_period_duration_title),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.onboarding_period_duration_desc),
                        fontSize = 11.sp,
                        color = tokens.muted
                    )
                }

                Text(
                    text = if (periodKnown) stringResource(R.string.onboarding_cycle_btn_dont_know) else stringResource(R.string.onboarding_choose),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.accent,
                    modifier = Modifier.clickable { onTogglePeriodKnown() }
                )
            }

            if (periodKnown) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundStepperButton(
                        text = "−",
                        enabled = periodLength > 1,
                        onClick = { onAdjustPeriodLength(periodLength - 1) },
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$periodLength",
                            fontSize = 31.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = tokens.accent
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.onboarding_days_unit),
                            fontSize = 12.sp,
                            color = tokens.muted,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    RoundStepperButton(
                        text = "+",
                        enabled = periodLength < 10,
                        onClick = { onAdjustPeriodLength(periodLength + 1) },
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(9.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (days in listOf(3, 4, 5, 6)) {
                        val active = periodLength == days
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .clip(CircleShape)
                                .background(if (active) tokens.accentSoft else tokens.panel2)
                                .border(1.dp, if (active) tokens.accent else tokens.line, CircleShape)
                                .clickable { onAdjustPeriodLength(days) }
                                .padding(horizontal = 11.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.days_short, days),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) tokens.accent else tokens.muted
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.onboarding_no_average_yet),
                    fontSize = 11.sp,
                    color = tokens.muted
                )
            }
        }

        Spacer(modifier = Modifier.height(19.dp))

        // Aspectos que quieres seguir
        Text(
            text = stringResource(R.string.onboarding_pay_attention_to),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = tokens.text
        )

        Spacer(modifier = Modifier.height(12.dp))

        val interestItems = listOf(
            Triple(stringResource(R.string.onboarding_goal_cramps_label), SecondaryGoal.MANAGE_CRAMPS, OnboardingIconKey.Heart),
            Triple(stringResource(R.string.onboarding_goal_sleep_label), SecondaryGoal.IMPROVE_SLEEP, OnboardingIconKey.Moon),
            Triple(stringResource(R.string.onboarding_goal_mood_label), SecondaryGoal.UNDERSTAND_MOOD, OnboardingIconKey.Sparkle),
            Triple(stringResource(R.string.onboarding_goal_fertility_label), SecondaryGoal.TRACK_FERTILITY, OnboardingIconKey.Leaf),
            Triple(stringResource(R.string.onboarding_goal_stress_label), SecondaryGoal.REDUCE_STRESS, OnboardingIconKey.Waves)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in interestItems.chunked(3)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for ((label, goal, iconKey) in row) {
                        val isSelected = secondaryGoals.contains(goal)
                        InterestTile(
                            label = label,
                            icon = DefaultOnboardingIconProvider.getVector(iconKey),
                            selected = isSelected,
                            onClick = { onToggleSecondaryGoal(goal) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size < 3) {
                        Spacer(modifier = Modifier.weight((3 - row.size).toFloat()))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(17.dp))

        // Perfil físico (acordeón opcional)
        PaperCard(modifier = Modifier.padding(0.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { profileExpanded = !profileExpanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.onboarding_physical_profile_title),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                Icon(
                    imageVector = if (profileExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = tokens.muted
                )
            }

            AnimatedVisibility(visible = profileExpanded) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileInputField(
                            label = stringResource(R.string.onboarding_physical_age),
                            placeholder = stringResource(R.string.onboarding_unit_years),
                            value = age?.toString() ?: "",
                            onValueChange = {
                                val newAge = it.filter { ch -> ch.isDigit() }.toIntOrNull()
                                onPhysicalProfileChange(newAge, height, weight)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ProfileInputField(
                            label = stringResource(R.string.onboarding_physical_height),
                            placeholder = stringResource(R.string.onboarding_unit_cm),
                            value = height?.toInt()?.toString() ?: "",
                            onValueChange = {
                                val newHeight = it.filter { ch -> ch.isDigit() }.toFloatOrNull()
                                onPhysicalProfileChange(age, newHeight, weight)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ProfileInputField(
                            label = stringResource(R.string.onboarding_physical_weight),
                            placeholder = stringResource(R.string.onboarding_unit_kg),
                            value = weight?.toInt()?.toString() ?: "",
                            onValueChange = {
                                val newWeight = it.filter { ch -> ch.isDigit() }.toFloatOrNull()
                                onPhysicalProfileChange(age, height, newWeight)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.onboarding_physical_share_note),
                        fontSize = 10.sp,
                        color = tokens.muted
                    )
                }
            }
        }
    }
}

@Composable
fun InterestTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiOnboardingThemeTokens.current

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 81.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) tokens.accentSoft else tokens.panel)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) tokens.accent else tokens.line, RoundedCornerShape(13.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tokens.accent,
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = tokens.text,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 13.sp
        )
    }
}

@Composable
fun ProfileInputField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiOnboardingThemeTokens.current

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = tokens.muted
        )
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(tokens.panel2)
                .border(1.dp, tokens.line, RoundedCornerShape(9.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    color = tokens.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(tokens.accent),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(placeholder, color = tokens.muted, fontSize = 12.sp)
                    }
                    inner()
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PASO 7: TODO LISTO / RESUMEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun Step7Summary(
    data: OnboardingData,
    onJumpToStep: (Int) -> Unit,
    saveError: String?,
    modifier: Modifier = Modifier,
    onBrandBounds: (Rect) -> Unit = {},
    brandVisible: Boolean = true
) {
    val tokens = LumiOnboardingThemeTokens.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            // HTML mide source.getBoundingClientRect() del logo, NO de toda la fila.
            Box(
                modifier = Modifier
                    .onGloballyPositioned { onBrandBounds(it.boundsInRoot()) }
                    .alpha(if (brandVisible) 1f else 0f)
            ) {
                LumiBrandLockup(
                    flowerSize = 46,
                    fontSize = 42.sp,
                    starSize = 16.sp,
                    letterSpacing = (-2.8).sp,
                    animated = true
                )
            }
        }

        LumiEditorialHeading(
            titleNormal = stringResource(R.string.onboarding_ready_title_normal) + ".",
            titleSerifItalic = "",
            description = stringResource(R.string.onboarding_ready_change_choices_desc),
            titleSize = 27.sp,
            serifSize = 28.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (data.mode == "support") {
            PaperCard {
                Text(
                    text = stringResource(R.string.onboarding_support_next_step),
                    fontSize = 11.sp,
                    color = tokens.muted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_support_connect_conf),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.onboarding_support_instructions),
                    fontSize = 12.sp,
                    color = tokens.muted,
                    lineHeight = 16.sp
                )
            }
        } else {
            // 2x2 grid de resumen con botón Editar
            val greetingVal = data.userName?.trim()?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.onboarding_hello_format, it) } ?: stringResource(R.string.onboarding_hello_simple)
            val cycleVal = if (data.cycleLengthKnown) stringResource(R.string.onboarding_summary_cycle_val, data.cycleLength) else stringResource(R.string.onboarding_summary_cycle_unknown)
            val periodVal = if (data.lastPeriodKnown && data.lastPeriodDate > 0L) {
                val d = Instant.ofEpochMilli(data.lastPeriodDate).atZone(ZoneId.systemDefault()).toLocalDate()
                d.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
            } else {
                stringResource(R.string.onboarding_summary_not_logged)
            }
            val goalVal = when (data.userGoal) {
                UserGoal.TRACK_CYCLE -> stringResource(R.string.onboarding_goal_understand_cycle)
                UserGoal.HEALTH_MONITORING -> stringResource(R.string.onboarding_goal_track_wellness)
                UserGoal.SEEK_PREGNANCY -> stringResource(R.string.onboarding_goal_seek_pregnancy)
                UserGoal.AVOID_PREGNANCY -> stringResource(R.string.onboarding_goal_avoid_pregnancy)
                null -> stringResource(R.string.onboarding_summary_not_selected)
            }

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    SummaryCard(
                        title = stringResource(R.string.onboarding_summary_greeting),
                        value = greetingVal,
                        onEdit = { onJumpToStep(2) },
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = stringResource(R.string.onboarding_summary_cycle),
                        value = cycleVal,
                        onEdit = { onJumpToStep(3) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    SummaryCard(
                        title = stringResource(R.string.onboarding_summary_last_period),
                        value = periodVal,
                        onEdit = { onJumpToStep(4) },
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = stringResource(R.string.onboarding_summary_goal),
                        value = goalVal,
                        onEdit = { onJumpToStep(5) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (saveError != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = saveError,
                fontSize = 11.5.sp,
                color = Color(0xFFED9AAC),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiOnboardingThemeTokens.current

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 73.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(tokens.panel)
            .border(1.dp, tokens.line, RoundedCornerShape(13.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 9.5.sp,
                color = tokens.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.btn_edit),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.accent,
                modifier = Modifier.clickable { onEdit() }
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = value,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = tokens.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
