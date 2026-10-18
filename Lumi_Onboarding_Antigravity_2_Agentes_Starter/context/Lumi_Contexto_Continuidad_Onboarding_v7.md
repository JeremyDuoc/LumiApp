# Lumi — Contexto de continuidad: onboarding / Home

**Estado:** Prototipo de onboarding v7 refinado visualmente; pendiente integrar los archivos reales del Home y seleccionar iconos definitivos antes de implementar en Android.

## Base visual oficial de trabajo

- Archivo de referencia: `lumi_onboarding_v7_luz_cromatica.html` (prototipo HTML local). La versión 7 es la base aprobada para continuar, **no rediseñar desde cero**.
- Implementación Android conocida por archivos del usuario: `OnboardingScreen(1).kt`, `OnboardingViewModel(1).kt`, `LumiTopBar(1).kt`.
- **Aún no se entregó `HomeScreen.kt` actual:** el Home ilustrativo del HTML no representa necesariamente la UI real. No asumir su jerarquía, componentes ni datos hasta inspeccionarlo.

## Aspectos visuales y funcionales acordados

- Onboarding de Lumi con 8 pasos para modo seguimiento (flujo reducido para modo acompañante, según ViewModel), títulos editoriales, tarjetas, botones, barra de progreso y logo animado oficial.
- Ocho temas: Noir, Ciruela, Bosque, Índigo (oscuros); Pétalo, Marfil, Salvia, Lavanda (claros). Respetar tokens y contrastes, incluido caso OLED.
- Fondo de estrellas que ascienden suavemente en capas; sin destellos blancos molestos en modos oscuros; reducción de movimiento según preferencias del sistema.
- Botón principal con degradado rosa/lavanda en movimiento adaptado al tema; checkbox de consentimiento con trazo que se dibuja; selección de opciones con transición suave; input de apodo animado; selector deslizante para anticoncepción hormonal; paso de últimos detalles completo con intereses, duración del sangrado y perfil físico opcional.
- Transición final del onboarding al Home: secuencia atmosférica de ~11,5 s, luz de rosa, lavanda, verde agua y melocotón (sin halo blanco/gris), logo vivo en el centro con respiración orgánica y mensajes de bienvenida no engañosos; luego movimiento final hacia esquina superior izquierda rápido (~590 ms) sin duplicar el logo; Home aparece suavemente.
- Dar opción de omitir la animación; mostrarla solo tras el primer onboarding y no en aperturas habituales. No permitir que una animación enmascare fallas de persistencia; guardar antes de dar finalización por exitosa.
- Reproducción nativa en Kotlin/Jetpack Compose, no un WebView; se busca máxima fidelidad visual y de movimiento respecto al HTML final, con medición y validación real en dispositivo.

## Iconos enviados (todos PNG RGBA de 512 × 512)

- `planificacion.png`: calendario simple negro. Candidato para «Seguir mi ciclo», tintado por tema.
- `seguro-medico.png`: corazón/escudo negro. Candidato para tarjeta de seguridad; verificar legibilidad en tamaños pequeños.
- `menstruacion (1).png`: calendario delineado negro con gota. Candidato para «Último periodo».
- `menstruacion.png`: calendario rosado con gota. Candidato para ilustración de sección, no iconografía funcional pequeña.
- `ciclo-menstrual (1).png`: calendario negro con símbolos de ciclo. Más adecuado en tamaño grande (intro/explicación) por complejidad.
- `ciclo-menstrual.png`: calendario ilustrado negro con gota y marcadores. No preferido para iconos pequeños por densidad y estilo.
- `ciclo-menstrual (2).png`: versión ilustrada multicolor del ciclo. Reservar para ilustración más grande, si encaja con la marca.
- **Decisión pendiente del usuario:** qué iconos usar y dónde. No imponer el uso de todos; evitar mezclas de contornos negros con ilustraciones multicolor en controles del mismo nivel.
- Para uso comercial revisar licencia de la fuente de cada icono. Un PNG transparente es técnicamente válido; no afirmar que una vectorización automática reproduce SVG original.

## Pendiente inmediato

1. Recibir archivos Kotlin pertinentes del Home (`HomeScreen.kt` y subcomponentes, tema/tokens, navegación y otros componentes invocados), y si es posible captura reciente.
2. Analizar su jerarquía real, posición y tamaño de `LumiBrandLockup`/`LumiTopBar`, contenido y estados.
3. Sustituir el Home provisional del HTML v7 por una representación fiel del Home actual; conectar transición y posición final del logo sin duplicaciones ni cambios no autorizados en el Home.
4. Elegir e integrar PNG finales en onboarding HTML; probar tamaños, contraste y temas.
5. Congelar HTML aprobado como especificación de referencia; luego implementar Kotlin Compose nativo y hacer comparativa de capturas, tiempos de animación, gestos, accesibilidad, tema y navegación.

## Criterios de producción

- No prometer igualdad matemática de cada píxel entre HTML/CSS en navegador y Jetpack Compose, que difieren en renderizado de fuentes, composición de color y dispositivos. Sí aspirar a equivalencia visual y funcional muy alta, verificarla mediante capturas a densidades/tamaños acordados y corregir discrepancias.
- Mantener intactos los datos reales, consentimiento informado, modos de usuario, lógica de pasos y persistencia existentes, salvo correcciones justificadas de bugs.
- No introducir cálculos de salud ficticios, indicadores de 'IA trabajando' o retrasos artificiales de almacenamiento para justificar mensajes.
