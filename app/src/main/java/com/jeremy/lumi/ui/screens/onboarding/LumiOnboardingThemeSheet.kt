package com.jeremy.lumi.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * BottomSheet para la selección de los 8 temas cohesivos del Onboarding v7.
 * Reproduce fielmente la cuadrícula de temas 2x2/2x4 y la ficha de swatch de HTML v7.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LumiOnboardingThemeSheet(
    selectedTheme: LumiOnboardingTheme,
    onSelectTheme: (LumiOnboardingTheme) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiOnboardingThemeTokens.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = tokens.panel,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(tokens.line)
            )
        },
        modifier = modifier
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
                        text = "Hazlo tuyo.",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.8).sp,
                        color = tokens.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Elige la atmósfera de Lumi",
                        fontSize = 11.5.sp,
                        color = tokens.muted
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(tokens.panel2)
                        .border(1.dp, tokens.line, CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
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

            val themes = LumiOnboardingTheme.entries
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in themes.chunked(2)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (theme in row) {
                            val isSelected = theme == selectedTheme
                            ThemeSheetCard(
                                theme = theme,
                                isSelected = isSelected,
                                onClick = {
                                    onSelectTheme(theme)
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSheetCard(
    theme: LumiOnboardingTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiOnboardingThemeTokens.current

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 88.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(tokens.bg)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) tokens.accent else tokens.line,
                shape = RoundedCornerShape(15.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Muestra de color (swatch)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(27.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(theme.previewBg)
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(9.dp))
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 3.dp)
                    .clip(CircleShape)
                    .background(theme.previewAccent)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = theme.displayName,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.text
            )
            Text(
                text = if (theme.isDark) "Oscuro" else "Claro",
                fontSize = 9.5.sp,
                color = tokens.muted
            )
        }
    }
}
