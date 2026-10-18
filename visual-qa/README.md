# Lumi Visual QA & Auditoría Independiente

Este directorio almacena los artefactos, evidencias, capturas, scripts y resultados de las auditorías visuales y funcionales independientes para la implementación nativa de **Lumi Onboarding v7**.

## Estructura de Directorios

- `evidence/checkpoint-1/`: Evidencias de navegación, 8 pasos, flujo acompañante y persistencia atómica.
- `evidence/checkpoint-2/`: Evidencias de fidelidad de los 8 temas visuales, tokens de color, contraste OLED y tipografía.
- `evidence/checkpoint-3/`: Evidencias de microinteracciones (botón animado con órbita y drift, checkbox progresivo, input de apodo, glider de anticoncepción, estrellas de 3 capas y revelación circular sin destellos).
- `evidence/checkpoint-4/`: Evidencias de la secuencia atmosférica final (~11.5s), mensajes, luz cromática sin velo gris/blanco, vuelo del logo hacia `LumiTopBar` (~590ms) y omisión con "Entrar ahora ↗".
- `evidence/checkpoint-5/`: Evidencias de edge cases, validación de accesibilidad (`reduced-motion`), tests automatizados y cierre de auditoría.
- `scripts/`: Scripts independientes de validación estática, comparación visual, recolección de métricas y chequeo de contratos.

## Reglas del Auditor
1. **Estricta independencia:** No modificar código en `app/src/main/` ni en `HomeScreen.kt`.
2. **Evidencias reales:** No declarar verificada ninguna funcionalidad o fidelidad sin soporte de prueba real (captura, log de prueba unitaria/instrumentada o traza).
3. **Clasificación de hallazgos:** P0 (bloqueante), P1 (mayor), P2 (menor), P3 (mejora).
