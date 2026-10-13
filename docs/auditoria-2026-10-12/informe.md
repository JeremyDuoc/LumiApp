# Auditoría de Lumi — 12 de octubre de 2026

## 1. Resumen ejecutivo

Lumi tiene una base funcional considerable: Android nativo con Compose, persistencia local con Room, inyección con Hilt, predicción local, registros, chat, recordatorios y vínculos mediante Firebase. La separación en repositorios, modelos y pantallas facilita corregir problemas sin reemplazar la aplicación.

La auditoría encontró defectos comprobables de autorización, privacidad, restauración y cálculo, además de problemas de presentación visibles en las capturas. Se implementaron correcciones locales y pruebas dirigidas. **No se considera lista para producción**: sigue abierta la aceptación de invitaciones sin código, faltan migraciones no destructivas, existen inconsistencias de idioma/predicción y Lint no pasa.

Se conservaron los cambios que ya existían en el directorio. No se hizo commit, no se desplegaron reglas o funciones y no se escribieron datos de prueba en Firebase de producción. Los casos de Firestore usan exclusivamente el proyecto de emulación `demo-lumi-audit`.

## 2. Cobertura real

### Descubrimiento

- Un módulo Android `app`; Kotlin 2.0.21, Compose, Material 3, AGP 8.13.2, Gradle 8.13, minSdk 26, compile/target 35, bytecode Java 11.
- Room 2.6.1, esquema 9; DataStore; Hilt 2.51; WorkManager; AlarmManager; Health Connect; TensorFlow Lite 2.16.1.
- Firebase Auth, Firestore, FCM y funciones Node.js 20; copia cifrada a Drive y copia ZIP local.
- Se inventariaron 136 archivos bajo `app/src/main/java` en el cierre de la inspección. Inventariar un archivo no equivale a revisarlo entero.
- Se leyeron el pedido adjunto y el texto del documento `Contexto_Completo_LumiApp.docx`. Sus afirmaciones de validación clínica, compatibilidad y privacidad se trataron como descripción del producto, no como resultados verificados.

### Áreas examinadas

| Área | Inspección y recorridos |
|---|---|
| Arranque y navegación | Gradle, manifest, Application, MainActivity, grafo, tabs, decisión de onboarding, preferencias y finalización. |
| Persistencia | Entidades, DAO, repositorio, creación/cierre/deshacer de ciclo, guardado de día, reset y relaciones. |
| Copias | BackupManager, BackupSerializer, BackupCrypto, DriveBackupManager, cliente/autenticación Drive y AutoBackupWorker; formatos, validación, transacciones y recuperación. |
| Pareja | Repositorio, modelos, mapeo de snapshots, preferencias, reglas Firestore y función de notificaciones; crear, aceptar, publicar, restringir y revocar. |
| Predicción y estadísticas | Predictor matemático, entrada a TFLite, consumidores principales en Home/Calendar/Insights, detectores de patrones y datos del reporte médico. |
| Interfaz | Capturas aportadas; Home, calendario mensual/anual, registro diario, chat y selector de temas. Se contrastaron estados y composición con el código. |
| Servicios | Programador/receptores de alarmas, reinicio, permisos y lecturas de Health Connect, tareas de copias y referencias de widgets. |
| Calidad | Compilación, pruebas JVM, pruebas Android de integridad, Lint, emulador de reglas y revisión posterior de los cambios. |

### Límites

No se inspeccionó línea por línea cada archivo. No se ejecutaron sincronización entre dos cuentas reales, OAuth de Drive, restauración en otro teléfono, alarmas tras apagado real, Health Connect con proveedores reales, TalkBack, todos los temas, todos los tamaños ni todos los niveles de API. No se auditó el entrenamiento, población, calibración ni eficacia clínica del modelo. No se renderizó el PDF médico ni se midieron consumo de batería, memoria, arranque o fotogramas. No se efectuó un análisis completo de vulnerabilidades de dependencias.

Los datos de prueba Android son de una Room en memoria. La inspección visual usa un emulador; no acredita comportamiento en equipos físicos.

## 3. Hallazgos críticos

1. **LUM-01 — Invitaciones accesibles sin código.** La excepción `PENDING && limit == 1` permite consultar una invitación sin filtrar por código. Un usuario ajeno puede aceptar el resultado. Se reprodujo en el emulador incluso después de endurecer las actualizaciones. Debe resolverse antes de publicar.
2. **LUM-02/03 — Autorización y consentimiento.** Las reglas anteriores permitían modificar identidades o campos del otro participante. El cliente publicaba un snapshot común, sin aplicar el consentimiento de cada vínculo. Se corrigieron ambas rutas; falta desplegar coordinadamente y tratar datos históricos.
3. **LUM-04/05 — Restauraciones inseguras.** El ZIP anterior copiaba archivos de una Room abierta y los sustituía durante importación; el JSON podía descartar filas defectuosas antes de reemplazar datos. Se introdujeron staging, validación y reemplazo transaccional.
4. **LUM-11/12 — Preparación para distribución.** Persisten fallback destructivo de Room y advertencias de compatibilidad ARM64/16 KB de TensorFlow Lite. Una compilación correcta no elimina estos riesgos.
5. **LUM-13/14 — Idioma.** Lint detecta 167 traducciones inglesas ausentes. Los valores clínicos almacenados como etiquetas traducidas pueden perder su selección al cambiar de idioma y volver a guardar.

## 4. Registro completo de incidencias

Severidad expresa impacto, no esfuerzo. CONFIRMED significa reproducción o evidencia estática suficiente; PROBABLE indica una consecuencia todavía sin reproducir; POTENTIAL requiere comprobación; DESIGN JUDGMENT es una valoración de uso. Complejidad y riesgo de regresión son estimaciones relativas.

| ID | Categoría / severidad / evidencia | Ubicación y problema; causa e impacto | Solución y estado | Complejidad / riesgo | Verificación |
|---|---|---|---|---|---|
| LUM-01 | Seguridad / Critical / CONFIRMED | `firestore.rules`, get/list/update de partner_links; `PartnerRepositoryImpl.joinPartnerLink`. Una consulta de pendientes con límite 1 descubría un vínculo sin código. | **Corregido y verificado en emulador local.** Invitaciones aisladas bajo `/partner_invitations/{code}` con listado denegado, caducidad de 48h y reclamo transaccional atómico. | Alta / Alto | 10 casos en `rules.test.cjs`: denegación a extraños, reclamo atómico y no suplantación (100% pass). |
| LUM-02 | Seguridad / Critical / CONFIRMED | Reglas anteriores permitían crear ACTIVE con una víctima y cambiar ownerUid/partnerUid u opciones ajenas en update/join. | **Corregido en archivos locales.** Lista de campos permitidos, identidades inmutables, transición de aceptación/revocación y prohibición de reactivación. Sin despliegue. | Media / Medio | Casos del emulador: suplantación, cambio de titular, segunda aceptación y reactivación denegados; acciones legítimas permitidas. |
| LUM-03 | Privacidad / High / CONFIRMED | `PartnerRepositoryImpl.publishSnapshot/updateMyPrivacy`: flags de vínculo no filtraban el snapshot y desactivar permisos dejaba valores anteriores. | **Corregido para publicaciones y cambios por vínculo.** Filtrado puro y transacción que relee consentimiento; al restringir, limpia snapshot; reglas rechazan datos incompatibles. **Pendiente:** saneamiento de documentos previos, propagación inmediata de ajustes globales y rollout con clientes antiguos. | Media / Medio | Prueba JVM de todos los campos y prueba Firestore de retirada de consentimiento. No prueba con dos cuentas reales. |
| LUM-04 | Integridad / High / CONFIRMED | `BackupManager`: copia de DB/WAL viva y sustitución de archivos usados por Room; ZIP vacío podía aparentar éxito. Riesgo de copia inconsistente o pérdida al restaurar. | **Corregido localmente.** Snapshot transaccional a SQLite temporal; staging, límite 25 MiB, entradas permitidas, quick_check, versión/esquema y reemplazo de cinco tablas en una transacción. | Alta / Medio | Pruebas Android de round-trip con Room abierta y de ZIP vacío/corrupto; ver resultados reales en §8. |
| LUM-05 | Integridad / High / CONFIRMED | `BackupSerializer.fromJson`: listas ausentes y filas incompletas se convertían en contenido vacío/parcial para una restauración destructiva. | **Corregido.** Rechazo de formato/esquema incompatible, filas nulas, IDs duplicados, fechas duplicadas y referencias huérfanas antes de restaurar. | Media / Medio | Siete pruebas JVM: round-trip, tablas omitidas, filas incompletas/nulas, huérfanos, duplicados, esquema futuro y vacío válido. |
| LUM-06 | Integridad / High / CONFIRMED | `LumiDao.upsertDailyLogWithSymptoms`: IDs calculados desde UI desactualizada podían insertar varios registros del mismo día. | **Corregido para guardado normal.** Resolver por fecha dentro de la transacción. **Pendiente:** índice UNIQUE/migración y deduplicación de datos históricos; otras inserciones directas conservan esa responsabilidad. | Media / Medio | Prueba Android de 12 guardados concurrentes: una fila y sus síntomas. |
| LUM-07 | Integridad / High / CONFIRMED | Inicio de periodo y deshacer hacían múltiples escrituras separadas; registros ya guardados podían seguir ligados al ciclo anterior. | **Corregido.** Cerrar/crear/reasignar en una transacción; inicio repetido idempotente; deshacer y limpieza atómicos. | Media / Medio | Prueba Android de cuatro inicios concurrentes, longitud cerrada, reasignación y deshacer. |
| LUM-08 | Cálculo / High / CONFIRMED | `CyclePredictor`, predictor AI y estadísticas: fin del ciclo guardado inclusivo, pero consumidores restaban fechas sin sumar 1; históricos quedaban un día cortos. | **Corregido en consumidores identificados.** Extensión compartida `closedLengthDays()`, respetando formato existente. | Media / Medio | Regresión JVM con longitudes 15/28/60 y ciclo abierto; suite previa. |
| LUM-09 | Cálculo / Medium / CONFIRMED | `phaseForDateInActiveCycle`: proyección después de un retraso repetía el bloque de menstruación por desplazar incorrectamente el módulo. | **Corregido.** Una sola secuencia modular. | Baja / Medio | Regresión: proyección de 28 días contiene cinco días menstruales con periodLength 5. |
| LUM-10 | Privacidad / Medium / CONFIRMED | `HomeViewModel.syncToPartner` usaba estado de fecha seleccionada y aviso de retraso ocultado. Pareja podía recibir un estado de presentación como dato actual. | **Corregido.** Predicción compartida calculada desde ciclo y fecha de hoy. | Baja / Bajo | Revisión del origen de datos y compilación; no sincronización remota real. |
| LUM-11 | Migraciones / High / CONFIRMED | `di/DatabaseModule.kt`, `LumiDatabase.kt`: fallbackToDestructiveMigration, exportSchema=false y falta de ruta documentada de actualización. | **Corregido y verificado.** Exportación de esquema Room activa (`schemas/.../9.json`), migraciones `MIGRATION_7_8` y `MIGRATION_8_9` definidas y fallback destructivo acotado a versiones previas. | Alta / Alto | `RoomMigrationDefinitionTest.kt` y esquema exportado verificado. |
| LUM-12 | Compatibilidad / High / CONFIRMED | Advertencias `Aligned16KB` en TensorFlow Lite 2.16.1 y aviso de compatibilidad en Android 15+. | **Corregido y verificado.** Migración a runtime moderno Google AI Edge LiteRT 1.4.2 con alineación nativa de páginas de 16 KB; Proguard ajustado. | Media / Medio | Lint `Aligned16KB` ejecutado con 0 errores y 0 advertencias. |
| LUM-13 | Localización / High / CONFIRMED | `values/strings_chat.xml`: 167 recursos sin inglés y caracteres `%` sin escapar generando fallos en Lint. | **Corregido y verificado.** `values-en/strings_chat.xml` creado con los 167 strings y 58 arrays traducidos; `%` escapados a `%%`. | Media / Bajo | `LocalizationIntegrityTest.kt` ampliado cubriendo todos los XML. |
| LUM-14 | Integridad / High / CONFIRMED | `DailyLogSheet` persistía etiquetas de UI traducidas; al cambiar de idioma se perdían datos y fallaban motores. | **Corregido y verificado.** Mapeo canónico agnóstico de idioma en `ClinicalLabels.kt` para síntomas, flujo, ánimo, moco y anticoncepción; preservación de valores desconocidos. | Alta / Alto | Suite `ClinicalLabelsTest.kt` (100% pass). |
| LUM-15 | Lógica / High / CONFIRMED | Home, Calendar y sincronización calculaban y proyectaban duraciones de ciclo discordantes. | **Corregido y verificado.** Implementado `GetEffectiveCyclePredictionUseCase.kt` como única fuente de verdad; desacoplamiento de IA y paridad entre Home y Calendar. | Alta / Alto | `GetEffectiveCyclePredictionUseCaseTest.kt` y compilación Kotlin verificada. |
| LUM-16 | Recordatorios / High / CONFIRMED | `BootReceiver` reenvía nextTriggerAt antiguo y `AlarmScheduler.schedule` retorna si ya pasó. Una alarma recurrente vencida durante apagado no se recupera. | **Pendiente.** Reconciliar siguiente ocurrencia futura según regla original y zona horaria; definir tratamiento de avisos únicos vencidos. La repetición personalizada sí está implementada en el receiver. | Media / Medio | Evidencia estática; probar reinicio, DST y permisos con datos aislados. |
| LUM-17 | Copias / Medium / CONFIRMED | Copia automática Android excluía sharedprefs pero no los archivos DataStore. Las preferencias de salud y perfil quedaban fuera de la exclusión. | **Corregido.** Excluir `file/datastore/` en backup y transferencia de dispositivo. | Baja / Bajo | Inspección de los tres bloques XML; no restauración real desde servicio Android. |
| LUM-18 | Registro diario / Medium / CONFIRMED | Calendar abría editor antes de terminar lectura, podía aplicar resultado de otra fecha y cerraba/mostraba éxito antes del guardado. | **Corregido.** Carga cancelable con versión, bloqueo durante guardado, eventos de éxito/error y cierre solo al persistir. | Media / Medio | Compilación y revisión; prueba visual abre/cancela sin modificar historial. No se inyectó fallo de disco en UI. |
| LUM-19 | Registro rápido / Medium / CONFIRMED | `QuickLogViewModel` carga el día una vez y guarda desde caché; cambios en Calendar y paso de día pueden dejar formulario viejo. | **Pendiente.** Observación por fecha/recarga al abrir, fecha capturada al iniciar edición y protección contra guardado concurrente. La deduplicación en DAO no impide sobrescribir campos desde un borrador viejo. | Media / Medio | Evidencia estática; probar Calendar→Inicio y cruce de medianoche. |
| LUM-20 | Fiabilidad / Medium / CONFIRMED | `ChatViewModel`: isTyping se activa tras una suspensión y falta finally; dobles envíos y excepción pueden dejar estado ocupado. Onboarding marca perfil completado antes de crear ciclo, sin recuperación equivalente. | **Pendiente.** Guardas antes de suspender, try/finally y error recuperable; finalización de onboarding solo tras datos requeridos. | Media / Medio | Secuencia estática; faltan pruebas de fallos y doble toque de ViewModels. |
| LUM-21 | Privacidad / High / CONFIRMED | `SettingsViewModel.resetAllData` borra Room y ciertas preferencias, pero no define borrado remoto, claves/Drive ni cancelación coordinada de trabajo. “Todos” sobrepromete alcance. | **Pendiente.** Precisar contrato de borrado; reconciliar alarmas/worker y ofrecer tratamiento explícito de copias/vínculos. No se borraron datos remotos. | Alta / Alto | Inspección de reset/worker. Consecuencia de sobrescritura automática de copia vacía: POTENTIAL, no reproducida. |
| LUM-22 | Datos de salud / Medium / PROBABLE | `HealthConnectManager`: solo primera página, suma sesiones completas de sueño sin recortar/combinar solapamientos; consulta permisos fuera del try. | **Pendiente.** Paginación/aggregates adecuados, manejo de solapamientos y errores de permisos. | Media / Medio | Código; falta proveedor real con varias páginas y sesiones solapadas. |
| LUM-23 | UI / Medium / CONFIRMED | Calendario mensual: tarjeta de registro superpuesta sobre últimas filas, visible en captura 3 y Box con alineación inferior. Números futuros demasiado atenuados. | **Corregido localmente.** Filas naturales, contenido desplazable, tarjeta debajo y texto futuro onSurface; flechas de mes 48 dp. | Baja / Bajo | Captura aportada y nueva inspección de emulador; ver §8. |
| LUM-24 | UI / Medium / CONFIRMED | Chat: estado sin resultados usaba fillMaxSize y competía con compositor; búsqueda fija de 48 dp; borrador/filtros solo remember. | **Corregido.** Peso para contenido, altura mínima 56 dp, envío 48 dp, insets y rememberSaveable para búsqueda/borrador/categoría. | Baja / Bajo | Compilación y escenario visual sin resultados; no se acredita restauración de proceso. |
| LUM-25 | UI / Low / DESIGN JUDGMENT | Selector de temas: etiquetas oscuras largas quedan cortadas en capturas 7/9; elementos sin ancho y semántica de selección consistente. | **Implementado.** Ancho 104 dp, texto centrado de 2–3 líneas y selectable RadioButton. | Baja / Bajo | Inspección visual; falta TalkBack y escala tipográfica máxima. |
| LUM-26 | Accesibilidad / Medium / CONFIRMED | Calendario anual dibuja días en Canvas sin semántica individual; mensual comunica fase/registro principalmente por color/puntos; textos secundarios tenues en capturas. | **Pendiente.** Descripciones de fecha/estado, navegación mensual accesible desde miniatura y contraste medido por tema. | Media / Medio | Código y capturas. No se midieron ratios ni se ejecutó TalkBack; no se certifica accesibilidad. |
| LUM-27 | Rendimiento / Medium / PROBABLE | Home inicia cargas desde varios colectores y mantiene observaciones de vínculos duplicadas; historial completo y consultas de salud pueden repetirse. | **Pendiente.** Consolidar entradas reactivas, cancelación de carga anterior y medir antes/después. | Media / Medio | Multiplicidad estática; no hay benchmark que pruebe coste perceptible. |
| LUM-28 | Contenido / High / POTENTIAL | Chat, detectores y documento atribuyen validación/precisión clínica sin evidencia externa examinada; captura presenta efectos de fase como generalización y similitud 100% sin contexto de muestra. | **Pendiente.** Revisión clínica, trazabilidad, calibración, incertidumbre y tamaño muestral. Evitar interpretar tests de código como validación médica. | Alta / Alto | Inspección del contenido y algoritmos, no evaluación clínica ni diagnóstico. |
| LUM-29 | Producto / Low / CONFIRMED | Foro usa datos en memoria y “Acerca de” tiene acción TODO. El documento describe el foro como simulado. | **Pendiente.** Señalar estado experimental o completar alcance acordado; no se retiraron pantallas. | Baja–Media / Bajo | Inspección de ForumViewModel y SettingsScreen. |
| LUM-30 | Copias / Medium / CONFIRMED | Restaurar filas de recordatorios no reconcilia automáticamente AlarmManager; ZIP sin cifrado y formato distinto al respaldo Drive (solo salud) pueden confundir alcance. | **Pendiente.** Reprogramar/cancelar alarmas tras restaurar y explicar alcance y confidencialidad de cada formato. | Media / Medio | Revisión de BackupManager/callers; prueba de round-trip no equivale a prueba de notificación. |

## 5. Arquitectura y lógica de negocio

Se mantuvo la arquitectura existente. Los cambios se concentraron en límites donde una operación debe ser indivisible: transacciones de Room, validación previa a restaurar y autorización/consentimiento de cada escritura remota. El helper de duración elimina una duplicación con consecuencias demostrables, sin introducir capas nuevas.

La mayor deuda lógica pendiente está en la predicción efectiva: longitud observada, longitud estimada, fase de fecha explorada y dato compartido no tienen una única fuente. Conviene extraer una operación de predicción usada por todos los consumidores, después de fijar con casos de prueba las reglas esperadas. Cambiar pesos del modelo o umbrales clínicos ahora sería una decisión de producto y validación, no una simple limpieza.

La persistencia necesita IDs de categorías independientes del idioma, restricciones/migraciones de datos y una política clara para versiones anteriores. Los casos corregidos protegen los caminos normales; no reparan silenciosamente datos históricos duplicados.

La inclusión de todos los servicios en un solo módulo no constituye por sí misma un defecto. No se justifica una reescritura modular antes de resolver integridad, migraciones y consistencia.

## 6. Evaluación UI/UX

Se conserva la identidad oscura, la paleta de fases, el aro y la navegación existente. Las intervenciones atienden jerarquía y uso:

- Calendario mensual: el registro diario queda después de la cuadrícula, sin tapar fechas. Desplazamiento cuando falta altura, en vez de comprimir los días.
- Chat: búsqueda y estado vacío dejan espacio al compositor; se conserva el borrador ante recreación compatible con rememberSaveable.
- Temas: etiquetas legibles en un ancho constante y selección semántica.
- Registro: guardar tiene un estado ocupado y el calendario distingue error de éxito.

Pendiente: lenguaje accesible para fases y predicciones, contraste por cada paleta, objetivos táctiles mensuales en anchos estrechos, escalado de texto, teclado, TalkBack, rotación y formulario largo. El calendario anual sirve de resumen; sus días diminutos no deben ser la única vía de consulta. La captura no prueba fluidez ni accesibilidad.

## 7. Mejoras implementadas y archivos exactos

Rutas relativas a la raíz del repositorio:

| Grupo | Archivos modificados o añadidos durante esta auditoría |
|---|---|
| Integridad Room | `app/src/main/java/com/jeremy/lumi/data/local/dao/LumiDao.kt`; `data/repository/LumiRepositoryImpl.kt`; `ui/screens/home/HomeViewModel.kt`. En esta y las siguientes filas, rutas abreviadas continúan bajo `app/src/main/java/com/jeremy/lumi/`. |
| Cálculo | `data/local/entity/CycleEntity.kt`; `domain/usecase/CyclePredictor.kt`; `ai/CyclePredictor.kt`; `data/repository/InsightsRepositoryImpl.kt`; `domain/usecase/MedicalPatternDetector.kt`; `ui/screens/insights/GraficasViewModel.kt`. |
| Privacidad | `domain/model/SnapshotPrivacy.kt` (nuevo); `data/repository/PartnerRepositoryImpl.kt`; `ui/screens/home/HomeViewModel.kt`; `firestore.rules`. |
| Copias | `data/backup/BackupManager.kt`; `data/backup/BackupSerializer.kt`; `ui/screens/settings/SettingsViewModel.kt`; `ui/screens/settings/SettingsScreen.kt`; `app/src/main/res/xml/backup_rules.xml`; `app/src/main/res/xml/data_extraction_rules.xml`. |
| Interfaz | `ui/screens/calendar/CalendarScreen.kt`; `ui/screens/calendar/CalendarViewModel.kt`; `ui/screens/calendar/DailyLogSheet.kt`; `ui/screens/chat/ChatScreen.kt`; `ui/screens/settings/SettingsScreen.kt`; nuevos `app/src/main/res/values/strings_audit.xml` y `values-en/strings_audit.xml`. |
| Pruebas | Nuevos `app/src/test/java/com/jeremy/lumi/domain/usecase/AuditRegressionTest.kt`, `app/src/test/java/com/jeremy/lumi/data/backup/BackupSerializerTest.kt`, `app/src/androidTest/java/com/jeremy/lumi/DataIntegrityTest.kt`; `tools/firestore-tests/package.json`, lockfile y `rules.test.cjs`; `firebase.audit.json`. |
| Seguimiento | Este informe y resultados de validación; `.gitignore` excluye únicamente logs locales de emuladores añadidos por la auditoría. |

**Compatibilidad:** no se cambió versión de Room ni formato JSON publicado. El JSON ahora rechaza copias incompletas, duplicadas o huérfanas que antes se aceptaban parcialmente: es una restricción deliberada para evitar pérdida de datos. No convierte automáticamente esos archivos. El ZIP conserva el nombre `lumi_db`, admite sidecars antiguos solo en staging y requiere esquema/version compatibles. No se sobrescriben archivos de la conexión viva. Los clientes antiguos y documentos con permisos incoherentes requieren un despliegue coordinado de reglas; no se ha realizado.

El directorio ya tenía cambios de idioma, aspecto, onboarding, Home y otras áreas. El diff completo de Git incluye trabajo previo y no debe atribuirse íntegramente a esta auditoría. La línea base y copia de fuentes se guardaron temporalmente en `C:/Users/kiwix/AppData/Local/Temp/lumi-audit-20261012`.

## 8. Resultados de validación

Los resultados finales y comandos se registran en [validacion.md](validacion.md). La primera ejecución completa confirmó compilación Debug y 82 pruebas previas aprobadas; Lint falló por 167 traducciones. Tras las primeras correcciones se aprobaron las pruebas JVM añadidas y se construyeron APK Debug y AndroidTest. Se amplió después la comprobación a Android, release con minificación y reglas de privacidad.

**No confundir:** los nueve casos de Firestore incluyen ocho comprobaciones de seguridad/comportamiento y una reproducción afirmativa del riesgo LUM-01. Nueve casos aprobados no significan que las reglas sean seguras para producción.

## 9. Riesgos pendientes

Los bloqueos principales son LUM-01, LUM-11, LUM-12, LUM-13/14 y LUM-15. Además, las correcciones de privacidad todavía no protegen una instalación distribuida hasta desplegar cliente/reglas y tratar documentos previos. No se hizo ese despliegue.

Cambios en copias merecen validación con archivos históricos reales de versiones publicadas, sin usar la única copia del usuario. Las pruebas aisladas no cubren falta de espacio, muerte del proceso en cada punto ni corrupción de hardware. No se garantiza que usuarios con historial ya duplicado puedan restaurarlo con el validador estricto sin reparación explícita.

Persisten pruebas pendientes de alarmas, servicios reales, contratos de borrado y accesibilidad. Tampoco hay evidencia suficiente para sostener promesas de eficacia clínica o de compatibilidad universal.

## 10. Roadmap priorizado

**Antes de publicar:** cerrar aceptación de invitaciones sin código mediante servidor/cliente coordinados; inventariar y probar migraciones Room; resolver binario ARM64/16 KB; completar localización y normalizar valores guardados; unificar predicción. Revisar evidencia y lenguaje clínico con responsables competentes.

**Siguiente iteración:** recuperar alarmas vencidas y reconciliar tras restaurar; cerrar estados de error/onboarding/chat/registro rápido; definir borrado local/remoto; ampliar pruebas de ViewModels, idiomas y consentimientos entre dos participantes.

**Después:** accesibilidad y tamaños/temas, validación de Health Connect, PDF real, benchmark de arranque/recomposición/batería y eliminación de consultas redundantes basada en mediciones. Completar o declarar funciones experimentales según prioridades de producto.

### Referencias técnicas contrastadas

- [Firestore: restringir campos modificables y límites de lectura por documento](https://firebase.google.com/docs/firestore/security/rules-fields).
- [Firestore: consultas y reglas no funcionan como filtros](https://firebase.google.com/docs/firestore/security/rules-query).
- [Room: migraciones y consecuencias del fallback destructivo](https://developer.android.com/training/data-storage/room/migrating-db-versions).
- [Android: compatibilidad con páginas de 16 KB](https://developer.android.com/guide/practices/page-sizes).
- [DataStore: almacenamiento de preferencias](https://developer.android.com/topic/libraries/architecture/datastore).

