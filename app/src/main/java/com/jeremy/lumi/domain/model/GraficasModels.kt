package com.jeremy.lumi.domain.model

// ─────────────────────────────────────────────────────────────────────────────
//  MODELOS DE UI PARA LAS GRÁFICAS AVANZADAS
//
//  Estas clases son datos ya procesados y listos para pintar en Canvas.
//  Los produce GraficasViewModel a partir de los datos crudos de Room.
//  No van en la base de datos — solo existen en memoria durante la sesión.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Representa una barra individual en la gráfica de historial de ciclos.
 *
 * @param etiqueta        Etiqueta corta del eje X (p.ej. "C1", "C2", … "C6").
 * @param duracionDias    Duración real del ciclo en días.
 * @param tipoCiclo       Clasificación según duración para asignar color.
 * @param fechaInicio     Texto de la fecha de inicio formateado (p.ej. "15 Ene").
 */
data class PuntoBarraCiclo(
    val etiqueta      : String,
    val duracionDias  : Int,
    val tipoCiclo     : CycleType,
    val fechaInicio   : String
)

/**
 * Un punto en la gráfica de tendencia de dolor y estrés.
 * Cada punto corresponde a un día del ciclo con al menos uno de los dos valores.
 *
 * @param diaCiclo    Número de día dentro del ciclo (1 = primer día de la regla).
 * @param nivelDolor  Intensidad del dolor (0–10). Null si no fue registrado ese día.
 * @param nivelEstres Nivel de estrés (0–10). Null si no fue registrado ese día.
 * @param fecha       Fecha formateada para el tooltip (p.ej. "Lun 23").
 */
data class PuntoSintomaDia(
    val diaCiclo   : Int,
    val nivelDolor : Int?,
    val nivelEstres: Int?,
    val fecha      : String
)

/**
 * Un punto en la gráfica de temperatura basal corporal (BBT).
 *
 * @param diaCiclo        Número de día dentro del ciclo.
 * @param temperaturaC    Temperatura en grados Celsius (p.ej. 36.7).
 * @param fecha           Fecha formateada para tooltip (p.ej. "Mar 24").
 * @param esOvulacion     True si este punto es el pico detectado (dip→spike).
 */
data class PuntoBbt(
    val diaCiclo      : Int,
    val temperaturaC  : Float,
    val fecha         : String,
    val esOvulacion   : Boolean = false
)

/**
 * Estado raíz que el GraficasViewModel expone como StateFlow.
 *
 * @param estaCargando          True mientras se ejecutan las coroutines de carga.
 * @param puntosCiclos          Datos para la gráfica de historial de ciclos (barras).
 * @param puntosSintomas        Datos para la gráfica de dolor/estrés (doble línea bezier).
 * @param puntosBbt             Datos para la gráfica de temperatura basal (BBT line).
 * @param promedioLongitudCiclo Promedio histórico en días, para la línea de referencia.
 * @param hayDatosBbt           True si hay al menos 3 registros de temperatura en el ciclo activo.
 * @param hayDatosSintomas      True si hay al menos 3 registros de dolor o estrés.
 * @param diaCicloActual        Día actual dentro del ciclo activo (para el marcador vertical).
 */
data class EstadoGraficasUi(
    val estaCargando          : Boolean             = true,
    val puntosCiclos          : List<PuntoBarraCiclo> = emptyList(),
    val puntosSintomas        : List<PuntoSintomaDia> = emptyList(),
    val puntosBbt             : List<PuntoBbt>        = emptyList(),
    val promedioLongitudCiclo : Float               = 28f,
    val hayDatosBbt           : Boolean             = false,
    val hayDatosSintomas      : Boolean             = false,
    val diaCicloActual        : Int                 = 0
)
