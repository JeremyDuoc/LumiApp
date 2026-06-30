package com.jeremy.lumi.ui.screens.insights

// ─────────────────────────────────────────────────────────────────────────────
//  GRÁFICAS AVANZADAS — Charts premium con Canvas de Compose
//
//  Contiene tres tarjetas de gráficas de alta calidad visual:
//   A. HistorialCiclosGrafica  — Barras redondeadas con gradiente (últimos 6 ciclos)
//   B. DolorYEstresGrafica     — Doble curva bezier con relleno de área degradado
//   C. TemperaturaBasalGrafica — Línea BBT con detección de spike de ovulación
//
//  Principios de diseño:
//   - Glassmorphism: contenedores con blur y bordes translúcidos
//   - Dark mode: colores adaptativos via MaterialTheme y LocalPhaseColors
//   - Animaciones: spring stagger en barras, tween en líneas (draw path animado)
//   - Sin dependencias externas: solo Canvas nativo de Compose
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CycleType
import com.jeremy.lumi.domain.model.EstadoGraficasUi
import com.jeremy.lumi.domain.model.PuntoBarraCiclo
import com.jeremy.lumi.domain.model.PuntoBbt
import com.jeremy.lumi.domain.model.PuntoSintomaDia
import com.jeremy.lumi.ui.theme.LocalPhaseColors

// ─────────────────────────────────────────────────────────────────────────────
//  PUNTO DE ENTRADA — sección completa de gráficas avanzadas
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Sección raíz que agrupa las tres gráficas avanzadas.
 * Se llama desde InsightsScreen con un único hiltViewModel().
 */
@Composable
fun GraficasAvanzadasSection(
    viewModel: GraficasViewModel = hiltViewModel()
) {
    val estado by viewModel.estadoUi.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {

        // Encabezado de la sección
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Rounded.AreaChart,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onPrimary,
                    modifier           = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text       = stringResource(R.string.graficas_titulo_seccion),
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text     = stringResource(R.string.graficas_subtitulo_seccion),
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        if (estado.estaCargando) {
            GraficaSkeletonLoader()
        } else {
            // A. Historial de ciclos (barras premium)
            if (estado.puntosCiclos.isNotEmpty()) {
                HistorialCiclosGrafica(
                    puntos   = estado.puntosCiclos,
                    promedio = estado.promedioLongitudCiclo
                )
            }

            // B. Tendencia de dolor y estrés (doble línea bezier)
            DolorYEstresGrafica(
                puntos     = estado.puntosSintomas,
                hayDatos   = estado.hayDatosSintomas,
                diaActual  = estado.diaCicloActual
            )

            // C. Temperatura basal (BBT line chart)
            TemperaturaBasalGrafica(
                puntos   = estado.puntosBbt,
                hayDatos = estado.hayDatosBbt
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  A. GRÁFICA DE HISTORIAL DE CICLOS — Barras redondeadas premium
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HistorialCiclosGrafica(
    puntos   : List<PuntoBarraCiclo>,
    promedio : Float
) {
    val phaseColors       = LocalPhaseColors.current
    val colorPrimary      = MaterialTheme.colorScheme.primary
    val colorSecondary    = MaterialTheme.colorScheme.secondary
    val colorTertiary     = MaterialTheme.colorScheme.tertiary
    val colorOnBackground = MaterialTheme.colorScheme.onBackground
    val colorSurface      = MaterialTheme.colorScheme.surface
    val colorOutline      = MaterialTheme.colorScheme.outline

    val maxDuracion = puntos.maxOf { it.duracionDias }.toFloat().coerceAtLeast(35f)

    // Animación staggered de cada barra con spring
    val progresos = puntos.indices.map { indice ->
        val animatable = remember { Animatable(0f) }
        LaunchedEffect(puntos) {
            kotlinx.coroutines.delay(indice * 80L)
            animatable.animateTo(
                targetValue    = 1f,
                animationSpec  = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessLow
                )
            )
        }
        animatable.value
    }

    // Tooltip al pulsar
    var indiceSeleccionado by remember { mutableStateOf<Int?>(null) }

    GlassmorphismCard {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Encabezado de la tarjeta
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = stringResource(R.string.graficas_seccion_ciclos),
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = colorOnBackground,
                        maxLines   = 1,
                        overflow   = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text     = stringResource(R.string.graficas_promedio) + ": ${promedio.toInt()} días",
                        fontSize = 12.sp,
                        color    = colorOnBackground.copy(alpha = 0.5f)
                    )
                }
                Spacer(Modifier.width(8.dp))
                // Chip de leyenda
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniChipLeyenda(color = colorPrimary,   texto = "Normal")
                    MiniChipLeyenda(color = colorSecondary, texto = "Corto")
                    MiniChipLeyenda(color = colorTertiary,  texto = "Largo")
                }
            }

            // Tooltip animado (aparece sobre la barra seleccionada)
            indiceSeleccionado?.let { idx ->
                val punto = puntos[idx]
                val colorBarra = colorDeBarra(punto.tipoCiclo, colorPrimary, colorSecondary, colorTertiary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colorBarra.copy(alpha = 0.12f))
                        .border(1.dp, colorBarra.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text       = "Ciclo ${idx + 1}  •  ${punto.fechaInicio}",
                        fontSize   = 12.sp,
                        color      = colorOnBackground.copy(alpha = 0.7f)
                    )
                    Text(
                        text       = "${punto.duracionDias} días",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color      = colorBarra
                    )
                }
            }

            // Gráfica de barras
            val alturaTotalDp = 150.dp
            val density       = LocalDensity.current

            Row(
                modifier              = Modifier.fillMaxWidth().height(alturaTotalDp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment     = Alignment.Bottom
            ) {
                puntos.forEachIndexed { indice, punto ->
                    val progreso   = progresos.getOrElse(indice) { 1f }
                    val fraccion   = (punto.duracionDias / maxDuracion) * progreso
                    val colorBarra = colorDeBarra(punto.tipoCiclo, colorPrimary, colorSecondary, colorTertiary)
                    val estaActivo = indiceSeleccionado == indice

                    Column(
                        modifier            = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Valor encima de la barra
                        Text(
                            text       = "${punto.duracionDias}",
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color      = if (estaActivo) colorBarra else colorOnBackground.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(3.dp))

                        // La barra en sí (Canvas para control total del gradiente y bordes)
                        val alturaBarraDp = alturaTotalDp * fraccion
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(alturaBarraDp.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .then(
                                    if (estaActivo) Modifier.border(
                                        1.5.dp,
                                        colorBarra,
                                        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                    ) else Modifier
                                )
                        ) {
                            // Gradiente vertical: color pleno arriba → semi-transparente abajo
                            drawRoundRect(
                                brush        = Brush.verticalGradient(
                                    listOf(
                                        colorBarra,
                                        colorBarra.copy(alpha = if (estaActivo) 0.6f else 0.35f)
                                    )
                                ),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                size         = this.size
                            )

                            // Brillo interior sutil (highlight en el borde izquierdo)
                            drawLine(
                                color       = Color.White.copy(alpha = 0.25f),
                                start       = Offset(4.dp.toPx(), 4.dp.toPx()),
                                end         = Offset(4.dp.toPx(), this.size.height - 4.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )
                        }

                        Spacer(Modifier.height(5.dp))
                        Text(
                            text       = punto.etiqueta,
                            fontSize   = 10.sp,
                            fontWeight = if (estaActivo) FontWeight.Bold else FontWeight.Normal,
                            color      = if (estaActivo) colorBarra else colorOnBackground.copy(alpha = 0.5f),
                            textAlign  = TextAlign.Center
                        )
                    }
                }
            }

            // Línea de promedio dibujada como indicador textual
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .width(30.dp)
                        .height(1.5.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Color.Transparent, colorOutline, Color.Transparent))
                        )
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text     = "─ ${stringResource(R.string.graficas_promedio)} ${promedio.toInt()} d",
                    fontSize = 10.sp,
                    color    = colorOutline
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  B. GRÁFICA DOLOR Y ESTRÉS — Doble curva bezier con área degradada
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DolorYEstresGrafica(
    puntos    : List<PuntoSintomaDia>,
    hayDatos  : Boolean,
    diaActual : Int
) {
    val phaseColors     = LocalPhaseColors.current
    val colorOnBg       = MaterialTheme.colorScheme.onBackground
    val colorSurface    = MaterialTheme.colorScheme.surface

    // Dolor usa el color de la fase menstrual (rojo/rosa) — semánticamente correcto
    val colorDolor  = phaseColors.menstrual
    // Estrés usa el color de la fase lútea (violeta) — la fase más asociada al estrés
    val colorEstres = phaseColors.luteal

    // Animación del path (se dibuja de izquierda a derecha)
    val progresoLinea = remember { Animatable(0f) }
    LaunchedEffect(puntos) {
        progresoLinea.snapTo(0f)
        progresoLinea.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
    }
    val progreso = progresoLinea.value

    GlassmorphismCard {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Encabezado
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text       = stringResource(R.string.graficas_seccion_dolor),
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = colorOnBg
                    )
                    Text(
                        text     = stringResource(R.string.graficas_dias_ciclo),
                        fontSize = 12.sp,
                        color    = colorOnBg.copy(alpha = 0.5f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniChipLeyenda(color = colorDolor,  texto = "Dolor")
                    MiniChipLeyenda(color = colorEstres, texto = "Estrés")
                }
            }

            if (!hayDatos) {
                // Estado vacío elegante
                GraficaEstadoVacio(
                    texto = stringResource(R.string.graficas_sin_datos_sintomas),
                    icono = Icons.Rounded.SentimentNeutral
                )
            } else {
                val alturaCanvas = 160.dp

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(alturaCanvas)
                ) {
                    val anchoCanvas  = size.width
                    val altoCanvas   = size.height
                    val padding      = 24.dp.toPx()
                    val anchoEfectivo = anchoCanvas - padding * 2
                    val altoEfectivo  = altoCanvas - padding * 1.5f

                    // Calcular rango de días para el eje X
                    val diasConDatos = puntos.map { it.diaCiclo }
                    val minDia = diasConDatos.minOrNull() ?: 1
                    val maxDia = diasConDatos.maxOrNull() ?: 28
                    val rangoDias = (maxDia - minDia).coerceAtLeast(1).toFloat()

                    fun xParaDia(dia: Int): Float =
                        padding + ((dia - minDia).toFloat() / rangoDias) * anchoEfectivo

                    fun yParaNivel(nivel: Int): Float =
                        altoCanvas - padding * 0.5f - (nivel.toFloat() / 10f) * altoEfectivo

                    // ── Líneas de cuadrícula horizontales ──────────────────────
                    val nivelesGrid = listOf(2, 4, 6, 8, 10)
                    nivelesGrid.forEach { nivel ->
                        val y = yParaNivel(nivel)
                        drawLine(
                            color       = colorOnBg.copy(alpha = 0.07f),
                            start       = Offset(padding, y),
                            end         = Offset(anchoCanvas - padding, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect  = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                        // Etiqueta del eje Y
                        drawContext.canvas.nativeCanvas.drawText(
                            "$nivel",
                            padding - 6.dp.toPx(),
                            y + 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color     = android.graphics.Color.argb(
                                    (colorOnBg.alpha * 0.4f * 255).toInt(),
                                    (colorOnBg.red * 255).toInt(),
                                    (colorOnBg.green * 255).toInt(),
                                    (colorOnBg.blue * 255).toInt()
                                )
                                textSize  = 9.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }

                    // ── Línea de dolor (bezier) con área degradada ──────────
                    val puntosDolor = puntos.filter { it.nivelDolor != null }
                    if (puntosDolor.size >= 2) {
                        dibujarCurvaBezierConArea(
                            drawScope   = this,
                            puntos      = puntosDolor.map {
                                Offset(xParaDia(it.diaCiclo), yParaNivel(it.nivelDolor!!))
                            },
                            color       = colorDolor,
                            progreso    = progreso,
                            altoCanvas  = altoCanvas,
                            paddingBase = padding * 0.5f
                        )
                    }

                    // ── Línea de estrés (bezier) con área degradada ──────────
                    val puntosEstres = puntos.filter { it.nivelEstres != null }
                    if (puntosEstres.size >= 2) {
                        dibujarCurvaBezierConArea(
                            drawScope   = this,
                            puntos      = puntosEstres.map {
                                Offset(xParaDia(it.diaCiclo), yParaNivel(it.nivelEstres!!))
                            },
                            color       = colorEstres,
                            progreso    = progreso,
                            altoCanvas  = altoCanvas,
                            paddingBase = padding * 0.5f
                        )
                    }

                    // ── Puntos marcadores sobre cada dato ────────────────────
                    // Solo se muestran los puntos que ya han sido "dibujados" por la animación
                    val radioMarcador   = 4.dp.toPx()
                    val numDolorVisibles = ((puntosDolor.size) * progreso).toInt()
                    val numEstresVisibles = ((puntosEstres.size) * progreso).toInt()

                    puntosDolor.take(numDolorVisibles).forEach { p ->
                        val cx = xParaDia(p.diaCiclo)
                        val cy = yParaNivel(p.nivelDolor!!)
                        drawCircle(color = colorDolor, radius = radioMarcador, center = Offset(cx, cy))
                        drawCircle(color = Color.White.copy(alpha = 0.6f), radius = radioMarcador * 0.45f, center = Offset(cx, cy))
                    }
                    puntosEstres.take(numEstresVisibles).forEach { p ->
                        val cx = xParaDia(p.diaCiclo)
                        val cy = yParaNivel(p.nivelEstres!!)
                        drawCircle(color = colorEstres, radius = radioMarcador, center = Offset(cx, cy))
                        drawCircle(color = Color.White.copy(alpha = 0.6f), radius = radioMarcador * 0.45f, center = Offset(cx, cy))
                    }

                    // ── Marcador del día actual del ciclo ────────────────────
                    if (diaActual in minDia..maxDia) {
                        val xHoy = xParaDia(diaActual)
                        drawLine(
                            color       = colorOnBg.copy(alpha = 0.25f),
                            start       = Offset(xHoy, padding),
                            end         = Offset(xHoy, altoCanvas - padding * 0.5f),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect  = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                        )
                    }
                }

                // Etiquetas del eje X (días del ciclo, mostrar 4–5 etiquetas)
                EjeXDias(
                    puntos        = puntos.map { it.diaCiclo },
                    etiquetas     = puntos.map { "D${it.diaCiclo}" },
                    colorOnBg     = colorOnBg,
                    maxEtiquetas  = 5
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  C. GRÁFICA DE TEMPERATURA BASAL (BBT)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TemperaturaBasalGrafica(
    puntos   : List<PuntoBbt>,
    hayDatos : Boolean
) {
    val colorPrimario = MaterialTheme.colorScheme.primary
    val colorOnBg     = MaterialTheme.colorScheme.onBackground
    val colorOutline  = MaterialTheme.colorScheme.outline

    val tempBase    = 36.5f // Línea de referencia de temperatura basal estándar
    val minTemp     = if (hayDatos) (puntos.minOf { it.temperaturaC } - 0.3f).coerceAtMost(36.0f) else 36.0f
    val maxTemp     = if (hayDatos) (puntos.maxOf { it.temperaturaC } + 0.3f).coerceAtLeast(37.2f) else 37.5f

    // Animación del trazo
    val progresoLinea = remember { Animatable(0f) }
    LaunchedEffect(puntos) {
        progresoLinea.snapTo(0f)
        progresoLinea.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
    }
    val progreso = progresoLinea.value

    GlassmorphismCard {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Encabezado
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text       = stringResource(R.string.graficas_seccion_bbt),
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = colorOnBg
                    )
                    Text(
                        text     = "°C por día del ciclo",
                        fontSize = 12.sp,
                        color    = colorOnBg.copy(alpha = 0.5f)
                    )
                }
                // Badge de ovulación detectada
                if (hayDatos && puntos.any { it.esOvulacion }) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(colorPrimario.copy(alpha = 0.15f))
                            .border(1.dp, colorPrimario.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text       = "✦ " + stringResource(R.string.graficas_ovulacion_detectada),
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = colorPrimario
                        )
                    }
                }
            }

            if (!hayDatos) {
                GraficaEstadoVacio(
                    texto = stringResource(R.string.graficas_sin_datos_bbt),
                    icono = Icons.Rounded.Thermostat
                )
            } else {
                val alturaCanvas = 180.dp

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(alturaCanvas)
                ) {
                    val anchoCanvas   = size.width
                    val altoCanvas    = size.height
                    val paddingLeft   = 42.dp.toPx()
                    val paddingRight  = 16.dp.toPx()
                    val paddingTop    = 16.dp.toPx()
                    val paddingBottom = 24.dp.toPx()

                    val anchoEfectivo = anchoCanvas - paddingLeft - paddingRight
                    val altoEfectivo  = altoCanvas - paddingTop - paddingBottom
                    val rangoTemp     = (maxTemp - minTemp).coerceAtLeast(0.5f)

                    fun xParaIndice(i: Int): Float =
                        paddingLeft + (i.toFloat() / (puntos.size - 1).coerceAtLeast(1)) * anchoEfectivo

                    fun yParaTemp(temp: Float): Float =
                        altoCanvas - paddingBottom - ((temp - minTemp) / rangoTemp) * altoEfectivo

                    // ── Etiquetas del eje Y ──────────────────────────────────
                    val nivelesTemp = listOf(minTemp, tempBase, maxTemp)
                    nivelesTemp.forEach { temp ->
                        val y = yParaTemp(temp)
                        drawContext.canvas.nativeCanvas.drawText(
                            "%.1f".format(temp),
                            paddingLeft - 6.dp.toPx(),
                            y + 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color     = android.graphics.Color.argb(
                                    (colorOnBg.alpha * 0.45f * 255).toInt(),
                                    (colorOnBg.red * 255).toInt(),
                                    (colorOnBg.green * 255).toInt(),
                                    (colorOnBg.blue * 255).toInt()
                                )
                                textSize  = 8.5.sp.toPx()
                                textAlign = android.graphics.Paint.Align.RIGHT
                            }
                        )
                    }

                    // ── Línea base de referencia (36.5°C) ───────────────────
                    val yBase = yParaTemp(tempBase)
                    drawLine(
                        color       = colorOutline.copy(alpha = 0.4f),
                        start       = Offset(paddingLeft, yBase),
                        end         = Offset(anchoCanvas - paddingRight, yBase),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect  = PathEffect.dashPathEffect(floatArrayOf(8f, 5f))
                    )

                    // ── Zona de relleno (área bajo la curva BBT) ─────────────
                    if (puntos.size >= 2) {
                        val pathArea = Path()
                        val puntosSistema = puntos.mapIndexed { i, p ->
                            Offset(xParaIndice(i), yParaTemp(p.temperaturaC))
                        }

                        // Número de puntos a mostrar según el progreso de animación
                        val numPuntosVisibles = ((puntos.size - 1) * progreso).toInt() + 1

                        pathArea.moveTo(puntosSistema[0].x, altoCanvas - paddingBottom)
                        pathArea.lineTo(puntosSistema[0].x, puntosSistema[0].y)

                        for (i in 1 until numPuntosVisibles.coerceAtMost(puntosSistema.size)) {
                            val p0 = puntosSistema[i - 1]
                            val p1 = puntosSistema[i]
                            val controlX = (p0.x + p1.x) / 2f
                            pathArea.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                        }

                        pathArea.lineTo(puntosSistema[numPuntosVisibles.coerceAtMost(puntosSistema.size) - 1].x, altoCanvas - paddingBottom)
                        pathArea.close()

                        drawPath(
                            path  = pathArea,
                            brush = Brush.verticalGradient(
                                listOf(
                                    colorPrimario.copy(alpha = 0.22f),
                                    colorPrimario.copy(alpha = 0.04f)
                                ),
                                startY = paddingTop,
                                endY   = altoCanvas - paddingBottom
                            )
                        )

                        // ── Línea bezier de temperatura ──────────────────────
                        val pathLinea = Path()
                        pathLinea.moveTo(puntosSistema[0].x, puntosSistema[0].y)
                        for (i in 1 until numPuntosVisibles.coerceAtMost(puntosSistema.size)) {
                            val p0 = puntosSistema[i - 1]
                            val p1 = puntosSistema[i]
                            val controlX = (p0.x + p1.x) / 2f
                            pathLinea.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                        }
                        drawPath(
                            path   = pathLinea,
                            color  = colorPrimario,
                            style  = Stroke(
                                width     = 2.5.dp.toPx(),
                                cap       = StrokeCap.Round,
                                join      = StrokeJoin.Round
                            )
                        )

                        // ── Puntos de temperatura ────────────────────────────
                        puntosSistema.take(numPuntosVisibles).forEachIndexed { i, offset ->
                            val esOvulacion = puntos[i].esOvulacion
                            val radio = if (esOvulacion) 7.dp.toPx() else 4.dp.toPx()

                            if (esOvulacion) {
                                // Anillo exterior brillante para el spike de ovulación
                                drawCircle(
                                    color  = colorPrimario.copy(alpha = 0.3f),
                                    radius = radio + 5.dp.toPx(),
                                    center = offset
                                )
                            }

                            drawCircle(color = colorPrimario, radius = radio, center = offset)
                            drawCircle(
                                color  = Color.White.copy(alpha = 0.7f),
                                radius = radio * 0.45f,
                                center = offset
                            )
                        }

                        // ── Etiqueta del día de ovulación ────────────────────
                        val idxOvulacion = puntos.indexOfFirst { it.esOvulacion }
                        if (idxOvulacion >= 0 && idxOvulacion < numPuntosVisibles) {
                            val offsetOvulacion = puntosSistema[idxOvulacion]
                            drawContext.canvas.nativeCanvas.drawText(
                                "✦ D${puntos[idxOvulacion].diaCiclo}",
                                offsetOvulacion.x,
                                offsetOvulacion.y - 12.dp.toPx(),
                                android.graphics.Paint().apply {
                                    color     = android.graphics.Color.argb(
                                        (colorPrimario.alpha * 255).toInt(),
                                        (colorPrimario.red * 255).toInt(),
                                        (colorPrimario.green * 255).toInt(),
                                        (colorPrimario.blue * 255).toInt()
                                    )
                                    textSize  = 9.sp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isFakeBoldText = true
                                }
                            )
                        }
                    }

                    // ── Etiquetas del eje X (fechas) ────────────────────────
                    val paso = (puntos.size / 4).coerceAtLeast(1)
                    puntos.filterIndexed { i, _ -> i % paso == 0 || i == puntos.size - 1 }.forEach { p ->
                        val i      = puntos.indexOf(p)
                        val x      = xParaIndice(i)
                        drawContext.canvas.nativeCanvas.drawText(
                            p.fecha,
                            x,
                            altoCanvas - 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color     = android.graphics.Color.argb(
                                    (colorOnBg.alpha * 0.5f * 255).toInt(),
                                    (colorOnBg.red * 255).toInt(),
                                    (colorOnBg.green * 255).toInt(),
                                    (colorOnBg.blue * 255).toInt()
                                )
                                textSize  = 8.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  FUNCIONES PRIVADAS DE DIBUJO (DrawScope helpers)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Dibuja una curva bezier suave con relleno de área degradada debajo.
 * El parámetro [progreso] (0f → 1f) controla qué fracción del trazo se muestra,
 * creando la animación de "dibujo de izquierda a derecha".
 */
private fun dibujarCurvaBezierConArea(
    drawScope   : DrawScope,
    puntos      : List<Offset>,
    color       : Color,
    progreso    : Float,
    altoCanvas  : Float,
    paddingBase : Float
) {
    if (puntos.size < 2) return

    with(drawScope) {
        val numVisibles = ((puntos.size - 1) * progreso).toInt() + 1

        // Área de relleno
        val pathArea = Path()
        pathArea.moveTo(puntos[0].x, altoCanvas - paddingBase)
        pathArea.lineTo(puntos[0].x, puntos[0].y)

        for (i in 1 until numVisibles.coerceAtMost(puntos.size)) {
            val p0 = puntos[i - 1]
            val p1 = puntos[i]
            val controlX = (p0.x + p1.x) / 2f
            pathArea.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
        }

        val ultimoPunto = puntos[numVisibles.coerceAtMost(puntos.size) - 1]
        pathArea.lineTo(ultimoPunto.x, altoCanvas - paddingBase)
        pathArea.close()

        drawPath(
            path  = pathArea,
            brush = Brush.verticalGradient(
                listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0.02f)),
                startY = 0f,
                endY   = altoCanvas
            )
        )

        // Línea bezier
        val pathLinea = Path()
        pathLinea.moveTo(puntos[0].x, puntos[0].y)
        for (i in 1 until numVisibles.coerceAtMost(puntos.size)) {
            val p0 = puntos[i - 1]
            val p1 = puntos[i]
            val controlX = (p0.x + p1.x) / 2f
            pathLinea.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
        }

        drawPath(
            path  = pathLinea,
            color = color,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap   = StrokeCap.Round,
                join  = StrokeJoin.Round
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  COMPONENTES DE SOPORTE
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Tarjeta con efecto glassmorphism:
 * - Fondo de superficie semi-transparente
 * - Borde translúcido con el color primario
 * - Sin elevación (el borde reemplaza la sombra)
 */
@Composable
private fun GlassmorphismCard(
    modifier  : Modifier = Modifier,
    content   : @Composable () -> Unit
) {
    val colorPrimario = MaterialTheme.colorScheme.primary
    val colorSurface  = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colorSurface.copy(alpha = 0.92f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        colorPrimario.copy(alpha = 0.35f),
                        colorPrimario.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        content()
    }
}

/** Chip compacto de leyenda para las gráficas */
@Composable
private fun MiniChipLeyenda(
    color  : Color,
    texto  : String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier         = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text     = texto,
            fontSize = 9.sp,
            color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            maxLines = 1,
            softWrap = false
        )
    }
}

/** Etiquetas compactas del eje X para la gráfica de síntomas */
@Composable
private fun EjeXDias(
    puntos       : List<Int>,
    etiquetas    : List<String>,
    colorOnBg    : Color,
    maxEtiquetas : Int = 5
) {
    if (puntos.isEmpty()) return
    val paso = (puntos.size / maxEtiquetas).coerceAtLeast(1)
    val etiquetasFiltradas = etiquetas.filterIndexed { i, _ -> i % paso == 0 || i == etiquetas.size - 1 }
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        etiquetasFiltradas.forEach { etiqueta ->
            Text(
                text      = etiqueta,
                fontSize  = 9.sp,
                color     = colorOnBg.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Estado vacío de una gráfica individual */
@Composable
private fun GraficaEstadoVacio(
    texto : String,
    icono : androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector        = icono,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
            modifier           = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text       = texto,
            fontSize   = 12.sp,
            textAlign  = TextAlign.Center,
            color      = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            lineHeight = 17.sp,
            modifier   = Modifier.padding(horizontal = 24.dp)
        )
    }
}

/** Skeleton loader mientras los datos se están cargando */
@Composable
private fun GraficaSkeletonLoader() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue   = 0.06f,
        targetValue    = 0.14f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    val colorOnBg = MaterialTheme.colorScheme.onBackground

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        repeat(3) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colorOnBg.copy(alpha = alpha))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HELPER — color de barra según tipo de ciclo
// ─────────────────────────────────────────────────────────────────────────────

private fun colorDeBarra(
    tipo      : CycleType,
    normal    : Color,
    corto     : Color,
    largo     : Color
): Color = when (tipo) {
    CycleType.NORMAL -> normal
    CycleType.SHORT  -> corto
    CycleType.LONG   -> largo
}
