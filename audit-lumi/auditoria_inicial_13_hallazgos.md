# Auditoría estática de Lumi Bonds — 12 de octubre de 2026

**Base examinada:** `Lumi_Bonds_Implementation.zip` (47 fuentes Kotlin incluidas, `firestore.rules`, recursos de cadenas y capturas). No se compiló ni ejecutó el proyecto porque el ZIP no contiene la totalidad del proyecto ni el Gradle wrapper. Las reglas Firestore examinadas podrían no coincidir con las publicadas en Firebase. Los hallazgos de ejecución remota requieren prueba de integración.

## Bloqueos críticos

### P0-1 — Los datos ficticios aparecen en producción y el envío a ellos se simula
**Evidencia:** `ui/screens/bonds/BondsViewModel.kt:102–184, 258–269, 318–330, 430–440, 465–476`. Cuando `memberList` está vacía, se insertan cuatro `SAMPLE_MEMBERS`. Para `sample_*`, el envío espera 250 ms y llama `onSuccess()` sin operación de red. Permisos y desvinculación de esos perfiles también se simulan.
**Corrección:** mover fixtures a `@Preview`/debug explícito y eliminar el fallback de producción; mostrar el estado vacío real y bloquear envío sin destinatario real.

### P0-2 — La revocación no bloquea la lectura directa de los datos remotos
**Evidencia:** `firestore.rules:28–36` autoriza `get/list` a owner/partner sin verificar `status == 'ACTIVE'`; `data/repository/PartnerRepositoryImpl.kt:368–375` solo cambia `status` a `REVOKED`, sin vaciar snapshots, tokens o datos de salud. Por ello un participante revocado que conoce el `linkId` sigue autorizado para obtener el documento bajo esas reglas.
**Corrección:** revisar estado en lecturas, resolver acceso a invitaciones pendientes y limpiar/cifrar o separar los snapshots y tokens. Probar `get` tras revocación desde ambas cuentas. No prometer borrar copias ya exportadas.

### P0-3 — Incompatibilidad probable en el canje de invitaciones
**Evidencia:** `PartnerRepositoryImpl.kt:255–270` hace `transaction.get(linkRef)` cuando aún está `PENDING`; `firestore.rules:28–31` niega el `get` al futuro participante, ya que aún no es owner ni partner. El canje puede fallar con `PERMISSION_DENIED` si esas son las reglas desplegadas.
**Corrección:** validar y canjear invitación mediante operación de servidor/Cloud Function segura y transaccional, sin abrir lecturas de snapshots a no participantes. Verificar con dos cuentas Firebase y reglas efectivamente publicadas.

### P0-4 — Compartición de información sensible activada por defecto
**Evidencia:** `domain/model/PartnerModels.kt:144–149, 64–73`, `domain/model/BondsPrivacySettings.kt:7–12`, `BondsViewModel.kt:76–81`, `PartnerRepositoryImpl.kt:172–179`. `phase`, `mood` y `predictions` vienen `true` por defecto.
**Corrección:** consentimiento opt-in por destinatario, `false` inicialmente para las cuatro categorías; pantalla explicativa previa y pruebas de revocación.

## Hallazgos altos

### P1-5 — Riesgo de invitación por código sin protección robusta
**Evidencia:** `PartnerRepositoryImpl.kt:140–153, 196–199`, `firestore.rules:89–113` y `:68–74`. Código aleatorio de seis dígitos sin validación previa de unicidad. Lectura de invitaciones posible por `get` a cualquier autenticado que acierte el código; en reglas de actualización de vínculo no se exige comprobación de invitación aceptada, válida y no expirada.
**Corrección:** códigos no predecibles o aleatoriedad criptográfica, unicidad atómica, expiración impuesta en servidor, limitación de intentos y comprobación vinculada al mismo canje.

### P1-6 — El permiso «Predicciones» puede permitir reconstruir fases sin permiso «Fase»
**Evidencia:** `SnapshotPrivacy.kt:4–13` publica fecha de inicio y duraciones con `predictions=true`; `BondsSharedCalendarScreen.kt:81–84, 507–515` habilita miembro si comparte *fase o predicciones* y calcula fases de días usando esos valores.
**Corrección:** definir claramente lo que revela cada categoría; no presentar fase si se negó permiso de fase, o requerir consentimiento explícito para un calendario de fases inferidas.

### P1-7 — Flujos de navegación duplicados y ViewModels con ámbitos distintos
**Evidencia:** `ui/navigation/MainScreen.kt:179–219, 249–278, 457–467`. La pestaña Bonds envía todas las acciones al `BONDS_HOME` raíz sin pasar miembro/gesto seleccionado; destinos raíz crean `hiltViewModel()` propios. Puede perderse selección o abrirse otro destinatario.
**Corrección:** una sola ruta canónica o un grafo anidado con ViewModel compartido y parámetros de navegación seguros; pasar explícitamente `linkId` y acción.

### P1-8 — Confirmación de gesto no equivale a entrega ni recepción
**Evidencia:** `PartnerRepositoryImpl.kt:359–365` actualiza `lastOwnerCareAction`/`lastPartnerCareAction` (último gesto), y `BondsViewModel.kt:339–351` muestra éxito al completarse esa escritura. No hay acuse de recepción en este flujo; tampoco historial persistente de gestos en Firestore aquí.
**Corrección:** diferenciar «guardado/enviado al servidor» de «entregado/leído», controlar duplicados e incorporar recepción/estado si se promete entrega.

### P1-9 — Diferencia visual con todos los temas y barras del sistema
**Evidencia:** `ui/screens/bonds/BondsTheme.kt:21–94` define paleta estática clara; pantallas usan `BondsTheme.WarmBackground`; `ui/theme/Theme.kt:520–528` define iconos de barra de estado según tema global (`isDark`). De aquí deriva la barra blanca sobre fondo claro observada en capturas cuando Lumi usa tema oscuro.
**Corrección:** migrar Bonds a tokens del tema activo; ajustar barras de sistema de acuerdo con fondo efectivo de pantalla o diseñar versión oscura accesible.

### P1-10 — Cálculo de calendario compartido reconstruye fases para el pasado
**Evidencia:** `BondsSharedCalendarScreen.kt:507–515` utiliza una única fecha de inicio y duración para calcular fases de cualquier mes con `CyclePredictor.dayInCycleWrapped`. No se muestran registros reales históricos provenientes de otros dispositivos en este archivo.
**Corrección:** marcar estimaciones y limitar proyección al intervalo justificable; no presentar días pasados inferidos como menstruaciones registradas.

## Hallazgos medios

### P2-11 — Componentes y repositorios declarados pero sin integración efectiva
**Evidencia:** `data/repository/BondsRepositoryImpl.kt:28–32` devuelve `emptyList()` siempre desde `observeBondsCircle()`. `data/local/LumiDatabase.kt:16–29` no registra entidades ni DAO de Bonds pese a existir `BondLinkEntity`, `BondsGestureEntity` y `BondsDao`. La pantalla usa `PartnerRepository`, no el contrato `BondsRepository`.
**Corrección:** eliminar estructuras muertas o completarlas e integrarlas; no afirmar que Room guarda los vínculos de Bonds sin pruebas.

### P2-12 — Mensajes de ánimo inventados cuando faltan registros y localización parcial
**Evidencia:** `BondsHomeScreen.kt:543–552` muestra `bonds_feeling_sample_status` si `member.mood == null`, en lugar de explicar que el dato no fue compartido. `res/values/strings_bonds.xml:5` mantiene el subtítulo en inglés. Parte de `BondsPersonDetailScreen.kt` y del calendario tiene literales en español sin recursos.
**Corrección:** diferenciar «no compartido», «sin registro» y «no disponible»; ubicar todo el texto en recursos localizados.

### P2-13 — La generación de invitación se ejecuta automáticamente y sin gestión visible del error
**Evidencia:** `BondsPairingPrivacyScreen.kt:127–139` llama `onGenerateCode()` dentro de `LaunchedEffect` cuando falta código y no maneja excepción en esa ruta. Entrar a la pantalla puede emitir invitaciones sin pulsar «Generar» y dejar la experiencia interrumpida ante fallos.
**Corrección:** botón explícito de crear/renovar invitación, estados de carga/error y revocación de códigos previos.

## Aclaraciones y puntos positivos
- Existe infraestructura remota real en `PartnerRepositoryImpl` mediante Firebase Auth + Firestore con observadores en tiempo real (`:67–135`), publicación filtrada por permisos (`:324–355`) y envío de últimos gestos (`:359–365`). **No es una app únicamente local.**
- La aplicación reutiliza el cálculo general de fase mediante `CyclePredictor` para el calendario compartido, aunque su fidelidad clínica e histórica debe validarse.
- Los archivos incluyen componentes de UI específicos de Bonds, navegación y recursos localizados ES/EN.
- No se ha verificado si el `firestore.rules` del ZIP está efectivamente desplegado, si las notificaciones FCM funcionan entre dos dispositivos, ni si el Gradle build del proyecto completo pasa.

## Pruebas imprescindibles antes de publicar
1. Compilar y ejecutar tests en el repositorio completo, no en el ZIP parcial.
2. Crear vínculo entre dos cuentas instaladas en dispositivos distintos con las reglas Firebase reales.
3. Verificar un código caducado, reutilizado, erróneo, propio y adivinado, además de intentos repetidos.
4. Con permisos todos `false`, probar lectura de snapshots, fases, ánimos, predicciones y calendario.
5. Revocar vínculo y confirmar que un `get(partner_links/{linkId})` del antiguo participante falla por reglas, además de probar el borrado/invalidez de datos bajo control de Lumi.
6. Enviar gesto a destinatario A desde el carrusel y comprobar que no se redirige a B, incluso después de entrar/salir de la pantalla.
7. Probar modo sin vínculos para asegurarse de que no aparezcan Camila/Sofía/Valentina/Anto.
8. Probar tema oscuro, tema claro, todos los presets y barras de estado.
9. Verificar qué significa «enviado», si el servidor almacenó el gesto y si el segundo dispositivo lo recibió.
10. Comparar calendario compartido con los registros originales y diferenciar estimación/registro.

## Orden de corrección recomendado
1. Revocación/lectura de datos y reglas Firestore (P0-2).
2. Canje seguro de invitaciones (P0-3 + P1-5).
3. Permisos opt-in y coherencia de datos inferibles (P0-4 + P1-6).
4. Eliminar muestras engañosas de producción (P0-1 + P2-12).
5. Unificar navegación y entrega de gestos (P1-7 + P1-8).
6. Reparar temas, status bar, calendarios e integración de repositorios (P1-9, P1-10, P2-11).

**Veredicto:** buena base visual y real infraestructura Firebase, pero no aprobar para despliegue de Bonds con intercambio de datos sensibles hasta corregir P0 y pasar pruebas de dos cuentas.
