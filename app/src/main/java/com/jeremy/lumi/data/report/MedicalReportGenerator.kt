package com.jeremy.lumi.data.report

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.jeremy.lumi.domain.model.CyclePhase
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Genera un reporte médico clínico en PDF usando exclusivamente la API nativa
 * de Android [PdfDocument] + [Canvas] — sin dependencias externas.
 *
 * Tamaño A4: 595 × 842 puntos (72 DPI).
 */
@Singleton
class MedicalReportGenerator @Inject constructor() {

    // ─── Dimensiones de página ────────────────────────────────────────────────
    private val PAGE_W   = 595
    private val PAGE_H   = 842
    private val MARGIN   = 36f
    private val CONTENT_W = PAGE_W - 2 * MARGIN

    // ─── Colores Clínicos ─────────────────────────────────────────────────────────────
    private val COLOR_PRIMARY   = Color.rgb(41, 98, 255)    // Azul médico
    private val COLOR_PRIMARY_L = Color.rgb(240, 244, 255)  // Azul muy claro (fondos)
    private val COLOR_DARK      = Color.rgb(33, 33, 33)     // Texto principal (Casi negro)
    private val COLOR_GRAY      = Color.rgb(110, 110, 130)  // Texto secundario
    private val COLOR_BORDER    = Color.rgb(220, 210, 225)  // Bordes / líneas
    private val COLOR_WHITE     = Color.WHITE

    // ─── Paints reutilizables ─────────────────────────────────────────────────
    private fun titlePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = COLOR_DARK
        textSize  = 18f
        isFakeBoldText = true
        typeface  = android.graphics.Typeface.DEFAULT_BOLD
    }
    private fun sectionPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_PRIMARY
        textSize = 10f
        isFakeBoldText = true
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private fun bodyPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = COLOR_DARK
        textSize = 9f
    }
    private fun smallPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = COLOR_GRAY
        textSize = 8f
    }
    private fun valuePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = COLOR_PRIMARY
        textSize = 9f
        isFakeBoldText = true
    }
    private fun disclaimerPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = COLOR_GRAY
        textSize = 7.5f
    }

    // ─── Punto de entrada ─────────────────────────────────────────────────────

    fun generate(data: MedicalReportData): ByteArray {
        val pdf = PdfDocument()
        val ctx = PageContext(pdf)

        drawHeader(ctx, data)
        drawProfileSection(ctx, data)
        drawStatsSection(ctx, data)
        drawCycleHistoryTable(ctx, data)
        drawClinicalGraphsSection(ctx, data) // Novedad: Gráficos Clínicos (BBT y Dolor)
        drawSymptomsSection(ctx, data)
        drawMoodSection(ctx, data)
        drawDisclaimer(ctx, data)

        ctx.finishCurrentPage()

        val out = ByteArrayOutputStream()
        pdf.writeTo(out)
        pdf.close()
        return out.toByteArray()
    }

    // ─── Header ──────────────────────────────────────────────────────────────

    private fun drawHeader(ctx: PageContext, data: MedicalReportData) {
        val c = ctx.canvas

        // Banda de color superior
        val paint = Paint().apply { color = COLOR_PRIMARY }
        c.drawRect(0f, 0f, PAGE_W.toFloat(), 56f, paint)

        // Título
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color    = COLOR_WHITE
            textSize = 16f
            isFakeBoldText = true
        }
        c.drawText("Reporte de Ciclo Menstrual", MARGIN, 26f, tp)

        // Subtítulo
        val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color    = Color.rgb(180, 200, 255)
            textSize = 9f
        }
        c.drawText("Generado por LumiApp · ${data.generatedAt.format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("es")))}", MARGIN, 42f, sp)

        // "Lumi" badge derecho
        val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color    = Color.argb(60, 255, 255, 255)
            textSize = 22f
        }
        c.drawText("✿", PAGE_W - MARGIN - 24f, 38f, bp)

        ctx.y = 68f
    }

    // ─── Perfil ───────────────────────────────────────────────────────────────

    private fun drawProfileSection(ctx: PageContext, data: MedicalReportData) {
        ctx.checkBreak(50f)
        drawSectionTitle(ctx, "PERFIL DE LA USUARIA")

        val items = mutableListOf<Pair<String, String>>()
        data.userName?.let { items += "Nombre" to it }
        data.userAge?.let  { items += "Edad" to "$it años" }
        data.userHeightCm?.let { h ->
            data.userWeightKg?.let { w ->
                val bmi = w / ((h / 100) * (h / 100))
                items += "Talla / Peso" to "${h.toInt()} cm · ${w.toInt()} kg · IMC ${String.format("%.1f", bmi)}"
            } ?: run { items += "Talla" to "${h.toInt()} cm" }
        }
        items += "Anticonceptivos hormonales" to if (data.isOnContraceptive) "Sí" else "No"
        items += "Ciclo de referencia" to "${data.avgCycleLength} días · Período ${data.avgPeriodLength} días"

        drawKeyValuePairs(ctx, items)
    }

    // ─── Estadísticas históricas ──────────────────────────────────────────────

    private fun drawStatsSection(ctx: PageContext, data: MedicalReportData) {
        ctx.checkBreak(70f)
        val stats = data.historicalStats

        drawSectionTitle(ctx, "RESUMEN ESTADÍSTICO")

        val items = mutableListOf<Pair<String, String>>()
        if (stats != null) {
            items += "Ciclos analizados"    to "${stats.cycleCount}"
            items += "Duración promedio"    to "${String.format("%.1f", stats.avgCycleLength)} días"
            items += "Período promedio"     to "${String.format("%.1f", stats.avgPeriodLength)} días"
            items += "Ciclo más corto"      to "${stats.shortestCycle} días"
            items += "Ciclo más largo"      to "${stats.longestCycle} días"
            items += "Variabilidad"         to "${stats.longestCycle - stats.shortestCycle} días"
            if (stats.maxHistoricalDelay > 0)
                items += "Retraso máximo histórico" to "${stats.maxHistoricalDelay} días"
            val isIrregular = (stats.longestCycle - stats.shortestCycle) > 7
            items += "Regularidad"          to if (isIrregular) "Irregular (variación >7 días)" else "Regular"
        } else {
            items += "Datos disponibles" to "Insuficientes (se necesitan ≥2 ciclos cerrados)"
        }

        drawKeyValuePairs(ctx, items)
    }

    // ─── Tabla de historial de ciclos ─────────────────────────────────────────

    private fun drawCycleHistoryTable(ctx: PageContext, data: MedicalReportData) {
        val cycles = data.recentCycles
        if (cycles.isEmpty()) return

        ctx.checkBreak(90f)
        drawSectionTitle(ctx, "HISTORIAL DE CICLOS (últimos ${cycles.size})")

        val headers  = listOf("#", "Inicio", "Duración", "Sangrado", "Dolor prom.")
        val colRatios= listOf(0.06f, 0.25f, 0.20f, 0.22f, 0.27f)
        val colWidths= colRatios.map { it * CONTENT_W }

        val ROW_H    = 16f
        val headerH  = 18f

        // Fondo cabecera
        ctx.checkBreak(headerH + ROW_H * min(cycles.size, 12) + 4f)
        val headerBg = Paint().apply { color = COLOR_PRIMARY_L }
        ctx.canvas.drawRect(MARGIN, ctx.y, MARGIN + CONTENT_W, ctx.y + headerH, headerBg)

        // Texto cabecera
        val hp = sectionPaint().apply { color = COLOR_PRIMARY; textSize = 8f }
        var x = MARGIN + 4f
        for (i in headers.indices) {
            ctx.canvas.drawText(headers[i], x, ctx.y + 13f, hp)
            x += colWidths[i]
        }
        ctx.y += headerH

        // Filas
        val bp = bodyPaint().apply { textSize = 8f }
        val fmt = DateTimeFormatter.ofPattern("d MMM yy", Locale("es"))
        cycles.forEachIndexed { idx, cycle ->
            ctx.checkBreak(ROW_H + 2f)

            // Fondo alterno
            if (idx % 2 == 0) {
                val altBg = Paint().apply { color = Color.rgb(248, 249, 250) }
                ctx.canvas.drawRect(MARGIN, ctx.y, MARGIN + CONTENT_W, ctx.y + ROW_H, altBg)
            }

            val startDate = data.cycleStartDates.getOrNull(idx)
            val cells = listOf(
                "${idx + 1}",
                startDate?.format(fmt) ?: "—",
                "${cycle.durationDays} días",
                "${cycle.bleedingDays} días",
                if (cycle.avgPainLevel > 0) "${String.format("%.1f", cycle.avgPainLevel)}/10" else "—"
            )

            x = MARGIN + 4f
            for (i in cells.indices) {
                ctx.canvas.drawText(cells[i], x, ctx.y + 11f, bp)
                x += colWidths[i]
            }

            // Línea divisoria
            val linePaint = Paint().apply { color = COLOR_BORDER; strokeWidth = 0.5f }
            ctx.canvas.drawLine(MARGIN, ctx.y + ROW_H, MARGIN + CONTENT_W, ctx.y + ROW_H, linePaint)
            ctx.y += ROW_H
        }
        ctx.y += 4f
    }

    // ─── Gráficos Clínicos (BBT y Síntomas) ───────────────────────────────────

    private fun drawClinicalGraphsSection(ctx: PageContext, data: MedicalReportData) {
        val logs = data.latestCycleLogs
        if (logs.isEmpty()) return

        ctx.checkBreak(220f)
        drawSectionTitle(ctx, "CURVA CLÍNICA DEL ÚLTIMO CICLO (T. BASAL Y DOLOR)")

        val graphH = 120f
        val startY = ctx.y
        val c = ctx.canvas

        // 1. Obtener rango de BBT válido
        val bbtLogs = logs.filter { it.basalBodyTemp != null && it.basalBodyTemp > 35f }
        
        // Determinar min y max de temperatura
        var minTemp = 36.0f
        var maxTemp = 37.5f
        if (bbtLogs.isNotEmpty()) {
            val actualMin = bbtLogs.minOf { it.basalBodyTemp!! }
            val actualMax = bbtLogs.maxOf { it.basalBodyTemp!! }
            minTemp = actualMin - 0.2f
            maxTemp = actualMax + 0.2f
        }

        // Si la diferencia es muy pequeña, forzar un rango clínico de al menos 1.0 grado
        if (maxTemp - minTemp < 1.0f) {
            val mid = (minTemp + maxTemp) / 2
            minTemp = mid - 0.5f
            maxTemp = mid + 0.5f
        }

        val totalDays = logs.size.coerceAtLeast(1)
        val dayStep = CONTENT_W / totalDays.toFloat()

        // Dibujar fondo y rejilla del gráfico
        val gridPaint = Paint().apply { color = COLOR_BORDER; strokeWidth = 0.5f }
        val gridTextPaint = smallPaint().apply { color = COLOR_GRAY; textSize = 6f }

        c.drawRect(MARGIN, startY, MARGIN + CONTENT_W, startY + graphH, Paint().apply { color = COLOR_PRIMARY_L; alpha = 100 })
        c.drawRect(MARGIN, startY, MARGIN + CONTENT_W, startY + graphH, Paint().apply { color = COLOR_BORDER; style = Paint.Style.STROKE; strokeWidth = 1f })

        // Líneas horizontales (T. Basal)
        for (i in 0..4) {
            val y = startY + (graphH * i / 4f)
            c.drawLine(MARGIN, y, MARGIN + CONTENT_W, y, gridPaint)
            val temp = maxTemp - ((maxTemp - minTemp) * i / 4f)
            c.drawText(String.format("%.1f°", temp), MARGIN - 22f, y + 3f, gridTextPaint)
        }

        // Eje X (Días)
        for (i in 0 until totalDays step 3) { // Mostrar cada 3 días para no saturar
            val x = MARGIN + (i * dayStep) + (dayStep / 2)
            c.drawLine(x, startY, x, startY + graphH, gridPaint)
            c.drawText("${i + 1}", x - 3f, startY + graphH + 10f, gridTextPaint)
        }

        // --- Dibujar Curva BBT ---
        val bbtPath = android.graphics.Path()
        val bbtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val bbtDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            style = Paint.Style.FILL
        }

        var isFirst = true
        logs.forEachIndexed { index, log ->
            val temp = log.basalBodyTemp
            if (temp != null && temp > 35f) {
                val x = MARGIN + (index * dayStep) + (dayStep / 2)
                val normalizedY = 1f - ((temp - minTemp) / (maxTemp - minTemp)).coerceIn(0f, 1f)
                val y = startY + (graphH * normalizedY)

                if (isFirst) {
                    bbtPath.moveTo(x, y)
                    isFirst = false
                } else {
                    bbtPath.lineTo(x, y)
                }
                c.drawCircle(x, y, 2f, bbtDotPaint)
            }
        }
        c.drawPath(bbtPath, bbtPaint)

        // --- Dibujar Barras de Dolor ---
        // Se dibuja en la parte inferior del gráfico, superpuesto, con un color contrastante (Gris oscuro)
        val painPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK
            alpha = 150
            style = Paint.Style.FILL
        }
        val maxPainH = 30f // Altura máxima de la barra de dolor
        logs.forEachIndexed { index, log ->
            if (log.painLevel > 0) {
                val x = MARGIN + (index * dayStep) + (dayStep / 2)
                val barH = (log.painLevel / 10f) * maxPainH
                c.drawRect(x - (dayStep * 0.3f), startY + graphH - barH, x + (dayStep * 0.3f), startY + graphH, painPaint)
            }
        }

        // Leyenda
        ctx.y = startY + graphH + 20f
        val legendPaint = smallPaint().apply { color = COLOR_DARK; textSize = 7f }
        
        // BBT
        c.drawCircle(MARGIN + 10f, ctx.y - 2.5f, 3f, bbtDotPaint)
        c.drawText("Temperatura Basal (°C)", MARGIN + 18f, ctx.y, legendPaint)
        
        // Dolor
        c.drawRect(MARGIN + 120f, ctx.y - 6f, MARGIN + 128f, ctx.y, painPaint)
        c.drawText("Nivel de Dolor (1-10)", MARGIN + 132f, ctx.y, legendPaint)

        ctx.y += 18f
    }
    
    // ─── Síntomas por fase ────────────────────────────────────────────────────

    private fun drawSymptomsSection(ctx: PageContext, data: MedicalReportData) {
        val correlations = data.symptomCorrelations.take(10)
        if (correlations.isEmpty()) return

        ctx.checkBreak(60f)
        drawSectionTitle(ctx, "SÍNTOMAS MÁS FRECUENTES")

        val bp = bodyPaint()
        val sp = smallPaint()
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_PRIMARY }

        correlations.forEach { corr ->
            ctx.checkBreak(18f)
            // Bullet
            ctx.canvas.drawCircle(MARGIN + 4f, ctx.y + 5f, 2.5f, dot)

            // Nombre del síntoma (negrita)
            val nbp = bodyPaint().apply { isFakeBoldText = true }
            ctx.canvas.drawText(corr.symptomName, MARGIN + 12f, ctx.y + 8f, nbp)

            // Fase dominante y ocurrencias
            val phaseLabel = phaseLabel(corr.dominantPhase)
            val detail = "Fase ${phaseLabel} · ${corr.totalOccurrences} registro(s)"
            ctx.canvas.drawText(detail, MARGIN + 12f, ctx.y + 17f, sp)
            ctx.y += 20f
        }
        ctx.y += 4f
    }

    // ─── Distribución de humor ────────────────────────────────────────────────

    private fun drawMoodSection(ctx: PageContext, data: MedicalReportData) {
        val mood = data.moodDistribution ?: return
        if (mood.totalDays == 0) return

        ctx.checkBreak(60f)
        drawSectionTitle(ctx, "DISTRIBUCIÓN DE HUMOR (${mood.totalDays} días registrados)")

        val sorted = mood.distribution.entries
            .sortedByDescending { it.value }
            .take(6)

        val bp  = bodyPaint()
        val vp  = valuePaint()
        val bar = Paint().apply { color = COLOR_PRIMARY }
        val bg  = Paint().apply { color = COLOR_PRIMARY_L }

        sorted.forEach { (moodName, count) ->
            ctx.checkBreak(18f)
            val ratio = count.toFloat() / mood.totalDays
            val pct   = (ratio * 100).toInt()
            val barW  = ratio * (CONTENT_W * 0.5f)

            // Etiqueta
            ctx.canvas.drawText(moodName, MARGIN, ctx.y + 9f, bp)
            // Barra de fondo
            ctx.canvas.drawRoundRect(
                RectF(MARGIN + 120f, ctx.y + 2f, MARGIN + 120f + CONTENT_W * 0.5f, ctx.y + 13f),
                4f, 4f, bg
            )
            // Barra de valor
            if (barW > 0) {
                ctx.canvas.drawRoundRect(
                    RectF(MARGIN + 120f, ctx.y + 2f, MARGIN + 120f + barW, ctx.y + 13f),
                    4f, 4f, bar
                )
            }
            // Porcentaje
            ctx.canvas.drawText("$pct%  ($count días)", MARGIN + 120f + CONTENT_W * 0.5f + 6f, ctx.y + 9f, vp)
            ctx.y += 16f
        }
        ctx.y += 4f
    }

    // ─── Disclaimer médico ────────────────────────────────────────────────────

    private fun drawDisclaimer(ctx: PageContext, data: MedicalReportData) {
        ctx.checkBreak(55f)

        // Línea separadora
        val line = Paint().apply { color = COLOR_BORDER; strokeWidth = 1f }
        ctx.canvas.drawLine(MARGIN, ctx.y, MARGIN + CONTENT_W, ctx.y, line)
        ctx.y += 8f

        val dp = disclaimerPaint()
        val disclaimer = listOf(
            "⚕️  AVISO MÉDICO IMPORTANTE",
            "Este reporte fue generado automáticamente por LumiApp con fines informativos y de bienestar personal.",
            "No constituye un diagnóstico médico ni reemplaza la consulta con un profesional de la salud.",
            "Las predicciones son estimaciones estadísticas basadas en los datos registrados por el usuario.",
            "Si tienes dudas sobre tu salud reproductiva, consulta a tu ginecóloga o matrona.",
        )

        val boldDp = Paint(dp).apply { isFakeBoldText = true; color = COLOR_PRIMARY }
        ctx.canvas.drawText(disclaimer[0], MARGIN, ctx.y + 9f, boldDp)
        ctx.y += 13f

        disclaimer.drop(1).forEach { line2 ->
            ctx.checkBreak(12f)
            drawWrappedText(ctx, line2, MARGIN, CONTENT_W, dp, lineHeight = 11f)
        }

        ctx.y += 8f

        // Pie de página
        val footerPaint = smallPaint().apply { textAlign = Paint.Align.CENTER }
        ctx.canvas.drawText(
            "Reporte generado por LumiApp · ${data.generatedAt} · Todos los datos se procesan localmente en el dispositivo",
            PAGE_W / 2f, ctx.y + 8f, footerPaint
        )
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun drawSectionTitle(ctx: PageContext, title: String) {
        val bg = Paint().apply { color = COLOR_PRIMARY_L }
        ctx.canvas.drawRect(MARGIN, ctx.y, MARGIN + CONTENT_W, ctx.y + 18f, bg)

        val accent = Paint().apply { color = COLOR_PRIMARY; strokeWidth = 3f }
        ctx.canvas.drawLine(MARGIN, ctx.y, MARGIN, ctx.y + 18f, accent)

        val tp = sectionPaint()
        ctx.canvas.drawText(title, MARGIN + 8f, ctx.y + 13f, tp)
        ctx.y += 22f
    }

    private fun drawKeyValuePairs(ctx: PageContext, pairs: List<Pair<String, String>>) {
        val keyW  = CONTENT_W * 0.45f
        val bp    = bodyPaint()
        val vp    = valuePaint()
        val sp    = smallPaint()

        pairs.forEachIndexed { i, (key, value) ->
            ctx.checkBreak(16f)

            if (i % 2 == 0) {
                val altBg = Paint().apply { color = Color.rgb(248, 249, 250) }
                ctx.canvas.drawRect(MARGIN, ctx.y, MARGIN + CONTENT_W, ctx.y + 15f, altBg)
            }

            ctx.canvas.drawText(key, MARGIN + 6f, ctx.y + 10f, sp)
            ctx.canvas.drawText(value, MARGIN + keyW, ctx.y + 10f, bp)
            ctx.y += 15f
        }
        ctx.y += 6f
    }

    private fun drawWrappedText(ctx: PageContext, text: String, x: Float, maxWidth: Float, paint: Paint, lineHeight: Float = 12f) {
        val words    = text.split(" ")
        var curLine  = StringBuilder()
        val lines    = mutableListOf<String>()

        for (word in words) {
            val test = if (curLine.isEmpty()) word else "${curLine} $word"
            if (paint.measureText(test) <= maxWidth) {
                curLine = StringBuilder(test)
            } else {
                if (curLine.isNotEmpty()) lines.add(curLine.toString())
                curLine = StringBuilder(word)
            }
        }
        if (curLine.isNotEmpty()) lines.add(curLine.toString())

        lines.forEach { line ->
            ctx.checkBreak(lineHeight)
            ctx.canvas.drawText(line, x, ctx.y + lineHeight - 2f, paint)
            ctx.y += lineHeight
        }
    }

    private fun phaseLabel(phase: CyclePhase): String = when (phase) {
        CyclePhase.MENSTRUAL  -> "Menstrual"
        CyclePhase.FOLLICULAR -> "Folicular"
        CyclePhase.OVULATION  -> "Ovulación"
        CyclePhase.LUTEAL     -> "Lútea"
        else                  -> "General"
    }

    // ─── Gestión de páginas ───────────────────────────────────────────────────

    inner class PageContext(private val pdf: PdfDocument) {
        private var pageNum: Int = 1
        private var currentPage: PdfDocument.Page = newPage()
        var canvas: Canvas = currentPage.canvas
        var y: Float = 0f

        private fun newPage(): PdfDocument.Page {
            val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create()
            return pdf.startPage(info)
        }

        /** Si el contenido que falta ([needed] pts) no cabe en la página actual, crea una nueva. */
        fun checkBreak(needed: Float) {
            if (y + needed > PAGE_H - MARGIN - 20f) {
                pdf.finishPage(currentPage)
                pageNum++
                currentPage = newPage()
                canvas = currentPage.canvas
                y = MARGIN

                // Encabezado mini en páginas secundarias
                val mp = disclaimerPaint().apply { color = COLOR_GRAY; textAlign = Paint.Align.RIGHT }
                canvas.drawText("Reporte de Ciclo Menstrual · página $pageNum", PAGE_W - MARGIN, 18f, mp)
                val line = Paint().apply { color = COLOR_BORDER; strokeWidth = 0.5f }
                canvas.drawLine(MARGIN, 22f, PAGE_W - MARGIN.toFloat(), 22f, line)
                y = 30f
            }
        }

        fun finishCurrentPage() {
            pdf.finishPage(currentPage)
        }
    }
}
