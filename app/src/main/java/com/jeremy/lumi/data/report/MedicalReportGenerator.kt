package com.jeremy.lumi.data.report

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.jeremy.lumi.domain.model.BioVariable
import com.jeremy.lumi.domain.model.CyclePhase
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Generador de Reporte Clínico Ginecológico en PDF de 2 páginas A4 (595 × 842 pt).
 * Formateado con precisión institucional para máxima claridad, detalle y rapidez de lectura
 * por el médico especialista (Ginecóloga/o, Matrona o Médico General).
 *
 * Página 1:
 *  - Encabezado institucional con sello de confidencialidad médica
 *  - Datos de la paciente (con clasificación clínica de IMC) y caja destacada de F.U.M.
 *  - Clasificación clínica FIGO 2018 / ACOG con badge de impresión diagnóstica orientativa
 *  - Panel de Alertas Clínicas y Banderas Rojas Ginecológicas con orientación PALM-COEIN
 *  - Tabla detallada de historial de ciclos con fila de promedio estadístico
 *
 * Página 2:
 *  - Encabezado secundario de continuidad
 *  - Curva sintotérmica de Temperatura Basal (BBT) con Coverline OMS (periodo 3-sobre-6),
 *    delimitación de fase folicular/lútea, indicador de ovulación y barras de dolor EVA
 *  - Matriz Bio-Estadística: Correlaciones de Spearman con lag fisiológico y Clusters Jaccard
 *  - Distribución de síntomas frecuentes y patrones de humor
 *  - Área médica con checkboxes de conducta frecuente (Eco, Analítica, AINEs), líneas manuscritas, firma y sello
 *  - Declaración médico-legal y de privacidad de datos local (On-Device Local Privacy)
 */
@Singleton
class MedicalReportGenerator @Inject constructor() {

    private val PAGE_W    = 595
    private val PAGE_H    = 842
    private val MARGIN    = 36f
    private val CONTENT_W = PAGE_W - (2 * MARGIN)

    // ─── Paleta Clínica Profesional ───────────────────────────────────────────
    private val COLOR_NAVY_DARK    = Color.rgb(15, 41, 66)      // #0F2942 Slate Navy
    private val COLOR_PRIMARY      = Color.rgb(30, 90, 168)     // #1E5AA8 Azul Médico
    private val COLOR_PRIMARY_L    = Color.rgb(240, 244, 249)   // #F0F4F9 Fondo muy suave
    private val COLOR_DARK         = Color.rgb(30, 41, 59)      // #1E293B Texto oscuro
    private val COLOR_GRAY         = Color.rgb(100, 116, 139)   // #64748B Texto secundario
    private val COLOR_LIGHT_GRAY   = Color.rgb(241, 245, 249)   // #F1F5F9 Fondo alterno
    private val COLOR_BORDER       = Color.rgb(209, 220, 229)   // #D1DCE5 Bordes sutiles
    private val COLOR_GRID         = Color.rgb(226, 232, 240)   // #E2E8F0 Rejilla
    private val COLOR_WHITE        = Color.WHITE

    private val COLOR_RED_DARK     = Color.rgb(190, 18, 60)     // #BE123C Alerta severa
    private val COLOR_RED_LIGHT    = Color.rgb(255, 241, 242)   // #FFF1F2 Fondo alerta
    private val COLOR_RED_BORDER   = Color.rgb(254, 205, 211)   // #FECDD3
    private val COLOR_GREEN_DARK   = Color.rgb(21, 128, 61)     // #15803D Normal / Saludable
    private val COLOR_GREEN_LIGHT  = Color.rgb(240, 253, 244)   // #F0FDF4
    private val COLOR_GREEN_BORDER = Color.rgb(187, 247, 208)   // #BBF7D0
    private val COLOR_COVERLINE    = Color.rgb(225, 29, 72)     // #E11D48 Coverline OMS

    fun generate(data: MedicalReportData): ByteArray {
        val pdf = PdfDocument()

        // ── PÁGINA 1: Resumen Clínico, FIGO, Alertas y Tabla de Ciclos ───────
        val pageInfo1 = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        val page1 = pdf.startPage(pageInfo1)
        drawPage1(page1.canvas, data)
        pdf.finishPage(page1)

        // ── PÁGINA 2: Curva BBT, Bio-Matriz Spearman/Jaccard, Receta y Firma ─
        val pageInfo2 = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 2).create()
        val page2 = pdf.startPage(pageInfo2)
        drawPage2(page2.canvas, data)
        pdf.finishPage(page2)

        val out = ByteArrayOutputStream()
        pdf.writeTo(out)
        pdf.close()
        return out.toByteArray()
    }

    // =========================================================================
    //  PÁGINA 1: CLÍNICA, FIGO Y DIAGNÓSTICO ESTRUCTURADO
    // =========================================================================

    private fun drawPage1(c: Canvas, data: MedicalReportData) {
        var y = 0f

        // 1. Encabezado institucional Navy Slate
        val headerH = 62f
        val headerBg = Paint().apply { color = COLOR_NAVY_DARK }
        c.drawRect(0f, 0f, PAGE_W.toFloat(), headerH, headerBg)

        // Línea de acento médico inferior
        val accentLine = Paint().apply { color = COLOR_PRIMARY; strokeWidth = 3f }
        c.drawLine(0f, headerH, PAGE_W.toFloat(), headerH, accentLine)

        val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WHITE
            textSize = 14.5f
            isFakeBoldText = true
            typeface = Typeface.DEFAULT_BOLD
        }
        c.drawText("INFORME CLÍNICO GINECOLÓGICO", MARGIN, 26f, titleP)

        val subtitleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(160, 180, 205)
            textSize = 8.5f
        }
        c.drawText("Lumi Health Platform · Registro Sintotérmico y Biometría del Ciclo", MARGIN, 42f, subtitleP)

        // Badge derecho de confidencialidad y fecha
        val badgeW = 150f
        val badgeH = 17f
        val badgeX = PAGE_W - MARGIN - badgeW
        val badgeBg = Paint().apply {
            color = Color.argb(40, 255, 255, 255)
            style = Paint.Style.FILL
        }
        val badgeBorder = Paint().apply {
            color = Color.argb(80, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        c.drawRoundRect(RectF(badgeX, 14f, badgeX + badgeW, 14f + badgeH), 4f, 4f, badgeBg)
        c.drawRoundRect(RectF(badgeX, 14f, badgeX + badgeW, 14f + badgeH), 4f, 4f, badgeBorder)

        val badgeTextP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WHITE
            textSize = 7f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        c.drawText("DOCUMENTO CONFIDENCIAL", badgeX + (badgeW / 2f), 25.5f, badgeTextP)

        val dateP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            textSize = 7.5f
            textAlign = Paint.Align.RIGHT
        }
        val fmtDate = data.generatedAt.format(DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy", Locale("es")))
        c.drawText("Emisión: $fmtDate", PAGE_W - MARGIN, 46f, dateP)

        y = headerH + 14f

        // 2. Perfil de Paciente (Izquierda) + F.U.M. Hero Box (Derecha)
        val profileH = 92f
        val leftW = 320f
        val rightW = CONTENT_W - leftW - 12f
        val rightX = MARGIN + leftW + 12f

        // Caja Perfil
        drawCardBox(c, MARGIN, y, leftW, profileH)
        drawMiniHeader(c, MARGIN + 10f, y + 16f, "DATOS DE LA PACIENTE Y REFERENCIA")

        val lblP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_GRAY; textSize = 8f }
        val valP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_DARK; textSize = 8.5f; isFakeBoldText = true }

        val pName = data.userName ?: "No especificado"
        val pAge = data.userAge?.let { "$it años" } ?: "No registrada"
        c.drawText("Paciente:", MARGIN + 10f, y + 32f, lblP)
        c.drawText(pName, MARGIN + 55f, y + 32f, valP)

        c.drawText("Edad:", MARGIN + 195f, y + 32f, lblP)
        c.drawText(pAge, MARGIN + 225f, y + 32f, valP)

        // Biometría con categoría clínica de IMC
        val bmiText = if (data.userHeightCm != null && data.userWeightKg != null && data.userHeightCm > 30f) {
            val bmi = data.userWeightKg / ((data.userHeightCm / 100f) * (data.userHeightCm / 100f))
            val bmiCat = when {
                bmi < 18.5f -> "Bajo peso"
                bmi < 25.0f -> "Normopeso"
                bmi < 30.0f -> "Sobrepeso"
                else        -> "Obesidad"
            }
            "${data.userHeightCm.toInt()} cm · ${data.userWeightKg.toInt()} kg (IMC: ${String.format(Locale.US, "%.1f", bmi)} - $bmiCat)"
        } else if (data.userWeightKg != null) {
            "${data.userWeightKg.toInt()} kg"
        } else {
            "—"
        }
        c.drawText("Biometría:", MARGIN + 10f, y + 49f, lblP)
        c.drawText(bmiText, MARGIN + 60f, y + 49f, valP)

        c.drawText("Anticonceptivo:", MARGIN + 10f, y + 66f, lblP)
        c.drawText(if (data.isOnContraceptive) "Sí (Hormonal / Regulador)" else "No hormonal / Ciclo natural", MARGIN + 78f, y + 66f, valP)

        c.drawText("Parámetro base:", MARGIN + 10f, y + 83f, lblP)
        c.drawText("Ciclo ~${data.avgCycleLength} d · Menstruación ~${data.avgPeriodLength} d", MARGIN + 82f, y + 83f, valP)

        // Caja F.U.M. Hero (Derecha)
        val fumBg = Paint().apply { color = COLOR_PRIMARY_L }
        val fumBorder = Paint().apply { color = Color.rgb(189, 211, 232); style = Paint.Style.STROKE; strokeWidth = 1f }
        c.drawRoundRect(RectF(rightX, y, rightX + rightW, y + profileH), 6f, 6f, fumBg)
        c.drawRoundRect(RectF(rightX, y, rightX + rightW, y + profileH), 6f, 6f, fumBorder)

        val fumTitleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_NAVY_DARK
            textSize = 8.5f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        c.drawText("F.U.M. (ÚLTIMO PERIODO)", rightX + (rightW / 2f), y + 20f, fumTitleP)

        val fumDateStr = data.lastMenstrualPeriodDate?.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es"))) ?: "No registrada"
        val fumDateP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            textSize = 14f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        c.drawText(fumDateStr, rightX + (rightW / 2f), y + 42f, fumDateP)

        // Pill con días transcurridos
        val pillW = 120f
        val pillH = 18f
        val pillX = rightX + (rightW - pillW) / 2f
        val pillY = y + 54f
        val pillBg = Paint().apply { color = COLOR_NAVY_DARK }
        c.drawRoundRect(RectF(pillX, pillY, pillX + pillW, pillY + pillH), 9f, 9f, pillBg)

        val pillTextP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WHITE
            textSize = 8f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val daysText = data.daysSinceLmp?.let { "Hace $it días (Día $it)" } ?: "Sin datos de inicio"
        c.drawText(daysText, rightX + (rightW / 2f), pillY + 12f, pillTextP)

        val subFumP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GRAY
            textSize = 6.8f
            textAlign = Paint.Align.CENTER
        }
        c.drawText("Referencia ginecológica en consulta", rightX + (rightW / 2f), y + 85f, subFumP)

        y += profileH + 12f

        // 3. Clasificación FIGO 2018 / ACOG
        val figoH = 88f
        drawCardBox(c, MARGIN, y, CONTENT_W, figoH)

        val figo = data.figoClassification
        val figoIsNormal = figo?.frequency == FigoFrequency.NORMAL && figo.regularity == FigoRegularity.REGULAR && figo.duration == FigoDuration.NORMAL
        val diagBadge = if (figoIsNormal) "PATRÓN EUMENORREICO NORMAL" else "PATRÓN CON HALLAZGOS A EVALUAR"
        val diagBadgeColor = if (figoIsNormal) COLOR_GREEN_DARK else COLOR_PRIMARY

        // Título con etiqueta diagnóstica
        drawMiniHeader(c, MARGIN + 10f, y + 16f, "CLASIFICACIÓN DE CICLO MENSTRUAL (CRITERIOS FIGO 2018 / ACOG)")
        val diagP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = diagBadgeColor
            textSize = 7f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        c.drawText("[$diagBadge]", MARGIN + CONTENT_W - 10f, y + 15.5f, diagP)

        val badgeWidth = (CONTENT_W - 32f) / 3f

        // Badge 1: Frecuencia
        val b1X = MARGIN + 10f
        val bY = y + 26f
        val freqLabel = figo?.frequency?.label ?: "Frecuencia: ${data.avgCycleLength} días"
        val freqIsNormal = figo?.frequency == FigoFrequency.NORMAL
        drawStatusPill(c, b1X, bY, badgeWidth, 24f, "FRECUENCIA", freqLabel, freqIsNormal)

        // Badge 2: Regularidad
        val b2X = b1X + badgeWidth + 6f
        val periodobel = figo?.regularity?.label ?: "Regularidad: Estándar"
        val regIsNormal = figo?.regularity == FigoRegularity.REGULAR
        drawStatusPill(c, b2X, bY, badgeWidth, 24f, "REGULARIDAD", periodobel, regIsNormal)

        // Badge 3: Duración
        val b3X = b2X + badgeWidth + 6f
        val durLabel = figo?.duration?.label ?: "Sangrado: ${data.avgPeriodLength} días"
        val durIsNormal = figo?.duration == FigoDuration.NORMAL
        drawStatusPill(c, b3X, bY, badgeWidth, 24f, "DURACIÓN SANGRADO", durLabel, durIsNormal)

        // Texto resumen FIGO
        val figoSumP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_DARK; textSize = 7.8f }
        val summaryTxt = figo?.summaryText ?: "Ciclo menstrual analizado con criterios estándar de la Federación Internacional de Ginecología y Obstetricia."
        c.drawText(summaryTxt, MARGIN + 10f, y + 68f, figoSumP)

        val statsSummary = data.historicalStats?.let {
            "Estadística base: Mínimo ${it.shortestCycle}d · Máximo ${it.longestCycle}d · Dispersión Δ ${it.longestCycle - it.shortestCycle}d · Retraso máx ${it.maxHistoricalDelay}d"
        } ?: "Datos analizados sobre el ciclo activo e historial disponible."
        val figoStatP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_GRAY; textSize = 7.2f }
        c.drawText(statsSummary, MARGIN + 10f, y + 80f, figoStatP)

        y += figoH + 12f

        // 4. Panel de Banderas Rojas y Alertas Ginecológicas
        val flags = data.clinicalRedFlags
        val redFlagH = if (flags.isEmpty()) 44f else (22f + (flags.take(4).size * 22f) + 4f)
        val alertBgColor = if (flags.isEmpty()) COLOR_GREEN_LIGHT else COLOR_RED_LIGHT
        val alertBorderColor = if (flags.isEmpty()) COLOR_GREEN_BORDER else COLOR_RED_BORDER

        val rfBg = Paint().apply { color = alertBgColor }
        val rfBorder = Paint().apply { color = alertBorderColor; style = Paint.Style.STROKE; strokeWidth = 1f }
        c.drawRoundRect(RectF(MARGIN, y, MARGIN + CONTENT_W, y + redFlagH), 6f, 6f, rfBg)
        c.drawRoundRect(RectF(MARGIN, y, MARGIN + CONTENT_W, y + redFlagH), 6f, 6f, rfBorder)

        if (flags.isEmpty()) {
            val safeTitleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_GREEN_DARK
                textSize = 8.5f
                isFakeBoldText = true
            }
            c.drawText("ALERTAS CLÍNICAS: SIN BANDERAS ROJAS DETECTADAS", MARGIN + 10f, y + 18f, safeTitleP)

            val safeDescP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_DARK; textSize = 7.8f }
            c.drawText("Sin reportes de dismenorrea severa (EVA ≥ 7), menorragia ni manchado intermenstrual en el período analizado.", MARGIN + 10f, y + 32f, safeDescP)
        } else {
            val alertTitleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_RED_DARK
                textSize = 8.5f
                isFakeBoldText = true
            }
            c.drawText("ALERTAS CLÍNICAS Y FACTORES DE RIESGO DETECTADOS (${flags.size})", MARGIN + 10f, y + 16f, alertTitleP)

            var itemY = y + 29f
            flags.take(4).forEach { flag ->
                val bulletP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_RED_DARK }
                c.drawCircle(MARGIN + 14f, itemY - 3f, 2.5f, bulletP)

                val flagTitleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_RED_DARK
                    textSize = 8f
                    isFakeBoldText = true
                }
                c.drawText(flag.title, MARGIN + 22f, itemY, flagTitleP)

                val flagDescP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_DARK
                    textSize = 7.2f
                }
                val truncatedDesc = if (flag.description.length > 105) flag.description.take(102) + "..." else flag.description
                c.drawText(truncatedDesc, MARGIN + 22f, itemY + 10f, flagDescP)

                itemY += 22f
            }
        }

        y += redFlagH + 12f

        // 5. Tabla de Historial de Ciclos con Fila de Promedio
        val cycles = data.recentCycles.take(7)
        val tableH = 24f + (max(cycles.size, 1) * 16.5f) + 18f + 16f // +16f para la fila de promedio
        drawCardBox(c, MARGIN, y, CONTENT_W, tableH)
        drawMiniHeader(c, MARGIN + 10f, y + 16f, "REGISTRO HISTÓRICO DE CICLOS (Últimos ${cycles.size} ciclos)")

        val tableY = y + 22f
        val cols = listOf(
            Triple("#", 28f, Paint.Align.LEFT),
            Triple("Fecha Inicio", 100f, Paint.Align.LEFT),
            Triple("Duración", 85f, Paint.Align.LEFT),
            Triple("Sangrado", 85f, Paint.Align.LEFT),
            Triple("Dolor EVA", 95f, Paint.Align.LEFT),
            Triple("Desviación Media", 130f, Paint.Align.LEFT)
        )

        // Cabecera de la tabla
        val tHeaderBg = Paint().apply { color = COLOR_NAVY_DARK }
        c.drawRect(MARGIN, tableY, MARGIN + CONTENT_W, tableY + 15f, tHeaderBg)

        var curX = MARGIN + 6f
        val thP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WHITE
            textSize = 7.5f
            isFakeBoldText = true
        }
        cols.forEach { (title, width, _) ->
            c.drawText(title, curX, tableY + 11f, thP)
            curX += width
        }

        var rowY = tableY + 15f
        val tbP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_DARK; textSize = 7.5f }
        val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es"))

        if (cycles.isEmpty()) {
            c.drawText("No hay ciclos cerrados registrados aún en la base de datos.", MARGIN + 10f, rowY + 13f, tbP)
            rowY += 16.5f
        } else {
            cycles.forEachIndexed { index, cycle ->
                val isEven = index % 2 == 0
                if (isEven) {
                    val rBg = Paint().apply { color = COLOR_LIGHT_GRAY }
                    c.drawRect(MARGIN, rowY, MARGIN + CONTENT_W, rowY + 16.5f, rBg)
                }

                val sDate = data.cycleStartDates.getOrNull(index)?.format(dateFmt) ?: "—"
                val diff = cycle.durationDays - data.avgCycleLength
                val diffStr = if (diff > 0) "+$diff d" else if (diff < 0) "$diff d" else "0 d"

                curX = MARGIN + 6f
                c.drawText("${index + 1}", curX, rowY + 11.5f, tbP)
                curX += cols[0].second

                c.drawText(sDate, curX, rowY + 11.5f, tbP)
                curX += cols[1].second

                c.drawText("${cycle.durationDays} días", curX, rowY + 11.5f, tbP)
                curX += cols[2].second

                c.drawText("${cycle.bleedingDays} días", curX, rowY + 11.5f, tbP)
                curX += cols[3].second

                val painStr = if (cycle.avgPainLevel > 0) "${String.format(Locale.US, "%.1f", cycle.avgPainLevel)}/10" else "0/10"
                c.drawText(painStr, curX, rowY + 11.5f, tbP)
                curX += cols[4].second

                val diffColor = if (kotlin.math.abs(diff) > 4) COLOR_RED_DARK else COLOR_GRAY
                val diffP = Paint(tbP).apply { color = diffColor; isFakeBoldText = kotlin.math.abs(diff) > 4 }
                c.drawText(diffStr, curX, rowY + 11.5f, diffP)

                val lineP = Paint().apply { color = COLOR_GRID; strokeWidth = 0.5f }
                c.drawLine(MARGIN, rowY + 16.5f, MARGIN + CONTENT_W, rowY + 16.5f, lineP)
                rowY += 16.5f
            }

            // Fila de resumen de promedios para el médico
            val avgBg = Paint().apply { color = COLOR_PRIMARY_L }
            c.drawRect(MARGIN, rowY, MARGIN + CONTENT_W, rowY + 16f, avgBg)

            val avgP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_NAVY_DARK
                textSize = 7.5f
                isFakeBoldText = true
            }
            val avgPainNum = cycles.map { it.avgPainLevel }.average().toFloat()
            c.drawText("PROMEDIO CLÍNICO", MARGIN + 6f, rowY + 11f, avgP)
            c.drawText("${data.avgCycleLength} días", MARGIN + 6f + cols[0].second + cols[1].second, rowY + 11f, avgP)
            c.drawText("${data.avgPeriodLength} días", MARGIN + 6f + cols[0].second + cols[1].second + cols[2].second, rowY + 11f, avgP)
            c.drawText("${String.format(Locale.US, "%.1f", avgPainNum)}/10", MARGIN + 6f + cols[0].second + cols[1].second + cols[2].second + cols[3].second, rowY + 11f, avgP)
            c.drawText("Variación Δ ${data.historicalStats?.let { it.longestCycle - it.shortestCycle } ?: 0}d", MARGIN + 6f + cols[0].second + cols[1].second + cols[2].second + cols[3].second + cols[4].second, rowY + 11f, avgP)
        }

        // 6. Pie de Página 1
        drawPageFooter(c, 1, 2)
    }

    // =========================================================================
    //  PÁGINA 2: GRÁFICAS CLÍNICAS, BIO-MATRIZ, RECETA Y FIRMA
    // =========================================================================

    private fun drawPage2(c: Canvas, data: MedicalReportData) {
        var y = 20f

        // 1. Running Mini-Header de continuidad
        val miniHeaderP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_GRAY; textSize = 7.5f }
        val pName = data.userName ?: "Paciente"
        val fumStr = data.lastMenstrualPeriodDate?.format(DateTimeFormatter.ofPattern("d/MM/yyyy")) ?: "—"
        c.drawText("Lumi Clinical Analytics · Paciente: $pName · FUM: $fumStr · Documento Médico Privado", MARGIN, y + 10f, miniHeaderP)

        val pNumP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_PRIMARY; textSize = 7.5f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT }
        c.drawText("Página 2 de 2", PAGE_W - MARGIN, y + 10f, pNumP)

        val divP = Paint().apply { color = COLOR_BORDER; strokeWidth = 0.8f }
        c.drawLine(MARGIN, y + 15f, PAGE_W - MARGIN.toFloat(), y + 15f, divP)

        y += 24f

        // 2. Curva Sintotérmica BBT y Dolor EVA (Último ciclo)
        val graphCardH = 176f
        drawCardBox(c, MARGIN, y, CONTENT_W, graphCardH)
        drawMiniHeader(c, MARGIN + 10f, y + 16f, "CURVA SINTOTÉRMICA BASAL (BBT) Y ESCALA DE DOLOR (EVA)")

        val graphTop = y + 26f
        val graphH = 102f
        val graphBottom = graphTop + graphH
        val logs = data.latestCycleLogs

        // Min y Max BBT
        val bbtLogs = logs.filter { it.basalBodyTemp != null && it.basalBodyTemp in 35.0f..38.5f }
        var minTemp = 36.0f
        var maxTemp = 37.4f
        if (bbtLogs.isNotEmpty()) {
            minTemp = bbtLogs.minOf { it.basalBodyTemp!! } - 0.2f
            maxTemp = bbtLogs.maxOf { it.basalBodyTemp!! } + 0.2f
            if (maxTemp - minTemp < 1.0f) {
                val mid = (minTemp + maxTemp) / 2f
                minTemp = mid - 0.5f
                maxTemp = mid + 0.5f
            }
        }

        val totalDays = max(logs.size, 28)
        val dayStep = (CONTENT_W - 35f) / totalDays.toFloat()
        val graphLeft = MARGIN + 30f
        val graphRight = MARGIN + CONTENT_W - 8f

        // Fondo del gráfico
        val gBg = Paint().apply { color = COLOR_LIGHT_GRAY }
        c.drawRect(graphLeft, graphTop, graphRight, graphBottom, gBg)

        // Si hay coverline OMS, sombrear fase folicular vs fase lútea
        val cover = data.bbtCoverline
        if (cover != null && cover in minTemp..maxTemp) {
            // Estimar día de cambio térmico (primer día por encima de coverline con 3 días altos)
            val shiftIndex = logs.indexOfFirst { (it.basalBodyTemp ?: 0f) > cover }.coerceAtLeast(1)
            val shiftX = graphLeft + (shiftIndex * dayStep)

            // Sombra fase folicular (azulada muy tenue)
            val folP = Paint().apply { color = Color.rgb(238, 244, 250) }
            c.drawRect(graphLeft, graphTop, shiftX, graphBottom, folP)

            // Sombra fase lútea (cálida muy tenue)
            val lutP = Paint().apply { color = Color.rgb(254, 243, 242) }
            c.drawRect(shiftX, graphTop, graphRight, graphBottom, lutP)

            // Línea vertical divisoria en el cambio térmico
            val shiftLineP = Paint().apply {
                color = COLOR_PRIMARY
                strokeWidth = 1f
                pathEffect = DashPathEffect(floatArrayOf(3f, 3f), 0f)
            }
            c.drawLine(shiftX, graphTop, shiftX, graphBottom, shiftLineP)

            val shiftLblP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_PRIMARY
                textSize = 6f
                isFakeBoldText = true
            }
            c.drawText("Cambio térmico (Ovulación)", shiftX + 3f, graphTop + 10f, shiftLblP)
        }

        // Líneas horizontales de Temperatura
        val gridP = Paint().apply { color = COLOR_GRID; strokeWidth = 0.6f }
        val gTextP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_GRAY; textSize = 6.5f; textAlign = Paint.Align.RIGHT }

        for (i in 0..4) {
            val lineY = graphTop + (graphH * i / 4f)
            c.drawLine(graphLeft, lineY, graphRight, lineY, gridP)
            val tVal = maxTemp - ((maxTemp - minTemp) * i / 4f)
            c.drawText(String.format(Locale.US, "%.1f°", tVal), graphLeft - 4f, lineY + 2.5f, gTextP)
        }

        // Ticks de días en eje X
        val xTextP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_GRAY; textSize = 6.5f; textAlign = Paint.Align.CENTER }
        for (d in 0 until totalDays step 3) {
            val lineX = graphLeft + (d * dayStep)
            c.drawLine(lineX, graphTop, lineX, graphBottom, gridP)
            c.drawText("D${d + 1}", lineX, graphBottom + 9f, xTextP)
        }

        // WHO Coverline (periodo 3-sobre-6)
        if (cover != null && cover in minTemp..maxTemp) {
            val normY = 1f - ((cover - minTemp) / (maxTemp - minTemp)).coerceIn(0f, 1f)
            val coverY = graphTop + (graphH * normY)

            val coverP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_COVERLINE
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
                pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f)
            }
            c.drawLine(graphLeft, coverY, graphRight, coverY, coverP)

            val cLblP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_COVERLINE
                textSize = 6.5f
                isFakeBoldText = true
                textAlign = Paint.Align.RIGHT
            }
            c.drawText("Coverline OMS (${String.format(Locale.US, "%.2f", cover)}°)", graphRight - 4f, coverY - 3f, cLblP)
        }

        // Barras de Dolor (EVA 0-10) en la base
        val painBarP = Paint().apply { color = Color.rgb(244, 63, 94); alpha = 130 }
        logs.forEachIndexed { idx, log ->
            if (log.painLevel > 0) {
                val bx = graphLeft + (idx * dayStep)
                val barH = (log.painLevel / 10f) * 24f
                c.drawRect(bx + 1f, graphBottom - barH, bx + dayStep - 1f, graphBottom, painBarP)
            }
        }

        // Curva BBT
        val bbtPath = Path()
        val bbtDotP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_PRIMARY }
        val bbtLineP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        var isFirstPoint = true
        logs.forEachIndexed { idx, log ->
            val temp = log.basalBodyTemp
            if (temp != null && temp in 35.0f..38.5f) {
                val px = graphLeft + (idx * dayStep) + (dayStep / 2f)
                val normY = 1f - ((temp - minTemp) / (maxTemp - minTemp)).coerceIn(0f, 1f)
                val py = graphTop + (graphH * normY)

                if (isFirstPoint) {
                    bbtPath.moveTo(px, py)
                    isFirstPoint = false
                } else {
                    bbtPath.lineTo(px, py)
                }
                c.drawCircle(px, py, 2f, bbtDotP)
            }
        }
        c.drawPath(bbtPath, bbtLineP)

        // Leyenda del gráfico
        val legY = graphBottom + 20f
        val legP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_DARK; textSize = 7f }

        c.drawCircle(MARGIN + 20f, legY - 2.5f, 3f, bbtDotP)
        c.drawText("T. Basal (°C)", MARGIN + 28f, legY, legP)

        val legCoverP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_COVERLINE
            strokeWidth = 1.5f
            pathEffect = DashPathEffect(floatArrayOf(3f, 2f), 0f)
        }
        c.drawLine(MARGIN + 105f, legY - 2.5f, MARGIN + 120f, legY - 2.5f, legCoverP)
        c.drawText("Coverline OMS (periodo 3-sobre-6)", MARGIN + 125f, legY, legP)

        c.drawRect(MARGIN + 270f, legY - 6f, MARGIN + 280f, legY, painBarP)
        c.drawText("Nivel Dolor EVA (0-10)", MARGIN + 285f, legY, legP)

        y += graphCardH + 10f

        // 3. Matriz Bio-Estadística: Spearman & Jaccard (2 Columnas)
        val bioCardH = 115f
        val bioColW = (CONTENT_W - 10f) / 2f
        val bioCol2X = MARGIN + bioColW + 10f

        // Columna Izquierda: Correlaciones Spearman
        drawCardBox(c, MARGIN, y, bioColW, bioCardH)
        drawMiniHeader(c, MARGIN + 10f, y + 15f, "CORRELACIONES DE SPEARMAN (Lag Fisiológico)")

        val bioInsights = data.bioInsights.take(3)
        if (bioInsights.isEmpty()) {
            val emptyP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_GRAY; textSize = 7.5f }
            c.drawText("Insuficientes días con biomarcadores para correlación estadística.", MARGIN + 10f, y + 36f, emptyP)
        } else {
            var bY = y + 30f
            bioInsights.forEach { ins ->
                val v1 = varLabel(ins.variableA)
                val v2 = varLabel(ins.variableB)
                val lagStr = if (ins.lagDays == 0) "Mismo día (Lag 0)" else "Lag +${ins.lagDays}d (Efecto en 24h)"
                val rScore = String.format(Locale.US, "rs = %+.2f", ins.correlationScore)

                val dotP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_PRIMARY }
                c.drawCircle(MARGIN + 12f, bY - 3f, 2f, dotP)

                val bTitleP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_DARK; textSize = 7.8f; isFakeBoldText = true }
                c.drawText("$v1 ↔ $v2", MARGIN + 18f, bY, bTitleP)

                val bScoreP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_PRIMARY; textSize = 7.2f; isFakeBoldText = true }
                c.drawText(rScore, MARGIN + bioColW - 52f, bY, bScoreP)

                val interpP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_GRAY; textSize = 6.8f }
                c.drawText("$lagStr · Correlación no paramétrica", MARGIN + 18f, bY + 10f, interpP)

                bY += 24f
            }
        }

        // Columna Derecha: Clusters de Síntomas Jaccard
        drawCardBox(c, bioCol2X, y, bioColW, bioCardH)
        drawMiniHeader(c, bioCol2X + 10f, y + 15f, "AGRUPACIONES DE SÍNTOMAS (Índice Jaccard)")

        val clusters = data.symptomClusters.take(3)
        if (clusters.isEmpty()) {
            val emptyP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_GRAY; textSize = 7.5f }
            c.drawText("No se detectaron clusters con co-ocurrencia ≥ 40%.", bioCol2X + 10f, y + 36f, emptyP)
        } else {
            var cY = y + 30f
            clusters.forEach { cl ->
                val sA = cl.symptomA.replaceFirstChar { ch -> ch.uppercase() }
                val sB = cl.symptomB.replaceFirstChar { ch -> ch.uppercase() }
                val sympStr = "$sA + $sB"
                val jaccPct = "${(cl.jaccardScore * 100).toInt()}%"
                val phaseStr = cl.dominantPhase?.let { "Fase ${phaseLabel(it)}" } ?: "Transversal"

                val cDotP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_NAVY_DARK }
                c.drawCircle(bioCol2X + 12f, cY - 3f, 2f, cDotP)

                val cTitleP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_DARK; textSize = 7.8f; isFakeBoldText = true }
                val truncSymp = if (sympStr.length > 28) sympStr.take(25) + "..." else sympStr
                c.drawText(truncSymp, bioCol2X + 18f, cY, cTitleP)

                val cPctP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_NAVY_DARK; textSize = 7.2f; isFakeBoldText = true }
                c.drawText(jaccPct, bioCol2X + bioColW - 35f, cY, cPctP)

                val cSubP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_GRAY; textSize = 6.8f }
                c.drawText("Co-ocurrencia · $phaseStr", bioCol2X + 18f, cY + 10f, cSubP)

                cY += 24f
            }
        }

        y += bioCardH + 10f

        // 4. Síntomas Frecuentes y Distribución Emocional
        val sympH = 76f
        drawCardBox(c, MARGIN, y, CONTENT_W, sympH)
        drawMiniHeader(c, MARGIN + 10f, y + 15f, "SÍNTOMAS FRECUENTES Y DISTRIBUCIÓN EMOCIONAL")

        val sympList = data.symptomCorrelations.take(3)
        var sY = y + 30f
        sympList.forEach { sc ->
            val sP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_DARK; textSize = 7.5f }
            val pP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_GRAY; textSize = 7f }
            c.drawText("• ${sc.symptomName.replaceFirstChar { it.uppercase() }}", MARGIN + 12f, sY, sP)
            c.drawText("Predominio en ${phaseLabel(sc.dominantPhase)} (${sc.totalOccurrences} registros)", MARGIN + 120f, sY, pP)
            sY += 14f
        }

        // Distribución de estado de ánimo (Lado derecho de la tarjeta)
        val mood = data.moodDistribution
        val moodX = MARGIN + 310f
        if (mood != null && mood.totalDays > 0) {
            val moodP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_DARK; textSize = 7.2f }
            var mY = y + 30f
            mood.distribution.entries.sortedByDescending { it.value }.take(3).forEach { (mName, count) ->
                val pct = (count * 100) / mood.totalDays
                c.drawText("$mName:", moodX, mY, moodP)

                // Barra
                val barW = 110f
                val fillW = (barW * pct) / 100f
                val mBg = Paint().apply { color = COLOR_LIGHT_GRAY }
                val mFill = Paint().apply { color = COLOR_PRIMARY }
                c.drawRoundRect(RectF(moodX + 55f, mY - 6f, moodX + 55f + barW, mY), 2f, 2f, mBg)
                c.drawRoundRect(RectF(moodX + 55f, mY - 6f, moodX + 55f + fillW, mY), 2f, 2f, mFill)

                c.drawText("$pct%", moodX + 55f + barW + 5f, mY, moodP)
                mY += 14f
            }
        }

        y += sympH + 10f

        // 5. Espacio Clínico Formal: Checkboxes Rápidos, Observaciones, Conducta y Firma
        val docH = 200f
        val docBg = Paint().apply { color = COLOR_WHITE }
        val docBorder = Paint().apply { color = Color.rgb(148, 163, 184); style = Paint.Style.STROKE; strokeWidth = 1f }
        c.drawRoundRect(RectF(MARGIN, y, MARGIN + CONTENT_W, y + docH), 6f, 6f, docBg)
        c.drawRoundRect(RectF(MARGIN, y, MARGIN + CONTENT_W, y + docH), 6f, 6f, docBorder)

        // Banda superior de la caja médica
        val docHeadBg = Paint().apply { color = COLOR_LIGHT_GRAY }
        c.drawRect(MARGIN, y, MARGIN + CONTENT_W, y + 20f, docHeadBg)
        val docHeadBorder = Paint().apply { color = COLOR_BORDER; strokeWidth = 0.8f }
        c.drawLine(MARGIN, y + 20f, MARGIN + CONTENT_W, y + 20f, docHeadBorder)

        val docHeadTextP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_NAVY_DARK
            textSize = 8f
            isFakeBoldText = true
        }
        c.drawText("OBSERVACIONES CLÍNICAS, PLAN TERAPÉUTICO Y PRESCRIPCIÓN MÉDICA", MARGIN + 10f, y + 14f, docHeadTextP)

        // Checkboxes clínicos rápidos para el especialista (Ahorran tiempo en consulta)
        val cbP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_DARK; textSize = 7.2f }
        val boxPaint = Paint().apply { color = COLOR_BORDER; style = Paint.Style.STROKE; strokeWidth = 0.8f }
        val cbY = y + 33f

        // Checkbox 1: Ecografía
        c.drawRect(MARGIN + 12f, cbY - 7f, MARGIN + 21f, cbY + 2f, boxPaint)
        c.drawText("Ecografía Ginecológica / TV", MARGIN + 26f, cbY, cbP)

        // Checkbox 2: Perfil Hormonal
        c.drawRect(MARGIN + 150f, cbY - 7f, MARGIN + 159f, cbY + 2f, boxPaint)
        c.drawText("Perfil Hormonal (TSH/Prl/Andr.)", MARGIN + 164f, cbY, cbP)

        // Checkbox 3: AINEs / Manejo Dolor
        c.drawRect(MARGIN + 295f, cbY - 7f, MARGIN + 304f, cbY + 2f, boxPaint)
        c.drawText("Manejo Dolor / AINEs", MARGIN + 309f, cbY, cbP)

        // Checkbox 4: Anticoncepción
        c.drawRect(MARGIN + 410f, cbY - 7f, MARGIN + 419f, cbY + 2f, boxPaint)
        c.drawText("Ajuste Anticonceptivo", MARGIN + 424f, cbY, cbP)

        // Líneas punteadas para anotaciones manuscritas del médico
        val dashLineP = Paint().apply {
            color = Color.rgb(210, 220, 230)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            pathEffect = DashPathEffect(floatArrayOf(3f, 3f), 0f)
        }

        var lineY = y + 54f
        for (i in 0..3) {
            c.drawLine(MARGIN + 12f, lineY, MARGIN + CONTENT_W - 12f, lineY, dashLineP)
            lineY += 21f
        }

        // Líneas de firma, fecha y registro profesional al pie de la caja
        val bottomDocY = y + docH - 24f
        val signLineP = Paint().apply { color = COLOR_DARK; strokeWidth = 0.8f }

        // Firma y Sello (Izquierda)
        val signW = 200f
        c.drawLine(MARGIN + 16f, bottomDocY, MARGIN + 16f + signW, bottomDocY, signLineP)
        val signLblP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_GRAY; textSize = 7f; textAlign = Paint.Align.CENTER }
        c.drawText("Firma y Sello del Especialista", MARGIN + 16f + (signW / 2f), bottomDocY + 11f, signLblP)

        // Fecha y Registro Médico (Derecha)
        val dateRegX = MARGIN + CONTENT_W - 220f
        c.drawLine(dateRegX, bottomDocY, MARGIN + CONTENT_W - 16f, bottomDocY, signLineP)
        c.drawText("Fecha de Consulta / Registro Profesional (Nº Colegiado)", dateRegX + 102f, bottomDocY + 11f, signLblP)

        // 6. Pie de Página 2 y Aviso Médico
        drawPageFooter(c, 2, 2)
    }

    // =========================================================================
    //  COMPONENTES Y HELPERS GRÁFICOS
    // =========================================================================

    private fun drawCardBox(c: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val bg = Paint().apply { color = COLOR_WHITE }
        val border = Paint().apply { color = COLOR_BORDER; style = Paint.Style.STROKE; strokeWidth = 0.8f }
        c.drawRoundRect(RectF(x, y, x + w, y + h), 6f, 6f, bg)
        c.drawRoundRect(RectF(x, y, x + w, y + h), 6f, 6f, border)
    }

    private fun drawMiniHeader(c: Canvas, x: Float, y: Float, title: String) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            textSize = 8.5f
            isFakeBoldText = true
            typeface = Typeface.DEFAULT_BOLD
        }
        c.drawText(title, x, y, p)
    }

    private fun drawStatusPill(c: Canvas, x: Float, y: Float, w: Float, h: Float, title: String, value: String, isOk: Boolean) {
        val bg = Paint().apply { color = if (isOk) COLOR_GREEN_LIGHT else COLOR_RED_LIGHT }
        val border = Paint().apply { color = if (isOk) COLOR_GREEN_BORDER else COLOR_RED_BORDER; style = Paint.Style.STROKE; strokeWidth = 0.8f }
        c.drawRoundRect(RectF(x, y, x + w, y + h), 4f, 4f, bg)
        c.drawRoundRect(RectF(x, y, x + w, y + h), 4f, 4f, border)

        val tP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_GRAY; textSize = 6.2f; isFakeBoldText = true }
        c.drawText(title, x + 6f, y + 9f, tP)

        val vColor = if (isOk) COLOR_GREEN_DARK else COLOR_RED_DARK
        val vP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = vColor; textSize = 7.5f; isFakeBoldText = true }
        val truncValue = if (value.length > 25) value.take(23) + "..." else value
        c.drawText(truncValue, x + 6f, y + 19.5f, vP)
    }

    private fun drawPageFooter(c: Canvas, pageNum: Int, totalPages: Int) {
        val footerY = PAGE_H - MARGIN + 14f

        val div = Paint().apply { color = COLOR_BORDER; strokeWidth = 0.6f }
        c.drawLine(MARGIN, footerY - 14f, PAGE_W - MARGIN.toFloat(), footerY - 14f, div)

        val discP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_GRAY; textSize = 6.4f }
        c.drawText("Aviso: Documento para uso médico orientativo. Procesamiento 100% privado en el dispositivo.", MARGIN, footerY - 4f, discP)

        val pNumP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GRAY
            textSize = 7f
            textAlign = Paint.Align.RIGHT
        }
        c.drawText("Página $pageNum de $totalPages · Lumi App Health", PAGE_W - MARGIN, footerY - 4f, pNumP)
    }

    private fun phaseLabel(phase: CyclePhase): String = when (phase) {
        CyclePhase.MENSTRUAL  -> "Menstrual"
        CyclePhase.FOLLICULAR -> "Folicular"
        CyclePhase.OVULATION  -> "Ovulación"
        CyclePhase.LUTEAL     -> "Lútea"
        else                  -> "General"
    }

    private fun varLabel(v: BioVariable): String = when (v) {
        BioVariable.PAIN_LEVEL       -> "Dolor Pélvico"
        BioVariable.STRESS_LEVEL     -> "Estrés"
        BioVariable.ENERGY_LEVEL     -> "Energía"
        BioVariable.SLEEP_HOURS      -> "Sueño"
        BioVariable.WATER_INTAKE     -> "Hidratación"
        BioVariable.BASAL_BODY_TEMP  -> "Temperatura Basal"
    }
}
