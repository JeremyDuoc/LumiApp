# Registro de Auditoría y Optimización Autónoma - Lumi

**Fecha de inicio:** 14 de octubre de 2026
**Rama:** `audit/codebase-autonomous-cleanup`
**Auditor:** `android_code_auditor` (Subagente Senior Android)
**Ejecutor:** Antigravity (Fixer)

---

## Estado del Sistema
- **Línea base de compilación:** Aprobada (`BUILD SUCCESSFUL`).
- **Recursos del sistema:** Liberados ~1.8 GB de memoria cerrando daemons inactivos de Gradle.
- **Canal de notificaciones:** Discord Webhook activo (`scripts/notify_discord.ps1`).

---

## Registro por Paquetes

### 1. `ui/screens` (Advertencias de compilación e Iconos Deprecados)
- **Estado:** ✅ APROBADO Y CONFIRMADO (`BUILD SUCCESSFUL in 40s`, 0 advertencias del compilador).
- **Commit:** `ef1ed52` - `fix(ui): eliminate compiler warnings and migrate deprecated Material icons`
- **Cambios realizados:**
  - `HomeScreen.kt`: Eliminada condición redundante (`uiState.currentPhase != null`) que siempre evaluaba a `true`.
  - `BondsSendGestureScreen.kt`: Migrado `Icons.Rounded.Send` a `Icons.AutoMirrored.Rounded.Send`.
  - `InsightsScreen.kt`: Migrado `Icons.Rounded.KeyboardArrowRight` a `Icons.AutoMirrored.Rounded.KeyboardArrowRight`.
  - `OnboardingScreen.kt`: Migrado `Icons.Rounded.HelpOutline` a `Icons.AutoMirrored.Rounded.HelpOutline`.
  - `BackupSettingsScreen.kt`: Migrados `Icons.Rounded.Logout` y `Icons.Rounded.HelpOutline` a sus versiones `AutoMirrored`.

### 2. `domain/usecase` (`CyclePredictor.kt` & `LumiRulesEngine.kt`)
- **Estado:** ✅ APROBADO Y CORREGIDO (`BUILD SUCCESSFUL in 33s`, Tests unitarios pasados `testDebugUnitTest`).
- **Commit:** `16929a7` - `fix(domain): fix ovulation phase calculation, pregnancy safeguards, and contextual rules engine`
- **Bugs Críticos y Mejoras Aplicadas:**
  - `CyclePredictor.kt`: Corregido bug crítico de cálculo en `dayOfPhase` de ovulación (estaba desfasado con la ventana OMS de 7 días y se congelaba en día 1 durante 5 días).
  - `CyclePredictor.kt`: Protegido cálculo de `dayOfPhase` en embarazo ante desfases horarios o fechas de inicio futuras (`.coerceAtLeast(1)`).
  - `CyclePredictor.kt`: Agregada verificación de continuidad temporal en `detectBiphasicShift` para evitar falsos positivos ante vacíos de registro de temperatura.
  - `CyclePredictor.kt`: Eliminado import huérfano `ZoneId` y función de extensión privada muerta `toLocalDate()`.
  - `LumiRulesEngine.kt`: Corregido mensaje incongruente de SPM (evita afirmar que es normal sentirse "feliz" por la caída hormonal; ahora valida emociones del SPM).
  - `LumiRulesEngine.kt`: Protegida alerta de dolor severo (`todayPainLevel >= 7`) para que no sea ignorada por retornos tempranos.
  - `LumiRulesEngine.kt`: Prevenido el formateo de días negativos en `answerNextPeriod` y manejado el estado de embarazo en fertilidad y selección de preguntas.
  - `LumiRulesEngine.kt`: Conectadas las respuestas contextuales para alivio de cefaleas y nutrición según la fase activa del ciclo.
  - `LumiRulesEngine.kt`: Reemplazado `.random()` por `.randomOrNull() ?: ""` para prevenir crashes fatales por `NoSuchElementException`.

### 3. `data/local` & `data/repository` (`LumiDao.kt` & `LumiRepositoryImpl.kt`)
- **Estado:** ✅ APROBADO Y OPTIMIZADO (`BUILD SUCCESSFUL in 34s`, Tests unitarios pasados `testDebugUnitTest in 52s`).
- **Commit:** `3ee5e2c` - `refactor(data): atomic cycle mutations, optimized upsertDailyLog, and main-safety in LumiRepository`
- **Vulnerabilidades Resueltas:**
  - `LumiRepositoryImpl.kt`: Corregida condición de carrera y falta de atomicidad en `endCurrentCycle`. Se reemplazó el uso de `insertCycle` (REPLACE destructivo) por un método atómico transaccional `@Transaction closeActiveCycle(...)` con `@Update`.
  - `LumiRepositoryImpl.kt`: Implementado **Main-Safety** exhaustivo en todos los métodos `suspend` inyectando `ioDispatcher: CoroutineDispatcher = Dispatchers.IO` y envolviendo en `withContext(ioDispatcher)`.
  - `LumiDao.kt`: Optimizada drásticamente la función `upsertDailyLogWithSymptoms`. Se eliminó la carga masiva y mapeo relacional de `DailyLogWithSymptoms`, reemplazándola por una consulta escalar directa `@Query getDailyLogIdByDate(date)`.
  - `LumiDao.kt`: Prevenida colisión de IDs en reinserción de síntomas reseteando `id = 0` al mapear la lista (`it.copy(id = 0, dailyLogId = resolvedLogId)`).
  - `LumiDao.kt`: Corregido cuello de botella en `undoLastPeriodStart()` que cargaba todos los ciclos de la historia del usuario en memoria; ahora utiliza `@Query getPreviousClosedCycle() ... LIMIT 1`.
  - `LumiDao.kt`: Agregada validación de días calendario contiguos (`if (!next.isAfter(start)) return`) en `startCycleReplacingActive` para prevenir ciclos con duración negativa ante milisegundos anómalos.

### 4. `ui/components` & Compose Performance (`LumiCycleRing`, `LumiTodayCard`, `LumiWeekStrip`, `HomeUiState`)
- **Estado:** ✅ APROBADO Y OPTIMIZADO (`BUILD SUCCESSFUL in 1m 9s`, Tests unitarios pasados `testDebugUnitTest in 1m 8s`).
- **Commit:** `ad352d4` - `perf(ui): optimize Compose allocations, cache brushes and shapes, and mark UI state @Immutable`
- **Optimizaciones de Renderizado y Estabilidad:**
  - `HomeUiState.kt`: Anotadas las data classes de presentación (`HomeUiState`, `PredictionFactorsUi`, `CycleDayUi`) con `@Immutable` de Compose runtime, garantizando que el compilador de Compose omita recomposiciones superfluas de árboles completos cuando las referencias no mutan.
  - `LumiCycleRing.kt`: Extraídas constantes estáticas de color (`DotColor`, `DotAuraColor`) fuera de la función de renderizado; cacheados los colores de resplandor `glowColors` mediante `remember(tokens.glow)`, previniendo alocaciones de `Brush` y listas de `Color` en cada frame del bucle de animación a 60/120 FPS.
  - `LumiTodayCard.kt`: Cacheados el gradiente de fondo (`Brush.linearGradient`) mediante `remember(tokens.panel, tokens.panel2)` y la forma redondeada `cardShape = remember { RoundedCornerShape(24.dp) }`, eliminando instanciaciones repetidas de objetos gráficos en cada recomposición.
  - `LumiWeekStrip.kt`: Cacheada la forma `dayShape = remember { RoundedCornerShape(16.dp) }` y centralizado `SpanishLocale = Locale("es")` como constante de nivel superior, erradicando alocaciones masivas de shapes en el renderizado de cada día de la semana.
  - `LumiTopBar.kt`: Reemplazados modificadores dinámicos separados (`scale`, `alpha`, `rotate`) con un único bloque `Modifier.graphicsLayer` impulsado por keyframes para la animación del logo, optimizando el canal de composición y GPU; reutilizada instancia de `Path` mediante `remember { Path() }`.

### 5. `data/preferences` & `data/backup` (Seguridad, Concurrencia y Robustez)
- **Estado:** ✅ APROBADO Y BLINDADO (`BUILD SUCCESSFUL in 1m 14s`, 124 tests unitarios pasados `testDebugUnitTest in 1m 6s`).
- **Commit:** `f417eae` - `fix(data): harden DataStore I/O, sync sqlite_sequence on restore, and enforce cryptographic constraints`
- **Vulnerabilidades y Bugs Críticos Corregidos:**
  - `ThemePreferenceManager.kt`: Resuelto bug crítico de truncado donde `selectedThemeFlow` forzaba `NOIR` en más de 30 temas visuales de la app; y `selectedPhasePaletteFlow` eliminaba la paleta `CUSTOM` impidiendo la personalización de colores de fase.
  - `ThemePreferenceManager.kt`, `OnboardingPreferenceManager.kt`, `ChatPreferenceManager.kt`, `BackupPreferenceManager.kt`, `LanguagePreferenceManager.kt`: Implementado `safeData` mediante `.catch { if (it is IOException) emit(emptyPreferences()) else throw it }` en más de 30 flujos de DataStore, blindando la app frente a crashes por corrupción o fallos de lectura I/O en almacenamiento.
  - `BackupManager.kt`: Corregida desincronización de `sqlite_sequence` tras la importación del archivo de respaldo; se sincroniza explícitamente el valor autoincremental de cada tabla para evitar `SQLiteConstraintException: UNIQUE constraint failed` en inserciones subsiguientes.
  - `BackupManager.kt`: Notificado el `database.invalidationTracker.refreshVersionsAsync()` tras la transacción de importación para que los flujos activos y observadores de Room DAOs en la UI actualicen sus datos inmediatamente.
  - `BackupManager.kt`: Prevenida colisión TOCTOU creando directorios temporales de forma atómica (`File(cacheDir, "lumi-archive-${UUID}").mkdirs()`) y añadida cláusula `AND sql IS NOT NULL` en la consulta de tablas.
  - `BackupCrypto.kt`: Agregada validación estricta de longitud de salt (`require(salt.size == 16)`), iteraciones mínimas (`>= 50_000`), verificación de longitud mínima del archivo binario y captura de `GeneralSecurityException` en descifrado; implementado método de borrado seguro en memoria `DerivedKey.clear()`.
  - `BackupSerializer.kt`: Añadida validación de rangos biológicos en longitudes de ciclo (`15..120` días) y periodo (`1..20` días), limitación de niveles de dolor (`0..10`) e intensidad (`0..5`), y captura combinada de `JsonSyntaxException` y `IllegalStateException`.
  - `BackupPreferenceManager.kt` & `ChatPreferenceManager.kt`: Reubicadas las claves estáticas de DataStore en sus respectivos `companion object`.

### 6. `ui/screens` & `ui/viewmodel` (Fugas de memoria, ciclo de vida y Compose Recomposition)
- **Estado:** ✅ APROBADO Y OPTIMIZADO (`BUILD SUCCESSFUL in 1m 8s`, 124 tests unitarios pasados `testDebugUnitTest in 53s`).
- **Archivos Auditados y Optimizados:**
  - `ChatViewModel.kt`, `ChatScreen.kt`
  - `HomeViewModel.kt`, `HomeScreen.kt`, `RemindersViewModel.kt`
- **Bugs Críticos y Cuellos de Botella Erradicados:**
  - `ChatViewModel.kt`:
    - **Protección contra bloqueo permanente de escritura:** Envuelto el ciclo de vida del indicador `_isTyping` en bloques `try-finally { _isTyping.value = false }`, evitando que fallos inesperados o cancelaciones de corrutina dejen el chat bloqueado indefinidamente.
    - **Prevención de carreras de pulsación doble:** Se conmuta `_isTyping.value = true` de manera síncrona en el hilo principal antes de lanzar `viewModelScope.launch`, rechazando envíos concurrentes duplicados.
    - **Re-lanzamiento de CancellationException:** Se propaga `CancellationException` de corrutinas para respetar la cancelación estructural de Kotlin.
    - **Mensaje de bienvenida inicial robusto:** Si el chat está vacío y aún no existe un ciclo activo cargado, se emite un mensaje de bienvenida contextual y empático en lugar de fallar silenciosamente.
  - `ChatScreen.kt`:
    - **Eliminación de bucle infinito de recomposición:** Removido el pulsador continuo `rememberInfiniteTransition` dentro de cada burbuja `PremiumChatBubble`, eliminando recomposiciones permanentes a 60/120 FPS en el hilo de UI.
    - **Solución al parpadeo de reciclaje en LazyColumn:** Restringida la animación de entrada en `AnimatedChatBubble` a mensajes con antigüedad menor a 3.5 segundos (`System.currentTimeMillis() - message.timestamp < 3500`), previniendo saltos y re-animaciones molestas al desplazarse por el historial.
    - **Micro-optimizaciones de parsing:** Centralizada la expresión regular de tokens markdown `MARKDOWN_TOKEN_REGEX` en nivel superior como `Regex`, evitando recompilaciones de Regex en cada frame o mensaje.
  - `HomeViewModel.kt`:
    - **Cancelación de carreras de carga:** Implementado `loadCycleJob?.cancel()` para evitar ejecuciones concurrentes desordenadas de `loadCurrentCycle()` disparadas simultáneamente por cambios en reloj, embarazo o anticonceptivos.
    - **Offload de cálculo pesado a segundo plano:** Reubicados el mapeo del timeline continuo de 112 días (`weekDays`) y el cálculo estadístico de factores de predicción a `withContext(Dispatchers.Default)`, liberando el hilo principal de caídas de frames al navegar al Home.
    - **Limpieza de dependencias y código muerto:** Removida dependencia no utilizada `aiPredictor: LumiAIPredictor` del constructor e importación; eliminado método residual de depuración `generateRandomSymptomsForToday()`.
  - `RemindersViewModel.kt` & `HomeScreen.kt`:
    - **Ruptura de bucle infinito de invalidación de Room:** En `RemindersViewModel.syncCycleReminders()`, agregado un guard de verificación `if (reminder.nextTriggerAt != triggerMillis)` antes de invocar `repository.updateNextTrigger()`, eliminando escrituras innecesarias en SQLite y re-programaciones redundantes del `AlarmScheduler`.
    - **Desacoplamiento de claves en HomeScreen:** Simplificado `LaunchedEffect(uiState.prediction)` para que dependa exclusivamente del objeto de predicción y no de hashes volátiles de la lista de recordatorios activos.
    - **Elevación de ViewModels (Hoisting):** Promovidos `quickLogViewModel` y `partnerViewModel` como parámetros con valores por defecto de la función composable `HomeScreen`, erradicando llamadas directas a `hiltViewModel()` dentro del cuerpo o ramas de la pantalla.
    - **Estructuración limpia de navegación:** Reemplazados los retornos tempranos (`return` en Compose) por una expresión idiomática exhaustiva `when (uiState.homeMode) { NORMAL, OBSERVER_GATE, OBSERVER_HOME }`.
    - **Eliminación de subscripción duplicada:** Removida la doble recolección de `todayLog` dentro de `showQuickLogSheet`, reutilizando el estado recolectado en el nivel superior.

---




### 7. Integración con el Sistema, Alarmas, Notificaciones, IA y DI (`reminders`, `receiver`, `notification`, `ai`, `di`, `utils`)
- **Estado:** ✅ APROBADO Y BLINDADO (`BUILD SUCCESSFUL in 1m 43s`, 124 tests unitarios pasados `testDebugUnitTest in 44s`).
- **Archivos Auditados y Optimizados:**
  - `AndroidManifest.xml`
  - `AlarmScheduler.kt`, `BootReceiver.kt`, `ReminderAlarmReceiver.kt`
  - `FcmService.kt`, `NotificationChannels.kt`, `LumiApp.kt`
  - `CyclePredictor.kt` (ai), `AIModule.kt`, `AppModule.kt`
  - `LocaleHelper.kt`, `MainActivity.kt`, `LumiClock.kt`
- **Bugs Críticos y Optimizaciones de Plataforma:**
  - `AndroidManifest.xml`: Exportado explícitamente `BootReceiver` con `android:exported="true"`, garantizando que el sistema operativo (`system_server`) entregue los broadcasts `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED` y `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`, previniendo la pérdida total silenciosa de alarmas tras reiniciar el dispositivo.
  - `AlarmScheduler.kt`:
    - **Verificación proactiva de alarmas exactas:** Verificado `canScheduleExactAlarms()` en Android 12+ (API 31+) antes de invocar `setExactAndAllowWhileIdle()`, evitando caídas por `SecurityException` no controlada.
    - **Fallback de Doze Mode:** Reemplazado `alarmManager.set(...)` por `setAndAllowWhileIdle(...)` en el fallback sin permiso exacto, garantizando que recordatorios críticos (pastilla, métodos anticonceptivos) despierten al dispositivo incluso en reposo profundo.
    - **Corrección de fechas pasadas:** Corregido `computeNextTrigger` para que avance automáticamente al día siguiente si la hora ya venció hoy, evitando descartar el recordatorio.
  - `LocaleHelper.kt` & `MainActivity.kt`:
    - **Prevención de doble recreación de Activity:** En Android 13+ (API 33+), se delega exclusivamente a `LocaleManager.applicationLocales` retornando inmediatamente, previniendo la colisión con la API deprecada y la doble recreación abrupta de la Activity.
    - **Optimización de arranque en frío:** Omitido `runBlocking` en `attachBaseContext` en Android 13+ donde el sistema gestiona los recursos de idioma por app de forma nativa.
  - `BootReceiver.kt` & `ReminderAlarmReceiver.kt`:
    - **Blindaje de corrutinas asíncronas:** Añadidos bloques `catch (e: Exception)` con registro seguro en Logcat en las corrutinas `goAsync()`, previniendo crashes en segundo plano si la base de datos Room o el programador de alarmas se bloquean durante el inicio del sistema.
  - `CyclePredictor.kt` (AI / LiteRT):
    - **Prevención de fuga de memoria nativa (C++ Heap):** En el constructor `init`, si `loadScalerParams()` falla, se cierra y anula explícitamente `interpreter?.close()`, evitando fugar el contexto nativo de TensorFlow Lite.
    - **Solución a error EBADF:** En `loadModelFile()`, reemplazado el cierre prematuro del descriptor APK por `fd.createInputStream().use`, garantizando un mapeo seguro de memoria en `FileChannel`.
    - **Limpieza de código muerto:** Eliminados imports obsoletos de `java.time` y `FileInputStream`.
  - `FcmService.kt` & `NotificationChannels.kt`:
    - **Ciclo de vida y fuga de corrutinas:** Implementado `onDestroy()` con `serviceScope.cancel()` en `FcmService`.
    - **Permiso POST_NOTIFICATIONS:** Añadida verificación de `Manifest.permission.POST_NOTIFICATIONS` en Android 13+ antes de despachar notificaciones push.
    - **Unificación de canales:** Integrado el canal `partner_channel` dentro de `LumiNotificationChannel.PARTNER` y normalizado el paquete a `com.jeremy.lumi.notification`.
  - `di/AIModule.kt` & `di/AppModule.kt`:
    - **Simplificación idiomática de Dagger Hilt:** Anotado `LumiAIPredictor` con `@Inject constructor` y refactorizado `AIModule` a clase abstracta con `@Binds`; eliminados métodos `@Provides` redundantes en `AppModule` que duplicaban constructores inyectables.
  - `LumiClock.kt`:
    - Documentado explícitamente `todayUtcMillis()` como la clave de partición canónica a medianoche UTC para la base de datos Room.

---
