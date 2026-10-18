> ACTUALIZACIÓN — 18/10/2026: la fase local descrita abajo fue sustituida por una
> implementación de backend REAL aún SIN DESPLEGAR. Firebase Auth anónimo,
> listeners Firestore, Cloud Functions de creación/voto/comentario/reporte/estado
> y reglas de seguridad están en la rama. Las antiguas propuestas locales y los
> ejemplos simulados no se publican ni migran. Ver
> docs/TU_VOZ_PRUEBA_DOS_DISPOSITIVOS.md para configuración, restricciones de
> privacidad, pruebas de seguridad y despliegue controlado. No publicar aún
> sin moderación y pruebas de dos cuentas/dispositivos.

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


## Correcciones tras la grabación del emulador (18 oct 2026)

Inspección visual de los 38 segundos de navegación y las capturas comparadas con Lumi_Tu_Voz_Animaciones_v3.html:

- El foro no mostraba la cabecera de marca; ahora reutiliza LumiTopBar real y AmbientBackgroundLayer. El botón de temas comparte el sistema de revelado del Home.
- Los iconos de menú del Home se superponían como una raya en estado abierto; las tres barras se dibujan ahora con un Canvas único que interpola sus extremos para formar una X visible.
- La cápsula y opciones del Home sincronizan mejor la apertura (menores esperas escalonadas).
- «Lo que estamos creando» aparecía incorrectamente en En camino y Mis votos; se limita a Ideas, con progreso y enlace de detalle.
- El HTML v3 tenía 5 posts ilustrativos que no aparecían en Android. Ahora se usan SOLO con BuildConfig.DEBUG; cada muestra está identificada y sus cambios nunca se escriben en SharedPreferences. Una build release no muestra esos datos.
- Voto: nuevo pulso reversible que retorna al tamaño normal, con halo pastel radial más suave y check blanco.
- Tarjetas: mínimo 116 dp, estado/categoría/comentarios juntos, divisor y textos más cercanos al HTML.
- Formulario: textos y categorías del HTML v3; evita prometer publicación pública y guarda de forma local.

### Aspectos que la grabación NO permite certificar
La grabación enseña navegación entre pestañas, el menú y comparaciones con Chrome, pero no cubre por completo comentarios, publicación, reportes, interacción administrativa ni sincronización multiusuaria. No hay backend comunitario, moderación remota o permisos verificados de equipo. Las opciones reales de administración siguen restringidas por diseño. No se ejecutó un emulador ni build Gradle en este entorno; compilación, TalkBack, temas claros/oscuro, tamaños 320–420dp, ciclo de vida y rendimiento requieren nueva pasada en Android Studio.
