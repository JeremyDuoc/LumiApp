# LUMI — INICIO INMEDIATO DE IMPLEMENTACIÓN NATIVA CON DOS AGENTES

**Objetivo**: empezar AHORA a reproducir el onboarding HTML v7 en Kotlin/Jetpack Compose en una rama aislada, mientras el diseño final del Home y la selección definitiva de iconos se trabajan en paralelo fuera de este agente.

## Documentos de referencia
- `reference/lumi_onboarding_v7_luz_cromatica.html`: contrato visual y de movimiento del onboarding. Debes abrirlo y probarlo, no limitarte a leer su código.
- `context/Lumi_Contexto_Continuidad_Onboarding_v7.md`: decisiones de producto ya aprobadas.
- `reference/kotlin-snapshots-READONLY/*`: ejemplos del código previo, solo para orientación. Los archivos reales del repositorio Android son la fuente de verdad funcional y arquitectónica.
- `candidates-icons-NOT-FINAL/*`: candidatos de iconos, NO aprobados para uso final. Conserva iconos compatibles temporales y deja una interfaz para reemplazar los recursos después.

## Roles y coordinación obligatoria
Usa exactamente **dos roles principales** en este proyecto:

**Agente 1 / IMPLEMENTADOR** (este agente principal): responsable de investigar dependencias, implementar Compose, ejecutar la app, crear pruebas y resolver observaciones. Trabaja sobre una rama o nuevo Git worktree específico para onboarding, nunca editando el Home mientras se está diseñando fuera.

**Agente 2 / AUDITOR** (subagente especializado): crea o reutiliza `.agents/agents/lumi-visual-auditor.md` e invócalo usando `invoke_subagent` si está disponible. Dale instrucciones autocontenidas, pues no hereda la conversación del implementador. Debe auditar de modo **solo lectura**. Puede ejecutar pruebas y crear evidencias bajo `visual-qa/` si los permisos lo permiten, pero jamás modificar código de producción ni autoaprobar su propio trabajo. Prepara casos de prueba desde el comienzo y reevalúa cada checkpoint del implementador. Si no hay `invoke_subagent` disponible, usa una segunda conversación/agente aislado con acceso de lectura y traspaso explícito de evidencias, y declara el método real utilizado.

Evita auditorías sobre archivos a medio escribir. Usa checkpoints concretos (commits locales o snapshots estables) y reporta resultados accionables; el implementador corrige y solicita una segunda revisión. Prioriza hallazgos P0/P1; no consumas miles de tokens debatiendo ajustes de 1 px antes de tener arquitectura estable.

## Alcance autorizado
SÍ: onboarding completo (8 pasos en modo seguimiento, flujo reducido en modo acompañante), tokens/tema según necesidades de onboarding, logo animado oficial, microinteracciones, fondo estrellado, cambio de tema circular, guardado, validación y transición final; pruebas y reportes.

NO: rediseñar o reescribir `HomeScreen.kt`, cambiar funcionalidades de otras pantallas, navegar a un Home inventado, publicar versiones, desplegar servidores, activar costos, borrar datos de usuario, alterar reglas de seguridad o sobreescribir trabajo ajeno.

**Contrato con Home**: respetar la API de navegación existente (`onComplete` y la arquitectura de rutas real). Dejar preparada la transición mediante un componente reusable / controlador de estado y un contrato documentado de origen/destino para el logo. Mientras no exista un Home definitivo, NO afirmar que la transición extremo a extremo está terminada ni editar `HomeScreen.kt`. Es válido usar un destino temporal SOLO dentro de pruebas o previews, no reemplazar el Home real.

**Iconos**: pendientes de selección; no bloquear la implementación general por ellos. Preparar recursos/referencias intercambiables y registrar los pendientes. No insertar los PNG candidatos como definitivos ni convertirlos arbitrariamente.

## Requisitos visuales no negociables
1. HTML aprobado es referencia visual y de movimiento, no licencia para WebView. Implementa en Kotlin/Jetpack Compose nativo.
2. Replicar estructura, proporciones, tipografías, jerarquía, colores, ocho temas, estados, fondos, tarjetas, selección, botón animado, checkbox trazado, input del apodo y selector deslizante de anticoncepción hormonal.
3. Estrellas: capas ascendentes reales, movimiento muy suave; respeto de reducción de movimiento y rendimiento.
4. Cambio de tema: revelación circular desde el control, con corrección de colores y sin flashes blancos en temas oscuros.
5. Secuencia final atmosférica: objetivo de ~11,5 s según HTML v7; varios mensajes de bienvenida, logo oficial animado en el centro, luz rosa/lavanda/melocotón/verde agua perceptible SIN halo blanco o gris, movimiento final del logo de unos 590 ms hacia posición real de TopBar. Habilitar omisión/reduced motion y evitar duplicación de logo.
6. No simular cálculos médicos o IA que no existan. No iniciar navegación de éxito hasta finalizar guardado real y consistente; no duplicar guardados.
7. Revisión funcional: permisos/consentimiento, pasos opcionales, observer vs tracking, guardado de perfil, primer ciclo solo con fecha real, edición, vuelta, errores, reintento y restauración de estado.

## Flujo de trabajo
**Etapa 0**: crear rama/worktree y commit de referencia; inspeccionar proyecto y HTML; preparar plan por hitos breve y matriz HTML ↔ Compose; lanzar auditor para preparar protocolo independiente.
**Etapa 1**: estructura de 8 pantallas, navegación y persistencia, paridad funcional.
**Etapa 2**: ocho temas, medidas, tipografías, logo, componentes compartidos y recursos intercambiables.
**Etapa 3**: animaciones de elementos (botón, checkbox, radio, apodo, tabs), estrellas y cambio de tema.
**Etapa 4**: onboarding → handoff reusable de Home, sin tocar Home real; pruebas de secuencia contra preview.
**Etapa 5**: revisión visual final, edge cases, performance, accesibilidad, informe, lista de pendientes.

Después de cada etapa: compilar, ejecutar pruebas, capturar estado HTML con browser/Playwright y Android con emulador/ADB cuando existan; igualar resolución lógica, densidad, tamaños de letra, dataset y tema; crear comparaciones y correcciones. Para animaciones grabar videos reales y extraer keyframes temporales. Si falta navegador/emulador, deja pruebas automatizables y explica el bloqueo sin atribuirte verificaciones que no hiciste.

## Entregables en el proyecto
- `visual-qa/`: capturas HTML, Android, diffs/overlays y videos si es posible.
- `ONBOARDING_IMPLEMENTATION_PLAN.md`: mapa de componentes y alcance.
- `ONBOARDING_HOME_HANDOFF.md`: contrato no invasivo para integración futura del Home y animación del logo.
- `ONBOARDING_AUDIT.md`: hallazgos del auditor, severidad, corrección y estado.
- `VISUAL_FIDELITY_REPORT.md`: pruebas ejecutadas, estado por pantalla/tema, tiempo/curva de movimiento y diferencias pendientes.

**Seguridad:** no hagas operaciones destructivas, migraciones con pérdida de datos, push, merges a main, cambios de producción, despliegues ni actualizaciones mayores sin permiso. Si necesitas aprobación de herramientas, solicita permiso; no inventes una autorización.

**Condición de fin:** NO dar como 100% terminado hasta recibir y aprobar Home/iconos finales y realizar QA real. Mientras tanto, completa todo lo posible y deja la integración claramente preparada.

Empieza ya: revisa archivos, arranca el auditor y ejecuta la etapa 0 y siguientes de forma autónoma. Reporta hitos y bloqueos concretos, no te detengas esperando confirmación por detalles que puedes resolver conservando el diseño aprobado.
