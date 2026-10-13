# Resultados de Pruebas Unitarias y Lógica de Negocio — Lumi
**Fecha:** 12 de octubre de 2026  
**Entorno:** Gradle 8.4, Kotlin 1.9.22, JUnit 4, Roborlectric / JVM  
**Comando:** `./gradlew testDebugUnitTest`

---

## 1. Resumen Ejecutivo de Pruebas

| Métrica | Valor |
|---|---|
| **Total de Pruebas Ejecutadas** | 109 |
| **Pruebas Exitosas (Passed)** | 109 |
| **Pruebas Fallidas (Failed)** | 0 |
| **Pruebas Ignoradas (Skipped)** | 0 |
| **Tasa de Éxito** | **100%** |
| **Tiempo Total de Ejecución** | 0.82 s (suite JVM) |

---

## 2. Desglose por Paquete y Clase

### 2.1 Dominio y Motor Predictivo (`com.jeremy.lumi.domain.usecase`)
- `CyclePredictorTest`: **PASS** (Cálculo de longitud de ciclo, fases y ventana de ovulación).
- `GetEffectiveCyclePredictionUseCaseTest`: **PASS** (Resolución de predicciones combinadas con historial).
- `IrregularCyclesPredictionTest`: **PASS** (Comportamiento y márgenes de error con variabilidad > 7 días).
- `AdvancedBioCorrelationMatrixEngineTest`: **PASS** (Matriz de correlación bivariable entre dolor, estrés y fase).
- `MedicalPatternDetectorExtendedTest`: **PASS** (Detección de patrones de sangrado excesivo y fase lútea corta).
- `SymptomClusterMiningEngineTest`: **PASS** (Agrupación de síntomas recurrentes por fase).
- `AuditRegressionTest`: **PASS** (Regresiones históricas de cálculo de fertilidad).

### 2.2 Auditoría Específica de Bonds (`BondsPrivacyInferenceTest`)
Suite creada específicamente para verificar empíricamente los hallazgos de privacidad:
- `testP04_defaultPrivacyFlagsAreTrueByDefault`: **PASS (CONFIRMA P0-4)**
  - Demuestra que `SharePreferences()` inicializa `phase = true`, `mood = true`, `predictions = true`.
- `testP16_predictionsExposeCycleStartAndAllowPhaseReconstructionWhenPhaseIsFalse`: **PASS (CONFIRMA P1-6)**
  - Demuestra que cuando `phase = false` y `predictions = true`, el snapshot retiene `cycleStartDateMs` y permite a un tercero reconstruir con exactitud cuándo la usuaria está en fase menstrual.
- `testFullOptOutRedactsAllHealthData`: **PASS**
  - Confirma que cuando los cuatro consentimientos son `false`, todos los campos del ciclo quedan censurados a valores nulos o vacíos.

### 2.3 Persistencia y Migraciones de Room (`com.jeremy.lumi.data.local`)
- `RoomMigrationDefinitionTest`: **PASS**
  - Comprueba la validez de las definiciones de migración `MIGRATION_7_8` (spotting, temperature, sleep) y `MIGRATION_8_9` (water intake).
  - *Nota de auditoría:* Confirma que `LumiDatabase` se encuentra actualmente en versión 9 y que no contiene tablas de Bonds.

### 2.4 Respaldo Cifrado y Datos (`com.jeremy.lumi.data.backup`)
- `BackupSerializerTest`: **PASS** (Serialización y firma de respaldos privados cifrados para Google Drive).

### 2.5 Reportes Médicos (`com.jeremy.lumi.data.report`)
- `MedicalReportClinicalTest`: **PASS** (Generación de PDF clínico sin exposición de datos de pareja o terceros).

### 2.6 Localización y Temas (`com.jeremy.lumi`)
- `LocalizationIntegrityTest`: **PASS** (Integridad de claves entre `values` y `values-en`).
- `ThemeAndDialStyleTest`: **PASS** (Consistencia de paletas de color en temas predeterminados de Lumi).
- `BiometricGaugeCalculationTest`: **PASS** (Indicadores visuales en dial de inicio).
- `PredictionFactorsCalculationTest`: **PASS** (Ponderación de factores de predicción).
