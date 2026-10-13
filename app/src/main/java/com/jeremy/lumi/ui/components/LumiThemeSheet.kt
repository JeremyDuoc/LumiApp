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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.ui.theme.AppThemePalette
import com.jeremy.lumi.ui.theme.LumiThemeTokens

data class ThemeAtmosphereOption(
    val palette: AppThemePalette,
    val name: String,
    val bgColor: Color,
    val ringColor: Color
)

val PrimaryAtmospheres = listOf(
    ThemeAtmosphereOption(AppThemePalette.NOIR, "Noir", Color(0xFF0B0B11), Color(0xFFCFB8F4)),
    ThemeAtmosphereOption(AppThemePalette.PETALO, "Blush", Color(0xFFFFFBF9), Color(0xFFA381C4)),
    ThemeAtmosphereOption(AppThemePalette.LUMI_SAGE_DARK, "Sage", Color(0xFF0D181A), Color(0xFFB9C8BA)),
    ThemeAtmosphereOption(AppThemePalette.LUMI_SPARK_DARK, "Spark", Color(0xFF1B1923), Color(0xFFE8A0C0)),
    ThemeAtmosphereOption(AppThemePalette.LUMI_OCEAN_DARK, "Océano", Color(0xFF0D2447), Color(0xFF00D2FF)),
    ThemeAtmosphereOption(AppThemePalette.LUMI_FOREST_DARK, "Bosque", Color(0xFF0F2E20), Color(0xFF93F9B9))
)

/**
 * Modal BottomSheet para elegir la atmósfera visual, tal como se aprobó en el prototipo HTML.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LumiThemeSheet(
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    selectedTheme: AppThemePalette,
    onSelectTheme: (AppThemePalette) -> Unit,
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
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(tokens.dim.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Elige tu atmósfera",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "La interfaz mantiene su estructura; cambia su personalidad.",
                        fontSize = 11.5.sp,
                        color = tokens.muted
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(tokens.pill)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cerrar",
                        tint = tokens.text,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(PrimaryAtmospheres) { item ->
                    val isPicked = selectedTheme == item.palette

                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(tokens.field)
                            .border(
                                width = if (isPicked) 1.5.dp else 1.dp,
                                color = if (isPicked) tokens.accent else tokens.line,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSelectTheme(item.palette)
                            }
                            .padding(8.dp)
                    ) {
                        // Vista previa de fondo con anillo
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(item.bgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .border(4.dp, item.ringColor, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = item.name,
                            fontSize = 12.sp,
                            fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Medium,
                            color = tokens.text
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Lumi respeta todas tus paletas y personalizaciones previas. Puedes alternar libremente sin alterar tus registros.",
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                color = tokens.muted
            )
        }
    }
}
