# Lumi · Auditoría UI/UX y seguridad clínica del calendario (21 oct 2026)

**Alcance:** revisión estática de `CyclePredictor.kt`, `GetEffectiveCyclePredictionUseCase.kt`, `CycleHistoryV2.kt`, `PredictionCalibrationV2.kt`, `CalendarViewModel.kt`, `LumiWeekTimeline.kt`, `DailyLogSheet.kt` y rama anterior `feat/onboarding-home-ceremony-safe-v2`. No es validación médica independiente ni un estudio prospectivo.

## Hallazgos de UX y tipografía
1. `LumiSans` = `FontFamily.SansSerif`; `LumiEditorialSerif` = `FontFamily.Serif` en `Type.kt`. **No hay fuente licenciada incluida** que replique los glifos del HTML, por lo que la paridad tipográfica completa NO existe aún.
2. Antes, «Tu semana» usaba una serif aislada y una pastilla vertical alta por día. Se corrigió en la rama con título editorial mixto, círculo compacto, seleccionador animado y texto contextual. *Hoy* y *seleccionado* no son sinónimos.
3. Anteriormente el punto inferior representaba fase **o** registro; se eliminó esa doble interpretación. **✓ significa registro diario guardado; rosa/menta de la fecha son convenciones de fase/ventana calculada**, no señales clínicas confirmadas.
4. El formulario `DailyLogSheet.kt` de `main` es monolítico (más de 2.800 líneas): cuatro pestañas y abundantes estilos Material mezclados con Lumi. El rediseño **recuperable** está en `feat/onboarding-home-ceremony-safe-v2`: usa `DailyLogSheet.kt` (404 líneas), `DailyLogCatalog.kt`, `DailyLogComponents.kt`, `DailyLogDraft.kt` y pruebas de UI. Tiene buenos principios: personalización explícita, favoritos, guardado de borrador, aviso de cambios sin guardar, secciones opcionales. **No copiar a ciegas:** revisar cambios posteriores de lógica, localización, flujos íntimos, persistencia y firma de `onSave`. Esta rama calendar no fusiona el formulario antiguo en vez del actual.
5. Se armonizaron cabecera y CTA del formulario actual con tokens tipográficos y degradado de Lumi, conservando la lógica del registro mientras se prepara la migración controlada.

## Hallazgos clínicos de la lógica (orden de riesgo)
| Prioridad | Evidencia en código | Interpretación / requisito |
| --- | --- | --- |
| **Alta** | `phaseForDay()`: `CyclePhase.OVULATION` cubre `(ovulationDay-5)..(ovulationDay+1)` (7 fechas) | La app no detecta siete días de ovulación. Es **franja de calendario calculada**. ASRM define seis días fértiles hasta el día de ovulación; no presentar el día individual como confirmado. Se corrigió el texto de Home, calendario e insights, **no el motor**. |
| **Alta** | `phaseForDateInActiveCycle()`: fases de fechas pasadas inferidas a partir de `startDate` y longitud del ciclo | Fecha pasada no significa fase fisiológica demostrada. Con datos solo del calendario no se confirma ovulación; evitar copy afirmativo o etiquetas «observada» sin señal objetiva. |
| **Alta** | `GetEffectiveCyclePredictionUseCase` / `predict`: `isOnContraceptive -> 28` | 28 días es un supuesto de presentación, no es fisiológicamente válido para todos los anticonceptivos (y el DIU de cobre no anula la ovulación). Distinguir método/regímenes antes de personalizar esto. |
| **Alta** | `predict()`: calcula día de ovulación como longitud efectiva menos fase lútea estimada y despliega una banda ± alrededor | Base razonable para una **aproximación calendárica**, pero no exactitud individual ni método anticonceptivo. Riesgo mayor si hay irregularidad, posparto, inicio de anticonceptivos, etc. |
| **Media** | `PredictionCalibrationV2.RATIO=1.093470f` describe calibración de cohorte pequeña | Rango UI «aproximado», **no** intervalo de confianza clínica ni garantía de cobertura. Requiere evaluación prospectiva por persona. |
| **Media** | `CycleHistoryV2.validCycles()` descarta duraciones fuera de 15–60 días | Reduce ruido extremo para estimación, pero las fechas reales fuera de ese rango deben seguir visibles en historia; no ocultar irregularidad ni inferir automáticamente patologías. |
| **Media** | `DelayState` y comentarios «fase lútea prolongada» tras superar longitud pronosticada | Retraso no demuestra por sí mismo una fase lútea prolongada: puede implicar ovulación tardía o ausencia de ovulación. Mostrar «tu periodo aún no está registrado» y datos observables, no un diagnóstico. |

## Qué se hizo ahora y qué falta
- **Aplicado:** copy clínico conservador en las seis traducciones para la ventana fértil, instrucciones, chat e insights; sustitución de «Ovulación» por «Ventana fértil estimada» donde corresponde; minimización de la UI, ✓ para registros, color de fases separado del registro.
- **Pendiente por decisión de producto + validación:** rediseñar clasificación de fases fisiológicas versus inferencias del calendario; modelar anticonceptivos por método; revisar variación luteal y fertilidad; evaluar cobertura de rangos por grupos/ciclos irregulares; incorporar validación con señales observadas (si se desea). **No cambiar las fórmulas sin pruebas de regresión y revisión médica.**
- **QA de calendario:** periodo registrado ≠ pronosticado, semanas cruzando meses, 28/29/30/31 días, idioma y fuente, tema claro/OLED, acceso con lector de pantalla, dimensiones pequeñas, registro desde fecha futura.

## Fuentes médicas
- CDC, *Standard Days Method* (nov 2024): https://www.cdc.gov/contraception/hcp/usspr/standard-days-method.html — la variabilidad de ventana fértil persiste incluso en ciclos regulares.
- ASRM, *Optimizing Natural Fertility* (2022): https://www.asrm.org/practice-guidance/practice-committee-documents/optimizing-natural-fertility-a-committee-opinion-2021/ — ventana fértil biológica de seis días hasta la ovulación; escasa precisión individual de apps solo calendáricas.
- ACOG, *Abnormal Uterine Bleeding*: https://www.acog.org/womens-health/faqs/abnormal-uterine-bleeding — intervalos menstruales típicos en adultos y variación.
- Jang et al., PLOS ONE (2025), metaanálisis de 102 estudios: https://doi.org/10.1371/journal.pone.0318576 — sin cambio cognitivo sistemático robusto entre fases.

**Regla de publicación:** Lumi es un registro personal y calendario informativo; no debe anunciar detección de ovulación, «días seguros» ni contracepción, ni reemplazar atención ginecológica.
