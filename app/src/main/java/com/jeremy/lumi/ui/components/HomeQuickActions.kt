package com.jeremy.lumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.ui.theme.LumiMotion
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import com.jeremy.lumi.ui.theme.lumiPressScale

/**
 * Acciones rápidas de alta jerarquía para el Home de Lumi:
 * - Acción principal: "Mi día" (más amplia, con subtexto de contexto y chevron de apertura).
 * - Acción secundaria: "Me llegó" (bloque compacto lateral de registro de inicio de sangrado).
 */
@Composable
fun HomeQuickActions(
    modifier: Modifier = Modifier,
    hasEntry: Boolean = false,
    entrySubtitle: String? = null,
    onLogClick: () -> Unit,
    onPeriodClick: () -> Unit
) {
    val tokens = LumiThemeTokens.current
    val shape = remember { RoundedCornerShape(22.dp) }

    val myDayInteraction = remember { MutableInteractionSource() }
    val periodInteraction = remember { MutableInteractionSource() }

    val resolvedSubtitle = entrySubtitle
        ?: stringResource(if (hasEntry) R.string.home_log_saved else R.string.home_action_my_day_sub)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── 1. Acción Principal: "Mi día" ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1.62f)
                .heightIn(min = 72.dp)
                .lumiPressScale(myDayInteraction, 0.97f)
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(tokens.panel, tokens.panel2)
                    )
                )
                .border(1.dp, tokens.line, shape)
                .clickable(
                    interactionSource = myDayInteraction,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.home_action_my_day),
                    onClick = onLogClick
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Contenedor orgánico del icono Lumi de Mi día
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(tokens.accentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    LumiIconMyDay(
                        tint = tokens.accent,
                        size = 22.dp
                    )
                }

                // Textos estructurados
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_action_my_day),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.text,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1
                    )
                    Text(
                        text = resolvedSubtitle,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = tokens.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Chevron sutil Lumi
                LumiIconChevronRight(
                    tint = tokens.dim.copy(alpha = 0.65f),
                    size = 14.dp
                )
            }
        }

        // ── 2. Acción Secundaria: "Me llegó" ──────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1.0f)
                .heightIn(min = 72.dp)
                .lumiPressScale(periodInteraction, 0.97f)
                .clip(shape)
                .background(tokens.panel)
                .border(1.dp, tokens.line, shape)
                .clickable(
                    interactionSource = periodInteraction,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.home_action_period_arrived),
                    onClick = onPeriodClick
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Contenedor de la gota Lumi
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.rose.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    LumiIconPeriod(
                        tint = tokens.rose,
                        size = 19.dp
                    )
                }

                Text(
                    text = stringResource(R.string.home_action_period_arrived),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.text,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1
                )
            }
        }
    }
}
