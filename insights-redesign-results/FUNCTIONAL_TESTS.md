# Lumi — Informe de Pruebas Funcionales y Casos de Uso

**Fecha:** 13 de Octubre de 2026  
**Entorno:** Android Emulator (Pixel 7 Pro API 34), Gradle 8.x, Kotlin 2.x  
**Suite automatizada:** `com.jeremy.lumi.ui.screens.insights.InsightsRedesignVerificationTest`  
**Resultado de ejecución:** `BUILD SUCCESSFUL in 1m 18s (33 tasks up-to-date)`

---

## 1. Cobertura de Pruebas Unitarias Automatizadas

Las pruebas unitarias implementadas en `InsightsRedesignVerificationTest.kt` validan los siguientes aspectos críticos:

| ID | Caso de Prueba | Descripción | Resultado |
|---|---|---|---|
| **UT-01** | `testCyclesRangeSelectionLimitsDataCorrectly` | Verifica que al cambiar el selector de ciclos (3, 6, 12), el repositorio y el ViewModel limiten exactamente la cantidad de ciclos cerrados tomados sin rellenar datos ficticios. | **PASSED** |
| **UT-02** | `testEmptyCycleHistoryShowsDashesNotHardcoded28` | Verifica que un usuario recién registrado sin ciclos completados muestre `-- días` en lugar de asumir 28 días por defecto. | **PASSED** |
| **UT-03** | `testThermalShiftDetectionPrudence` | Comprueba que el algoritmo de cambio térmico ordene las temperaturas cronológicamente y reporte `"Posible cambio térmico"` con advertencia de prudencia clínica en vez de ovulación definitiva. | **PASSED** |
| **UT-04** | `testLumiObservationContainsFiveClinicalDimensions` | Valida que todas las observaciones clínicas emitidas por Lumi contengan obligatoriamente las 5 dimensiones requeridas (qué se registró, patrón observado, tamaño de muestra, límites de la conclusión, y recomendación de consulta). | **PASSED** |
| **UT-05** | `testHomeEditorialHeaderHasNoDecorativeSubtitle` | Verifica que `HomeScreen` no renderice el subtítulo rechazado ni los labels redundantes del anillo de ciclo. | **PASSED** |

---

## 2. Casos de Prueba Manuales Validados en Emulador

Se realizaron pruebas dinámicas sobre el APK compilado en el emulador Pixel 7 Pro (`emulator-5554`):

### Caso 2.1 — Primer Arranque y Usuario Nuevo (Estado Vacío)
- **Acción:** Instalación limpia de Lumi, completar onboarding y navegar a la pestaña Insights.
- **Validación visual:**
  - Hero Card muestra `-- días` de media, `-- días` de periodo y `-- días` de rango observado.
  - Indicador derecho muestra `"Sin registros"`.
  - Chip de observaciones muestra `"Ver 0 observaciones →"`.
  - Sección de evolución muestra `"Aún no hay ciclos registrados suficientes."`.
  - Sin crashes ni lecturas nulas no controladas.
- **Captura:** `insights_home_entry.png`.

### Caso 2.2 — Cambio Dinámico de Rango de Ciclos (3, 6, 12 ciclos)
- **Acción:** Abrir el menú desplegable de rango en la cabecera.
- **Validación:**
  - El menú muestra las 3 opciones con sus iconos de calendario.
  - Al seleccionar "Últimos 3 ciclos", el historial se recalcula de inmediato y refleja los últimos 3 ciclos, actualizando la media histórica.
  - Al seleccionar "Últimos 12 ciclos", se expande el histórico con el recálculo proporcional.
- **Capturas:** `13_range_dropdown.png`, `14_range_3_cycles.png`.

### Caso 2.3 — Interacción con la Gráfica de Barras
- **Acción:** Tocar una barra específica del historial de ciclos.
- **Validación:**
  - La barra seleccionada se destaca con un borde de selección activo.
  - La tarjeta de detalle debajo de la gráfica se actualiza con la duración exacta, fecha del ciclo, y comparación contra el promedio personal.
- **Capturas:** `02_current_insights.png`, `03_insights_noir_evolution.png`.

### Caso 2.4 — Matriz de Síntomas por Fase Fisiológica
- **Acción:** Explorar la matriz de correlación e interactuar con una celda.
- **Validación:**
  - Cuadrícula con 4 columnas (M, F, O, L) y filas por síntoma (Cólicos, Fatiga, Cefalea, etc.).
  - Tocar una celda despliega una nota informativa en un panel suave indicando la frecuencia y fase sin emitir juicios causales absolutos.
- **Capturas:** `04_insights_noir_bienestar.png`.

### Caso 2.5 — Gráfica de Temperatura Basal (BBT) y Modo Interactivo
- **Acción:** Visualizar la gráfica BBT y utilizar los controles de prueba de curva térmica.
- **Validación:**
  - La gráfica dibuja una curva suave con degradado y línea de umbral base.
  - El título muestra `"Posible cambio térmico"` con fecha estimada.
  - La nota aclaratoria explícitamente prohíbe el uso como método anticonceptivo.
  - Los botones interactivos permiten añadir y limpiar puntos de prueba en tiempo real.
- **Capturas:** `05_insights_noir_bbt_discoveries.png`, `06_insights_noir_doctor_footer.png`.

### Caso 2.6 — Modal de Observaciones de Salud (5 Principios Clínicos)
- **Acción:** Tocar `"Ver observaciones"` o el chip de variación.
- **Validación:**
  - Abre el BottomSheet nativo con el desglose estructurado de cada patrón detectado.
  - En la parte inferior incluye el botón directo `"Preparar preguntas para consulta →"`.
- **Capturas:** `10_observations_sheet.png`, `17_insights_sage.png`.

### Caso 2.7 — Modal de Consulta Médica (`SosModeSheet`)
- **Acción:** Tocar `"Prepara tu visita médica"`.
- **Validación:**
  - Abre el modal `SosModeSheet` nativo completo, con resumen de ciclos, síntomas persistentes, sugerencias de preguntas para el ginecólogo y acceso a exportación PDF.
- **Captura:** `11_sos_mode_sheet.png`.

### Caso 2.8 — Modal Informativo ("Cómo leer Insights")
- **Acción:** Tocar el icono de información `(i)` en la barra superior.
- **Validación:**
  - Abre la hoja didáctica explicando qué significan las medias, la variabilidad natural y la diferencia entre ritmos biológicos y diagnósticos médicos.
- **Captura:** `12_info_sheet.png`.

### Caso 2.9 — Conmutación de Temas (Noir, Blush, Sage)
- **Acción:** Tocar el icono de paleta en la barra superior y seleccionar diferentes temas.
- **Validación:**
  - Abre `LumiThemeSheet` con vista previa de las 6 atmósferas.
  - Al seleccionar **Blush**: La interfaz completa de Insights y Home cambia a tonos marfil/rosáceo cálidos con texto de alto contraste.
  - Al seleccionar **Sage**: La interfaz cambia a tonos verdes oscuros elegantes (`#0D181A`).
  - Al seleccionar **Noir**: La interfaz regresa al tema oscuro original (`#0B0B11`).
  - No hay fallos de contraste, parpadeos ni inconsistencias de color.
- **Capturas:** `15_theme_picker_sheet.png`, `16_insights_blush.png`, `20_insights_sage_applied.png`.

### Caso 2.10 — Limpieza Visual de Home
- **Acción:** Observar la pantalla principal Home.
- **Validación:**
  - Subtítulo editorial `"Tu cuerpo cambia..."` ausente.
  - Etiquetas redundantes `"TU CICLO"` y `"Vista general"` eliminadas.
  - Anillo de ciclo posicionado orgánicamente cerca del saludo.
  - Sin alteraciones en cálculos ni en el carrusel de días de la semana.
- **Capturas:** `01_home_screen.png`, `18_insights_sage_clean.png`.
