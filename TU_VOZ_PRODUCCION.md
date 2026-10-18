# Lumi · Tu voz — Integración y despliegue

Rama: feat/tu-voz-produccion. **NO desplegada ni compilada aún**.

## Qué cambia
Reemplaza el foro de preguntas médicas simuladas del remoto main por Tu voz: cabecera Lumi, propuestas de mejora, votos, ficha con avance y comentarios, pestañas Ideas / En camino / Mis votos, ordenamiento y formulario. Usa los tokens de los ocho temas y escucha Firestore en tiempo real.

**Importante:** El main remoto no tenía el Tu voz que aparece en tus capturas actuales. Antes del merge, compara estos cambios con la versión LOCAL de Android Studio.

## Privacidad
Los datos del ciclo menstrual nunca son leídos ni enviados por este módulo. Las sugerencias y comentarios son texto público después de moderación humana; las identidades anónimas Firebase (UID), votos, propuestas propias y reportes se guardan en Firestore de forma privada. Identidad anónima **no equivale** a anonimato frente al operador. Borrar datos/reinstalar pierde el UID y no restaura los votos.

Advertir que Tu voz es una excepción a la política general de almacenamiento local. Actualizar consentimiento/política legal antes de liberar. No publicar detalles médicos, personas identificables ni contactos. El filtro automático NO evita todo texto sensible.

## Configuración requerida
1. Firebase: proyecto de producción lumi-7c924, Firestore, Authentication con proveedor Anonymous, Cloud Functions y plan Blaze según tarifas actuales.
2. NOTA: El alias default de .firebaserc es demo-lumi-bonds. Nunca ejecutar despliegues sin el selector de proyecto explícito.
3. Instalar firebase-tools; probar reglas en emulador, revisar seguridad del resto del proyecto.
4. Desplegar: firebase deploy --project lumi-7c924 --only firestore:rules,firestore:indexes
5. Desplegar: firebase deploy --project lumi-7c924 --only functions:submitVoiceIdea,functions:voteVoiceIdea,functions:submitVoiceComment,functions:reportVoiceIdea,functions:reviewVoiceIdea,functions:reviewVoiceComment,functions:setVoiceIdeaStatus,functions:hideVoiceIdea
6. Antes de lanzamiento público integrar Firebase App Check (Play Integrity + proveedor debug para pruebas) y activar enforcement en Firestore y Functions; NO está habilitado en la rama.
7. Activar presupuestos/alertas, monitor de abuso, backups, controles de retención/borrado y proceso de reportes/moderación.

## Moderación (obligatoria)
Las propuestas nuevas van a voice_pending_ideas. Los comentarios van a voice_pending_comments. Solo son públicos cuando el equipo los aprueba. Script: functions/moderate-voice.cjs. Se ejecuta con credenciales ADMIN de Google Cloud fuera del repositorio (Application Default Credentials o GOOGLE_APPLICATION_CREDENTIALS) y GOOGLE_CLOUD_PROJECT=lumi-7c924.

Comandos desde la carpeta functions, después de npm ci:
- node moderate-voice.cjs list
- node moderate-voice.cjs approve ID
- node moderate-voice.cjs reject ID
- node moderate-voice.cjs comments
- node moderate-voice.cjs approve-comment ID
- node moderate-voice.cjs reject-comment ID
- node moderate-voice.cjs reports
- node moderate-voice.cjs status ID "En desarrollo"
- node moderate-voice.cjs reply ID "Estamos trabajando en ello."
- node moderate-voice.cjs hide ID

Las funciones callable administrativas exigen custom claim lumiVoiceModerator=true. No crear moderadores a partir de identidades anónimas públicas. No subir claves al repositorio.

## Pruebas automatizadas
- npm ci --prefix functions
- node --check functions/voice.js
- node --check functions/index.js
- node --test functions/voice.test.cjs
- npm ci --prefix tools/firestore-tests
- firebase emulators:exec --project demo-lumi-bonds --only firestore "cd tools/firestore-tests && node --test voice_rules.test.cjs"
- ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest

Requieren máquina con Internet, Android SDK/JDK y Firebase CLI. No se pudo compilar Android ni hacer deploy desde este entorno.

## Test real con dos teléfonos
1. Instalar la misma rama y conectar ambos al MISMO proyecto Firebase, comprobar que cada móvil tiene UID anónimo diferente.
2. Crear una idea en A: debe aparecer en Mis votos / pendientes de A. B no la ve aún.
3. Aprobar idea mediante CLI: aparece en B sin reiniciar y se actualiza la vista de A.
4. Votar/quitar voto en B: conteo cambia en A y B y persiste después de cerrar/reabrir la app.
5. Comentar desde A: espera revisión; tras aprobación aparece el comentario y su contador en ambos.
6. Reportar desde B: llega a voice_reports. Cambiar estado vía CLI: aparece la hoja de ruta y avance.
7. Simular mala red, dos votos simultáneos, cambio de tema, navegación, teclado, Back y tamaño de fuente.
8. Borrar datos de A: la sesión anónima se pierde, así como la identificación de sus votos/propuestas anteriores. Es limitación conocida.

## Bloqueadores antes de producción abierta
- Configurar/activar App Check, antifraude, monitoreo de costos.
- Gestionar denuncias y revisión regularmente; el script ya permite moderación manual, no existe panel admin.
- Formalizar política de privacidad para texto público y almacenamiento de UIDs seudónimos.
- Verificar paridad visual grabando HTML vs Kotlin: halo animado del voto y brillo del CTA no son aún reproducción pixel-perfect.
- La consulta limita a 100 ideas recientes y 100 comentarios por idea. El ordenamiento se aplica a las 100 cargadas; falta paginación.
- No se ha construido ni ejecutado en Android Studio en esta sesión.
