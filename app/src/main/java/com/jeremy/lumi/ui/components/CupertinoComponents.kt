package com.jeremy.lumi.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// CUPERTINO SWITCH (Estilo nativo de iOS)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CupertinoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    checkedTrackColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedTrackColor: Color = Color(0xFFE9E9EA), // Gris estándar de iOS
    thumbColor: Color = Color.White
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) checkedTrackColor else uncheckedTrackColor,
        animationSpec = tween(durationMillis = 200),
        label = "trackColor"
    )

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 21.dp else 2.dp, // Desplazamiento del círculo
        animationSpec = tween(durationMillis = 200),
        label = "thumbOffset"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .width(51.dp)
            .height(31.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Quitar el efecto "ripple" de Android
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(27.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CUPERTINO ACTION SHEET (Menú inferior apilado estilo iOS)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CupertinoActionSheet(
    onDismissRequest: () -> Unit,
    title: @Composable (() -> Unit)? = null,
    message: @Composable (() -> Unit)? = null,
    buttons: @Composable ColumnScope.() -> Unit,
    cancelButton: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.4f) // Fondo oscurecido
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp) // Espacio para el Safe Area
        ) {
            // BLOQUE SUPERIOR (Botones principales)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            ) {
                if (title != null || message != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (title != null) {
                            ProvideTextStyle(TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))) {
                                title()
                            }
                        }
                        if (message != null) {
                            Spacer(Modifier.height(4.dp))
                            ProvideTextStyle(TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))) {
                                message()
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), thickness = 0.5.dp)
                }

                buttons()
            }

            Spacer(modifier = Modifier.height(8.dp))

            // BLOQUE INFERIOR (Botón Cancelar separado)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                cancelButton()
            }
        }
    }
}

@Composable
fun CupertinoActionSheetButton(
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val textColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProvideTextStyle(TextStyle(fontSize = 20.sp, color = textColor, fontWeight = FontWeight.Normal)) {
                content()
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), thickness = 0.5.dp)
    }
}

@Composable
fun CupertinoActionSheetCancelButton(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProvideTextStyle(TextStyle(fontSize = 20.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) {
            content()
        }
    }
}
