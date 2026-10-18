package com.jeremy.lumi.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.ui.theme.LocalPhaseColors
import com.jeremy.lumi.ui.theme.LumiMotion
import com.jeremy.lumi.ui.theme.LumiThemeTokens

/**
 * LumiActionDock — Componente unificado de acciones principales de Lumi.
 *
 * Se presenta como una sola pieza física en el espacio con dos zonas interactivas
 * asimétricas:
 * - "Mi día" (~68% de ancho): Registro holístico de ánimo, síntomas y sueño.
 * - "Me llegó" (~32% de ancho): Registro instantáneo del inicio de menstruación.
 *
 * Características de diseño:
 * - Superficie sólida Lumi con iluminación óptica superior y radial localizada.
 * - Alojamientos ópticos ("pits") que alojan glifos vectoriales sin cajas pesadas.
 * - Prensado independiente por región con SpatialSpring sin deformar el dock entero.
 * - Sin chevrons ni iconos genéricos de Material.
 */
@Composable
fun LumiActionDock(
    onMyDayClick: () -> Unit,
    onPeriodArrivedClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasEntry: Boolean = false,
    moodText: String? = null
) {
    val tokens = LumiThemeTokens.current
    val phaseColors = LocalPhaseColors.current
    val menstrualColor = phaseColors.menstrual

    val configuration = LocalConfiguration.current
    val isNarrowScreen = configuration.screenWidthDp < 380

    val dockShape = RoundedCornerShape(26.dp)

    val interactionSourceMyDay = remember { MutableInteractionSource() }
    val isPressedMyDay by interactionSourceMyDay.collectIsPressedAsState()

    val interactionSourcePeriod = remember { MutableInteractionSource() }
    val isPressedPeriod by interactionSourcePeriod.collectIsPressedAsState()

    // Animación de escala independiente para cada región
    val scaleMyDay by animateFloatAsState(
        targetValue = if (isPressedMyDay) 0.975f else 1f,
        animationSpec = LumiMotion.SpatialSpring,
        label = "my_day_scale"
    )
    val glyphTransYMyDay by animateFloatAsState(
        targetValue = if (isPressedMyDay) 1.5f else 0f,
        animationSpec = LumiMotion.SpatialSpring,
        label = "my_day_glyph_y"
    )
    val glyphScaleMyDay by animateFloatAsState(
        targetValue = if (isPressedMyDay) 0.96f else 1f,
        animationSpec = LumiMotion.SpatialSpring,
        label = "my_day_glyph_scale"
    )

    val scalePeriod by animateFloatAsState(
        targetValue = if (isPressedPeriod) 0.975f else 1f,
        animationSpec = LumiMotion.SpatialSpring,
        label = "period_scale"
    )
    val glyphTransYPeriod by animateFloatAsState(
        targetValue = if (isPressedPeriod) 1.5f else 0f,
        animationSpec = LumiMotion.SpatialSpring,
        label = "period_glyph_y"
    )
    val glyphScalePeriod by animateFloatAsState(
        targetValue = if (isPressedPeriod) 0.96f else 1f,
        animationSpec = LumiMotion.SpatialSpring,
        label = "period_glyph_scale"
    )
    val periodInnerAlpha by animateFloatAsState(
        targetValue = if (isPressedPeriod) 0.45f else 0.85f,
        animationSpec = LumiMotion.SpatialSpring,
        label = "period_inner_alpha"
    )

    // Divisor sutil entre acciones (se atenúa si una está presionada)
    val dividerAlpha by animateFloatAsState(
        targetValue = if (isPressedMyDay || isPressedPeriod) 0.05f else 0.20f,
        animationSpec = LumiMotion.SpatialSpring,
        label = "divider_alpha"
    )

    // Estructura física exterior del Dock
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(dockShape)
            .background(tokens.panel)
            .border(1.dp, tokens.line.copy(alpha = 0.55f), dockShape)
            .drawBehind {
                // Edge highlight superior sutil (White alpha 0.06f)
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(26.dp.toPx(), 0.5f),
                    end = Offset(size.width - 26.dp.toPx(), 0.5f),
                    strokeWidth = 1f
                )

                // Iluminación localizada detrás de "Mi día" (accent)
                val accentGlowAlpha = if (tokens.isDark) 0.08f else 0.05f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tokens.accent.copy(alpha = accentGlowAlpha),
                            tokens.accent.copy(alpha = 0f)
                        ),
                        center = Offset(size.width * 0.28f, size.height * 0.5f),
                        radius = size.width * 0.35f
                    )
                )

                // Iluminación localizada detrás de "Me llegó" (menstrual)
                val menstrualGlowAlpha = if (tokens.isDark) 0.07f else 0.04f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            menstrualColor.copy(alpha = menstrualGlowAlpha),
                            menstrualColor.copy(alpha = 0f)
                        ),
                        center = Offset(size.width * 0.84f, size.height * 0.5f),
                        radius = size.width * 0.25f
                    )
                )
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── REGIÓN 1: "Mi día" (~68%) ────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(0.68f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = scaleMyDay
                        scaleY = scaleMyDay
                    }
                    .clickable(
                        interactionSource = interactionSourceMyDay,
                        indication = null,
                        role = Role.Button,
                        onClick = onMyDayClick
                    )
                    .padding(start = 16.dp, end = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Optical Pit de "Mi día"
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .drawBehind {
                                // Sombra de cavidad óptica inferior
                                drawOval(
                                    color = Color.Black.copy(alpha = 0.14f),
                                    topLeft = Offset(1.dp.toPx(), 2.dp.toPx()),
                                    size = size
                                )
                                // Gradiente de fondo del pit
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            tokens.accent.copy(alpha = 0.14f),
                                            tokens.accent.copy(alpha = 0.03f)
                                        )
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        LumiJournalGlyph(
                            tint = tokens.accent,
                            hasEntry = hasEntry,
                            size = 22.dp,
                            modifier = Modifier.graphicsLayer {
                                translationY = glyphTransYMyDay.dp.toPx()
                                scaleX = glyphScaleMyDay
                                scaleY = glyphScaleMyDay
                            }
                        )
                    }

                    // Textos de "Mi día"
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_action_my_day),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.text,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))

                        val subtitleText = when {
                            hasEntry && !moodText.isNullOrBlank() -> {
                                stringResource(R.string.home_action_mood_prefix, moodText.lowercase())
                            }
                            hasEntry -> {
                                stringResource(R.string.home_action_registered)
                            }
                            isNarrowScreen -> {
                                stringResource(R.string.home_action_my_day_sub_short)
                            }
                            else -> {
                                stringResource(R.string.home_action_my_day_sub)
                            }
                        }

                        Text(
                            text = subtitleText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = tokens.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }

            // ── DIVISOR INTERNO LIGERO (1dp, ~40dp altura) ───────────────────
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(38.dp)
                    .background(tokens.line.copy(alpha = dividerAlpha))
            )

            // ── REGIÓN 2: "Me llegó" (~32%) ──────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(0.32f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = scalePeriod
                        scaleY = scalePeriod
                    }
                    .clickable(
                        interactionSource = interactionSourcePeriod,
                        indication = null,
                        role = Role.Button,
                        onClick = onPeriodArrivedClick
                    )
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Optical Pit de "Me llegó"
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .drawBehind {
                                drawOval(
                                    color = Color.Black.copy(alpha = 0.14f),
                                    topLeft = Offset(1.dp.toPx(), 2.dp.toPx()),
                                    size = size
                                )
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            menstrualColor.copy(alpha = 0.14f),
                                            menstrualColor.copy(alpha = 0.03f)
                                        )
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        LumiPeriodGlyph(
                            tint = menstrualColor,
                            innerAlpha = periodInnerAlpha,
                            size = 21.dp,
                            modifier = Modifier.graphicsLayer {
                                translationY = glyphTransYPeriod.dp.toPx()
                                scaleX = glyphScalePeriod
                                scaleY = glyphScalePeriod
                            }
                        )
                    }

                    Text(
                        text = stringResource(R.string.home_action_period_arrived),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.text,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
