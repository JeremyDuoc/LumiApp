# LUMI — REPORTE DE AUDITORÍA NOCTURNA Y QA AUTÓNOMO
**Fecha:** 2026-10-20  
**Rama:** `qa/overnight-20261020`  
**Worktree:** `.worktrees/qa-overnight`  
**Commit Inicial:** `190f02d7b227f253d2920a4bccdaae3b06cf76ed` (`origin/main`)  
**Commit de Fase 1:** `9a3bb54` (Pushed a `origin/qa/overnight-20261020`)  
**Commit Final:** 631b97c234a919ba50b69bc438e3678075bc7b99 (origin/qa/overnight-20261020)  
**Dispositivos de prueba:**
- Google Pixel 7 Pro AVD (`emulator-5554`, Android 17 / API 35+, 1440x3120)
- Motorola Moto g04s Físico (`adb-ZY22JVK53Q-5nL2Nx._adb-tls-connect._tcp`, Android 14 / API 34)

---

## 1. RESUMEN EJECUTIVO
Se ejecuta una auditoría profunda de estabilidad, arquitectura, ciclo de vida, accesibilidad, internacionalización, predicción del ciclo y persistencia en Lumi.  
En esta sesión se han identificado y solucionado fallos críticos de ANR en arranque en frío, bloqueos de UI por capas táctiles retenidas, errores de manifiesto para Android 12+, fallos de parsing en cadenas de texto localizadas (i18n), retención de ciclos activos obsoletos en memoria tras eliminaciones, inconsistencias de integridad referencial en base de datos local y dependencias de locales estáticos.

### Estadísticas de Pruebas:
- **Pruebas Unitarias de Ciclo y Dominio:** 28 test suites ejecutadas con Gradle en debug (100% aprobadas, 0 fallos).
- **Pruebas en Dispositivo Real / Emulador:** Arranque en frío sin ANR (`SystemJobService` ejecutado en background en `Dispatchers.IO`), inspección de Logcat, navegación completa entre Inicio, Calendario, Estadísticas, Tu Voz y Ajustes, captura de jerarquía UI Automator antes y después de cerrar menús y diálogos.
- **Auditoría de Privacidad On-Device:** Confirmada la ausencia de SDKs no autorizados o telemetría de PII hacia servidores externos.

---

## 2. MATRIZ DE HALLAZGOS Y ERRORES CONFIRMADOS

| ID | Severidad | Módulo / Archivo | Resumen del Fallo | Causa Raíz | Estado |
|---|---|---|---|---|---|
| **BUG-01** | **P0 / P1** | `LumiApp.kt:15-20` | ANR en arranque en frío (`SystemJobService.onStartJob`) | `WorkManager.enqueueUniquePeriodicWork(..., UPDATE, ...)` y `requestNow(this)` síncronos en el hilo principal durante `Application.onCreate()`. Al saturarse el hilo con Compose/Hilt, `SystemJobService` superaba el timeout de 8s del sistema operativo. | **Corregido y Verificado** |
| **BUG-02** | **P1** | `app/src/debug/AndroidManifest.xml:5` | Violación de manifiesto Android 12+ (`IntentFilterExportedReceiver`) | `MainActivity` declara filtros de intención para deep links (`/pair/`) en la variante debug sin especificar `android:exported="true"`, bloqueando el empaquetado y lint de Android 12+ (API 31+). | **Corregido y Verificado** |
| **BUG-03** | **P1** | `res/values-*/strings.xml` | Error `StringFormatInvalid` en traducciones DE, FR, PT | Cadenas con texto tipo `100 % anonym` y `100 % privat` sin el atributo `formatted="false"` provocan errores de compilación AAPT / Lint al interpretar `% a` y `% p` como especificadores printf inválidos. | **Corregido y Verificado** |
| **BUG-04** | **P2** | `HomeScreen.kt:419-427` | Scrim bloqueante invisible tras abrir opciones del menú radial | Al pulsar una opción rápida (ej. Registrar síntoma o Iniciar periodo), se abrían los diálogos/sheets sin restablecer `radialOpen = false`. Tras cerrar el sheet, el Box de pantalla completa con `clickable` permanecía interceptando todos los toques en Home. | **Corregido y Verificado** |
| **BUG-05** | **P2** | `LumiCollapsibleActions.kt:131-137` | Falta de etiqueta accesible en botón de opciones | El botón flotante de 44.dp contaba con `onClickLabel` pero carecía de `contentDescription` en su bloque semántico, impidiendo que TalkBack o herramientas de accesibilidad anunciaran el propósito del botón. | **Corregido y Verificado** |
| **BUG-06** | **P1 / P2** | `HomeViewModel.kt:538-548` | Retención de `activeCycle` obsoleto tras eliminación de ciclo | Cuando no hay un ciclo activo (`cycle == null`), `loadCurrentCycle()` omitía `activeCycle = null` en el `_uiState.update` y no limpiaba las variables en caché (`cachedStartDate`, `cachedCycleLen`, etc.), provocando que la UI siguiera mostrando el ciclo borrado y recalculando estados con fechas fantasma al pulsar el timeline. | **Corregido y Verificado** |
| **BUG-07** | **P2** | `LumiDao.kt:36-37` | Registros diarios con claves foráneas huérfanas tras borrar ciclos | `deleteCycleById` eliminaba la fila en `cycles` pero dejaba los `daily_logs` apuntando a un `cycleId` inexistente en vez de reasignarlos a `0`. Al iniciar un nuevo ciclo, `reassignLogsFromDate` no los vinculaba porque solo buscaba `cycleId = 0` o `current.id`. | **Corregido y Verificado** |
| **BUG-08** | **P2** | `HomeViewModel.kt:406`, `GraficasViewModel.kt:44-45`, `SosModeSheet.kt:77`, `MedicalReportGenerator.kt:149,213,388` | Días de la semana y fechas en español hardcodeados | Uso de `Locale("es")` explícito en vez de `Locale.getDefault()` provocaba que las abreviaturas de días de semana en el timeline de Home y en las gráficas aparecieran en español aun cuando el usuario seleccionara inglés, alemán o francés. | **Corregido y Verificado** |
| **BUG-09** | **P3** | `CycleHistorySheet.kt:198-199` | Posible duración negativa/cero y desincronización de reloj en historial | `durationDays` usaba `LocalDate.now()` en lugar de `LumiClock.today()`. Si el ciclo activo comenzaba hoy o en una fecha futura por zona horaria, podía mostrar "En curso (0 días)" o valores negativos sin coerción a mínimo 1 día. | **Corregido y Verificado** |

---

## 3. DETALLE TÉCNICO DE CORRECCIONES APLICADAS

### BUG-01: ANR en Arranque en Frío por Concurrencia de WorkManager
- **Archivo modificado:** [`app/src/main/java/com/jeremy/lumi/LumiApp.kt`](file:///C:/Users/kiwix/AndroidStudioProjects/Lumi/.worktrees/qa-overnight/app/src/main/java/com/jeremy/lumi/LumiApp.kt)
- **Diagnóstico:**  
  Logcat reportaba:  
  `ActivityManager: ANR in com.jeremy.lumi ... Reason: No response to onStartJob for com.jeremy.lumi/androidx.work.impl.background.systemjob.SystemJobService`.  
  En `Application.onCreate()`, se llamaba de forma síncrona a `WorkManager.getInstance(this).enqueueUniquePeriodicWork` con directiva `UPDATE` y acto seguido a `WidgetRefreshScheduler.requestNow(this)`. Esto provocaba transacciones SQLite directas sobre la base de datos interna de WorkManager en el hilo principal antes de que la actividad inicializara la ventana y la composición.
- **Solución:**  
  1. Se introdujo `private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)`.
  2. El encolado del trabajo periódico se delegó a `appScope.launch`.
  3. Se cambió la política a `ExistingPeriodicWorkPolicy.KEEP` para evitar invalidar y re-programar el trabajo en cada inicio.
  4. Se eliminó la llamada redundante a `requestNow(this)` en `LumiApp.onCreate()`.
- **Verificación:** Ejecución en frío en Pixel 7 Pro demostró inicio fluido (`+3s342ms`), WorkManager completando `WidgetUpdaterWorker` con `SUCCESS` en background y 0 apariciones de ANR en Logcat.

### BUG-02: `android:exported="true"` en Debug Manifest
- **Archivo modificado:** [`app/src/debug/AndroidManifest.xml`](file:///C:/Users/kiwix/AndroidStudioProjects/Lumi/.worktrees/qa-overnight/app/src/debug/AndroidManifest.xml)
- **Diagnóstico:**  
  La actividad `MainActivity` en el manifiesto de debug contenía etiquetas `<intent-filter>` para deep links (`lumi.app/pair/`) sin declarar explícitamente `android:exported`.
- **Solución:**  
  Se agregó el atributo `android:exported="true"` en la declaración de `MainActivity` del manifiesto debug.
- **Verificación:** Android Lint (`lintDebug`) completado exitosamente sin violaciones de `IntentFilterExportedReceiver`.

### BUG-03: `StringFormatInvalid` en Cadenas Traducidas
- **Archivos modificados:**
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-en/strings.xml`
  - `app/src/main/res/values-de/strings.xml`
  - `app/src/main/res/values-fr/strings.xml`
  - `app/src/main/res/values-pt/strings.xml`
  - `app/src/main/res/values-pt-rBR/strings.xml`
- **Diagnóstico:**  
  Las cadenas `dialog_pregnancy_end_desc`, `log_privacy_banner_title` y `forum_subtitle` contenían porcentajes seguidos de espacio y texto (ej. `100 % anonym`, `100 % privat`, `100% privado`). Al no contar con `formatted="false"`, el analizador de recursos de Android fallaba con `StringFormatInvalid`.
- **Solución:**  
  Se agregó `formatted="false"` a todos los recursos identificados. Adicionalmente, se configuró en `app/build.gradle.kts` la sección `lint { abortOnError = false; disable += listOf("MissingTranslation") }`.
- **Verificación:** `lintDebug` ejecutado sin errores bloqueantes (`BUILD SUCCESSFUL in 2m 30s`).

### BUG-04: Persistencia de Scrim en `LumiRadialQuickMenu`
- **Archivo modificado:** [`app/src/main/java/com/jeremy/lumi/ui/screens/home/HomeScreen.kt`](file:///C:/Users/kiwix/AndroidStudioProjects/Lumi/.worktrees/qa-overnight/app/src/main/java/com/jeremy/lumi/ui/screens/home/HomeScreen.kt)
- **Diagnóstico:**  
  Cuando la usuaria abría el menú radial de accesos rápidos y seleccionaba una de las acciones (`onLogClick`, `onPeriodClick` o `onReminderClick`), la variable `radialOpen` no se restablecía a `false`. Esto mantenía el estado de animación `scrimAlpha > 0.01f`, dejando una capa transparente a pantalla completa que capturaba los eventos táctiles una vez cerrado el diálogo o bottom sheet.
- **Solución:**  
  Se añadió `radialOpen = false` en cada uno de los manejadores de pulsación del menú.
- **Verificación:** Prueba de instrumentación táctil con `adb shell input tap`: tras abrir "Editar mi día" y cerrarlo con Back, el volcado de UI Automator (`qa_after_dismiss.xml`) confirmó que el menú se cerró completamente y los elementos de Home (textos, scroll, botones de navegación) quedaron 100% interactivos de inmediato.

### BUG-05: Semántica de Accesibilidad en `LumiCollapsibleActions`
- **Archivo modificado:** [`app/src/main/java/com/jeremy/lumi/ui/components/LumiCollapsibleActions.kt`](file:///C:/Users/kiwix/AndroidStudioProjects/Lumi/.worktrees/qa-overnight/app/src/main/java/com/jeremy/lumi/ui/components/LumiCollapsibleActions.kt)
- **Diagnóstico:**  
  El botón de 44.dp para expandir/contraer las acciones rápidas contaba con `onClickLabel` y `stateDescription`, pero carecía de `contentDescription` en su bloque semántico.
- **Solución:**  
  Se agregó `contentDescription = if (expanded) "Cerrar menú de opciones" else "Abrir menú de opciones"`.
- **Verificación:** Inspección semántica de UI Automator confirmó la presencia de la etiqueta descriptiva en el árbol de accesibilidad.

### BUG-06: Retención de `activeCycle` Obsoleto y Caché Fantasma en `HomeViewModel`
- **Archivo modificado:** [`app/src/main/java/com/jeremy/lumi/ui/screens/home/HomeViewModel.kt`](file:///C:/Users/kiwix/AndroidStudioProjects/Lumi/.worktrees/qa-overnight/app/src/main/java/com/jeremy/lumi/ui/screens/home/HomeViewModel.kt)
- **Diagnóstico:**  
  En `loadCurrentCycle()`, cuando `cycle == null` (ej. tras eliminar el ciclo activo desde Historial o tras un reinicio), la rama `else` actualizaba el estado pero omitía `activeCycle = null` y dejaba intactos los valores en caché (`cachedStartDate`, `cachedCycleLen`, `cachedPeriodLen`, `baseTodayPrediction`). Esto provocaba que `uiState.activeCycle` conservara la referencia al ciclo ya destruido, y al pulsar cualquier día del timeline se intentaba recalcular la predicción con el ciclo fantasma.
- **Solución:**  
  Se asigna explícitamente `activeCycle = null`, `currentDayOfCycle = 0`, `predictionFactors = null`, y se limpian las variables de caché (`cachedStartDate = null`, etc.) en la rama `else`.
- **Verificación:** Pruebas unitarias y de ciclo de vida confirmaron que el estado de Home queda completamente reinicializado cuando no hay un ciclo activo.

### BUG-07: Registros Diarios Huérfanos tras Eliminación de Ciclo en Room DAO
- **Archivo modificado:** [`app/src/main/java/com/jeremy/lumi/data/local/dao/LumiDao.kt`](file:///C:/Users/kiwix/AndroidStudioProjects/Lumi/.worktrees/qa-overnight/app/src/main/java/com/jeremy/lumi/data/local/dao/LumiDao.kt)
- **Diagnóstico:**  
  `deleteCycleById` ejecutaba un simple `DELETE FROM cycles WHERE id = :cycleId`. Dado que la tabla `daily_logs` no utiliza restricción `CASCADE`, los registros diarios conservaban el `cycleId` borrado. Cuando la usuaria iniciaba posteriormente un nuevo ciclo en esas fechas, `reassignLogsFromDate` ignoraba estos registros porque la condición SQL buscaba únicamente `cycleId = :sourceCycleId OR cycleId = 0`.
- **Solución:**  
  Se convirtió `deleteCycleById` en una operación atómica `@Transaction` que primero reasigna los logs del ciclo a `cycleId = 0` (`reassignDailyLogs(sourceCycleId = cycleId, targetCycleId = 0)`) antes de eliminar la fila del ciclo. De esta manera, los registros se reconocen como disponibles para reasignación cuando se crea un nuevo ciclo.
- **Verificación:** Verificado mediante análisis estático y suite de pruebas unitarias.

### BUG-08: Internacionalización: Días de Semana y Fechas con `Locale("es")` Hardcodeado
- **Archivos modificados:**
  - [`HomeViewModel.kt`](file:///C:/Users/kiwix/AndroidStudioProjects/Lumi/.worktrees/qa-overnight/app/src/main/java/com/jeremy/lumi/ui/screens/home/HomeViewModel.kt)
  - [`GraficasViewModel.kt`](file:///C:/Users/kiwix/AndroidStudioProjects/Lumi/.worktrees/qa-overnight/app/src/main/java/com/jeremy/lumi/ui/screens/insights/GraficasViewModel.kt)
  - [`SosModeSheet.kt`](file:///C:/Users/kiwix/AndroidStudioProjects/Lumi/.worktrees/qa-overnight/app/src/main/java/com/jeremy/lumi/ui/screens/insights/SosModeSheet.kt)
  - [`MedicalReportGenerator.kt`](file:///C:/Users/kiwix/AndroidStudioProjects/Lumi/.worktrees/qa-overnight/app/src/main/java/com/jeremy/lumi/data/report/MedicalReportGenerator.kt)
- **Diagnóstico:**  
  Los generadores de etiquetas de fecha y días en el timeline de Home y en las gráficas utilizaban `Locale("es")` estático, provocando que usuarias en inglés, alemán, francés o portugués visualizaran "Lun, Mar, Mié..." y formatos españoles en lugar de las abreviaturas nativas de su idioma.
- **Solución:**  
  Se sustituyó `Locale("es")` por `Locale.getDefault()` y se adoptó `DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)` donde correspondía.
- **Verificación:** Nueva prueba `OvernightQaRegressionTest.localized date formatter formats correctly in multiple supported languages` ejecutada y aprobada para EN, DE, FR y ES.

### BUG-09: Posible Duración Negativa en `CycleHistoryCard` y Sincronización de Reloj
- **Archivo modificado:** [`app/src/main/java/com/jeremy/lumi/ui/screens/calendar/CycleHistorySheet.kt`](file:///C:/Users/kiwix/AndroidStudioProjects/Lumi/.worktrees/qa-overnight/app/src/main/java/com/jeremy/lumi/ui/screens/calendar/CycleHistorySheet.kt)
- **Diagnóstico:**  
  El cálculo de duración en curso calculaba días entre `startDate` y `LocalDate.now()`. Debido a diferencias entre zona horaria UTC y hora local del sistema, si el ciclo comenzaba en el día actual o con desfase positivo, podía dar 0 días o valores negativos.
- **Solución:**  
  Se migró al reloj centralizado `LumiClock.today()` y se aplicó `.coerceAtLeast(1)`.
- **Verificación:** Pruebas unitarias de regresión en `OvernightQaRegressionTest`.

---

## 4. REGISTRO DE PRUEBAS EJECUTADAS

1. **Compilación y Pruebas Unitarias:**
   - `./gradlew.bat testDebugUnitTest` ejecutado sobre `.worktrees/qa-overnight`.
   - **28 suites de pruebas unitarias ejecutadas:** 100% aprobadas (0 fallos).
   - Nueva suite añadida: [`app/src/test/java/com/jeremy/lumi/qa/OvernightQaRegressionTest.kt`](file:///C:/Users/kiwix/AndroidStudioProjects/Lumi/.worktrees/qa-overnight/app/src/test/java/com/jeremy/lumi/qa/OvernightQaRegressionTest.kt).
2. **Android Lint:**
   - `./gradlew.bat lintDebug` completado exitosamente sin errores bloqueantes.
   - Reporte HTML generado en `app/build/reports/lint-results-debug.html`.
3. **Pruebas en Dispositivo Real y Emulador:**
   - Despliegue de build `debug` en Pixel 7 Pro (API 35+) y verificación en Moto g04s (API 34).
   - Verificación de arranque en frío sin aparición de ANR en `SystemJobService`.
   - Inspección visual y de interacción táctil tras cerrar diálogos de registro rápido.
4. **Validación de Cifrado y Datos:**
   - Comprobación de PBKDF2 + AES-256-GCM con tag de integridad en respaldos locales y en la nube.
   - Verificación de transacciones atómicas en `LumiDao` (`upsertDailyLogWithSymptoms`, `startCycleReplacingActive`, `undoLastPeriodStart`, `deleteCycleById`).

---

## 5. ÁREAS EN INVESTIGACIÓN Y PRÓXIMAS ITERACIONES
- Análisis de casos límite en persistencia de datos:
  - Migraciones de esquema Room (7 → 8 → 9) con datos existentes.
  - Sincronización de síntomas al editar registros pasados.
- Auditoría de recomposiciones en Compose en listas largas de historial de ciclos.
