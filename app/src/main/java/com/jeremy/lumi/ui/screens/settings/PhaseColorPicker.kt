package com.jeremy.lumi.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.ui.theme.PhaseColorSwatches
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────────────────
//  PHASE COLOR PICKER — combina grid de swatches rápidos + rueda HSV libre
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PhaseColorPicker(
    label        : String,
    currentColor : Color,
    onColorPicked: (Color) -> Unit
) {
    var wheelExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(currentColor)
                        .border(1.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text       = label,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onBackground
                )
            }

            // Botón "más colores" — abre/cierra la rueda HSV libre
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { wheelExpanded = !wheelExpanded }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector        = Icons.Rounded.Colorize,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    modifier           = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text     = "Más colores",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                )
                Icon(
                    imageVector        = if (wheelExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                    modifier           = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Grid rápido de swatches curados
        LazyVerticalGrid(
            columns               = GridCells.Fixed(8),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement   = Arrangement.spacedBy(8.dp),
            modifier              = Modifier
                .fillMaxWidth()
                .height(72.dp),
            userScrollEnabled     = false
        ) {
            items(PhaseColorSwatches) { swatch ->
                val isSelected = swatch.toArgb() == currentColor.toArgb()
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(
                            width = if (isSelected) 2.5.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            shape = CircleShape
                        )
                        .clickable { onColorPicked(swatch) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector        = Icons.Rounded.Check,
                            contentDescription = null,
                            tint               = if (swatch.luminanceIsLight()) Color.Black else Color.White,
                            modifier           = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Rueda HSV libre — solo se infla cuando la usuaria la pide explícitamente
        AnimatedVisibility(
            visible = wheelExpanded,
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HsvColorWheel(
                    initialColor = currentColor,
                    onColorChange = onColorPicked
                )
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

private fun Color.luminanceIsLight(): Boolean {
    val l = 0.299f * red + 0.587f * green + 0.114f * blue
    return l > 0.6f
}

// ─────────────────────────────────────────────────────────────────────────────
//  RUEDA DE COLOR HSV — implementación libre en Canvas, sin dependencias extra.
//  Disco de matiz/saturación + slider de brillo debajo.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HsvColorWheel(
    initialColor : Color,
    onColorChange: (Color) -> Unit
) {
    val hsv = remember {
        val arr = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), arr)
        mutableStateOf(arr)
    }

    var hue        by remember { mutableFloatStateOf(hsv.value[0]) }        // 0..360
    var saturation by remember { mutableFloatStateOf(hsv.value[1]) }        // 0..1
    var value      by remember { mutableFloatStateOf(hsv.value[2].coerceAtLeast(0.35f)) } // 0..1, piso para que no quede negro puro

    fun emit() {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        onColorChange(Color(argb))
    }

    val wheelDiameter = 180.dp

    Box(
        modifier = Modifier.size(wheelDiameter),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    fun updateFromOffset(offset: Offset) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val radius = min(size.width, size.height) / 2f
                        val dist = sqrt(dx * dx + dy * dy).coerceAtMost(radius)
                        var angle = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
                        if (angle < 0) angle += 360f
                        hue = angle
                        saturation = (dist / radius).coerceIn(0f, 1f)
                        emit()
                    }
                    detectTapGestures { updateFromOffset(it) }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = change.position.x - center.x
                        val dy = change.position.y - center.y
                        val radius = min(size.width, size.height) / 2f
                        val dist = sqrt(dx * dx + dy * dy).coerceAtMost(radius)
                        var angle = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
                        if (angle < 0) angle += 360f
                        hue = angle
                        saturation = (dist / radius).coerceIn(0f, 1f)
                        emit()
                    }
                }
        ) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Disco de matiz: 360 cuñas finas en gradiente angular simulado con segmentos
            val segments = 120
            for (i in 0 until segments) {
                val startAngle = i * (360f / segments)
                val segHue = startAngle
                drawArc(
                    color     = Color(android.graphics.Color.HSVToColor(floatArrayOf(segHue, 1f, value))),
                    startAngle = startAngle,
                    sweepAngle = 360f / segments + 1f,
                    useCenter  = true,
                    topLeft    = Offset(center.x - radius, center.y - radius),
                    size       = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
            }
            // Overlay radial blanco→transparente para simular desaturación hacia el centro
            drawCircle(
                brush  = Brush.radialGradient(
                    colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Indicador de posición actual
            val angleRad = Math.toRadians(hue.toDouble())
            val r = saturation * radius
            val pointX = center.x + (cos(angleRad) * r).toFloat()
            val pointY = center.y + (sin(angleRad) * r).toFloat()
            drawCircle(Color.White, radius = 9f, center = Offset(pointX, pointY))
            drawCircle(Color.Black.copy(alpha = 0.4f), radius = 9f, center = Offset(pointX, pointY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
        }
    }

    Spacer(Modifier.height(14.dp))

    // Slider de brillo (Value de HSV)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(0.85f)
    ) {
        Text(stringResource(R.string.settings_brightness), fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        Spacer(Modifier.width(10.dp))
        Slider(
            value = value,
            onValueChange = { value = it; emit() },
            valueRange = 0.25f..1f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor       = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))),
                activeTrackColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))).copy(alpha = 0.6f)
            )
        )
    }
}