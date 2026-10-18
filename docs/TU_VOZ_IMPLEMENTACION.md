# Tu voz · implementación por fases

Referencia visual aprobada: Lumi_Tu_Voz_Animaciones_v3.html. Encabezado Home: Lumi_Home_Menu_Desplegable_v1.html.

## Implementado en esta rama

- Menú superior del Home en Jetpack Compose: hamburguesa ↔ X; cápsula hacia la izquierda; Chat, Temas, Notificaciones con badge y Ajustes. Mantiene las callbacks existentes y el logo vivo, sin modificar el FAB radial.
- Reemplazo provisional de la 4ª pestaña habitual (Bonds) por «Tu voz». El flujo especial de observadores mantiene su navegación previa; las rutas históricas de Bonds no se borran.
- Diseño editorial de «Tu voz» basado en v3 y en LumiThemeTokens: encabezado, CTA, secciones Ideas / En camino / Mis votos, carril indicador 500 ms con cubic-bezier(.33,.83,.99,.98), tarjeta destacada cuando existe una propuesta realmente en desarrollo, ordenación, estado vacío, ficha de detalle y línea de tiempo.
- Botón de apoyo 23 dp y halo reducido, reversible; comentarios; propuesta nueva con límites; ocultación; registro de reporte local.
- Persistencia de las propias propuestas/votos/comentarios con un archivo SharedPreferences **separado** de los registros médicos, calendario y respaldos. Validación de longitud y categoría, con pruebas unitarias en VoiceValidationTest.
- Texto visible deja claro que todo esto es una **demostración local**, sin la falsa pretensión de que haya votos/estados/comentarios de otras usuarias.

## Puntos pendientes antes de que «Tu voz» sea comunidad en producción

1. **Decidir proveedor y alojamiento**. La app ya contiene dependencias Firebase, pero ninguna decisión definitiva para el foro ha sido aprobada. Esta rama **NO** implementa sincronización ni hace despliegues.
2. **Identidad privada** para prevenir votos múltiples y abuso: usar un ID técnico sujeto a autentificación, sin mostrarlo en respuestas públicas. No enviar perfiles, síntomas, fechas menstruales, copias cifradas ni diarios a colecciones públicas.
3. **Escrituras desde backend confiable**, no desde cliente: propuestas, votos, comentarios, reportes y cambios de estado con permisos según autor/rol. Credenciales y roles administrativos deben verificarse EN EL SERVIDOR y por reglas, jamás por toggle de UI.
4. **Moderación real y seguridad**: reportes revisables, ocultación/remoción, normas de comunidad, límites por usuario/IP donde corresponda, protección anti-spam, límites de tamaño, denegación por defecto, App Check, pruebas con varias cuentas, monitoreo de costos y política de retención/borrado.
5. **Lecturas sincronizadas** y paginación, tratamiento de offline/errores, contadores correctos en transacciones atómicas, estados solo administrados por el equipo y accesibilidad/contenido localizado.
6. **Política de privacidad** específica para textos públicos y retención. En modo público, advertir explícitamente que la anonimidad es frente a la comunidad y NO implica anonimato absoluto ante proveedores/administradores de la infraestructura.
7. **Retirar el modo local de demostración** solo después de pruebas de seguridad, producto y backend; nunca presentar votos locales como comunitarios.

## Verificación de diseño

- Comparar con ambos HTML adjuntos: Noir y tema claro, 320/360/390/420 dp, orientación, TalkBack, fuente ampliada y movimiento reducido.
- Menú: comprobar orden Chat–Tema–Notificaciones–Ajustes, persistencia del punto rosa y cierre tras seleccionar opción o pulsar Atrás.
- Foro: crear una idea válida, reiniciar el proceso y comprobar persistencia; votarla dos veces y comprobar reversión; publicar comentario; ordenarla; ocultarla; verificar tabs vacías; navegar al calendario y regresar.
- Ejecutar **:app:assembleDebug** y **:app:testDebugUnitTest** en Android Studio o CI. En esta sesión no había SDK Android disponible para compilar ni emulador para comparar frames. La revisión sintáctica no sustituye esa compilación.

La rama no debe fusionarse a main ni distribuirse como un foro público funcional sin completar las verificaciones y dependencias indicadas.
