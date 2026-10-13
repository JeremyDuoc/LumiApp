package com.jeremy.lumi.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.ui.theme.LumiThemeTokens

/**
 * Modal BottomSheet que explica con transparencia clínica cómo calcula Lumi las predicciones,
 * tal como fue aprobado en el prototipo interactivo HTML.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LumiPredictionInfoSheet(
    onDismiss: () -> Unit
) {
    val tokens = LumiThemeTokens.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Cómo interpreta Lumi tu ciclo",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Las predicciones son orientativas.",
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

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "En Lumi, tu fecha probable de periodo y la fase activa se calculan exclusivamente a partir de tus registros previos, aplicando algoritmos de media ponderada y detección de patrones biológicos.\n\nEl indicador de certeza refleja la regularidad de tus ciclos completados y el historial acumulado, evitando transmitir una falsa seguridad matemática.\n\nImportante: Estas estimaciones no sustituyen métodos anticonceptivos ni asesoramiento médico profesional.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = tokens.muted
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.accent)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Entendido",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (tokens.isDark) tokens.bg else androidx.compose.ui.graphics.Color.White
                )
            }
        }
    }
}
