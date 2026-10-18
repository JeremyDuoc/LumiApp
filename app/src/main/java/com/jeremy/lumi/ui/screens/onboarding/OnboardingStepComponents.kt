package com.jeremy.lumi.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import com.jeremy.lumi.ui.theme.LumiThemeTokens
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
    val tokens = LumiThemeTokens.current
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
    val tokens = LumiThemeTokens.current
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
            titleNormal = "Tu ciclo, ",
            titleSerifItalic = "a tu manera.",
            description = "Elige cómo quieres usar Lumi.",
            titleSize = 27.sp,
            serifSize = 28.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            OptionCard(
                title = "Seguir mi ciclo",
                subtitle = "Registros y estimaciones personales.",
                icon = DefaultOnboardingIconProvider.getVector(OnboardingIconKey.Calendar),
                selected = mode == "track",
                onClick = { onSelectMode("track") }
            )
            OptionCard(
                title = "Acompañar a alguien",
                subtitle = "Conectar con una persona de confianza.",
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
    val tokens = LumiThemeTokens.current

    Column(modifier = modifier.fillMaxWidth()) {
        LumiEditorialHeading(
            titleNormal = "Tu bienestar, ",
            titleSerifItalic = "con claridad.",
            description = "Antes de comenzar.",
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
                    text = "Lumi acompaña, no diagnostica",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
            }
            Spacer(modifier = Modifier.height(11.dp))
            Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(start = 4.dp)) {
                BulletItem(text = "Las predicciones son estimaciones, no certezas.")
                BulletItem(text = "No utilices Lumi como anticonceptivo.")
                BulletItem(text = "Si algo te preocupa o tienes síntomas intensos, busca atención profesional.")
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
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (accepted) tokens.accent else tokens.panel2)
                    .border(1.2.dp, if (accepted) tokens.accent else tokens.line, RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (accepted) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = tokens.bg,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Entiendo estas limitaciones.",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                Text(
                    text = "Puedo cambiar mis preferencias más adelante.",
                    fontSize = 11.sp,
                    color = tokens.muted,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                Text(
                    text = "Acepto la política de privacidad",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                Text(
                    text = "y autorizo el tratamiento de mis datos para prestar el servicio.",
                    fontSize = 11.sp,
                    color = tokens.muted,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                Text(
                    text = "Consultar política",
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
    val tokens = LumiThemeTokens.current
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
    val tokens = LumiThemeTokens.current

    Column(modifier = modifier.fillMaxWidth()) {
        LumiEditorialHeading(
            titleNormal = "¿Cómo te gustaría ",
            titleSerifItalic = "que te saludemos?",
            description = "Puedes usar un apodo o dejarlo vacío.",
            titleSize = 27.sp,
            serifSize = 28.sp
        )

        Spacer(modifier = Modifier.height(19.dp))

        Text(
            text = "Nombre o apodo · opcional",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = tokens.muted
        )

        Spacer(modifier = Modifier.height(9.dp))

        // Input estilizado con subrayado y adorno de destello
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(tokens.panel)
                .border(1.dp, tokens.line, RoundedCornerShape(15.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = name,
                    onValueChange = { if (it.length <= 32) onNameChange(it) },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = tokens.text,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(tokens.accent),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (name.isEmpty()) {
                            Text(
                                text = "Tu apodo",
                                color = tokens.muted,
                                fontSize = 14.5.sp
                            )
                        }
                        innerTextField()
                    }
                )

                Icon(
                    imageVector = DefaultOnboardingIconProvider.getVector(OnboardingIconKey.Sparkle),
                    contentDescription = null,
                    tint = tokens.accent,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(17.dp))

        // Preview del saludo
        PaperCard {
            Text(
                text = "Así aparecerá en Inicio",
                fontSize = 11.sp,
                color = tokens.muted
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Hola",
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
    val tokens = LumiThemeTokens.current

    Column(modifier = modifier.fillMaxWidth()) {
        LumiEditorialHeading(
            titleNormal = "Conozcamos ",
            titleSerifItalic = "tu ciclo.",
            description = "No tienes que saberlo todo para empezar.",
            titleSize = 27.sp,
            serifSize = 28.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            OptionCard(
                title = "Sí, es regular",
                subtitle = "Mi ciclo suele mantener un patrón parecido.",
                icon = DefaultOnboardingIconProvider.getVector(OnboardingIconKey.Calendar),
                selected = regularityChoice == "yes",
                onClick = { onSelectRegularity("yes") }
            )
            OptionCard(
                title = "No, es irregular",
                subtitle = "La duración cambia de un mes a otro.",
                icon = DefaultOnboardingIconProvider.getVector(OnboardingIconKey.Calendar),
                selected = regularityChoice == "no",
                onClick = { onSelectRegularity("no") }
            )
            OptionCard(
                title = "No estoy segura",
                subtitle = "Prefiero descubrirlo con el tiempo.",
                icon = DefaultOnboardingIconProvider.getVector(OnboardingIconKey.Calendar),
                selected = regularityChoice == "unknown",
                onClick = { onSelectRegularity("unknown") }
            )
        }

        Spacer(modifier = Modifier.height(17.dp))

        PaperCard {
            Text(
                text = "Duración aproximada",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.text
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Es opcional y podrás modificarla después.",
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
                    text = if (cycleKnown) "No lo sé" else "Estimar duración",
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
                    text = "No pasa nada si todavía no conoces tu duración habitual.",
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
    val tokens = LumiThemeTokens.current
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
    val tokens = LumiThemeTokens.current

    Column(modifier = modifier.fillMaxWidth()) {
        LumiEditorialHeading(
            titleNormal = "Tu último ",
            titleSerifItalic = "periodo.",
            description = "¿Recuerdas cuándo comenzó?",
            titleSize = 27.sp,
            serifSize = 28.sp
        )

        Spacer(modifier = Modifier.height(19.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipButton(
                label = "Elegir fecha",
                selected = dateKnown,
                onClick = { onSelectDateKnown(true) }
            )
            ChipButton(
                label = "No recuerdo",
                selected = !dateKnown,
                onClick = { onSelectDateKnown(false) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        PaperCard {
            if (dateKnown) {
                Text(
                    text = "Primer día de sangrado",
                    fontSize = 11.sp,
                    color = tokens.muted
                )
                Spacer(modifier = Modifier.height(8.dp))

                val formattedDate = if (lastPeriodDate > 0L) {
                    val date = Instant.ofEpochMilli(lastPeriodDate).atZone(ZoneId.systemDefault()).toLocalDate()
                    date.format(DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy", Locale("es", "CL")))
                } else {
                    "Toca para seleccionar fecha"
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
                    text = "Puedes cambiar la fecha desde el calendario.",
                    fontSize = 11.sp,
                    color = tokens.muted
                )
            } else {
                Text(
                    text = "No necesitas recordarlo ahora",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Podrás registrar una fecha cuando la conozcas.",
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
    val tokens = LumiThemeTokens.current
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
    val tokens = LumiThemeTokens.current

    Column(modifier = modifier.fillMaxWidth()) {
        LumiEditorialHeading(
            titleNormal = "¿Qué te gustaría ",
            titleSerifItalic = "hacer con Lumi?",
            description = "Para organizar tu experiencia inicial.",
            titleSize = 27.sp,
            serifSize = 28.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Grid 2x2 de objetivos
        val goals = listOf(
            Triple("Entender mi ciclo", UserGoal.TRACK_CYCLE, OnboardingIconKey.Calendar),
            Triple("Conocer mi bienestar", UserGoal.HEALTH_MONITORING, OnboardingIconKey.Heart),
            Triple("Buscar embarazo", UserGoal.SEEK_PREGNANCY, OnboardingIconKey.Leaf),
            Triple("Evitar embarazo", UserGoal.AVOID_PREGNANCY, OnboardingIconKey.Shield)
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
                text = "¿Usas anticoncepción hormonal?",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.text
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "Puede afectar el sangrado y las predicciones.",
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
    val tokens = LumiThemeTokens.current

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
    val tokens = LumiThemeTokens.current
    val options = listOf("yes" to "Sí", "no" to "No", "unsure" to "Prefiero no decir")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(tokens.panel2)
            .border(1.dp, tokens.line, CircleShape)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for ((key, label) in options) {
            val isSelected = selectedChoice == key
            Box(
                modifier = Modifier
                    .weight(if (key == "unsure") 1.6f else 1f)
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) tokens.accent else Color.Transparent)
                    .clickable { onSelectChoice(key) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) tokens.bg else tokens.muted,
                    maxLines = 1
                )
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
    val tokens = LumiThemeTokens.current
    var profileExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        LumiEditorialHeading(
            titleNormal = "Últimos ",
            titleSerifItalic = "detalles.",
            description = "Todo esto es opcional y podrás cambiarlo.",
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
                        text = "Duración del sangrado",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Si conoces tu promedio.",
                        fontSize = 11.sp,
                        color = tokens.muted
                    )
                }

                Text(
                    text = if (periodKnown) "No lo sé" else "Elegir",
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
                            text = "días",
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
                                text = "$days d",
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
                    text = "Sin promedio por ahora.",
                    fontSize = 11.sp,
                    color = tokens.muted
                )
            }
        }

        Spacer(modifier = Modifier.height(19.dp))

        // Aspectos que quieres seguir
        Text(
            text = "Quiero prestar atención a...",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = tokens.text
        )

        Spacer(modifier = Modifier.height(12.dp))

        val interestItems = listOf(
            Triple("Cólicos", SecondaryGoal.MANAGE_CRAMPS, OnboardingIconKey.Heart),
            Triple("Sueño", SecondaryGoal.IMPROVE_SLEEP, OnboardingIconKey.Moon),
            Triple("Estado de ánimo", SecondaryGoal.UNDERSTAND_MOOD, OnboardingIconKey.Sparkle),
            Triple("Fertilidad", SecondaryGoal.TRACK_FERTILITY, OnboardingIconKey.Leaf),
            Triple("Estrés", SecondaryGoal.REDUCE_STRESS, OnboardingIconKey.Waves)
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
                    text = "Perfil físico (opcional)",
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
                            label = "Edad",
                            placeholder = "Años",
                            value = age?.toString() ?: "",
                            onValueChange = {
                                val newAge = it.filter { ch -> ch.isDigit() }.toIntOrNull()
                                onPhysicalProfileChange(newAge, height, weight)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ProfileInputField(
                            label = "Estatura",
                            placeholder = "cm",
                            value = height?.toInt()?.toString() ?: "",
                            onValueChange = {
                                val newHeight = it.filter { ch -> ch.isDigit() }.toFloatOrNull()
                                onPhysicalProfileChange(age, newHeight, weight)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ProfileInputField(
                            label = "Peso",
                            placeholder = "kg",
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
                        text = "Solo completa los datos que quieras compartir.",
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
    val tokens = LumiThemeTokens.current

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
    val tokens = LumiThemeTokens.current

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
    modifier: Modifier = Modifier
) {
    val tokens = LumiThemeTokens.current

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
            LumiBrandLockup(
                flowerSize = 46,
                fontSize = 42.sp,
                starSize = 16.sp,
                letterSpacing = (-2.8).sp,
                animated = true
            )
        }

        LumiEditorialHeading(
            titleNormal = "Todo listo.",
            titleSerifItalic = "",
            description = "Puedes cambiar tus elecciones cuando quieras.",
            titleSize = 27.sp,
            serifSize = 28.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (data.mode == "support") {
            PaperCard {
                Text(
                    text = "Tu próximo paso",
                    fontSize = 11.sp,
                    color = tokens.muted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Conectar con alguien",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Puedes vincularte con una persona de confianza desde la aplicación.",
                    fontSize = 12.sp,
                    color = tokens.muted,
                    lineHeight = 16.sp
                )
            }
        } else {
            // 2x2 grid de resumen con botón Editar
            val greetingVal = data.userName?.trim()?.takeIf { it.isNotBlank() }?.let { "Hola, $it." } ?: "Hola."
            val cycleVal = if (data.cycleLengthKnown) "${data.cycleLength} días" else "Por conocer"
            val periodVal = if (data.lastPeriodKnown && data.lastPeriodDate > 0L) {
                val d = Instant.ofEpochMilli(data.lastPeriodDate).atZone(ZoneId.systemDefault()).toLocalDate()
                d.format(DateTimeFormatter.ofPattern("d MMM", Locale("es", "CL")))
            } else {
                "Sin registrar"
            }
            val goalVal = when (data.userGoal) {
                UserGoal.TRACK_CYCLE -> "Entender mi ciclo"
                UserGoal.HEALTH_MONITORING -> "Conocer bienestar"
                UserGoal.SEEK_PREGNANCY -> "Buscar embarazo"
                UserGoal.AVOID_PREGNANCY -> "Evitar embarazo"
                null -> "Sin seleccionar"
            }

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    SummaryCard(
                        title = "Tu saludo",
                        value = greetingVal,
                        onEdit = { onJumpToStep(2) },
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Tu ciclo",
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
                        title = "Último periodo",
                        value = periodVal,
                        onEdit = { onJumpToStep(4) },
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Tu objetivo",
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
    val tokens = LumiThemeTokens.current

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
                text = "Editar",
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
