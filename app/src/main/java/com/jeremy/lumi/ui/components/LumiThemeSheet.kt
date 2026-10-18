package com.jeremy.lumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jeremy.lumi.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.ui.theme.AppThemePalette
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

data class ThemeAtmosphereOption(
    val palette: AppThemePalette,
    val name: String,
    val isDark: Boolean,
    val bgColor: Color,
    val accentColor: Color
)

val AllThemeAtmospheres = listOf(
    ThemeAtmosphereOption(AppThemePalette.NOIR, "Noir", true, Color(0xFF0E0C13), Color(0xFFCBB4F1)),
    ThemeAtmosphereOption(AppThemePalette.CIRUELA, "Ciruela", true, Color(0xFF241D29), Color(0xFFE8ADC8)),
    ThemeAtmosphereOption(AppThemePalette.BOSQUE, "Bosque", true, Color(0xFF17231F), Color(0xFFA3D4BD)),
    ThemeAtmosphereOption(AppThemePalette.INDIGO, "Índigo", true, Color(0xFF1A2037), Color(0xFFC0C9FF)),
    ThemeAtmosphereOption(AppThemePalette.PETALO, "Pétalo", false, Color(0xFFFFF5F8), Color(0xFFCD9AC9)),
    ThemeAtmosphereOption(AppThemePalette.MARFIL, "Marfil", false, Color(0xFFFFF9EF), Color(0xFFC99193)),
    ThemeAtmosphereOption(AppThemePalette.SALVIA, "Salvia", false, Color(0xFFE4EEE7), Color(0xFF648F76)),
    ThemeAtmosphereOption(AppThemePalette.LAVANDA, "Lavanda", false, Color(0xFFF8F1FF), Color(0xFF9B7CC5))
)

// Alias para retrocompatibilidad
val PrimaryAtmospheres = AllThemeAtmospheres

/**
 * Modal BottomSheet para elegir entre los 8 temas cohesivos oficiales (HTML v11).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LumiThemeSheet(
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    selectedTheme: AppThemePalette,
    onSelectTheme: (AppThemePalette) -> Unit,
    onSelectThemeAt: ((AppThemePalette, Offset) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val tokens = LumiThemeTokens.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.panel,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(tokens.dim.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.theme_sheet_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        color = tokens.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.theme_sheet_subtitle),
                        fontSize = 12.sp,
                        color = tokens.muted
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(tokens.panel2)
                        .border(1.dp, tokens.line, CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.btn_close),
                        tint = tokens.text,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Cuadrícula 2x4 fiel al prototipo HTML v11
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                items(AllThemeAtmospheres) { item ->
                    var centerInWindow by remember { mutableStateOf(Offset.Unspecified) }
                    val isPicked = selectedTheme == item.palette ||
                            (item.palette == AppThemePalette.CIRUELA && selectedTheme == AppThemePalette.VINO) ||
                            (item.palette == AppThemePalette.BOSQUE && (selectedTheme == AppThemePalette.LUMI_FOREST || selectedTheme == AppThemePalette.LUMI_FOREST_DARK)) ||
                            (item.palette == AppThemePalette.INDIGO && (selectedTheme == AppThemePalette.LUMI_OCEAN || selectedTheme == AppThemePalette.LUMI_OCEAN_DARK)) ||
                            (item.palette == AppThemePalette.MARFIL && selectedTheme == AppThemePalette.ARENA) ||
                            (item.palette == AppThemePalette.SALVIA && (selectedTheme == AppThemePalette.LUMI_SAGE || selectedTheme == AppThemePalette.LUMI_SAGE_DARK || selectedTheme == AppThemePalette.CACTUS)) ||
                            (item.palette == AppThemePalette.LAVANDA && (selectedTheme == AppThemePalette.LUMI_LAVENDER || selectedTheme == AppThemePalette.LUMI_LAVENDER_DARK))

                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(15.dp))
                            .background(tokens.bg)
                            .border(
                                width = if (isPicked) 2.dp else 1.dp,
                                color = if (isPicked) tokens.accent else tokens.line,
                                shape = RoundedCornerShape(15.dp)
                            )
                            .onGloballyPositioned {
                                centerInWindow = it.boundsInWindow().center
                            }
                            .clickable {
                                if (onSelectThemeAt != null && centerInWindow.isSpecified) {
                                    onSelectThemeAt(item.palette, centerInWindow)
                                } else {
                                    onSelectTheme(item.palette)
                                }
                            }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Muestra de color (swatch-pair exacto al HTML)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(item.bgColor)
                                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(9.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 2.dp, y = 3.dp)
                                    .clip(CircleShape)
                                    .background(item.accentColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.name,
                                fontSize = 13.sp,
                                fontWeight = if (isPicked) FontWeight.Bold else FontWeight.SemiBold,
                                color = tokens.text
                            )
                            Text(
                                text = if (item.isDark) stringResource(R.string.theme_mode_dark) else stringResource(R.string.theme_mode_light),
                                fontSize = 10.sp,
                                color = tokens.muted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.theme_sheet_footer),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = tokens.muted
            )
        }
    }
}
