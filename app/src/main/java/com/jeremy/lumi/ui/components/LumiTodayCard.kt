package com.jeremy.lumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import com.jeremy.lumi.ui.theme.lumiPressScale

@Composable
fun LumiTodayCard(
    modifier: Modifier = Modifier,
    hasEntry: Boolean = false,
    entrySubtitle: String? = null,
    onLogClick: () -> Unit
) {
    val tokens = LumiThemeTokens.current
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(20.dp)
    val label = stringResource(if (hasEntry) R.string.view_my_log else R.string.log_my_day)
    val subtitle = entrySubtitle ?: stringResource(if (hasEntry) R.string.home_log_saved else R.string.home_log_pending)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .lumiPressScale(interaction, 0.98f)
            .clip(shape)
            .background(tokens.panel)
            .border(1.dp, tokens.line, shape)
            .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current,
                role = Role.Button, onClickLabel = label, onClick = onLogClick)
            .heightIn(min = 88.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(44.dp).background(tokens.accentSoft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.EditCalendar, contentDescription = null, tint = tokens.accent, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = tokens.text)
            Text(subtitle, fontSize = 12.sp, lineHeight = 17.sp, color = tokens.muted)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = tokens.accent, modifier = Modifier.size(22.dp))
    }
}

/**
 * Nota inferior explicativa con enlace modal "Cómo lo calcula Lumi".
 */
@Composable
fun LumiPredictionFootnote(
    modifier: Modifier = Modifier,
    onLearnMoreClick: () -> Unit
) {
    val tokens = LumiThemeTokens.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "✳",
            fontSize = 12.sp,
            color = tokens.muted
        )
        Column {
            Text(
                text = stringResource(R.string.predictions_notice),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = tokens.muted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.how_lumi_calculates),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = tokens.accent,
                modifier = Modifier.clickable(onClick = onLearnMoreClick)
            )
        }
    }
}
