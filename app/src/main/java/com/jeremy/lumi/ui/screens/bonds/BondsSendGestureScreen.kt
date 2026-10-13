package com.jeremy.lumi.ui.screens.bonds

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CareAction
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * Pantalla de Envío de Gestos / Detalles — Diseñada con máxima fidelidad al Teléfono 2.
 *
 * Estética:
 * - Fondo cálido y luminoso con aura orgánica
 * - Elemento central flotante de vidrio/luz
 * - Corazones ascendentes sutiles
 * - Mensaje opcional afectivo
 * - Confirmación inferior animada (Pill Toast: "Tea sent! / ¡Té enviado!")
 */
@Composable
fun BondsSendGestureScreen(
    initialAction: CareAction = CareAction.TEA,
    recipient: BondMember?,
    allMembers: List<BondMember>,
    isSending: Boolean,
    onSend: (linkId: String, action: CareAction, message: String?, onComplete: () -> Unit) -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedAction by remember { mutableStateOf(initialAction) }
    var selectedRecipient by remember { mutableStateOf(recipient ?: allMembers.firstOrNull()) }
    var customMessage by remember { mutableStateOf("") }
    var showSentPill by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "aura_float")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BondsTheme.WarmBackground)
    ) {
        // Fondo con ilustración orgánica y aura (estética exacta Phone 2)
        BondsAuraBackground(modifier = Modifier.fillMaxSize(), pulseFactor = pulseScale)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera superior
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.pm_back),
                        tint = BondsTheme.DeepNavy
                    )
                }
            }

            // Títulos
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.bonds_send_detail_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = BondsTheme.DeepNavy,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.bonds_send_detail_subtitle),
                    fontSize = 15.sp,
                    color = BondsTheme.TextSecondary
                )
            }

            Spacer(modifier = Modifier.weight(0.12f))

            // ── Tarjeta central flotante iluminada (Phone 2) ─────────────────────
            Box(
                modifier = Modifier
                    .size(175.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = BondsTheme.CardShapeLarge,
                        spotColor = BondsTheme.Peach.copy(alpha = 0.5f),
                        ambientColor = BondsTheme.Lavender.copy(alpha = 0.3f)
                    )
                    .clip(BondsTheme.CardShapeLarge)
                    .background(Color.White.copy(alpha = 0.94f))
                    .border(1.5.dp, Color.White, BondsTheme.CardShapeLarge)
                    .clickable {
                        val target = selectedRecipient ?: allMembers.firstOrNull()
                        if (target != null && !isSending) {
                            onSend(target.linkId, selectedAction, customMessage.ifBlank { null }) {
                                showSentPill = true
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Icono del gesto
                    BondsGestureIcon(
                        action = selectedAction,
                        tint = BondsTheme.DeepNavy,
                        size = 46.dp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Nombre del gesto (ej: "Tea")
                    Text(
                        text = gestureTitle(selectedAction),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BondsTheme.DeepNavy
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Subtítulo del gesto (ej: "Warmth & calm")
                    Text(
                        text = gestureSubtitle(selectedAction),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = BondsTheme.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.15f))

            // Selector del destinatario si hay varias personas en el círculo
            if (allMembers.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Para:",
                        fontSize = 13.sp,
                        color = BondsTheme.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    allMembers.forEach { member ->
                        val isChosen = (selectedRecipient?.linkId ?: allMembers.firstOrNull()?.linkId) == member.linkId
                        Surface(
                            shape = BondsTheme.PillShape,
                            color = if (isChosen) BondsTheme.DeepNavy else Color.White,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clickable { selectedRecipient = member }
                        ) {
                            Text(
                                text = member.displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isChosen) Color.White else BondsTheme.DeepNavy,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Campo de mensaje afectivo opcional
            OutlinedTextField(
                value = customMessage,
                onValueChange = { if (it.length <= 140) customMessage = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.bonds_send_custom_message_hint),
                        fontSize = 13.sp,
                        color = BondsTheme.TextTertiary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = BondsTheme.CardShapeMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.9f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.75f),
                    focusedBorderColor = BondsTheme.Lavender,
                    unfocusedBorderColor = BondsTheme.BorderSubtle,
                    focusedTextColor = BondsTheme.DeepNavy,
                    unfocusedTextColor = BondsTheme.DeepNavy
                ),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Botón de confirmación
            Button(
                onClick = {
                    val target = selectedRecipient ?: allMembers.firstOrNull()
                    if (target != null && !isSending) {
                        onSend(target.linkId, selectedAction, customMessage.ifBlank { null }) {
                            showSentPill = true
                        }
                    }
                },
                enabled = (selectedRecipient != null || allMembers.isNotEmpty()) && !isSending,
                shape = BondsTheme.PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BondsTheme.DeepNavy,
                    contentColor = Color.White,
                    disabledContainerColor = BondsTheme.TextTertiary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.bonds_sending),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.bonds_send_button),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── Pill Toast de Confirmación (Phone 2 bottom pill) ──────────────────
        AnimatedVisibility(
            visible = showSentPill,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
        ) {
            SentNotificationPill(
                action = selectedAction,
                onDismiss = {
                    showSentPill = false
                    onNavigateBack()
                }
            )
        }
    }
}

/**
 * Toast / Notificación inferior: "Tea sent! She'll know you're thinking of her. 💜"
 */
@Composable
private fun SentNotificationPill(
    action: CareAction,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2500)
        onDismiss()
    }

    Surface(
        shape = BondsTheme.CardShapeMedium,
        color = Color.White.copy(alpha = 0.96f),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BondsTheme.Peach.copy(alpha = 0.6f), BondsTheme.CardShapeMedium)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de check con círculo melocotón
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BondsTheme.Peach),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = stringResource(R.string.bonds_gesture_sent_pill, gestureTitle(action)),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BondsTheme.DeepNavy
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.bonds_gesture_sent_sub),
                    fontSize = 12.5.sp,
                    color = BondsTheme.TextSecondary
                )
            }
        }
    }
}

/**
 * Fondo con aura orgánica degradada y corazones ascendentes sutiles (Phone 2)
 */
@Composable
private fun BondsAuraBackground(
    modifier: Modifier = Modifier,
    pulseFactor: Float = 1f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.42f

        // Capa 1: Gran resplandor etéreo lavanda y rosa envolvente
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE8D4F8).copy(alpha = 0.65f),
                    Color(0xFFFDE0EC).copy(alpha = 0.45f),
                    Color(0xFFFFECE5).copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = Offset(cx + w * 0.08f, cy - h * 0.03f),
                radius = (w * 0.72f) * pulseFactor
            ),
            center = Offset(cx + w * 0.08f, cy - h * 0.03f),
            radius = (w * 0.72f) * pulseFactor
        )

        // Capa 2: Resplandor melocotón y coral cálido central
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFC0A8).copy(alpha = 0.75f),
                    Color(0xFFFFD4C4).copy(alpha = 0.50f),
                    Color(0xFFFBE4DC).copy(alpha = 0.20f),
                    Color.Transparent
                ),
                center = Offset(cx - w * 0.06f, cy + h * 0.02f),
                radius = (w * 0.55f) * pulseFactor
            ),
            center = Offset(cx - w * 0.06f, cy + h * 0.02f),
            radius = (w * 0.55f) * pulseFactor
        )

        // Capa 3: Estela de vapor / curvas ascendentes sutiles (Phone 2)
        val steamPath1 = Path().apply {
            moveTo(cx - w * 0.08f, cy - h * 0.12f)
            cubicTo(cx - w * 0.18f, cy - h * 0.22f, cx - w * 0.02f, cy - h * 0.28f, cx - w * 0.10f, cy - h * 0.36f)
        }
        drawPath(
            steamPath1,
            color = Color(0xFFFFD5C6).copy(alpha = 0.5f),
            style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
        )

        val steamPath2 = Path().apply {
            moveTo(cx + w * 0.06f, cy - h * 0.10f)
            cubicTo(cx + w * 0.16f, cy - h * 0.20f, cx + w * 0.02f, cy - h * 0.27f, cx + w * 0.12f, cy - h * 0.35f)
        }
        drawPath(
            steamPath2,
            color = Color(0xFFE9D0F8).copy(alpha = 0.45f),
            style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round)
        )

        // Corazones decorativos flotantes (fieles a Phone 2)
        drawFloatingHeart(Offset(w * 0.30f, h * 0.26f), 14.dp.toPx(), Color(0xFFFF9E8D).copy(alpha = 0.8f))
        drawFloatingHeart(Offset(w * 0.72f, h * 0.24f), 12.dp.toPx(), Color(0xFFF09AB8).copy(alpha = 0.75f))
        drawFloatingHeart(Offset(w * 0.80f, h * 0.42f), 19.dp.toPx(), Color(0xFFE8758F).copy(alpha = 0.85f))
        drawFloatingHeart(Offset(w * 0.22f, h * 0.50f), 15.dp.toPx(), Color(0xFFF394A8).copy(alpha = 0.8f))
        drawFloatingHeart(Offset(w * 0.74f, h * 0.60f), 17.dp.toPx(), Color(0xFFE8758F).copy(alpha = 0.75f))
        drawFloatingHeart(Offset(w * 0.38f, h * 0.64f), 11.dp.toPx(), Color(0xFFF5A3B5).copy(alpha = 0.65f))
    }
}

private fun DrawScope.drawFloatingHeart(center: Offset, sizePx: Float, color: Color) {
    val hs = sizePx * 0.5f
    val cx = center.x
    val cy = center.y
    val heartPath = Path().apply {
        moveTo(cx, cy + hs * 0.7f)
        cubicTo(cx - hs, cy + hs * 0.1f, cx - hs * 0.9f, cy - hs * 0.6f, cx, cy - hs * 0.2f)
        cubicTo(cx + hs * 0.9f, cy - hs * 0.6f, cx + hs, cy + hs * 0.1f, cx, cy + hs * 0.7f)
        close()
    }
    drawPath(heartPath, color = color)
}

@Composable
private fun gestureTitle(action: CareAction): String = when (action) {
    CareAction.HUG -> stringResource(R.string.bonds_gesture_hug_title)
    CareAction.TEA -> stringResource(R.string.bonds_gesture_tea_title)
    CareAction.COFFEE -> stringResource(R.string.bonds_gesture_coffee_title)
    CareAction.CHOCOLATE -> stringResource(R.string.bonds_gesture_chocolate_title)
    CareAction.HEATING_PAD -> stringResource(R.string.bonds_gesture_heating_pad_title)
    CareAction.PHARMACY -> stringResource(R.string.bonds_gesture_pharmacy_title)
}

@Composable
private fun gestureSubtitle(action: CareAction): String = when (action) {
    CareAction.HUG -> stringResource(R.string.bonds_gesture_hug_desc)
    CareAction.TEA -> stringResource(R.string.bonds_gesture_tea_desc)
    CareAction.COFFEE -> stringResource(R.string.bonds_gesture_coffee_desc)
    CareAction.CHOCOLATE -> stringResource(R.string.bonds_gesture_chocolate_desc)
    CareAction.HEATING_PAD -> stringResource(R.string.bonds_gesture_heating_pad_desc)
    CareAction.PHARMACY -> stringResource(R.string.bonds_gesture_pharmacy_desc)
}
