package com.jeremy.lumi.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import com.jeremy.lumi.ui.widget.LumiWidgetKind

/** Theme-aware picker with miniature previews: only examples, never user medical data. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LumiWidgetPickerSheet(
    onDismiss: () -> Unit,
    onSelect: (LumiWidgetKind) -> Unit
) {
    val t = LumiThemeTokens.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = t.bg
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.widget_settings_title),
                        color = t.text, fontSize = 25.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = (-.5).sp
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        stringResource(R.string.widget_settings_subtitle),
                        color = t.muted, fontSize = 13.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.btn_close), tint = t.muted)
                }
            }
            Spacer(Modifier.height(18.dp))
            WidgetOption(
                kind = LumiWidgetKind.CYCLE,
                title = stringResource(R.string.widget_type_cycle),
                subtitle = stringResource(R.string.widget_type_cycle_desc),
                onSelect = onSelect
            ) {
                Text("lumi", color = t.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Text("12", color = t.text, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text(stringResource(R.string.pm_phase_follicular), color = t.accent, fontSize = 10.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf(t.rose, t.accent, t.line, t.line).forEach { color ->
                        Box(Modifier.size(width = 16.dp, height = 4.dp).background(color))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            WidgetOption(
                kind = LumiWidgetKind.WEEK,
                title = stringResource(R.string.widget_type_week),
                subtitle = stringResource(R.string.widget_type_week_desc),
                onSelect = onSelect
            ) {
                Text("lumi", color = t.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    (14..20).forEachIndexed { idx, num ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$num", fontSize = 10.sp, color = t.text)
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.size(width = 10.dp, height = 3.dp)
                                .background(if (idx < 2) t.rose else if (idx < 5) t.accent else t.mint))
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            WidgetOption(
                kind = LumiWidgetKind.QUIET,
                title = stringResource(R.string.widget_type_quiet),
                subtitle = stringResource(R.string.widget_type_quiet_desc),
                onSelect = onSelect
            ) {
                Text("lumi", color = t.text, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                Spacer(Modifier.height(5.dp))
                Text(stringResource(R.string.widget_quiet_subtitle), color = t.muted, fontSize = 10.sp)
            }
            Spacer(Modifier.height(17.dp))
            Text(
                stringResource(R.string.widget_privacy_note),
                color = t.muted, fontSize = 12.sp, lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun WidgetOption(
    kind: LumiWidgetKind,
    title: String,
    subtitle: String,
    onSelect: (LumiWidgetKind) -> Unit,
    preview: @Composable ColumnScope.() -> Unit
) {
    val t = LumiThemeTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(t.panel)
            .border(1.dp, t.line, RoundedCornerShape(16.dp))
            .clickable { onSelect(kind) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .width(115.dp)
                .height(88.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(t.panel2)
                .padding(11.dp),
            verticalArrangement = Arrangement.Center,
            content = preview
        )
        Spacer(Modifier.width(15.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = t.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Text(subtitle, color = t.muted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = t.accent, modifier = Modifier.size(20.dp))
    }
}
