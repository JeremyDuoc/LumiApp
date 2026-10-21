# Registro diario: rediseño nativo

El registro ahora permite elegir ánimo o síntomas y guardar desde una sola pantalla. Periodo, dolor, bienestar, flujo cervical, intimidad y notas se abren a petición. No exige completar pasos ni llenar todos los campos.

## Personalización y edición

- Preferencias de secciones, síntomas favoritos y síntomas propios almacenadas en DataStore.
- Catálogo agrupado, búsqueda sin distinguir tildes y creación de síntomas con validación de duplicados.
- Intensidades individuales de 0 (sin especificar) a 10, independientes del dolor general. Se conservan los valores históricos.
- Los síntomas propios se pueden quitar de un día sin que reaparezcan al guardar.
- Ocultar una sección conserva su información y la mantiene accesible al editar un día que ya contiene datos.
- Borrador guardado fuera de la ventana modal para restaurar las selecciones al recrear el contenido.
- Guardado bloqueado durante la escritura; cierre y confirmación de éxito solo cuando corresponde. Los errores de guardado dejan el borrador disponible para reintentar.
- Integrado en Inicio y Calendario. No requiere migración de Room.

## Diseño y accesibilidad

Tokens de los ocho temas de Lumi, tipografía editorial, superficies suaves, contraste reforzado en botones claros, áreas táctiles de al menos 48 dp, semántica de selección y expansión, indicadores de foco y etiquetas para las acciones. Las selecciones no dependen solo del color.

Transiciones de páginas, cambios de color, presión de botones, desplegables, contadores y respuesta háptica. Las animaciones usan Compose y la escala de animación del sistema.

Textos nuevos en español e inglés. El resto de idiomas utiliza los recursos de respaldo.

## Verificación final

- `assembleDebug` y `assembleDebugAndroidTest`: correctos.
- `testDebugUnitTest`: **171 pruebas, 0 fallos** (incluye 6 pruebas del borrador).
- Pruebas instrumentadas: **6 pruebas, 0 fallos**, AVD **Pixel_7_Pro**, Android 17.
- Capturas de Noir, Ciruela, Bosque, Índigo, Pétalo, Marfil, Salvia y Lavanda.
- Capturas de selección, personalización, catálogo, detalles y texto al 160 %.
- `git diff --check`: correcto.

La prueba de interfaz cubre guardado con un solo ánimo, restauración del borrador, eliminación de síntomas propios, conservación de intensidades, creación de síntomas, personalización sin registrar síntomas accidentalmente, cancelación del cierre, errores de guardado y bloqueo de envíos duplicados.

Evidencias finales: `evidence/daily-log/final-run.txt`, `evidence/daily-log/pixel-7-tests.txt` y `evidence/daily-log/daily-log-qa/`.

Las pruebas automatizadas no sustituyen una sesión de usabilidad con usuarias ni una auditoría manual completa con TalkBack. No se midió rendimiento con Macrobenchmark.

## Repetir las pruebas

Arrancar el Pixel 7 Pro y ejecutar desde la raíz, con un JDK compatible:

```powershell
.\visual-qa\scripts\test-daily-log.ps1 -Serial emulator-5554
```

El script valida que el destino sea un emulador Pixel 7 y usa instalación incremental sin desinstalación final. No selecciona automáticamente un teléfono.

## Incidencia durante la verificación inicial

La primera ejecución mediante `connectedDebugAndroidTest` utilizó el teléfono conectado y el comportamiento predeterminado de AGP desinstaló los APK al terminar. Se reinstaló Lumi en ese teléfono y, tras la indicación del usuario, las pruebas restantes se ejecutaron exclusivamente en el Pixel 7 Pro. No se pudo verificar la conservación de los datos locales anteriores del teléfono.

Se añadió `android.injected.androidTest.leaveApksInstalledAfterRun=true` a `gradle.properties` para evitar esa limpieza en futuras ejecuciones. Los registros iniciales de fallos se conservan como historial; `final-run.txt` y `pixel-7-tests.txt` corresponden a la verificación final correcta.
