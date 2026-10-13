# Matriz de Hallazgos y Trazabilidad Técnica — Lumi Bonds
**Fecha de auditoría:** 12 de octubre de 2026  
**Auditor:** Principal Android Software Auditor & Security Specialist  
**Entorno de evaluación:** Repositorio real Lumi Android (Kotlin 1.9.22, Jetpack Compose, Room 2.6.1, Firebase Firestore Emulator, JDK 21 / OpenJDK 17)

---

## 1. Clasificación Rigurosa por Método de Verificación

Para garantizar absoluta transparencia con el Segundo Auditor Independiente, cada hallazgo se clasifica formalmente según el método con el que fue demostrado:

1. **PRUEBA AUTOMATIZADA (FIREBASE EMULATOR):** Ejecutada contra el emulador oficial de Firestore con aserciones de seguridad (`assertSucceeds`, `assertFails`) en contratos RPC reales.
2. **PRUEBA AUTOMATIZADA (JVM KOTLIN):** Ejecutada con Gradle JUnit en la JVM sin mocks que alteren la lógica de negocio.
3. **EVIDENCIA VISUAL DINÁMICA (EMULADOR ANDROID):** Verificada en tiempo de ejecución en emulador Android Pixel 8 API 34 con capturas de pantalla de la interfaz.
4. **ANÁLISIS ESTÁTICO DE CÓDIGO Y TRAZA:** Verificada mediante inspección profunda del árbol sintáctico, dependencias y rutas de ejecución en el código fuente.

---

## 2. Matriz Resumen de Hallazgos y Trazabilidad

| ID | Título del Hallazgo | Severidad | Método de Verificación | Estado | Artefacto de Prueba / Evidencia |
|---|---|---|---|---|---|
| **P0-1** | Datos ficticios en producción y simulación de gestos | **P0 (Crítico)** | **ANÁLISIS ESTÁTICO DE CÓDIGO** | **CONFIRMADO** | `BondsViewModel.kt:258, 318–330` (Bypass `delay(250)`) |
| **P0-2** | Revocación no bloquea lectura remota en Firestore | **P0 (Crítico)** | **PRUEBA AUTOMATIZADA (FIREBASE EMULATOR)** | **CONFIRMADO** | `audit_security_verification.test.cjs:47–71` (`assertSucceeds` en revocado) |
| **P0-3** | Incompatibilidad en canje de invitaciones (`get` en `PENDING`) | **P0 (Crítico)** | **PRUEBA AUTOMATIZADA (FIREBASE EMULATOR)** | **CONFIRMADO** | `audit_security_verification.test.cjs:73–85` (`PERMISSION_DENIED` en get) |
| **P0-4** | Compartición de datos sensibles activada por defecto | **P0 (Crítico)** | **PRUEBA AUTOMATIZADA (JVM KOTLIN)** | **CONFIRMADO** | `BondsPrivacyInferenceTest.kt:15–28` (`SharePreferences` defaults) |
| **P1-5** | Riesgo de enumeración en códigos de invitación de 6 dígitos | **P1 (Alto)** | **PRUEBA AUTOMATIZADA (FIREBASE EMULATOR)** | **CONFIRMADO** | `audit_security_verification.test.cjs:87–100` (`getDoc` sin rate limit) |
| **P1-6** | Inferencia indirecta de fases menstruales vía «Predicciones» | **P1 (Alto)** | **PRUEBA AUTOMATIZADA (JVM KOTLIN)** | **CONFIRMADO** | `BondsPrivacyInferenceTest.kt:30–48` (Reconstrucción matemática) |
| **P1-7** | Navegación duplicada y ViewModels desvinculados | **P1 (Alto)** | **ANÁLISIS ESTÁTICO DE CÓDIGO** | **CONFIRMADO** | `MainScreen.kt:457–471`, `AppRoutes.kt:25` (Rutas planas sin shared VM) |
| **P1-8** | Confirmación de gesto no equivale a entrega ni recepción | **P1 (Alto)** | **ANÁLISIS ESTÁTICO DE CÓDIGO** | **CONFIRMADO** | `PartnerRepositoryImpl.kt:359–366`, `BondsViewModel.kt:339` |
| **P1-9** | Conflicto de contraste de StatusBar en modo oscuro | **P1 (Alto)** | **EVIDENCIA VISUAL (EMULADOR ANDROID)** | **CONFIRMADO** | `screenshots/screenshot_bonds_home.png`, `Theme.kt:527` |
| **P1-10**| Proyección de calendario compartido reconstruye el pasado | **P1 (Alto)** | **ANÁLISIS ESTÁTICO DE CÓDIGO** | **CONFIRMADO** | `BondsSharedCalendarScreen.kt:507–515` (Módulo `% cycleLength`) |
| **P2-11**| Componentes/Repositorios Bonds sin integración efectiva | **P2 (Medio)** | **ANÁLISIS ESTÁTICO DE CÓDIGO** | **CONFIRMADO** | `BondsRepositoryImpl.kt:28–32` (`emptyList()`), `LumiDatabase.kt` |
| **P2-12**| Mensajes de ánimo ficticios por defecto y textos sin traducir | **P2 (Medio)** | **ANÁLISIS ESTÁTICO DE CÓDIGO** | **CONFIRMADO** | `BondsHomeScreen.kt:543–552`, `strings_bonds.xml:5` |
| **P2-13**| Generación automática de código sin control de error | **P2 (Medio)** | **ANÁLISIS ESTÁTICO DE CÓDIGO** | **CONFIRMADO** | `BondsPairingPrivacyScreen.kt:127–139` (`LaunchedEffect` sin catch) |
| **NEW-1**| Configuración de KSP y dependencias de Room | **P2 (Medio)** | **ANÁLISIS ESTÁTICO DE GRADLE** | **CONFIRMADO** | `app/build.gradle.kts:181`, logs de compilación KSP |
| **NEW-2**| Falta de `statusBarsPadding` en Header de Bonds (Edge-to-edge) | **P2 (Medio)** | **EVIDENCIA VISUAL (EMULADOR ANDROID)** | **CONFIRMADO** | `screenshots/screenshot_pairing_opened.png`, `BondsHomeScreen.kt` |

---

## 3. Detalle Exhaustivo de Trazabilidad por Hallazgo

### [P0-1] Datos ficticios en producción y simulación de envío
- **Método de verificación:** ANÁLISIS ESTÁTICO DE CÓDIGO Y TRAZA DE EJECUCIÓN.
- **Archivos y líneas:** `BondsViewModel.kt` (Líneas 102–184, 258, 318–330, 430–440, 465–476).
- **Evidencia en código:**
  ```kotlin
  // Línea 258: Inyección directa de mocks en flujo de producción
  val resolvedMembers = if (memberList.isNotEmpty()) memberList else SAMPLE_MEMBERS

  // Líneas 318-324: Simulación artificial de envío de gesto
  if (linkId.startsWith("sample_")) {
      delay(250)
      onSuccess()
      return
  }
  ```
- **Limitación:** No se ejecutó en dos teléfonos físicos reales intercambiando datos (no aplica por ser código mock local).
- **Veredicto:** CONFIRMADO.

---

### [P0-2] Revocación no bloquea lectura remota de datos en Firestore
- **Método de verificación:** PRUEBA AUTOMATIZADA (FIREBASE EMULATOR).
- **Archivo de prueba ejecutado:** `tools/firestore-tests/audit_security_verification.test.cjs` (Líneas 47–71).
- **Log de ejecución:** `audit-results/logs/firestore_security_verification.log`.
- **Resultado empírico:**
  ```javascript
  ✔ [P0-2 CONFIRMADO] Un participante revocado TODAVÍA puede leer los datos de salud del documento (2317.595ms)
  ```
  La llamada `getDoc` sobre el vínculo en estado `REVOKED` retornó exitosamente (`assertSucceeds`) con los datos médicos íntegros de la usuaria.
- **Veredicto:** CONFIRMADO.

---

### [P0-3] Incompatibilidad en canje de invitaciones (`get` en `PENDING`)
- **Método de verificación:** PRUEBA AUTOMATIZADA (FIREBASE EMULATOR).
- **Archivo de prueba ejecutado:** `tools/firestore-tests/audit_security_verification.test.cjs` (Líneas 73–85).
- **Log de ejecución:** `audit-results/logs/firestore_security_verification.log`.
- **Resultado empírico:**
  ```javascript
  ✔ [P0-3 CONFIRMADO] El futuro participante NO puede hacer get(linkRef) mientras está PENDING (95.2876ms)
  ```
  La regla de `firestore.rules` rechaza con `PERMISSION_DENIED` la lectura previa que hace la transacción de Android (`PartnerRepositoryImpl.kt:268`), abortando el canje.
- **Veredicto:** CONFIRMADO.

---

### [P0-4] Compartición de datos sensibles activada por defecto (Opt-out)
- **Método de verificación:** PRUEBA AUTOMATIZADA (JVM KOTLIN).
- **Archivo de prueba ejecutado:** `app/src/test/java/com/jeremy/lumi/domain/usecase/BondsPrivacyInferenceTest.kt` (`testP04_defaultPrivacyFlagsAreTrueByDefault`).
- **Log de ejecución:** `audit-results/logs/unit_tests_run.log` y XML `TEST-com.jeremy.lumi.domain.usecase.BondsPrivacyInferenceTest.xml`.
- **Resultado empírico:** Las clases de modelo `SharePreferences` y `BondsPrivacySettings` instancian por defecto `phase = true`, `mood = true`, `predictions = true`.
- **Veredicto:** CONFIRMADO.

---

### [P1-5] Riesgo de enumeración en códigos de invitación de 6 dígitos
- **Método de verificación:** PRUEBA AUTOMATIZADA (FIREBASE EMULATOR) + ANÁLISIS CRIPTOGRÁFICO.
- **Archivo de prueba ejecutado:** `tools/firestore-tests/audit_security_verification.test.cjs` (Líneas 87–100).
- **Log de ejecución:** `audit-results/logs/firestore_security_verification.log`.
- **Resultado empírico:** Un usuario atacante (`attacker_stranger`) puede leer cualquier documento de `partner_invitations/{code}` sin comprobación de pertenencia ni rate limit. En Kotlin, se usa `(100000..999999).random()` sin `SecureRandom`.
- **Veredicto:** CONFIRMADO.

---

### [P1-6] Inferencia indirecta de fases menstruales vía «Predicciones»
- **Método de verificación:** PRUEBA AUTOMATIZADA (JVM KOTLIN).
- **Archivo de prueba ejecutado:** `app/src/test/java/com/jeremy/lumi/domain/usecase/BondsPrivacyInferenceTest.kt` (`testP16_inferenceLeakWhenPredictionsTrueAndPhaseFalse`).
- **Resultado empírico:** Cuando `sharePhase = false` y `sharePredictions = true`, `SnapshotPrivacy.kt` filtra el nombre de la fase pero mantiene `cycleStartDateMs` y `cycleLengthDays`. Se demostró matemáticamente en el test que un tercero calcula el día del ciclo (`reconstructedDayInCycle = 10`) burlando la privacidad de la usuaria.
- **Veredicto:** CONFIRMADO.

---

### [P1-7] Navegación duplicada y ViewModels desvinculados
- **Método de verificación:** ANÁLISIS ESTÁTICO DE CÓDIGO Y NAVEGACIÓN COMPOSE.
- **Archivos:** `MainScreen.kt:457–471`, `AppRoutes.kt:25`.
- **Evidencia en código:** Cada entrada a Bonds en el grafo de navegación crea un nuevo ViewModel mediante `hiltViewModel()` sin retener el scope ni pasar argumentos de navegación estructurados.
- **Veredicto:** CONFIRMADO.

---

### [P1-8] Confirmación de gesto no equivale a entrega ni recepción
- **Método de verificación:** ANÁLISIS ESTÁTICO DE CÓDIGO.
- **Archivos:** `PartnerRepositoryImpl.kt:359–366`, `BondsViewModel.kt:339–351`.
- **Evidencia en código:** La interfaz muestra "Enviado con amor" en cuanto la promesa local de Firestore retorna, sin soporte de acuse de recibo, lectura o subcolección histórica.
- **Veredicto:** CONFIRMADO.

---

### [P1-9] Conflicto de contraste de StatusBar en modo oscuro
- **Método de verificación:** EVIDENCIA VISUAL DINÁMICA (EMULADOR ANDROID).
- **Evidencia:** Captura de pantalla real `audit-results/screenshots/screenshot_bonds_home.png`.
- **Código:** `Theme.kt:527` fuerza `darkIcons = !useDarkTheme`. En modo oscuro, los iconos son blancos; sobre el fondo claro crema de Bonds (`WarmBackground`), el ratio de contraste es inferior a 1.2:1 (ilegible).
- **Veredicto:** CONFIRMADO.

---

### [P1-10] Proyección de calendario compartido reconstruye el pasado
- **Método de verificación:** ANÁLISIS ESTÁTICO DE LÓGICA DE NEGOCIO.
- **Archivos:** `BondsSharedCalendarScreen.kt:507–515`.
- **Evidencia en código:** La función proyecta cíclicamente el estado menstrual hacia meses anteriores usando `dayInCycleWrapped = ((dateMillis - cycleStartMs) / DAY_MS) % cycleLength`, mostrando fases pasadas inventadas.
- **Veredicto:** CONFIRMADO.

---

### [P2-11] Componentes/Repositorios Bonds sin integración efectiva
- **Método de verificación:** ANÁLISIS ESTÁTICO DE PERSISTENCIA ROOM.
- **Archivos:** `BondsRepositoryImpl.kt:28–32`, `LumiDatabase.kt:17–26`.
- **Evidencia en código:** `BondsRepositoryImpl.observeBondsCircle()` retorna un `flowOf(emptyList())` hardcodeado. No existen entidades Room para Bonds.
- **Veredicto:** CONFIRMADO.

---

### [P2-12] Mensajes de ánimo ficticios por defecto y textos sin traducir
- **Método de verificación:** ANÁLISIS ESTÁTICO DE RECURSOS.
- **Archivos:** `BondsHomeScreen.kt:543–552`, `strings_bonds.xml:5, 14`.
- **Evidencia en código:** Se inyecta la cadena fija "Radiante y con energía" cuando el estado es nulo. Se detectaron cadenas en inglés en el archivo de recursos en español.
- **Veredicto:** CONFIRMADO.

---

### [P2-13] Generación automática de código sin control de error
- **Método de verificación:** ANÁLISIS ESTÁTICO DE RESILIENCIA.
- **Archivos:** `BondsPairingPrivacyScreen.kt:127–139`.
- **Evidencia en código:** `LaunchedEffect(Unit)` invoca `onGenerateCode()` sin envolver en bloque `try/catch` ni ofrecer botón de reintento si falla la red.
- **Veredicto:** CONFIRMADO.

---

### [NEW-1] Configuración de KSP y dependencias de Room
- **Método de verificación:** ANÁLISIS ESTÁTICO DE GRADLE.
- **Archivos:** `app/build.gradle.kts`.
- **Evidencia en código:** Falta de argumentos para esquemas incrementales en KSP y advertencias de Room runtime.
- **Veredicto:** CONFIRMADO.

---

### [NEW-2] Falta de `statusBarsPadding` en Header de Bonds (Edge-to-edge)
- **Método de verificación:** EVIDENCIA VISUAL DINÁMICA (EMULADOR ANDROID).
- **Evidencia:** Captura de pantalla real `audit-results/screenshots/screenshot_pairing_opened.png`.
- **Código:** `BondsTopHeader` carece de `Modifier.statusBarsPadding()`, lo que provoca que en dispositivos con Android 14/15 con Edge-to-Edge activado, el botón de retroceso colisione con el área del recorte de pantalla (notch).
- **Veredicto:** CONFIRMADO.
