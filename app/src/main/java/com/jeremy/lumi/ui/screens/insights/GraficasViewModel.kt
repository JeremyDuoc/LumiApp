package com.jeremy.lumi.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.data.local.dao.LumiDao
import com.jeremy.lumi.domain.model.CycleType
import com.jeremy.lumi.domain.model.EstadoGraficasUi
import com.jeremy.lumi.domain.model.PuntoBarraCiclo
import com.jeremy.lumi.domain.model.PuntoBbt
import com.jeremy.lumi.domain.model.PuntoSintomaDia
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
//  GRAFICAS VIEW MODEL
//
//  Responsabilidad única: leer los datos crudos del DAO y transformarlos
//  en los modelos de UI limpios que GraficasAvanzadas.kt necesita para pintar.
//
//  No duplica lógica de InsightsViewModel — ese calcula estadísticas históricas
//  agregadas; este produce datos de series temporales para los Canvas de gráficas.
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class GraficasViewModel @Inject constructor(
    private val dao: LumiDao
) : ViewModel() {

    private val _estadoUi = MutableStateFlow(EstadoGraficasUi())
    val estadoUi: StateFlow<EstadoGraficasUi> = _estadoUi.asStateFlow()

    private val zona         = ZoneId.systemDefault()
    private val formatoCorto = DateTimeFormatter.ofPattern("d MMM", java.util.Locale("es"))
    private val formatoDia   = DateTimeFormatter.ofPattern("EEE d", java.util.Locale("es"))

    init { cargarDatos() }

    // ─────────────────────────────────────────────────────────────────────────
    //  PUNTO DE ENTRADA — lanza las tres cargas en paralelo
    // ─────────────────────────────────────────────────────────────────────────

    fun cargarDatos() {
        viewModelScope.launch {
            _estadoUi.update { it.copy(estaCargando = true) }
            try {
                val ciclosCerrados  = dao.getClosedCycles()
                val cicloActivo     = dao.getCurrentActiveCycle()

                // ── 1. Gráfica de barras: últimos 6 ciclos cerrados ──────────
                val barras = construirBarrasCiclos(ciclosCerrados.take(6))

                // Promedio ponderado simple para la línea de referencia
                val promedio = if (ciclosCerrados.isEmpty()) 28f
                else ciclosCerrados.take(12).map { c ->
                    val dur = c.endDate?.let { e ->
                        ChronoUnit.DAYS.between(
                            Instant.ofEpochMilli(c.startDate).atZone(zona).toLocalDate(),
                            Instant.ofEpochMilli(e).atZone(zona).toLocalDate()
                        ).toInt()
                    } ?: c.cycleLength
                    dur.coerceIn(15, 90)
                }.average().toFloat()

                // ── 2. Datos de síntomas del ciclo activo (o último cerrado) ─
                val cicloPararSintomas = cicloActivo ?: ciclosCerrados.firstOrNull()
                val logsParaSintomas = cicloPararSintomas?.let {
                    dao.getLogsDelCiclo(it.id)
                } ?: emptyList()

                val puntosSintomas = logsParaSintomas
                    .filter { log ->
                        log.painLevel > 0 || (log.stressLevel != null && log.stressLevel > 0)
                    }
                    .mapIndexed { indice, log ->
                        val diaDelCiclo = cicloPararSintomas?.let { ciclo ->
                            ChronoUnit.DAYS.between(
                                Instant.ofEpochMilli(ciclo.startDate).atZone(zona).toLocalDate(),
                                Instant.ofEpochMilli(log.date).atZone(zona).toLocalDate()
                            ).toInt() + 1
                        } ?: (indice + 1)

                        PuntoSintomaDia(
                            diaCiclo    = diaDelCiclo.coerceAtLeast(1),
                            nivelDolor  = if (log.painLevel > 0) log.painLevel else null,
                            nivelEstres = log.stressLevel?.takeIf { it > 0 },
                            fecha       = Instant.ofEpochMilli(log.date)
                                .atZone(zona).toLocalDate()
                                .format(formatoDia)
                                .replaceFirstChar { it.uppercase() }
                        )
                    }

                // ── 3. BBT del ciclo activo (o último cerrado) ───────────────
                val logsParaBbt = cicloPararSintomas?.let {
                    dao.getLogsDelCiclo(it.id)
                } ?: emptyList()

                val puntosBbtCrudos = logsParaBbt
                    .filter { log -> log.basalBodyTemp != null && log.basalBodyTemp > 35f }
                    .map { log ->
                        val diaDelCiclo = cicloPararSintomas?.let { ciclo ->
                            ChronoUnit.DAYS.between(
                                Instant.ofEpochMilli(ciclo.startDate).atZone(zona).toLocalDate(),
                                Instant.ofEpochMilli(log.date).atZone(zona).toLocalDate()
                            ).toInt() + 1
                        } ?: 1
                        Pair(diaDelCiclo, log)
                    }

                val puntosBbt = detectarOvulacion(puntosBbtCrudos.map { (dia, log) ->
                    PuntoBbt(
                        diaCiclo     = dia.coerceAtLeast(1),
                        temperaturaC = log.basalBodyTemp!!,
                        fecha        = Instant.ofEpochMilli(log.date)
                            .atZone(zona).toLocalDate()
                            .format(formatoCorto)
                            .replaceFirstChar { it.uppercase() }
                    )
                })

                // ── Día actual del ciclo activo ──────────────────────────────
                val diaActual = cicloActivo?.let { ciclo ->
                    ChronoUnit.DAYS.between(
                        Instant.ofEpochMilli(ciclo.startDate).atZone(zona).toLocalDate(),
                        Instant.now().atZone(zona).toLocalDate()
                    ).toInt() + 1
                } ?: 0

                _estadoUi.update {
                    EstadoGraficasUi(
                        estaCargando          = false,
                        puntosCiclos          = barras,
                        puntosSintomas        = puntosSintomas,
                        puntosBbt             = puntosBbt,
                        promedioLongitudCiclo = promedio,
                        hayDatosBbt           = puntosBbt.size >= 3,
                        hayDatosSintomas      = puntosSintomas.size >= 3,
                        diaCicloActual        = diaActual.coerceAtLeast(0)
                    )
                }
            } catch (e: Exception) {
                // Si hay error de base de datos mostramos estado vacío limpio
                _estadoUi.update { EstadoGraficasUi(estaCargando = false) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FUNCIONES PRIVADAS DE TRANSFORMACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Convierte los últimos [n] ciclos cerrados en puntos de barra.
     * Los ciclos vienen del DAO ordenados del más reciente al más antiguo (DESC),
     * así que invertimos para mostrarlos cronológicamente en la gráfica.
     */
    private fun construirBarrasCiclos(ciclosCerrados: List<com.jeremy.lumi.data.local.entity.CycleEntity>): List<PuntoBarraCiclo> {
        return ciclosCerrados
            .reversed() // Orden cronológico: el más antiguo a la izquierda
            .mapIndexed { indice, ciclo ->
                val duracion = ciclo.endDate?.let { fin ->
                    ChronoUnit.DAYS.between(
                        Instant.ofEpochMilli(ciclo.startDate).atZone(zona).toLocalDate(),
                        Instant.ofEpochMilli(fin).atZone(zona).toLocalDate()
                    ).toInt().coerceIn(1, 90)
                } ?: ciclo.cycleLength

                val tipo = when {
                    duracion < 21 -> CycleType.SHORT
                    duracion > 35 -> CycleType.LONG
                    else          -> CycleType.NORMAL
                }

                val fechaInicio = Instant.ofEpochMilli(ciclo.startDate)
                    .atZone(zona).toLocalDate()
                    .format(formatoCorto)
                    .replaceFirstChar { it.uppercase() }

                PuntoBarraCiclo(
                    etiqueta     = "C${indice + 1}",
                    duracionDias = duracion,
                    tipoCiclo    = tipo,
                    fechaInicio  = fechaInicio
                )
            }
    }

    /**
     * Detecta el posible día de ovulación en la curva BBT usando el
     * patrón clásico: busca el punto más bajo (nadir) seguido de una
     * subida de al menos 0.2°C sostenida al menos 2 días.
     *
     * Marca como [PuntoBbt.esOvulacion] = true el día POSTERIOR al nadir
     * (primer día de la subida confirmada).
     */
    private fun detectarOvulacion(puntos: List<PuntoBbt>): List<PuntoBbt> {
        if (puntos.size < 4) return puntos

        val umbralSubida = 0.18f // 0.18°C de subida para detectar el spike

        // Ventana deslizante: buscamos nadir → subida confirmada
        var indicePicoOvulacion = -1

        for (i in 1 until puntos.size - 1) {
            val prevTemp  = puntos[i - 1].temperaturaC
            val currTemp  = puntos[i].temperaturaC
            val nextTemp  = if (i + 1 < puntos.size) puntos[i + 1].temperaturaC else currTemp

            // El nadir es más frío que los vecinos y la subida posterior es significativa
            if (currTemp < prevTemp && nextTemp - currTemp >= umbralSubida) {
                indicePicoOvulacion = i + 1 // El spike es el día siguiente al nadir
                break
            }
        }

        return puntos.mapIndexed { indice, punto ->
            punto.copy(esOvulacion = indice == indicePicoOvulacion)
        }
    }
}
