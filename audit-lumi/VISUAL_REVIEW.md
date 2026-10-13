# Revisión Visual y Accesibilidad — Lumi Bonds
**Dispositivo de prueba:** Emulador Pixel 7 Pro (1440 × 3120 px, 560 dpi, Android 14/15)  
**Fecha:** 12 de octubre de 2026  
**Auditor:** Principal Android UI/UX & Mobile QA Reviewer  
**Referencia visual:** Imagen de 3 dispositivos (`media_1791831210125_89be9333.jpg`)

---

## 1. Galería de Capturas de Evidencia

Todas las capturas fueron tomadas directamente del emulador durante la ejecución de la app y se encuentran archivadas en la carpeta `screenshots/`:

| Pantalla | Archivo de Evidencia | Estado Visual |
|---|---|---|
| **Bonds Home (Teléfono 1)** | `screenshots/screenshot_phone1_home.png` | Alta fidelidad visual a la referencia. |
| **Envío de Gesto (Teléfono 2)** | `screenshots/screenshot_phone2_send_gesture.png` | Alta fidelidad visual (aura orgánica y tarjeta). |
| **Píldora de Confirmación** | `screenshots/screenshot_phone2_sent.png` | Animación y diseño alineados. |
| **Vinculación Superior (Teléfono 3)** | `screenshots/screenshot_phone3_pairing.png` | Hero navy card con brillo y toggles fieles. |
| **Privacidad Inferior (Teléfono 3)** | `screenshots/screenshot_phone3_bottom.png` | Tarjeta de privacidad y escudo. |
| **Calendario Compartido** | `screenshots/screenshot_shared_calendar.png` | Matriz mensual con puntos cromáticos por fase. |
| **Detalle de Miembro (Superior)** | `screenshots/screenshot_member_detail.png` | Perfil con avatar flor y estado de fase. |
| **Detalle de Miembro (Inferior)** | `screenshots/screenshot_member_detail_bottom.png` | Toggles individuales y botón de desvinculación. |

---

## 2. Evaluación Comparativa contra la Referencia

### 2.1 Teléfono 1 — Bonds Home
- **Cumplimiento Positivo:**
  - Anillos circulares con gradientes diferenciados por miembro (`Camila`, `Sofia`, `Valentina`, `Anto`).
  - Tarjeta pastel `"Cómo se siente Camila hoy"` con gradiente melocotón/rosa/lavanda y subtarjeta interna con bombilla para recomendaciones afectivas.
  - Fila de gestos afectivos con el primer elemento (`Abrazo`) destacado con fondo squircle suave.
- **Deficiencias Visuales Detectadas:**
  - **Superposición de StatusBar:** La cabecera superior `BondsTopHeader` carece de `Modifier.statusBarsPadding()`. En dispositivos con recorte o notch (Pixel 7 Pro), los iconos de reloj y batería del sistema se renderizan parcialmente sobre el logotipo y los iconos de acción.
  - **Fondo Fijo:** El fondo `BondsTheme.WarmBackground` (`0xFFFAF7F5`) es estático y no se adapta al tema oscuro del sistema.

### 2.2 Teléfono 2 — Envío de Gesto Afectivo
- **Cumplimiento Positivo:**
  - Aura orgánica multicapa con gradientes radiales y micro-partículas flotantes.
  - Tarjeta central blanca brillante con elevación sutil y taza de té estilizada con vectores de vapor.
  - Botón de acción principal en Deep Navy con esquinas completamente redondeadas (estilo píldora).
  - Píldora inferior de confirmación de envío animada (`[✓] ¡Té enviado! Sabrá que estás pensando en su bienestar. 💜`).
- **Deficiencias Visuales Detectadas:**
  - El campo de texto opcional tiene altura fija pequeña que podría truncar mensajes largos si el usuario incrementa el tamaño de fuente del sistema (Font Scaling > 1.3x).

### 2.3 Teléfono 3 — Vinculación y Privacidad
- **Cumplimiento Positivo:**
  - Hero card en Deep Navy (`#193458`) con resplandor púrpura suave en la esquina superior derecha.
  - Contenedor blanco de código QR de alto contraste y código de 6 dígitos con botón de copia al portapapeles.
  - 4 switches granulares con iconografía exacta (Luna para fase, Sonrisa para ánimo, Rayo para síntomas y Destellos para predicciones).
  - Tarjeta de declaración ética de privacidad con icono de escudo.

---

## 3. Conflicto Crítico de Tema Oscuro (Hallazgo P1-9)

- **Causa técnica:** `Theme.kt` define el color de los iconos de la barra de estado según `!isDark`. Si el usuario tiene activo el modo oscuro en Lumi, los iconos del sistema (hora, batería, Wi-Fi) se pintan en **color blanco**.
- **Comportamiento en Bonds:** Como las pantallas de Bonds utilizan un fondo claro fijo (`WarmBackground`), los iconos blancos del sistema resultan prácticamente invisibles por falta de contraste.
- **Solución requerida:** Envolver las pantallas de Bonds con un `DisposableEffect` que fuerce `isAppearanceLightStatusBars = true` (iconos oscuros) mientras se encuentre en pantallas claras, o desarrollar la versión temática nocturna para Bonds.
