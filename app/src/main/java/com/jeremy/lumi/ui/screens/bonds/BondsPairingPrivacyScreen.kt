package com.jeremy.lumi.ui.screens.bonds

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.SharePreferences
import kotlinx.coroutines.launch

/**
 * Pantalla de Vinculación y Privacidad — Diseñada con máxima fidelidad al Teléfono 3.
 *
 * Estética:
 * - Tarjeta superior azul oscuro Deep Navy ("Link your circles")
 * - Código QR de alta nitidez + código formateado "482 917" con botón copiar
 * - Sección "What you share" con 4 switches refinados
 * - Tarjeta informativa "Your privacy matters"
 */
@Composable
fun BondsPairingPrivacyScreen(
    currentInviteCode: String?,
    initialPrivacy: SharePreferences,
    isObserverOnly: Boolean,
    onGenerateCode: suspend () -> String,
    onJoinWithCode: (String, SharePreferences, (Boolean) -> Unit) -> Unit,
    onUpdatePrivacy: (SharePreferences) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var activeCode by remember { mutableStateOf(currentInviteCode ?: "") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGeneratingCode by remember { mutableStateOf(false) }

    // Estado local de los toggles de privacidad
    var sharePhase by remember { mutableStateOf(initialPrivacy.phase) }
    var shareMood by remember { mutableStateOf(initialPrivacy.mood) }
    var shareSymptoms by remember { mutableStateOf(initialPrivacy.symptoms) }
    var sharePredictions by remember { mutableStateOf(initialPrivacy.predictions) }

    // Diálogo o sección para unirse con código
    var isJoinExpanded by remember { mutableStateOf(false) }
    var inputJoinCode by remember { mutableStateOf("") }
    var isJoining by remember { mutableStateOf(false) }

    // Generar o refrescar código al cargar
    LaunchedEffect(currentInviteCode) {
        if (!currentInviteCode.isNullOrBlank()) {
            activeCode = currentInviteCode
            qrBitmap = generateQrCode("lumi://bonds/pair/$currentInviteCode", 320)
        } else if (activeCode.isBlank()) {
            isGeneratingCode = true
            val newCode = onGenerateCode()
            activeCode = newCode
            qrBitmap = generateQrCode("lumi://bonds/pair/$newCode", 320)
            isGeneratingCode = false
        }
    }

    Scaffold(
        containerColor = BondsTheme.WarmBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.pm_back),
                        tint = BondsTheme.DeepNavy
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            // Títulos principales (Phone 3)
            Text(
                text = stringResource(R.string.bonds_pairing_privacy_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = BondsTheme.DeepNavy,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.bonds_pairing_privacy_subtitle),
                fontSize = 15.sp,
                color = BondsTheme.TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Tarjeta Azul Oscuro: "Link your circles" (Phone 3) ───────────────
            Card(
                shape = BondsTheme.CardShapeLarge,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BondsTheme.DarkCardGradient)
                ) {
                    // Resplandor orgánico superior derecho (Phone 3)
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF8A6DF0).copy(alpha = 0.45f),
                                    Color(0xFF6749DB).copy(alpha = 0.20f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.94f, size.height * 0.12f),
                                radius = size.width * 0.48f
                            ),
                            center = Offset(size.width * 0.94f, size.height * 0.12f),
                            radius = size.width * 0.48f
                        )
                    }

                    Column(modifier = Modifier.padding(22.dp)) {
                        Text(
                            text = stringResource(R.string.bonds_link_circles_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.bonds_link_circles_desc),
                            fontSize = 12.5.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // QR Code + Pairing Code
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Código QR blanco redondeado
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(BondsTheme.CardShapeMedium)
                                    .background(Color.White)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (qrBitmap != null) {
                                    Image(
                                        bitmap = qrBitmap!!.asImageBitmap(),
                                        contentDescription = "QR Code",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = BondsTheme.DeepNavy,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }

                            // Tarjeta de código numérico
                            Surface(
                                shape = BondsTheme.CardShapeMedium,
                                color = Color.White,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(96.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.bonds_pairing_code_label),
                                        fontSize = 11.sp,
                                        color = BondsTheme.TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = formatPairingCode(activeCode),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BondsTheme.DeepNavy,
                                            letterSpacing = 1.sp
                                        )

                                        IconButton(
                                            onClick = {
                                                if (activeCode.isNotBlank()) {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Bonds Code", activeCode))
                                                    Toast.makeText(context, context.getString(R.string.bonds_code_copied), Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ContentCopy,
                                                contentDescription = "Copiar",
                                                tint = BondsTheme.DeepNavy,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón para unirse a un vínculo con código
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isJoinExpanded = !isJoinExpanded }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isJoinExpanded) "Ocultar ingreso de código" else "¿Tienes un código de invitación? Toca aquí",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BondsTheme.SoftViolet
                )
            }

            AnimatedVisibility(visible = isJoinExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp)
                ) {
                    OutlinedTextField(
                        value = inputJoinCode,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) inputJoinCode = it },
                        label = { Text(stringResource(R.string.bonds_enter_code_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = BondsTheme.CardShapeMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = BondsTheme.DeepNavy,
                            unfocusedBorderColor = BondsTheme.BorderSubtle
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (inputJoinCode.length == 6 && !isJoining) {
                                isJoining = true
                                val privacy = SharePreferences(sharePhase, shareMood, shareSymptoms, sharePredictions)
                                onJoinWithCode(inputJoinCode, privacy) { success ->
                                    isJoining = false
                                    if (success) {
                                        Toast.makeText(context, "¡Vínculo conectado con éxito!", Toast.LENGTH_SHORT).show()
                                        onNavigateBack()
                                    }
                                }
                            }
                        },
                        enabled = inputJoinCode.length == 6 && !isJoining,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = BondsTheme.PillShape,
                        colors = ButtonDefaults.buttonColors(containerColor = BondsTheme.DeepNavy)
                    ) {
                        if (isJoining) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.bonds_join_button), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Sección "What you share" (Phone 3) ──────────────────────────────
            Text(
                text = stringResource(R.string.bonds_what_you_share_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BondsTheme.DeepNavy
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.bonds_what_you_share_desc),
                fontSize = 13.sp,
                color = BondsTheme.TextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Lista de los 4 switches con estética exacta
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PrivacyToggleRow(
                    icon = Icons.Rounded.Nightlight,
                    title = stringResource(R.string.bonds_toggle_phase_title),
                    description = stringResource(R.string.bonds_toggle_phase_desc),
                    checked = sharePhase,
                    onCheckedChange = {
                        sharePhase = it
                        onUpdatePrivacy(SharePreferences(sharePhase, shareMood, shareSymptoms, sharePredictions))
                    }
                )

                PrivacyToggleRow(
                    icon = Icons.Rounded.Mood,
                    title = stringResource(R.string.bonds_toggle_mood_title),
                    description = stringResource(R.string.bonds_toggle_mood_desc),
                    checked = shareMood,
                    onCheckedChange = {
                        shareMood = it
                        onUpdatePrivacy(SharePreferences(sharePhase, shareMood, shareSymptoms, sharePredictions))
                    }
                )

                PrivacyToggleRow(
                    icon = Icons.Rounded.FlashOn,
                    title = stringResource(R.string.bonds_toggle_symptoms_title),
                    description = stringResource(R.string.bonds_toggle_symptoms_desc),
                    checked = shareSymptoms,
                    onCheckedChange = {
                        shareSymptoms = it
                        onUpdatePrivacy(SharePreferences(sharePhase, shareMood, shareSymptoms, sharePredictions))
                    }
                )

                PrivacyToggleRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = stringResource(R.string.bonds_toggle_predictions_title),
                    description = stringResource(R.string.bonds_toggle_predictions_desc),
                    checked = sharePredictions,
                    onCheckedChange = {
                        sharePredictions = it
                        onUpdatePrivacy(SharePreferences(sharePhase, shareMood, shareSymptoms, sharePredictions))
                    }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Tarjeta "Your privacy matters" (Phone 3) ────────────────────────
            Surface(
                shape = BondsTheme.CardShapeMedium,
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BondsTheme.DeepNavy.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = BondsTheme.DeepNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.bonds_privacy_matters_title),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BondsTheme.DeepNavy
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.bonds_privacy_matters_desc),
                            fontSize = 12.sp,
                            color = BondsTheme.TextSecondary,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = BondsTheme.TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BondsTheme.CardShapeMedium)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, BondsTheme.BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BondsTheme.DeepNavy,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BondsTheme.DeepNavy
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = BondsTheme.TextSecondary
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BondsTheme.DeepNavy,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = BondsTheme.BorderSubtle
            )
        )
    }
}

private fun formatPairingCode(code: String): String {
    val digits = code.filter { it.isDigit() }
    return if (digits.length == 6) {
        "${digits.substring(0, 3)} ${digits.substring(3, 6)}"
    } else {
        code.ifBlank { "••• •••" }
    }
}

private fun generateQrCode(content: String, sizePx: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
