# Lumi — Informe de Implementación Técnica del Rediseño de Insights y Limpieza de Home

**Fecha:** 13 de Octubre de 2026  
**Rama:** `feature/lumi-insights-redesign` (Worktree: `.worktrees/insights`)  
**Autor:** Principal Android Engineer & Compose Architect

---

## 1. Resumen Ejecutivo

Se ha implementado de forma 100% nativa en Jetpack Compose y Kotlin el rediseño visual y funcional de la pantalla **Insights** de Lumi, tomando como especificación canónica el prototipo aprobado en `lumi_insights_redesign_aprobado.html`, junto con el ajuste visual de limpieza solicitado en la pantalla **Home**.

El trabajo se ejecutó en estricto aislamiento respecto a las ramas y procesos concurrentes del repositorio (sin tocar Firebase emulator, Cloud Functions, Firestore Rules ni módulos de Bonds). Toda la lógica consume los repositorios y entidades reales de Lumi (`InsightsRepository`, `CycleDao`, `DailyLogDao`), asegurando honestidad estadística: **cero datos ficticios o hardcodeados en producción**.

---

## 2. Ajuste Obligatorio en Home (Completado y Verificado)

De acuerdo con la directiva obligatoria:
- **Textos eliminados:**
  - Se eliminó el subtítulo editorial `"Tu cuerpo cambia, y está bien. Aquí tienes lo importante de hoy."` en `LumiEditorialHeading`.
  - Se suprimieron las etiquetas redundantes `"TU CICLO"` y `"Vista general"` dentro de `LumiCycleRing.kt`.
- **Composición:**
  - El anillo del ciclo se posicionó más cercano al saludo (`"Hola, [Nombre]. Un día a tu ritmo."`), eliminando el espaciado vertical vacío innecesario.
  - Se mantuvieron inalterados los cálculos del ciclo actual, el carrusel semanal ("Tu semana") y las acciones de registro diario.
- **Evidencia visual:**
  - Capturas: `01_home_screen.png`, `18_insights_sage_clean.png` (en modo Blush), `current_state.png`.

---

## 3. Implementación de Insights: Arquitectura y Componentes

### 3.1 Eliminación Obligatoria del Encabezado Decorativo Rechazado
Se eliminaron taxativamente:
- `"TU HISTORIA EN DATOS · OCTUBRE 2026"`
- `"Conoce tu ritmo."`
- `"Tus registros cuentan una historia. Aquí puedes reconocer cómo cambia, sin convertirla en un diagnóstico."`

En su lugar, la pantalla abre directamente con la cabecera de marca Lumi (`lumi ✦`, selector de atmósfera y botón de información) seguida por el selector compacto de rango de ciclos (`Últimos 3 ciclos`, `Últimos 6 ciclos`, `Últimos 12 ciclos`) y la tarjeta principal de resumen estadístico (*Hero Card*), maximizando la densidad de información útil con máxima elegancia.

### 3.2 Sección 00 — Resumen Estadístico (Hero Card)
- **Duración media de ciclos:** Muestra la media histórica calculada reactivamente en tipografía editorial (72sp). Si no hay ciclos cerrados (estado inicial o de nuevo usuario), muestra limpiamente `-- días` sin inventar promedios de 28 días.
- **Rango observado y Días de periodo:** Métricas secundarias calculadas directamente de los ciclos cerrados disponibles.
- **Chip de variación contextual:** Tag dinámico con micro-estado (`Ritmo constante`, `Variación ligera`, `Variación notable`) y acceso directo al modal de observaciones (`Ver N observaciones →`).

### 3.3 Sección 01 — Evolución: Historial de Ciclos
- **Gráfica de barras nativa (`Canvas`):**
  - Cada barra representa la duración en días de un ciclo real registrado.
  - Codificación por colores basada en umbrales clínicos seguros (ciclos cortos <24d, habituales 24-35d, largos >35d).
  - Línea guía punteada que proyecta la media real del usuario.
  - Micro-anillo de selección activa que destaca la barra inspeccionada y actualiza dinámicamente la tarjeta de contexto inferior.
  - Accesibilidad completa con descripciones semánticas.
- **Distribución de fases:**
  - Acordeón colapsable con gráfico de anillo nativo.
  - Si no hay suficientes registros de fase lútea/ovular confirmados, se explica claramente que se trata de un modelo orientativo y no de un diagnóstico biológico definitivo.

### 3.4 Sección 02 — Bienestar: Lo que Registras
- **Matriz de correlación de síntomas por fase:**
  - Columnas organizadas por fases fisiológicas: Menstrual (M), Folicular (F), Ovulatoria (O), Lútea (L).
  - Celdas con tonalidades proporcionales a la frecuencia real.
  - Al tocar una celda, se despliega una nota explicativa contextual que indica exactamente qué se registró, el tamaño de muestra y aclara que no implica causalidad médica.
- **Distribución de estados emocionales:**
  - Barras de progreso porcentual para ánimos frecuentes.
  - Botón de alternancia para ver lista completa o resumida.
- **Temperatura Basal (BBT):**
  - Gráfica continua por `Canvas` con trazado suave de curva y gradiente.
  - Terminología prudente: Reemplazo estricto de cualquier afirmación de "ovulación confirmada" por **"Posible cambio térmico"** y nota al pie advirtiendo que la gráfica no constituye un método anticonceptivo ni diagnóstico de fertilidad.
  - Modo interactivo para pruebas de curva térmica integrado.

### 3.5 Sección 03 — Interpretación: Descubrimientos Lumi
- Tarjetas con análisis bivariados derivados de los registros cruzados (síntomas y fases o síntomas y estados emocionales).
- Estados vacíos explicativos cuando no se alcanza el umbral estadístico mínimo necesario.

### 3.6 Sección 04 — Acompañamiento: Cuando Quieras Hablarlo
- **Preparación de consulta médica (`SosModeSheet`):**
  - Acceso directo al generador de preguntas clínicas y síntesis para ginecología.
- **Exportación a PDF:**
  - Integración nativa con el `CreateDocument("application/pdf")` de Android vía `InsightsViewModel.generateMedicalReport()`, preservando el motor PDF en Kotlin.
- **Descargo de responsabilidad médica:**
  - Mensaje ético aclarando el propósito de acompañamiento y registro personal de Lumi.

---

## 4. Modales y Sistema de Diseño (Hojas BottomSheet)

1. **`LumiThemeSheet`:** Selector nativo de atmósferas (`Noir`, `Blush`, `Sage`, `Spark`, `Océano`, `Bosque`) con vista previa circular y vinculación reactiva al `SettingsViewModel`.
2. **`ObservationsSheet`:** Modal con los 5 principios clínicos aprobados:
   - Qué se ha registrado.
   - Qué patrón se observa.
   - Sobre cuántos datos se basa (muestra).
   - Qué no puede concluirse.
   - Cuándo conviene consultar con un profesional de la salud.
3. **`InfoExplanationSheet`:** Guía didáctica para la lectura de métricas y variabilidad.
4. **`SosModeSheet`:** Flujo completo de acompañamiento ginecológico y exportación de datos.

---

## 5. Integración con el Sistema de Temas de Lumi

La pantalla utiliza exclusivamente los tokens semánticos de `LumiThemeTokens.current` (`tokens.bg`, `tokens.panel`, `tokens.field`, `tokens.text`, `tokens.muted`, `tokens.accent`, `tokens.line`, `tokens.pill`, `tokens.danger`, `tokens.warning`, etc.). No existen colores hex arbitrarios en la interfaz. La pantalla se adapta orgánicamente a todos los temas de la aplicación tanto en modo oscuro como en modo claro.

---

## 6. Estado de Verificación y Entregables

- **Compilación Gradle:** `BUILD SUCCESSFUL` (0 errores).
- **Pruebas unitarias:** `BUILD SUCCESSFUL` en suite completa (`InsightsRedesignVerificationTest`).
- **Verificación en Emulador Pixel 7 Pro (1440x3120):** 20 capturas de pantalla tomadas y catalogadas en `insights-redesign-results/screenshots/`.
- **Integridad Git:** Sin cambios destructivos, aislado en `feature/lumi-insights-redesign`.
