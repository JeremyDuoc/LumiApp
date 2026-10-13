# Resultados de Compilación y Análisis Estático — Lumi Android
**Fecha:** 12 de octubre de 2026  
**Auditor:** Principal Android Software Auditor  
**Herramientas:** Gradle 8.4, AGP 8.2.2, Kotlin 1.9.22, KSP 1.9.22-1.0.17, JDK 17 / JDK 21

---

## 1. Historial de Ejecuciones de Compilación

### Ejecución 1 — Build Completo APK Debug
- **Comando:** `./gradlew assembleDebug`
- **Resultado:** **EXITOSO** (`BUILD SUCCESSFUL in 7m 56s`)
- **APK generado:** `app/build/outputs/apk/debug/app-debug.apk`
- **Instalación:** Desplegado e instalado en `emulator-5554` (Pixel 7 Pro, Android 14/15).

### Ejecución 2 — Intento de Ejecución de Pruebas Unitarias
- **Comando:** `./gradlew testDebugUnitTest --continue`
- **Resultado:** **FALLIDO** (en tarea `:app:kspDebugKotlin`)
- **Error reportado:**
  ```text
  e: file:///C:/Users/kiwix/AndroidStudioProjects/Lumi/app/src/main/java/com/jeremy/lumi/ui/screens/insights/InsightsScreen.kt:1227:2 Expecting '}'
  Execution failed for task ':app:kspDebugKotlin'.
  ```
- **Causa raíz:** En `InsightsScreen.kt:426`, se había insertado una función `@Composable fun LumiCycleDurationsCard` sin cerrar las tres llaves finales (`Column`, `Card`, `fun CycleStatsCard`).

### Ejecución 3 — Compilación Tras Cierre de Llaves en InsightsScreen
- **Comando:** `./gradlew testDebugUnitTest --continue`
- **Resultado:** **FALLIDO** (en tarea `:app:compileDebugKotlin`)
- **Errores reportados:**
  1. `BondsRepositoryImpl.kt:80:31 Unresolved reference 'revokeLink'`: La interfaz `PartnerRepository` define `unlink(linkId)`, pero la implementación invocaba `revokeLink(linkId)`.
  2. `CalendarScreen.kt:189, 216`: Desajustes de parámetros en `LumiEditorialHeading` y discrepancia de tipos (`DailyLogWithSymptoms?` vs `DailyLogEntity?`).

### Ejecución 4 — Compilación Aislada y Ejecución Exitosa de Tests
- **Comando:** `./gradlew testDebugUnitTest`
- **Resultado:** **EXITOSO** (`BUILD SUCCESSFUL in 57s`)
- **Resumen:** 109 pruebas unitarias ejecutadas, 0 fallos, 0 ignoradas.

---

## 2. Advertencias de Compilación Relevantes

Durante la compilación de `compileDebugKotlin` se identificaron las siguientes advertencias:

1. **Icono Deprecado en Compose:**
   ```text
   w: BondsSendGestureScreen.kt:327:53 'val Icons.Rounded.Send: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Rounded.Send.
   ```
2. **Condición Redundante:**
   ```text
   w: HomeScreen.kt:195:86 Condition is always 'true'.
   ```
3. **KSP Incremental en Windows:**
   - La opción `ksp.incremental=false` debió mantenerse en `gradle.properties` para prevenir el fallo intermitente de Room `NoSuchFileException: ...\LumiDao_Impl.java` típico del daemon de Kotlin en entornos Windows.

---

## 3. Diagnóstico de Módulos y Dependencias

- **Room:** Declarado en versión 2.6.1 con exportSchema habilitado. La base de datos `LumiDatabase` funciona en versión 9 con migraciones probadas. Las tablas de Bonds (`bonds_links`, `bonds_gestures`) no están añadidas al esquema Room oficial.
- **Hilt:** Inyección Hilt operativa para ViewModels (`@HiltViewModel`) y repositorios (`@Singleton`).
- **Firebase:** `firebase-bom:32.8.0`, `firebase-firestore-ktx`, `firebase-auth-ktx` integrados y funcionales.
- **Compose:** Compose BOM `2024.02.00` con Material 3 `1.2.0`.
