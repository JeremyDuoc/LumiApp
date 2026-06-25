package com.jeremy.lumi.ui.screens.partner

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Bitmap
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun PartnerWizardScreen(
    viewModel: PartnerViewModel,
    initialCode: String? = null,
    onClose: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    // 0=Ninguno, 1=Compartir mi ciclo, 2=Seguir a alguien
    var selectedMode by remember { mutableIntStateOf(0) }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var linkCode by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }
    
    val totalSteps = if (selectedMode == 1) 4 else if (selectedMode == 2) 3 else 2
    val primary = MaterialTheme.colorScheme.primary
    val scope = rememberCoroutineScope()

    // Manejar initialCode por Deep Link
    LaunchedEffect(initialCode) {
        if (!initialCode.isNullOrEmpty()) {
            selectedMode = 2
            currentStep = 1
            codeInput = initialCode
            viewModel.joinLink(initialCode)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            
            // â”€â”€ Header (Botón atrás + Puntos) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 16.dp, start = 8.dp, end = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (currentStep > 0) currentStep-- else onClose()
                }) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack, 
                        contentDescription = "Volver", 
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Spacer(Modifier.weight(1f))
                
                if (currentStep < totalSteps - 1) {
                    WizardPageIndicator(totalPages = totalSteps - 1, currentPage = currentStep)
                }
                
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }

            // â”€â”€ Contenido de Pasos Animado â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { if (forward) it else -it } + fadeIn(tween(320))) togetherWith
                    (slideOutHorizontally { if (forward) -it else it } + fadeOut(tween(220)))
                },
                modifier = Modifier.weight(1f),
                label = "wizard_step"
            ) { step ->
                when (step) {
                    0 -> WizardModeSelection(
                        onShareClick = {
                            selectedMode = 1
                            currentStep++
                        },
                        onTrackClick = {
                            selectedMode = 2
                            currentStep++
                        }
                    )
                    1 -> {
                        if (selectedMode == 1) {
                            WizardPrivacyConfig(
                                sharePhase = uiState.sharePhase,
                                shareMood = uiState.shareMood,
                                shareSymptoms = uiState.shareSymptoms,
                                onTogglePhase = { viewModel.setSharePhase(!uiState.sharePhase) },
                                onToggleMood = { viewModel.setShareMood(!uiState.shareMood) },
                                onToggleSymptoms = { viewModel.setShareSymptoms(!uiState.shareSymptoms) },
                                onNext = {
                                    viewModel.createLink()
                                    currentStep++
                                }
                            )
                        } else {
                            WizardEnterCode(
                                code = codeInput,
                                onCodeChange = { codeInput = it },
                                error = inputError,
                                isLoading = uiState.isLoading,
                                onJoin = {
                                    inputError = null
                                    viewModel.joinLink(codeInput)
                                }
                            )
                        }
                    }
                    2 -> {
                        if (selectedMode == 1) {
                            WizardShowCode(code = uiState.pendingCode ?: "", uiState = uiState)
                        } else {
                            WizardSuccess(onClose = onClose)
                        }
                    }
                    3 -> {
                        if (selectedMode == 1) {
                            WizardSuccess(onClose = onClose)
                        }
                    }
                }
            }
        }
    }
    
    // Auto-avance: cuando la pareja se une al código generado (modo OWNER)
    LaunchedEffect(uiState.activeLinks) {
        if (currentStep == 2 && selectedMode == 1 && uiState.pendingCode != null) {
            val matchingLink = uiState.activeLinks.find {
                it.linkCode == uiState.pendingCode && it.status == com.jeremy.lumi.domain.model.LinkStatus.ACTIVE
            }
            if (matchingLink != null) {
                delay(800)
                currentStep = 3
            }
        }
    }

    // Auto-avance al unirse con código (modo PARTNER)
    // BUG FIX: Solo avanza si hay un link NUEVO y ACTIVO en la lista, no solo por isLoading=false
    val prevLinksCount = remember { mutableIntStateOf(uiState.activeLinks.size) }
    LaunchedEffect(uiState.activeLinks) {
        val newCount = uiState.activeLinks.size
        if (currentStep == 1 && selectedMode == 2 && newCount > prevLinksCount.intValue) {
            // Hubo un link nuevo -> éxito real
            delay(300)
            currentStep++
        }
        prevLinksCount.intValue = newCount
    }

    // Mostrar error si joinLink falló
    LaunchedEffect(uiState.error) {
        if (currentStep == 1 && selectedMode == 2 && uiState.error != null) {
            inputError = uiState.error
        }
    }
}

@Composable
private fun WizardPageIndicator(totalPages: Int, currentPage: Int, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(totalPages) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (selected) 24.dp else 8.dp,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                label = "dot_width"
            )
            val alpha by animateFloatAsState(
                targetValue = if (selected) 1f else 0.35f,
                animationSpec = tween(300),
                label = "dot_alpha"
            )
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .alpha(alpha)
                    .background(primary)
            )
        }
    }
}

@Composable
private fun WizardModeSelection(onShareClick: () -> Unit, onTrackClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            text = "¿Qué deseas hacer?",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Elige si compartes tu ciclo con alguien, o si quieres acompañar el ciclo de otra persona.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(64.dp))
        
        // Tarjeta Compartir
        Surface(
            onClick = onShareClick,
            shape = RoundedCornerShape(24.dp),
            color = primary.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, primary.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth().height(110.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, tint = primary, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Compartir mi ciclo", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    Text("Genero un código de invitación", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.6f))
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Tarjeta Seguir
        Surface(
            onClick = onTrackClick,
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth().height(110.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.FavoriteBorder, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Acompañar a alguien", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    Text("Ingresaré un código", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.6f))
                }
            }
        }
    }
}

@Composable
private fun WizardPrivacyConfig(
    sharePhase: Boolean,
    shareMood: Boolean,
    shareSymptoms: Boolean,
    onTogglePhase: () -> Unit,
    onToggleMood: () -> Unit,
    onToggleSymptoms: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Tu Privacidad",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Elige qué información podrá ver la persona con quien te vincules. Puedes cambiar esto en cualquier momento.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
        )
        
        Spacer(Modifier.height(48.dp))
        
        PrivacyToggleRow(
            title = "Fase del ciclo actual",
            subtitle = "Menstrual, folicular, ovulación...",
            isChecked = sharePhase,
            onCheckedChange = { onTogglePhase() }
        )
        Spacer(Modifier.height(16.dp))
        PrivacyToggleRow(
            title = "Estado de ánimo",
            subtitle = "Compartir cómo te sientes",
            isChecked = shareMood,
            onCheckedChange = { onToggleMood() }
        )
        Spacer(Modifier.height(16.dp))
        PrivacyToggleRow(
            title = "Síntomas",
            subtitle = "Compartir si tienes cólicos, etc.",
            isChecked = shareSymptoms,
            onCheckedChange = { onToggleSymptoms() }
        )
        
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 24.dp)
        ) {
            Text("Generar Código", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PrivacyToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onCheckedChange(!isChecked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.6f))
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun WizardShowCode(code: String, uiState: PartnerUiState) {
    val context = LocalContext.current
    
    // Generador de QR
    val qrBitmap = remember(code) {
        if (code.isNotEmpty()) {
            try {
                val size = 512
                // URL que se comparte
                val linkUrl = "https://lumi.app/pair/$code"
                val bitMatrix = QRCodeWriter().encode(linkUrl, BarcodeFormat.QR_CODE, size, size)
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                // Colores para el QR
                val black = android.graphics.Color.BLACK
                val white = android.graphics.Color.TRANSPARENT
                for (x in 0 until size) {
                    for (y in 0 until size) {
                        bitmap.setPixel(x, y, if (bitMatrix[x, y]) black else white)
                    }
                }
                bitmap.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Código de Invitación", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Tu pareja puede escanear este código QR o usar el enlace.", fontSize = 15.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.65f))
        
        Spacer(Modifier.height(40.dp))
        
        // Contenedor del QR
        Box(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(24.dp))
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha=0.2f), RoundedCornerShape(24.dp))
                .padding(24.dp)
                .size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            if (qrBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = qrBitmap,
                    contentDescription = "Código QR de invitación",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Código en texto
        Text(
            text = code, 
            fontSize = 32.sp, 
            letterSpacing = 8.sp, 
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(Modifier.weight(1f))
        
        // Botón Share
        Button(
            onClick = {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "¡Hola! Sigue mi ciclo en Lumi con este enlace:\n\nhttps://lumi.app/pair/$code")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Compartir enlace de Lumi")
                context.startActivity(shareIntent)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 8.dp)
        ) {
            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Enviar Enlace por Mensaje", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text("Esperando que tu pareja se una...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.5f))
        }
    }
}

@Composable
private fun WizardEnterCode(
    code: String,
    onCodeChange: (String) -> Unit,
    error: String?,
    isLoading: Boolean,
    onJoin: () -> Unit
) {
    // Lanzador del escáner de ZXing
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            // Extraer el código si viene de un enlace https://lumi.app/pair/123456
            val scannedText = result.contents
            val extractedCode = if (scannedText.contains("/pair/")) {
                scannedText.substringAfterLast("/")
            } else {
                scannedText
            }.take(6).uppercase()
            
            onCodeChange(extractedCode)
            if (extractedCode.length == 6) {
                onJoin()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Ingresar Código", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Escanea el QR de la otra persona o pega el código manualmente.", fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.65f))
        
        Spacer(Modifier.height(40.dp))

        // Botón grande para escanear QR
        Surface(
            onClick = {
                val options = ScanOptions().apply {
                    setPrompt("Escanea el QR del vínculo de Lumi")
                    setBeepEnabled(false)
                    setOrientationLocked(false)
                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                }
                scanLauncher.launch(options)
            },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth().height(100.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_camera), 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text("Escanear Código QR", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        Text("O introduce el código manual:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6) onCodeChange(it.filter { c -> c.isLetterOrDigit() }.uppercase()) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 28.sp,
                letterSpacing = 12.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            ),
            singleLine = true,
            isError = error != null,
            supportingText = { if (error != null) Text(error) }
        )
        
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onJoin,
            enabled = code.length == 6 && !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 24.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Vincular", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun WizardSuccess(onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val transition = rememberInfiniteTransition()
        val floatOffsetY by transition.animateFloat(
            initialValue = -10f, targetValue = 10f,
            animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse)
        )
        
        Box(
            modifier = Modifier
                .offset(y = floatOffsetY.dp)
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha=0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("✨", fontSize = 48.sp)
        }
        
        Spacer(Modifier.height(32.dp))
        Text("¡Vínculo Creado!", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("Ahora están conectados. Toda la información seleccionada se sincronizará mágicamente.", 
             fontSize = 16.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.7f))
             
        Spacer(Modifier.height(64.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Ir a Mis Vínculos", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

