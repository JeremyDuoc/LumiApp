# Lumi · Tu voz en Spark (GRATIS, sin Blaze)

## Qué funciona sin Cloud Functions
- Ideas, votos, estado de la hoja de ruta y comentarios aprobados con Firestore.
- Firebase Auth **Anonymous** para identificar la instalación sin revelar el nombre.
- Las propuestas y comentarios enviados por la app quedan **privados y pendientes de moderación**.
- Voto único por UID con **transacción Firestore**: se cambia el estado del voto y el contador a la vez. Las reglas verifican las dos operaciones, impidiendo alterar el contador directamente.
- Votos y propuestas privadas solamente accesibles por su UID; las colecciones públicas no contienen UID de votantes.
- Equipo Lumi aprueba/rechaza/modera con Firebase Console o con scripts locales (Admin SDK), sin desplegar servidor.

## Primera publicación: cuidado con las reglas
Desde la carpeta raíz, estando en la rama correcta y con los cambios actualizados:

    git pull --ff-only origin feat/tu-voz-produccion

ANTES de desplegar: comparar firestore.rules con lo que ya esté publicado en Firebase Console, porque el comando reemplaza **todas** las reglas, incluyendo Bonds:

    firebase deploy --project lumi-7c924 --only firestore:rules

NO EJECUTAR firebase deploy --only functions:...; Tu voz en Spark NO utiliza Cloud Functions.
El archivo firebase.json conserva la configuración del código de Bonds, pero eso no implica desplegarlo.

Anonymous Auth debe estar activado. Índice compuesto voice_ideas(visible ASC,createdAtMs DESC) ya está descrito en firestore.indexes.json. Si no está instalado:

    firebase deploy --project lumi-7c924 --only firestore:indexes

No se necesitan colecciones iniciales: se crean con la primera publicación. Una lista pública vacía es normal hasta que haya ideas aprobadas.

## Cómo probar con dos dispositivos
1. Instalar el último APK con el google-services.json de lumi-7c924.
2. Abrir Tu voz en móvil A: no debe haber "PERMISSION_DENIED".
3. Publicar una sugerencia en A; en Mis votos / propias aparece Pendiente de revisión. La idea NO debe aparecer públicamente en móvil B.
4. Aprobar manualmente la idea (ver abajo). Confirmar que aparece en ambos móviles.
5. A vota; el contador sube en ambos dispositivos. A quita el voto; baja. B vota; se actualiza en ambos. Cerrar y reabrir para comprobar persistencia.
6. A envía un comentario: solo irá a moderación. Aprobarlo y comprobar aparición en ambos.
7. Reportar en B; aparece en voice_reports (no accesible por otros usuarios).
8. Desconectar internet y comprobar mensajes de error/pendientes; no prometer éxito si se rechazó.
9. Revisar los ocho temas, TalkBack, botones físicos Back, tamaño de fuente, pantallas y rotación.

## Moderación SIN Blaze

### Opción A: Firebase Console (manual, sin instalar nada)
En Firebase Console -> Firestore Database -> Data:
- Ver cola voice_pending_ideas/{id}. Allí aparecen title,description,category,ownerUid,createdAtMs.
- Leer contenido y datos personales antes de aprobar.
- Si se aprueba: crear documento en voice_ideas con **el mismo ID** que la propuesta pendiente, y estos campos:
  - title, description, category, createdAtMs: copiar de la solicitud.
  - status: string "Recibida"; visible: boolean true; votesCount: number 0; commentsCount: number 0; updatedAtMs: number (timestamp Unix en milisegundos).
  - NO COPIAR ownerUid ni cualquier dato privado al documento público.
- Actualizar voice_my_submissions/{ownerUid}/items/{id}: status "Recibida", public true.
- Después de verificar el resultado, eliminar el documento de voice_pending_ideas.
- Para rechazar: marcar "No publicada" en la copia privada y borrar la cola.
- Comentarios: ver voice_pending_comments, crear voice_ideas/{ideaId}/comments/{commentId} con body,team false,createdAtMs, e incrementar commentsCount del documento padre; después borrar el pendiente.
- Cambiar la hoja de ruta: editar **status** del documento público, a "En revisión", "Aceptada", "En desarrollo", "Disponible" o "No prevista". Los clientes NO pueden cambiarlo.
- Los reportes se ven en voice_reports. Para ocultar una idea cambia visible a false.

En Firebase Console, los administradores con permisos de proyecto pueden escribir directamente; las reglas restringen a las apps, no a los administradores de Firebase Console.

**Riesgo operativo:** editar manualmente varios documentos NO es atómico. Para revisar muchas solicitudes o garantizar la consistencia de varios campos, preferir la CLI local.

### Opción B: script de moderación local (Admin SDK)
La rama contiene functions/moderate-voice.cjs, que se ejecuta en tu propio PC. No se despliega ni requiere Blaze. Usa credenciales de administrador autorizadas por IAM (Google Cloud Application Default Credentials), nunca copias de claves en Git.

    cd functions
    npm ci
    # CONFIGURAR ADC en la terminal local, según tu cuenta de Google Cloud.
    # Por ejemplo, con el SDK de Google Cloud instalado:
    gcloud auth application-default login
    export GOOGLE_CLOUD_PROJECT=lumi-7c924
    node moderate-voice.cjs list
    node moderate-voice.cjs approve ID
    node moderate-voice.cjs reject ID
    node moderate-voice.cjs comments
    node moderate-voice.cjs approve-comment ID
    node moderate-voice.cjs reject-comment ID
    node moderate-voice.cjs reports
    node moderate-voice.cjs status ID "En desarrollo"
    node moderate-voice.cjs hide ID

Los comandos que aprueban ideas/comentarios usan transacciones del lado administrativo.

## Riesgos y limitaciones reales

1. **Spark no tiene Cloud Functions**. No se debe ejecutar despliegue de functions.
2. Sin un backend que valide identidad, un usuario malicioso puede crear múltiples cuentas anónimas y manipular la popularidad entre esas identidades. Las reglas protegen contra aumentar el contador sin un voto correspondiente de cada UID, no contra identidades falsas.
3. No hay rate limit servidor fuerte para la creación de ideas y comentarios: podrían generar spam/consumo de cuota. Mantener un lanzamiento CERRADO mientras se implementa Firebase App Check (gratuito) y se evalúan herramientas de moderación y límites más robustos.
4. El modo Tu voz **sí** guarda contenido enviado por las usuarias, identificadores anónimos y votos en Firestore: actualizar política de privacidad; esto es una excepción al resto de Lumi on-device. No compartir datos clínicos.
5. Se listan 100 ideas públicas y 100 comentarios por propuesta; requiere paginación para crecer.
6. La sesión anónima de una instalación se pierde al borrar datos de la app. Los votos antiguos no pueden recuperarse automáticamente después de reinstalar.
7. Firestore Spark tiene cuotas gratuitas compartidas con todo el proyecto y puede restringir el servicio al agotarse. Consultar Firebase Console y los cupos actuales.
8. Se necesita revisar contenido denunciado y atender solicitudes de eliminación de publicaciones y datos; los avisos y filtros no equivalen a moderación completa.
9. Falta ejecutar pruebas de punta a punta en dos teléfonos con diferentes cuentas y validar UI 1:1 con el HTML.

## Tests (sin tocar producción)
La rama incluye tests de reglas que se ejecutan en el emulador Firestore (GitHub Actions: Tu voz · Android y reglas Spark):

    npm ci --prefix tools/firestore-tests
    firebase emulators:exec --project demo-lumi-bonds --only firestore "cd tools/firestore-tests && node --test voice_rules.test.cjs"
    ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest

**No subir esta funcionalidad a todas las usuarias hasta validar la moderación, App Check, protección contra abuso, privacidad y los tests.**
