# Tu voz — prueba en dos dispositivos (beta privada)

El backend se ha preparado en la rama feat/tu-voz-compose-menu, pero NO está desplegado desde este chat. No confundir con un foro público listo para producción.

## Privacidad y arquitectura

Android reutiliza FirebaseAuth, FirebaseFirestore y FirebaseFunctions inyectados con Hilt. Si no tiene sesión crea una cuenta anónima técnica. Las propuestas públicas contienen solo título, descripción, categoría, estado, contadores y fecha. Los comentarios públicos no exponen un UID. Los comprobantes de voto son legibles solamente por su cuenta. Propietarios técnicos, informes y límites viven en colecciones privadas. No se leen ni envían ciclos menstruales, diarios, respaldos de Drive ni datos Bonds.

Solo las Cloud Functions escriben en la comunidad. La app lee con listeners en tiempo real. Un voto es atómico por UID y los cambios de estado requieren un claim de administrador comprobado en servidor. El feed ya no inyecta ejemplos ni migra publicaciones antiguas guardadas solo en el dispositivo.

## Pasos previos obligatorios

1. En Firebase Console del proyecto lumi-7c924, Authentication / Sign-in method, habilitar Anonymous. Si App Check está aplicado, preparar los dispositivos.
2. Verificar que Firestore existe y que el proyecto es el correcto.
3. Las Cloud Functions de segunda generación requieren un proyecto con facturación habilitada (Blaze). Revisar presupuesto y alertas antes de habilitar facturación. Como alternativa para probar sin publicar en internet, utilizar Firebase Emulator Suite.
4. REVISAR las reglas de Firestore actualmente desplegadas antes de reemplazarlas. El despliegue de reglas reemplaza las de todo el proyecto y podría afectar a Bonds.

## Test de reglas en Emulator Suite (raíz del repo, Node 20)

    cd tools/firestore-tests
    npm ci
    cd ../..
    firebase emulators:exec --project lumi-7c924 --only firestore "node --test tools/firestore-tests/voice_rules.test.cjs"

Comprueba lectura autenticada, rechazo sin autenticar, invisibilidad de documentos privados, votos propios y prohibición de escritura directa.

## Desplegar funciones y reglas SOLO después de revisar los requisitos

    cd functions
    npm ci
    cd ..
    firebase login
    firebase use lumi-7c924
    firebase deploy --project lumi-7c924 --only "firestore:rules,functions:voiceCreateIdea,functions:voiceToggleVote,functions:voiceAddComment,functions:voiceReportIdea,functions:voiceSetIdeaStatus"

No ejecutar el despliegue sin revisar el proyecto y sus reglas de seguridad. La primera invocación de Functions puede ser más lenta por arranque.

## Comprobación con dos teléfonos o emuladores

1. Compilar app:assembleDebug y app:testDebugUnitTest en Android Studio desde la rama.
2. Instalar la misma versión actualizada en ambos dispositivos, ambos apuntando al proyecto real, no a emuladores distintos.
3. Abrir Tu voz en los dos y confirmar indicador Comunidad sincronizada.
4. Dispositivo A: publicar una propuesta válida. Sin refrescar, dispositivo B debe recibirla automáticamente.
5. Dispositivo B: votar. A debe ver el contador actualizado. B: quitar voto. A observa la reversión.
6. Abrir detalle de la misma idea en los dos; B publica comentario, A lo recibe sin cerrar la pantalla.
7. Reabrir la app en ambos: las publicaciones, los votos y los comentarios deben persistir.
8. Probar sin internet, límites de frecuencia, errores de servidor y reportes.
9. Revisar en Firestore que ninguna colección del foro contenga datos menstruales ni información personal identificable.

También puede probarse en red local con Emulator Suite y dos dispositivos apuntando al MISMO host LAN, con configuración de debug y cleartext permitido solo en debug; no habilitar HTTP sin cifrar para release.

## Antes de lanzamiento público

Todavía faltan App Check endurecido, moderación operativa, revisión de reportes, retirada/borrado de contenido, normas comunitarias, políticas de privacidad y retención, medidas contra abuso de múltiples cuentas, pruebas de carga, paginación y vigilancia de costos. Los límites por UID NO detienen por sí solos la creación masiva de identidades anónimas. El flujo de sesión compartido con Bonds también debe someterse a pruebas de regresión. Mantener el PR como borrador.
