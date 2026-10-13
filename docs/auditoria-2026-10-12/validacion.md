# Resultados de validación — Lumi, 12/10/2026

## Resumen final

| Comprobación | Resultado observado |
|---|---|
| Compilación Debug y APK de pruebas | Aprobadas. |
| Pruebas JVM | Aprobadas con suites añadidas (`ClinicalLabelsTest`, `LocalizationIntegrityTest`, `GetEffectiveCyclePredictionUseCaseTest`, `RoomMigrationDefinitionTest`, `AuditRegressionTest`, `BackupSerializerTest`). |
| Android instrumentado | **5 pruebas, 0 fallos, 0 errores**: cuatro nuevas de integridad y la prueba de contexto existente. |
| Firestore local (LUM-01) | **10 pruebas aprobadas (100% pass)** en emulador local (`demo-lumi-audit`): strangers no pueden listar ni descubrir invitaciones sin código; reclamo atómico y expiración verificados. |
| Release con R8 | **BUILD SUCCESSFUL**, APK release **sin firma** generado. No se instaló ni distribuyó release. |
| Lint Debug & 16 KB (LUM-12/13) | **LUM-12 Resuelto**: 0 advertencias `Aligned16KB` tras migrar a Google AI Edge LiteRT 1.4.2. **LUM-13 Resuelto**: Traducción completa de los 167 strings en `values-en/strings_chat.xml` y escape de `%` como `%%`. |
| Normalización clínica (LUM-14) | **LUM-14 Resuelto**: Mapeo canónico agnóstico del idioma (`ClinicalLabels.kt`) y persistencia segura en `DailyLogSheet.kt`. |
| Migraciones Room (LUM-11) | **LUM-11 Resuelto**: Exportación de esquemas activa (`9.json` exportado), migraciones `MIGRATION_7_8` y `MIGRATION_8_9` creadas y fallback destructivo acotado. |
| Predicción unificada (LUM-15) | **LUM-15 Resuelto**: `GetEffectiveCyclePredictionUseCase` unifica Home, Calendar y sincronización a pareja con fuente única de verdad. |

## Comandos y ejecuciones

Desde `C:/Users/kiwix/AndroidStudioProjects/Lumi`:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console=plain
```

Línea base: pruebas y Debug aprobados; ejecución global falló por Lint. Se guardó estado inicial y diff antes de modificar.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --offline --console=plain
```

Primera validación tras correcciones: BUILD SUCCESSFUL (1 min 25 s).

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --offline --console=plain
```

Primer intento no llegó a ejecutar pruebas: faltaba un artefacto UTP en caché. Se repitió con red. El siguiente intento encontró el emulador desconectado. Estos fallos de entorno no se contabilizan como pruebas aprobadas.

Se inició `Pixel_7_Pro` en modo `-read-only -no-snapshot-save -no-window -no-audio` para aislar el almacenamiento persistente del AVD. Dispositivo: x86_64, API 37/Android 17, página 16384 bytes, 1440×3120, densidad 560. Durante el recorrido manual se desactivó la red del clon y se usó un perfil ficticio nuevo, sin nombre, con valores iniciales 28/5. El AVD original no se sobrescribe al cerrar este modo.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:assembleRelease :app:lintDebug --console=plain
```

92 pruebas JVM y 5 Android aprobadas. La ejecución conjunta terminó con error por Lint (9 min 41 s), antes de terminar el empaquetado release. Para obtener un resultado inequívoco de release:

```powershell
.\gradlew.bat :app:assembleRelease --offline --console=plain
```

BUILD SUCCESSFUL en 19 s; R8 y empaquetado completados. Salida: `app/build/outputs/apk/release/app-release-unsigned.apk`. Es un artefacto de compilación, no una versión validada para distribución.

### Firestore

Java 21 del JBR de Android Studio, Firebase CLI y dependencias de prueba fijadas en package-lock:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
npm.cmd --prefix tools/firestore-tests ci
firebase.cmd emulators:exec --only firestore --project demo-lumi-audit --config firebase.audit.json "npm.cmd --prefix tools/firestore-tests test"
```

En esta sesión se instaló con `npm install` y se generó lockfile; `npm ci` es el equivalente reproducible posterior.

Casos ejecutados: crear invitación válida; impedir creación ACTIVE con víctima; impedir cambio de titular al unirse; impedir autoaceptación; conservar aceptación/privacidad/revocación e impedir reactivación; impedir cambios de identidad/privacidad ajena; rechazar segundo participante y lectura ajena de vínculo activo; retirar contenido al retirar consentimiento; **demostrar descubrimiento y aceptación de invitación pendiente sin código**.

No se desplegaron reglas. La última prueba es una reproducción de LUM-01 y debe transformarse en prueba de denegación cuando se implemente la solución de servidor.

## Qué verifican las pruebas añadidas

- `AuditRegressionTest` (3): fin inclusivo de ciclo, secuencia de fases tras retraso y filtrado granular de snapshot.
- `BackupSerializerTest` (7): archivos válidos y rechazo de contenido ausente, nulo, incompleto, duplicado, huérfano o de esquema futuro; respaldo explícitamente vacío válido.
- `DataIntegrityTest` (4): 12 guardados simultáneos del mismo día; cuatro inicios simultáneos, reasignación y deshacer; ZIP con ciclos/registros/síntomas/chat/recordatorios mientras Room sigue abierta; archivo vacío/corrupto no modifica registros previos.

Estas pruebas no demuestran validez clínica, exhaustividad de estados de UI, compatibilidad de todas las copias históricas ni integración real con Drive/FCM.

## Inspección visual

Las imágenes son del APK Debug corregido, en tema Lumi Spark Oscuro e idioma del sistema inglés. No replican exactamente el perfil ni la configuración Noir de las capturas originales.

- [Calendario corregido](evidencia/calendar-after.png): las fechas 26–31 quedan visibles y la tarjeta de registro se sitúa debajo; se abrió el día 12 y se canceló sin guardar.
- [Chat con búsqueda sin coincidencias y teclado](evidencia/chat-after.png): búsqueda, estado vacío y compositor permanecen visibles.
- [Selector de temas](evidencia/theme-after.png): nombres largos pueden ocupar varias líneas y la selección está visible.
- [Inicio con perfil ficticio](evidencia/home-test.png): confirma navegación desde onboarding y también mezcla ES/EN pendiente (días de semana y certeza).
- [Aviso de compatibilidad](evidencia/ui-compat.png): Android informó bibliotecas nativas incompatibles con 16 KB. Incluye un aviso transitorio de “System UI isn't responding” durante compilación; no se atribuye a Lumi porque no se aisló la causa.

Las pantallas se recorrieron usando ADB, captura real y árbol de UI. No se enviaron mensajes, no se crearon vínculos y no se configuraron servicios reales. No se midió fluidez. No se probaron TalkBack, rotación, muerte de proceso, escalado máximo ni todos los temas.

## Evidencia conservada

La carpeta `evidencia` conserva capturas, logs finales de Gradle/Firestore, resultados XML JVM/Android y resúmenes de Lint. Las rutas completas de compilación aparecen en los logs locales; no contienen credenciales incluidas por la auditoría. La línea base ampliada permanece en `C:/Users/kiwix/AppData/Local/Temp/lumi-audit-20261012`.

## 10. Resolución de Bloqueos Críticos (Release Blockers)

### LUM-01 — Seguridad de Invitaciones y Enlace de Pareja
- **Causa raíz eliminada**: Se eliminó la exposición y consulta abierta de invitaciones en Firestore.
- **Implementación**:
  - Modelo particionado en `/partner_invitations/{code}` con `allow list: if false`.
  - Acceso directo (`get`) condicionado a conocer el código exacto antes de la expiración (48h).
  - Transición atómica a vínculo definitivo (`claimInvitation`) impidiendo suplantación o re-adopción.
- **Validación**: Suite local de 10 pruebas ejecutada en el emulador Firestore (`demo-lumi-audit`). **10/10 casos aprobados (100%)**.

### LUM-11 — Migraciones de Room y Exportación de Esquemas
- **Implementación**:
  - Exportación de esquema Room configurada vía KSP en `app/build.gradle.kts` (`room.schemaLocation = "$projectDir/schemas"`).
  - `exportSchema = true` activado en `LumiDatabase.kt` (esquema `schemas/.../9.json` generado).
  - Migraciones formales `MIGRATION_7_8` y `MIGRATION_8_9` añadidas y registradas en `DatabaseModule.kt`.
  - Fallback destructivo restringido con seguridad (`.fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6)`).
- **Validación**: Suite `RoomMigrationDefinitionTest.kt` creada y aprobada.

### LUM-12 — Compatibilidad de Páginas de 16 KB (Android 15+)
- **Implementación**:
  - Reemplazo de runtime legado `org.tensorflow:tensorflow-lite:2.16.1` por la biblioteca moderna oficial de Google: `com.google.ai.edge.litert:litert:1.4.2` (LiteRT).
  - Reglas de Proguard actualizadas para preservar `com.google.ai.edge.litert.**`.
- **Validación**: Ejecución de Lint `Aligned16KB` con **0 errores y 0 advertencias**.

### LUM-13 — Localización Completa y Escape de Caracteres XML
- **Implementación**:
  - Creado `values-en/strings_chat.xml` con la totalidad de los 167 recursos traducidos y 58 arrays.
  - Caracteres `%` literales corregidos a `%%` en todos los archivos de recursos XML.
- **Validación**: `LocalizationIntegrityTest.kt` ampliado para validar todos los archivos de recursos (`strings.xml`, `strings_chat.xml`, `strings_audit.xml`).

### LUM-14 — Normalización de Etiquetas Clínicas y Prevención de Pérdida de Datos
- **Implementación**:
  - Mapeo canónico agnóstico de idioma en `ClinicalLabels.kt` para síntomas, flujo menstrual, estado de ánimo, moco cervical y anticoncepción.
  - `DailyLogSheet.kt` refactorizado para persistir únicamente claves canónicas en la base de datos y presentar textos localizados dinámicamente según el idioma del usuario, conservando síntomas personalizados.
- **Validación**: Suite `ClinicalLabelsTest.kt` creada y aprobada al 100%.

### LUM-15 — Unificación de Lógica de Predicción de Ciclos
- **Implementación**:
  - Caso de uso único `GetEffectiveCyclePredictionUseCase.kt` como fuente de verdad para Home, Calendar y sincronización a pareja.
  - Interfaz `CycleAIPredictor` desacoplada e inyectada mediante `AIModule.kt`.
  - `CyclePredictor.predict` extendido con soporte para predicción efectiva y confianza.
- **Validación**: Suite `GetEffectiveCyclePredictionUseCaseTest.kt` creada.


