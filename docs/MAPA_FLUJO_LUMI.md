# Mapa del Flujo Visual y Guía de Auditoría UX/UI — Lumi App

**Destinatario:** Modelo Auditor Externo (Gemini 4 Argon)  
**Proyecto:** Lumi — Aplicación Android Nativa de Salud Femenina y Registro de Ciclo  
**Plataforma de Referencia:** Jetpack Compose (Kotlin) · Material 3 Design Tokens  
**Resolución de Captura:** Móvil Estándar 390 × 844 px (Viewport iPhone 14/15 / Android Moderno)  
**Fecha de Generación:** 16 de Octubre de 2026  
**Estructura de Artefactos:** `screenshots/flow/`  

---

## 1. Resumen Ejecutivo y Propósito de la Auditoría

Lumi es una aplicación enfocada en el bienestar femenino, el seguimiento del ciclo menstrual y el cuidado compartido en pareja bajo la filosofía **"Cuidar sin vigilar"**. La arquitectura de diseño prioriza la intimidad, la soberanía de los datos (almacenamiento local SQLite/Room y respaldo cifrado en Google Drive privado) y una estética visual orgánica ("Atmósferas" Noir y Blush).

### Misión del Auditor Gemini 4 Argon:
1. **Reducción de Carga Cognitiva y Texto Innecesario:** Detectar redundancias, advertencias clínicas excesivamente largas que generan fricción o ansiedad, y simplificar el microcopy a mensajes empáticos, directos y accionables.
2. **Ergonomía y Zonas de Alcance Pulgar:** Evaluar la accesibilidad con una sola mano (Thumb Reach Zone) en pantallas alargadas (390×844), identificando botones clave situados en la zona superior o modales con scroll infinito.
3. **Optimización de Hojas Inferiores (Bottom Sheets):** Resolver la sobrecarga de navegación causada por múltiples pestañas internas dentro de un mismo `BottomSheet` (ej. registro diario de síntomas).
4. **Accesibilidad Sensorial y Contraste WCAG 2.1 AA/AAA:** Evaluar el contraste de colores de las fases del ciclo (menstrual, folicular, ovulatoria, lútea) sobre fondos oscuros (Noir) y claros (Blush), garantizando legibilidad para usuarias con deficiencias visuales (daltonismo / baja visión).

---

## 2. Mapa Completo de Pantallas y Correspondencia con el Código Fuente

El conjunto de capturas organizadas secuencialmente en `screenshots/flow/` se mapea 1:1 con las capas de presentación en Jetpack Compose:

```
screenshots/flow/
├── 01_onboarding/             (Configuración inicial y consentimiento informado)
├── 02_home/                   (Panel principal del ciclo, rueda biológica y hoy)
├── 03_sintomas/               (Registro diario multifactorial de síntomas)
├── 04_calendario_insights/    (Vista mensual, evolución histórica y reporte clínico)
└── 05_perfil_privacidad/      (Ajustes de salud, respaldo cifrado y vínculos)
```

### Tabla de Correspondencia Técnica

| ID Captura | Archivo en `screenshots/flow/` | Componente / Archivo Kotlin | Estado / Función de UI |
| :--- | :--- | :--- | :--- |
| **01.01** | `01_onboarding/01_onboarding_bienvenida.png` | [`OnboardingScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/onboarding/OnboardingScreen.kt) | Bienvenida de marca, selección de rol ("Seguir mi ciclo" vs "Acompañar a alguien"). |
| **01.02** | `01_onboarding/02_onboarding_seguridad_permisos.png` | [`OnboardingScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/onboarding/OnboardingScreen.kt) | Límites clínicos, descargo de responsabilidad médica y casilla de consentimiento explícito. |
| **01.03** | `01_onboarding/03_onboarding_politica_dialog.png` | [`OnboardingScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/onboarding/OnboardingScreen.kt) | Modal dialog con política de privacidad local-first y compromiso de no-venta de datos. |
| **01.04** | `01_onboarding/04_onboarding_nombre_saludo.png` | [`OnboardingScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/onboarding/OnboardingScreen.kt) | Campo de texto de nombre/apodo opcional con renderizado de saludo en vivo ("Hola, [Nombre]."). |
| **01.05** | `01_onboarding/05_onboarding_ciclo_regularidad.png` | [`OnboardingScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/onboarding/OnboardingScreen.kt) | Selector de regularidad del ciclo y control deslizante de duración estimada (21–45 días). |
| **01.06** | `01_onboarding/06_onboarding_ultimo_periodo.png` | [`OnboardingScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/onboarding/OnboardingScreen.kt) | Selector de fecha de inicio del último periodo menstrual con fallback "No lo recuerdo". |
| **01.07** | `01_onboarding/07_onboarding_preferencias_objetivos.png` | [`OnboardingScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/onboarding/OnboardingScreen.kt) | Selección de objetivo principal (predecir ciclo, fertilidad, bienestar) y método anticonceptivo. |
| **01.08** | `01_onboarding/08_onboarding_personalizacion_sintomas.png` | [`OnboardingScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/onboarding/OnboardingScreen.kt) | Duración de sangrado habitual, selección rápida de síntomas de interés y biotipo. |
| **01.09** | `01_onboarding/09_onboarding_listo_resumen.png` | [`OnboardingScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/onboarding/OnboardingScreen.kt) | Resumen final con tarjetas editables antes de confirmar el ingreso a la app (Tema Noir). |
| **01.10** | `01_onboarding/10_onboarding_tema_blush.png` | [`OnboardingScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/onboarding/OnboardingScreen.kt) | Resumen en modo claro cálido (Tema Blush con fondo orquídea/crema). |
| **02.01** | `02_home/01_home_rueda_ciclo.png` | [`HomeScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/home/HomeScreen.kt) & [`LumiCycleRing.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/components/LumiCycleRing.kt) | Dashboard principal: Rueda biológica interactiva del ciclo, fase lútea y días restantes. |
| **02.02** | `02_home/02_home_semana_strip.png` | [`LumiWeekStrip.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/components/LumiWeekStrip.kt) | Franja semanal horizontal de días pasados/futuros y leyenda cromática de fases. |
| **02.03** | `02_home/03_home_tarjeta_hoy.png` | [`LumiTodayCard.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/components/LumiTodayCard.kt) | Tarjeta contextual del día ("Un momento para ti"), botón primario de registro y recordatorios. |
| **02.04** | `02_home/04_home_prediccion_factores.png` | [`LumiPredictionInfoSheet.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/components/LumiPredictionInfoSheet.kt) | Hoja inferior de desglose algorítmico y certeza bayesiana de la predicción actual. |
| **02.05** | `02_home/05_home_cambio_atmosfera_tema.png` | [`LumiThemeSheet.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/components/LumiThemeSheet.kt) | Selector flotante de atmósferas sensoriales (Noir, Blush, Ébano, Amatista). |
| **03.01** | `03_sintomas/01_sintomas_emocion_humor.png` | [`DailyLogSheet.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/calendar/DailyLogSheet.kt) | Pestaña "Hoy": Selector rápido de estado anímico emocional (Bien, En calma, Sensible, Triste). |
| **03.02** | `03_sintomas/02_sintomas_intimidad_heartswitch.png` | [`DailyLogSheet.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/calendar/DailyLogSheet.kt) | Registro de actividad sexual con control `HeartSwitch`, protección y métodos anticonceptivos. |
| **03.03** | `03_sintomas/03_sintomas_flujo_sangrado.png` | [`DailyLogSheet.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/calendar/DailyLogSheet.kt) | Intensidad de flujo menstrual (ligero, moderado, abundante) y manchado intermenstrual. |
| **03.04** | `03_sintomas/04_sintomas_dolor_escala.png` | [`DailyLogSheet.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/calendar/DailyLogSheet.kt) | Slider de dolor corporal 0–10 con banner de alerta clínica ante dolor severo (≥ 7). |
| **03.05** | `03_sintomas/05_sintomas_catalogo_busqueda.png` | [`DailyLogSheet.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/calendar/DailyLogSheet.kt) | Pestaña "Síntomas": Catálogo clasificado por categorías corporales con barra de búsqueda. |
| **03.06** | `03_sintomas/06_sintomas_bienestar_habitos.png` | [`DailyLogSheet.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/calendar/DailyLogSheet.kt) | Pestaña "Bienestar": Hábitos diarios (horas de sueño, hidratación, nivel de energía y estrés). |
| **03.07** | `03_sintomas/07_sintomas_notas_guardar.png` | [`DailyLogSheet.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/calendar/DailyLogSheet.kt) | Pestaña "Notas": Diario de texto libre y botón de acción flotante de guardado rápido. |
| **04.01** | `04_calendario_insights/01_calendario_mensual_fases.png` | [`CalendarScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/calendar/CalendarScreen.kt) | Cuadrícula mensual completa con marcadores circulares de fases y predicción de ovulación. |
| **04.02** | `04_calendario_insights/02_calendario_detalle_dia.png` | [`CalendarScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/calendar/CalendarScreen.kt) | Despliegue de ficha detallada del día seleccionado con registro histórico de síntomas. |
| **04.03** | `04_calendario_insights/03_insights_hero_duracion_media.png` | [`InsightsScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/insights/InsightsScreen.kt) | Tarjeta hero de analítica: Duración media de ciclo (ej. 32 días), rango de variación y confianza. |
| **04.04** | `04_calendario_insights/04_insights_graficos_duracion.png` | [`GraficasAvanzadas.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/insights/GraficasAvanzadas.kt) | Visualización de barras comparativas históricas de ciclos previos y desvíos. |
| **04.05** | `04_calendario_insights/05_insights_observaciones_clinicas.png` | [`InsightsScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/insights/InsightsScreen.kt) | Hoja inferior de correlaciones fisiológicas y detección de patrones atípicos. |
| **04.06** | `04_calendario_insights/06_insights_reporte_medico_pdf.png` | [`MedicalReportGenerator.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/data/report/MedicalReportGenerator.kt) | Generador de informe estructurado y exportación segura a PDF para consulta ginecológica. |
| **05.01** | `05_perfil_privacidad/01_ajustes_salud_objetivos.png` | [`SettingsScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/settings/SettingsScreen.kt) | Configuración de objetivos fisiológicos, modo embarazo, anticoncepción y Health Connect. |
| **05.02** | `05_perfil_privacidad/02_ajustes_apariencia_temas.png` | [`SettingsScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/settings/SettingsScreen.kt) | Personalización cromática: selector de temas de fase y gradientes de fondo ambiental. |
| **05.03** | `05_perfil_privacidad/03_privacidad_respaldo_drive.png` | [`BackupSettingsScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/settings/backup/BackupSettingsScreen.kt) | Copia de seguridad en la nube privada de Google Drive (carpeta AppData oculta) cifrada con contraseña. |
| **05.04** | `05_perfil_privacidad/04_privacidad_gestion_datos.png` | [`SettingsScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/settings/SettingsScreen.kt) | Soberanía digital: Descarga de archivo ZIP con datos en bruto y borrado permanente sin huella. |
| **05.05** | `05_perfil_privacidad/05_bonds_vinculo_compartido.png` | [`BondsHomeScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/bonds/BondsHomeScreen.kt) | Módulo Bonds: Conexión segura con la pareja o persona de confianza para acompañamiento. |
| **05.06** | `05_perfil_privacidad/06_bonds_permisos_granulares.png` | [`PartnerHubScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/partner/PartnerHubScreen.kt) | Matriz de privacidad granular: Interruptores individuales para compartir fase, fertilidad o síntomas. |

---

## 3. Diagnóstico de Dolores de UX/UI y Accesibilidad

A partir de la auditoría preliminar del código fuente y los layouts renderizados, se han identificado los siguientes cuellos de botella para que Gemini 4 Argon priorice sus recomendaciones de refactorización:

### A. Densidad de Texto y Carga Cognitiva (Text Verbosity)
1. **Descargos de Responsabilidad en Onboarding (Paso 1):**  
   *Archivo:* [`OnboardingScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/onboarding/OnboardingScreen.kt)  
   *Problema:* Se presentan párrafos completos con jerga jurídica y médica ("Lumi es una herramienta de estimación algorítmica y no constituye diagnóstico ni método anticonceptivo garantizado..."). Genera una barrera de entrada fría y alarmista en los primeros 15 segundos de uso.  
   *Oportunidad de Mejora:* Reemplazar por 3 viñetas concisas con íconos empáticos y un enlace discreto a "Términos completos".

2. **Explicación Matemática de Factores de Predicción:**  
   *Archivo:* [`LumiPredictionInfoSheet.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/components/LumiPredictionInfoSheet.kt)  
   *Problema:* La hoja inferior describe "Desviación estándar", "Certeza bayesiana ponderada" y "Percentiles de ventana fértil". La mayoría de usuarias buscan tranquilidad y comprensión sencilla ("Tus ciclos varían unos 2 días"), no una lección de estadística.  
   *Oportunidad de Mejora:* Uso de un indicador gráfico de nivel (Baja / Media / Alta regularidad) con microcopy de una sola línea.

### B. Arquitectura de Navegación y Sobrecarga de Hojas Inferiores (Bottom Sheets)
1. **Modal de Registro Diario con 4 Sub-Pestañas:**  
   *Archivo:* [`DailyLogSheet.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/calendar/DailyLogSheet.kt)  
   *Problema:* Meter cuatro pestañas horizontales ("Hoy", "Síntomas", "Bienestar", "Notas") dentro de un `ModalBottomSheet` obliga a la usuaria a realizar múltiples gestos de deslizamiento y toques para registrar un día común. Si además se despliega el teclado para notas, el modal pierde su proporción de scroll.  
   *Oportunidad de Mejora:* Flujo de una sola página vertical con guardado continuo por interacción (auto-save reactivo) y colapso de secciones no utilizadas.

2. **Aparición Intempestiva de Alertas de Dolor:**  
   *Archivo:* [`DailyLogSheet.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/calendar/DailyLogSheet.kt) (Líneas 240-280)  
   *Problema:* Al arrastrar el slider de dolor a valores ≥ 7, se inyecta dinámicamente un banner rojo de alerta médica ("Consulta a un especialista si este dolor es incapacitante"). Este salto de layout empuja bruscamente los botones inferiores hacia abajo bajo el dedo de la usuaria (Layout Shift perjudicial).  
   *Oportunidad de Mejora:* Reservar espacio fijo en la tarjeta de dolor o mostrar la advertencia como un badge flotante no obstructivo.

### C. Ergonomía en Pantallas Móviles Modernas (Thumb Reach Zone)
1. **Botones de Navegación y Filtros Superiores:**  
   *Archivo:* [`HomeScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/home/HomeScreen.kt)  
   *Problema:* Los botones de cambio de tema/atmósfera y acceso al perfil están situados en la barra superior (TopAppBar, Y < 100px), fuera del alcance natural del pulgar en dispositivos con pantallas de más de 6 pulgadas sin forzar la postura de la mano.  
   *Oportunidad de Mejora:* Integrar accesos rápidos contextuales en la tarjeta del día o en el menú inferior.

2. **Sliders Continuos vs. Controles por Pasos (Steppers):**  
   *Archivo:* [`OnboardingScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/onboarding/OnboardingScreen.kt) (Paso 3: Duración del Ciclo)  
   *Problema:* El slider continuo para elegir entre 21 y 45 días requiere una motricidad fina excesiva para clavar números exactos (ej. diferenciar entre 28 y 29 días).  
   *Oportunidad de Mejora:* Control tipo `WheelPicker` o botones de incremento/decremento `[-] 28 días [+]` con retroalimentación háptica.

### D. Accesibilidad Visual y Contraste (WCAG 2.1 AA)
1. **Contraste de Fases en Modo Blush:**  
   *Archivo:* [`PhaseColorPalette.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/theme/PhaseColorPalette.kt)  
   *Problema:* En el tema claro (Blush), el amarillo/dorado de la "Fase Folicular" y el verde suave de "Ovulación" pueden presentar un ratio de contraste inferior a 3.5:1 frente a fondos crema o blanco marfil, dificultando la diferenciación en exteriores con sol directo.  
   *Oportunidad de Mejora:* Ajustar la saturación y añadir doble codificación (forma + color o patrón de trazo en los círculos del calendario).

### E. Privacidad y Seguridad sin Fricción Técnica
1. **Configuración de Cifrado en Google Drive:**  
   *Archivo:* [`BackupSettingsScreen.kt`](file:///c:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/settings/backup/BackupSettingsScreen.kt)  
   *Problema:* Pedir a la usuaria que invente una contraseña de cifrado AES-256 propia sin un medidor de fortaleza ni opción de derivación biométrica (Android Keystore / BiometricPrompt) resulta intimidante para usuarias no técnicas.  
   *Oportunidad de Mejora:* Ofrecer dos opciones: "Cifrado automático con biometría de este dispositivo (Recomendado)" y "Contraseña manual personalizada (Avanzado)".

---

## 4. Guía para la Ejecución del Script de Captura

Para reejecutar o ampliar el conjunto de capturas en cualquier momento:

```powershell
# Ejecución del arnés de captura sobre Node.js y Edge Headless a 390x844:
node scripts/capture_lumi_flow.js
```

El script realiza:
1. Lectura de las 34 pantallas secuenciales a través de maquetas HTML de alta fidelidad sincronizadas con los componentes Jetpack Compose.
2. Invocación síncrona de Microsoft Edge Headless (`--window-size=390,844`, `--hide-scrollbars`).
3. Copia automática de imágenes complementarias de alta resolución obtenidas desde el emulador Android de desarrollo (`*_native_emulator.png`).
4. Organización estructurada en `screenshots/flow/01_onboarding/` hasta `05_perfil_privacidad/`.

---

## 5. Instrucciones de Auditoría para Gemini 4 Argon

Al auditar este proyecto, Gemini 4 Argon debe concentrarse en:
1. **Auditoría de Textos (Microcopy & Voice):** Proponer textos alternativos breves, cálidos y desprovistos de tecnicismos para cada pantalla documentada.
2. **Propuesta de Layouts Reducidos:** Simplificar las tarjetas de Home y Registro Diario eliminando elementos visuales redundantes.
3. **Checklist de Accesibilidad:** Validar que los tamaños mínimos de toque (`MinTouchTarget = 48dp`) y el contraste cromático cumplan estrictamente con las directrices médicas y de accesibilidad digital.
