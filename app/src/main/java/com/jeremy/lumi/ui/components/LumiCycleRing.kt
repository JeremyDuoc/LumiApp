package com.jeremy.lumi.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.draw.drawWithCache
import com.jeremy.lumi.ui.theme.LumiMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import kotlin.math.cos
import kotlin.math.sin

private val DotColor = Color(0xFFF7E8FE)
private val DotAuraColor = Color(0xFFF7E8FE).copy(alpha = 0.35f)

/**
 * Aro de visualización del ciclo menstrual aprobado en el prototipo interactivo Lumi.
 *
 * Reproduce exactamente:
 * - Geometría circular de 238dp con track `tokens.ringTrack`
 * - Progreso animado con degradado (`tokens.ringGradient`) y punta redondeada
 * - Indicador de punto luminoso en la cabeza del arco
 * - Indicador central: gota de agua, "DÍA DEL CICLO", número en 56sp, fase, días restantes
 * - Píldora de certeza de fase
 * - Nota orientativa al pie
 */
@Composable
fun LumiCycleRing(
    modifier: Modifier = Modifier,
    currentDay: Int,
    cycleLength: Int = 28,
    phaseName: String,
    estimatedDaysText: String,
    confidenceLabel: String = stringResource(R.string.cycle_ring_confidence_moderate),
    ringSize: Dp = 238.dp,
    strokeWidth: Dp = 9.dp,
    onConfidenceClick: (() -> Unit)? = null
) {
    val tokens = LumiThemeTokens.current

    val rawProgress = if (cycleLength > 0) {
        (currentDay.toFloat() / cycleLength.toFloat()).coerceIn(0.04f, 1f)
    } else 0.5f

    val progressAnim = remember { Animatable(0f) }
    LaunchedEffect(rawProgress) {
        progressAnim.animateTo(rawProgress, animationSpec = LumiMotion.CycleSpring)
    }

    val breathing = rememberInfiniteTransition(label = "cycle_dot")
    val dotRadius = breathing.animateFloat(0.70f, 0.95f, LumiMotion.DotBreathing, label = "dot_halo")
    val glowColors = remember(tokens.glow) { listOf(tokens.glow, tokens.glow.copy(alpha = 0f)) }

    val semanticsDesc = "Día $currentDay de $cycleLength del ciclo, $phaseName, $estimatedDaysText, $confidenceLabel"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = semanticsDesc
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Contenedor del Aro con resplandor suave
        Box(
            modifier = Modifier.size(ringSize),
            contentAlignment = Alignment.Center
        ) {
            // Canvas del Aro
            Box(modifier = Modifier.fillMaxSize().drawWithCache {
                val glowRadius = size.minDimension / 2f + strokeWidth.toPx() * 1.5f
                val glowBrush = Brush.radialGradient(glowColors, Offset(size.width / 2f, size.height / 2f), glowRadius)
                val trackStroke = Stroke(width = strokeWidth.toPx())
                val arcStroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                onDrawBehind {
                val strokePx = strokeWidth.toPx()
                val diameter = size.minDimension - strokePx
                val topLeft = Offset(strokePx / 2f, strokePx / 2f)
                val arcSize = Size(diameter, diameter)

                // 0. Resplandor radial suave circular (elimina el artefacto cuadrado en GPUs físicas)
                drawCircle(
                    brush = glowBrush,
                    radius = (diameter / 2f) + (strokePx * 2f),
                    center = center
                )

                // 1. Pista circular de fondo
                drawCircle(
                    color = tokens.ringTrack,
                    radius = diameter / 2f,
                    center = center,
                    style = trackStroke
                )

                // 2. Arco de progreso con degradado
                val sweepAngle = progressAnim.value * 360f
                drawArc(
                    brush = tokens.ringGradient,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = arcStroke
                )

                // 3. Indicador de punto en la cabeza del arco
                val angleRad = Math.toRadians((sweepAngle - 90.0)).toFloat()
                val radius = diameter / 2f
                val dotX = center.x + radius * cos(angleRad)
                val dotY = center.y + radius * sin(angleRad)

                // Aura del dot
                drawCircle(
                    color = DotAuraColor,
                    radius = strokePx * dotRadius.value,
                    center = Offset(dotX, dotY)
                )
                // Centro del dot
                drawCircle(
                    color = DotColor,
                    radius = strokePx * 0.45f,
                    center = Offset(dotX, dotY)
                )
                }
            })

            // Contenido central del aro
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LumiIconPeriod(
                    tint = tokens.accent,
                    size = 20.dp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.day_of_cycle_caps),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = tokens.muted
                )
                AnimatedContent(
                    targetState = currentDay,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        ((slideInVertically(LumiMotion.StepSlide) { direction * it / 2 } + fadeIn(LumiMotion.ShortFade))
                            togetherWith (slideOutVertically(LumiMotion.StepExitSlide) { -direction * it / 2 } + fadeOut(LumiMotion.ShortFade)))
                            .using(SizeTransform(clip = false))
                    }, label = "cycle_day"
                ) { day ->
                Text(
                    text = if (day > 0) "$day" else "—",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-3.5).sp,
                    lineHeight = 58.sp,
                    color = tokens.text
                )
                }
                Text(
                    text = phaseName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.text
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = estimatedDaysText,
                    fontSize = 10.5.sp,
                    color = tokens.muted
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Status chip informativo integrado al hero: ● En calibración ⓘ
        Box(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .then(
                    if (onConfidenceClick != null) {
                        Modifier.clickable(
                            role = Role.Button,
                            onClickLabel = confidenceLabel,
                            onClick = onConfidenceClick
                        )
                    } else Modifier
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.5.dp)
                        .clip(CircleShape)
                        .background(tokens.accent.copy(alpha = 0.90f))
                )
                Text(
                    text = confidenceLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = tokens.muted
                )
                LumiIconInfo(
                    tint = tokens.dim.copy(alpha = 0.65f),
                    size = 11.dp
                )
            }
        }
    }
}
