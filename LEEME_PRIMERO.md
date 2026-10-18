# Lumi Android v19 — temas en Home/Ajustes y apodo animado

Base: entrega v18 íntegra, que incluía los cambios v17. **No mezclar con ZIPs anteriores.**

## Diagnóstico de los videos

- `1000035587.mp4`: la transición circular del onboarding funciona; al navegar a Home y Ajustes, la selección cambia colores pero no se percibe la misma animación conectada.
- `1000035588.mp4`: el campo del nombre en Compose no tiene la elevación, el halo exterior ni la luz interna expansiva que acompañan al foco en el HTML.
- `1000035590.mp4`: HTML de referencia: el campo se eleva, aparece el glow, el subrayado crece hacia la derecha, el pequeño destello rota y las partículas están desfasadas.

## Cambios aplicados

1. `ui/navigation/MainScreen.kt`: el pulso temático de 720 ms se dibuja **por encima de Scaffold**, por lo tanto también cubre Ajustes, la barra inferior y pestañas. Sigue activo aunque Home no esté visible.
2. `ui/screens/home/HomeScreen.kt`: elimina pulso duplicado (se dibujaría dos veces al estar en Home).
3. `ui/theme/Theme.kt`: CompositionLocal con el tema real desde la raíz para impedir un falso cambio Noir→tema guardado en el primer montaje; curva ease-out CSS para el pulso. No se toca el efecto circular del onboarding.
4. `ui/screens/onboarding/OnboardingStepComponents.kt`: Step2Nickname vuelve a simular el CSS de referencia: borde animado de 360 ms, elevación de -2 dp, shadow de acento, ripple interno de 600 ms (250 px aprox.), subrayado de 550 ms, destello SVG de 19 dp con giro de 13°/420 ms, puntos luminosos de 2300 ms con delays de 0/250/490/720 ms empezando al obtener foco. Mantiene BasicTextField nativo, límite 32 caracteres, teclado y vista previa.

## Comportamiento esperado y delimitación

- Onboarding: revelación circular de 690 ms (SIN cambiarla).
- Home: pulso radial del HTML de 720 ms en todo el contenedor principal, no círculo.
- Ajustes: exactamente el mismo pulso radial, ahora visible aun sin Home compuesto.
- Campo apodo: la animación solo se activa mientras el usuario tiene foco.

## Instalación

Haz commit/back-up. Extrae el ZIP a la raíz del proyecto para reemplazar los archivos **en sus rutas**. Usa únicamente esta v19; incluye archivos heredados de v18.

## Pruebas de regresión obligatorias

1. Arranca con tema guardado distinto de Noir: no debe producir un pulso al abrir MainScreen.
2. Cambia Noir→Pétalo→Noir desde Home y Ajustes; ambos deben reproducir un pulso único y cubrir también la barra inferior. No debe aparecer overlay gris o blanco.
3. Cambia temas dentro del onboarding; su círculo debe seguir igual a v18.
4. En pantalla "Tu nombre": foco repetido, escribir/borrar, mover cursor, cerrar teclado y volver a enfocar. El glow, la rotación y la entrada de partículas deben reiniciarse cada vez.
5. Prueba tema claro y oscuro, tamaños de letra grandes y teléfono estrecho.

## Limitaciones

Revisión de código y comparación visual de fotogramas, **sin compilación ni emulador Android** porque falta el proyecto Gradle y sus dependencias. Esto no garantiza fidelidad píxel a píxel. Los blur y shadow de Android pueden diferir de CSS según versión de GPU/API.
