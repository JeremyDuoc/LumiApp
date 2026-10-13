# Archivos Modificados y Creados — Rediseño Nativo de Insights + Limpieza de Home

Este documento detalla todas las modificaciones y nuevos archivos creados en la rama `feature/lumi-insights-redesign` de Lumi.

---

## 1. Ajustes en Home

### `app/src/main/java/com/jeremy/lumi/ui/screens/home/HomeScreen.kt`
- **Cambio**: Se eliminó el parámetro `description = "Tu cuerpo cambia, y está bien. Aquí tienes lo importante de hoy."` del componente `LumiEditorialHeading`.
- **Razón**: Reducción de ruido visual y ahorro de espacio vertical, conservando el saludo editorial `"Hola, kiwi."` y subtítulo `"Un día a tu ritmo."`.

### `app/src/main/java/com/jeremy/lumi/ui/components/LumiCycleRing.kt`
- **Cambio**: Se eliminaron las etiquetas superiores redundantes `"TU CICLO"` y `"Vista general"`, junto con su espaciador vertical.
- **Razón**: Permitir que el anillo del ciclo se aproxime naturalmente al bloque de saludo sin perder claridad funcional ni alterar cálculos ni la tira semanal `"Tu semana"`.

---

## 2. Dominio y Datos

### `app/src/main/java/com/jeremy/lumi/domain/model/InsightsModels.kt`
- **Cambio**: Adición del modelo de datos `LumiObservation` con soporte para:
  - `id`: Identificador único.
  - `categoria`: Categoría clínica (`DURACIÓN`, `RITMO`, `SÍNTOMAS`, `VARIACIÓN`).
  - `titulo`: Titular descriptivo del hallazgo.
  - `descripcion`: Detalle empírico de lo registrado.
  - `tamanoMuestra`: Número de ciclos u observaciones en que se basa el análisis.
  - `limites`: Qué NO puede concluirse médicamente.
  - `recomendacion`: Orientación médica prudente y proporcionada.
  - `severidad`: Nivel (`INFO`, `ATTENTION`, `CLINICAL`).
  - `esDestacada`: Indicador para resaltar en la tarjeta de resumen.

### `app/src/main/java/com/jeremy/lumi/data/repository/InsightsRepositoryImpl.kt`
- **Cambio**: Se utilizó el parámetro `cyclesLimit: Int = 6` en el método `getInsights(cyclesLimit: Int)` para restringir el cálculo de `computeRecentCycleStats` a los `closedCycles.take(cyclesLimit)`, recalculando media, rango y desviación estándar en función del rango seleccionado (3, 6, 12).

---

## 3. Lógica de Negocio y ViewModels

### `app/src/main/java/com/jeremy/lumi/ui/screens/insights/InsightsViewModel.kt`
- **Cambio**: Reescritura completa y reactiva del ViewModel de Insights:
  - Observación reactiva de ciclos desde `lumiRepository.getAllCycles()`.
  - Soporte de selector de rango (3, 6, 12 ciclos) con recálculo en tiempo real de estadísticas, barras y observaciones.
  - Construcción de barras interactivas `PuntoBarraCiclo` con datos reales de la base de datos (duración en días, etiqueta mensual, fecha de inicio y tipificación clínica `SHORT`, `NORMAL`, `LONG`).
  - Generación de `LumiObservation` respetando estrictamente las 5 dimensiones clínicas (qué se registró, qué patrón se observa, sobre cuántos datos se basa, qué no puede concluirse y cuándo consultar).
  - Integración reactiva de Temperatura Basal (BBT) con orden cronológico estricto y detección de "Posible cambio térmico" (sin aseveraciones diagnósticas).
  - Gestión de modales (`ObservationsSheet`, `InfoExplanationSheet`, `SosModeSheet`) y exportación nativa a PDF vía `GenerateMedicalReportUseCase`.

### `app/src/main/java/com/jeremy/lumi/ui/screens/insights/GraficasViewModel.kt`
- **Cambio**: Reactividad mediante `dao.getAllCycles().collect`; eliminación del valor por defecto ficticio de 28 días cuando no hay ciclos cerrados (reemplazado por `null` o estado vacío real); ordenamiento cronológico de puntos BBT antes del análisis de subida térmica.

### `app/src/main/java/com/jeremy/lumi/ui/screens/insights/GraficasAvanzadas.kt`
- **Cambio**: Actualización de terminología de ovulación a `"Posible cambio térmico"` e incorporación de nota de prudencia contextual.

---

## 4. UI y Presentación Jetpack Compose

### `app/src/main/java/com/jeremy/lumi/ui/screens/insights/InsightsScreen.kt`
- **Cambio**: Implementación Jetpack Compose fiel al diseño aprobado `lumi_insights_redesign_aprobado.html`:
  - **Eliminación obligatoria**: Se eliminó totalmente el encabezado decorativo rechazado (*"TU HISTORIA EN DATOS · OCTUBRE 2026"*, *"Conoce tu ritmo."*, *"Tus registros cuentan una historia..."*).
  - **Top Bar**: Flor de marca `LumiBrandFlower`, isotipo `"lumi ✦"`, selector de atmósfera/tema `LumiThemeSheet` y botón de información `InfoExplanationSheet`.
  - **Selector de rango**: Pill compacto interactivo (3, 6, 12 ciclos) con contador real de registros analizados.
  - **Hero Card**: Tarjeta de resumen con tipografía grande (72sp), días de periodo, rango observado, tag contextual y acceso a observaciones.
  - **Sección 01 / EVOLUCIÓN**: Gráfica de barras interactiva con selección de barra, línea de promedio, tarjeta explicativa de variación y disclosure desplegable con distribución ilustrativa de fases.
  - **Sección 02 / BIENESTAR**: Heatmap de síntomas por fase estimada (M, F, O, L) con interactividad celda a nota explicativa; distribución de estados emocionales con barras de progreso y toggle "Ver todos"; curva de temperatura basal con estado vacío y probador interactivo.
  - **Sección 03 / INTERPRETACIÓN**: Tarjeta "Descubrimientos Lumi" basada en correlaciones reales.
  - **Sección 04 / ACOMPAÑAMIENTO**: Acceso a "Prepara tu visita médica" (`SosModeSheet`), botón nativo para exportar reporte a PDF y descargo de responsabilidad médico.
  - **Hojas modales**: `ObservationsSheet`, `InfoExplanationSheet`, `LumiThemeSheet` y `SosModeSheet`.
  - **Tokens y Temas**: Integración total con `LumiThemeTokens.current` (Noir, Blush, Sage, etc.).

---

## 5. Recursos y Localización

### `app/src/main/res/values/strings.xml` y `app/src/main/res/values-en/strings.xml`
- **Cambio**: Sustitución de cadenas de texto de BBT (`graficas_ovulacion_detectada` -> `"Posible cambio térmico"` / `"Possible thermal shift"`).

---

## 6. Pruebas Unitarias

### `app/src/test/java/com/jeremy/lumi/ui/screens/insights/InsightsRedesignVerificationTest.kt`
- **Archivo Nuevo**: Pruebas unitarias que verifican:
  1. Clasificación correcta de ciclos (Corto < 24 días, Normal 24–35 días, Largo > 35 días).
  2. Detección cronológica prudente de cambio térmico en BBT sin orden predeterminado.
  3. Cumplimiento de las 5 dimensiones clínicas en las observaciones generadas.
