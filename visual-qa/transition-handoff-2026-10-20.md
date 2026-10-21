# Lumi: ceremonia y llegada al Home

Revisión del video `C:/Users/kiwix/Videos/2026-10-19 23-11-21.mp4` e implementación en los cinco archivos principales solicitados.

## Hallazgos del video

- Al comienzo, el logo atraviesa el resumen mientras el formulario conserva demasiado protagonismo.
- El escalado desde la esquina desplaza el centro visual de la marca.
- La atmósfera cambia poco durante la espera; los textos son pequeños respecto de la marca.
- Cerca de 13,0 s del video, Home aparece detrás de la marca central y sus textos se mezclan con los de la ceremonia.
- El último mensaje apenas llega a estabilizarse antes del vuelo.
- La entrada del contenido de Home se ejecutaba durante la precarga, detrás del velo.
- La marca en vuelo escalaba un lockup grande sin igualar exactamente la tipografía, estrella y espaciado de la cabecera.

## Secuencia implementada

Los tiempos son relativos al inicio de la ceremonia, después de guardar correctamente el onboarding. Duración normal: **13,8 s**; antes: 11,52 s.

| Momento | Comportamiento |
| --- | --- |
| 0–1,2 s | El resumen se desvanece y la marca se recoge en el centro desde sus coordenadas reales. |
| 0,9 s | Se monta Home una sola vez, con el velo completamente opaco. |
| 1,4 / 3,55 / 5,7 / 7,85 / 10 s | Cinco mensajes, con salida antes del siguiente, tipografía editorial y cinco luces de progreso. |
| 11,7–12 s | Despedida de los textos. |
| 12–13,25 s | Vuelo curvo hacia la marca medida del Home; la flor se recoge y la tipografía converge a las métricas reales de la cabecera. |
| 12,7–13,65 s | La marca ya ha dejado atrás la zona del saludo. Se revela Home con entrada escalonada del saludo, núcleo, contenido, acciones y navegación. |
| 13,8 s | Se retira el overlay con Home ya visible y sin volver a crearlo. |

La atmósfera conserva rosa, violeta, salvia y melocotón. Sus luces orbitan lentamente; los contornos asimétricos hacen referencia al núcleo vivo de Lumi. Los degradados proporcionan suavidad sin cuatro capas de blur.

“Entrar ahora” y Atrás usan el mismo aterrizaje abreviado. Con las animaciones del sistema desactivadas se omite la ceremonia. La llegada espera a que Home esté listo. El reloj se pausa al salir de la aplicación. El Home oculto no expone sus controles a accesibilidad durante la ceremonia.

Los pasos del onboarding tienen entrada y salida separadas, dirección coherente, protección frente a avances repetidos y espacio para teclado y navegación del sistema.

## Validación

- `:app:assembleDebug`: correcto.
- `:app:testDebugUnitTest`: 165 tests, sin fallos.
- `LumiCeremonyTest`: 6 tests instrumentados correctos: duración completa y último mensaje, callbacks únicos, Home lento, omisión temprana con Atrás, movimiento reducido, botón Entrar ahora y separación de textos/progreso en Pétalo con letra al 140 %.
- Recorrido real desde el onboarding hasta Home en un Pixel 7 Pro virtual aislado.
- Inspección visual de Noir y Pétalo, incluyendo la captura con texto ampliado.
- `git diff --check`: correcto.

Evidencias locales (generadas, no incorporadas al control de versiones):

- [Grabación final](../build/transition-review/lumi-arrival-final.mp4)
- [Ceremonia Noir](../build/transition-review/theme-captures/ceremony-noir.png)
- [Pétalo, texto al 140 %](../build/transition-review/theme-captures/ceremony-petalo-large-text.png)
- [Log de pruebas instrumentadas](../build/transition-review/final-ceremony-tests.log)
- [Log de compilación y tests unitarios](../build/transition-review/verified-build.log)

El emulador usa SwiftShader/renderizado por software. La grabación permite revisar composición, orden y continuidad, pero su cadencia no acredita el rendimiento de un teléfono físico. No se ha medido rendimiento en hardware real.
