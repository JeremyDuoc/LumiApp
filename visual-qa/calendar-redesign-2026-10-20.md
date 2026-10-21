# Calendario y semana de Inicio

Rediseño nativo de Lumi, con consulta de fechas y registro diario como acciones principales.

## Comportamiento

- Tocar un día selecciona la fecha y muestra su resumen: fase calculada, ánimo, flujo, dolor, síntomas y notas cuando existen.
- La acción de registrar o editar permanece sobre la navegación inferior e incluye la fecha elegida.
- Las fechas futuras muestran contexto y estimaciones; no permiten guardar vivencias futuras.
- Meses navegables con flechas, gesto horizontal y selector de fecha. Acceso explícito a Hoy.
- Vista anual con dos meses por fila, cantidad de días registrados y apertura del mes seleccionado. Mantiene su desplazamiento separado de la vista mensual.
- Accesos con texto a registros y ciclos; leyenda visible junto al calendario.
- Los registros y las fases calculadas son datos separados. Un ✓ indica un registro; el borde discontinuo identifica proyecciones futuras. Hoy y selección tienen marcas distintas.
- La ventana fértil se nombra como ventana, conforme a los días que devuelve el predictor existente.

## Inicio

La semana comparte las celdas del calendario. Ofrece navegación por semanas, retorno a hoy, estado del registro y fase estimada de la fecha seleccionada. Registrar o editar hoy abre el formulario; otras fechas abren el calendario en esa fecha.

El círculo principal del Home continúa describiendo hoy al explorar la semana. Los indicadores de registro se actualizan con los cambios de la base local. La semana también se muestra sin ciclo activo, con fase desconocida, para permitir registrar días.

## Registro y datos

El editor existente conserva su catálogo, favoritos, personalización, borradores y manejo de errores. Se ajustan el encabezado al editar y la pregunta para fechas pasadas. El guardado del calendario utiliza la fecha completa que abrió el editor, no el mes que pueda estar mostrándose después.

No se cambian las tablas de Room ni el algoritmo de predicción. Se conservan los cambios que ya existían en el espacio de trabajo.

## Apariencia y accesibilidad

Tipografía editorial, superficies de Lumi y colores del tema activo. Contraste adaptativo del texto sobre el acento, estados de selección accesibles y fecha completa en las etiquetas de las celdas. Contenido desplazable y acción principal disponible con texto ampliado.

Textos nuevos en español e inglés; otros idiomas usan los recursos de respaldo.

## Repetir la verificación

Desde la raíz del proyecto, con el JDK de Android Studio:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\visual-qa\test-calendar.ps1 -Serial emulator-5554
```

El script comprueba que el destino sea un emulador Pixel 7. Instala con actualización y ejecuta pruebas sintéticas sin escribir registros de salud. No desinstala la aplicación.

Evidencia: [capturas](evidence/calendar/calendar-qa/) y [resultado de interfaz](evidence/calendar/pixel-7-tests.txt).

## Resultado de la verificación

- Compilación final: `assembleDebug`, `assembleDebugAndroidTest` y `testDebugUnitTest` correctos.
- 175 pruebas unitarias: 0 fallos y 0 errores, incluidos cambios de mes con febrero, año bisiesto y cambio de año.
- 11 pruebas de interfaz en Pixel 7 Pro: 0 fallos. Cubren selección y edición, fechas futuras, vista anual, semana del Inicio y regresiones del formulario diario.
- Capturas revisadas en los ocho temas: Noir, Ciruela, Bosque, Índigo, Pétalo, Marfil, Salvia y Lavanda. Con texto al 160 %, la acción principal sigue visible y el contenido se puede desplazar.
- Recorrido manual: Inicio → seleccionar 19 de octubre → Ver día → Registrar. El calendario y el editor mantienen el 19 de octubre de 2026; el formulario pregunta «¿Cómo te sentiste?». El núcleo del Inicio conserva el día de hoy. El editor se cerró sin guardar cambios.
- `git diff --check` sin errores de espacios.

Capturas de la aplicación integrada: [calendario](evidence/calendar/live-calendar.png) e [Inicio](evidence/calendar/live-home.png). La compilación debug queda en `app/build/outputs/apk/debug/app-debug.apk`.

