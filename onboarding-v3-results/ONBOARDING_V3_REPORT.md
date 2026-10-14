# Lumi — Reporte de Implementación: Onboarding v3 + Logo Unificado y Animado

## Resumen Ejecutivo

Se completó con éxito la implementación nativa en **Jetpack Compose + Kotlin** del nuevo diseño y flujo de Onboarding a partir de la especificación `Lumi_Onboarding_Logo_Contextual_v3 (1).html`.

Adicionalmente:
1. Se implementó el **nuevo logo de Lumi animado** (`LumiBrandFlower` + `LumiBrandLockup`) con respiración orgánica, oscilación angular, aura difusa y estrella `✦` titilante.
2. Se unificó dicho logo en la barra superior (`LumiTopBar`) de **Home** y en todas las pantallas principales (`InsightsScreen`, `BondsHomeScreen` y `OnboardingScreen`).
3. Se corrigió el saludo en `HomeScreen` para respetar la auditoría: `"Hola."` cuando no hay nombre registrado y `"Hola, [Nombre]."` con tipografía serif itálica de acento cuando existe un nombre.
4. Se trabajó en un **worktree aislado** (`.worktrees/onboarding`) sobre la rama `feature/lumi-onboarding-v3`, protegiendo el trabajo concurrente de otros agentes en el repositorio.

---

## Componentes y Arquitectura Desarrollada

### 1. Logo Animado e Identidad de Marca (`LumiTopBar.kt`)
- **`LumiBrandFlower`**:
  - Animación cíclica infinita (`rememberInfiniteTransition`):
    - Escala orgánica tipo respiración: `1.0f` a `1.038f` (curva `EaseInOut`).
    - Oscilación de ángulo sutil: `-25f` a `-23.2f`.
  - Shader de gradiente sweep multicolor orgánico (amarillo pálido, rosa orquídea, violeta lavanda, azul cielo).
  - Aura radial con desenfoque difuso y pulsación de opacidad.
- **`LumiBrandLockup`**:
  - Empareja el isotipo floral con el wordmark `"lumi"` (con peso 800 y tracking apretado).
  - Integra la estrella `✦` animada con respiración de escala (`0.92f` a `1.15f`), opacidad (`0.45f` a `1.0f`) y rotación (`-8f` a `12f`).
  - Ahora es el componente canónico utilizado en `LumiTopBar` y en todos los encabezados.

### 2. Flujo de Onboarding Nativo (`OnboardingScreen.kt` + `OnboardingViewModel.kt`)
- **Gestión de Estados y Navegación**:
  - Modos admitidos: `track` (8 pasos: 0 a 7) y `support` (4 pasos: 0, 1, 2, 7).
  - Reglas de visibilidad en el encabezado:
    - Logo del encabezado superior oculto en Paso 0 y Paso 7 (donde se muestra el logo central grande).
    - Barra de progreso (`Stepbar`) visible en Pasos 0..6 y oculta en Paso 7.
- **Paso a Paso**:
  - **Paso 0 (Bienvenida)**: Logo central grande animado, título con acento en itálica, selector de modo ("Seguir mi ciclo" vs "Acompañar a alguien"), botón "Empezar →".
  - **Paso 1 (Seguridad)**: Tarjeta de límites clínicos con viñetas, doble checkbox obligatorio (descargo médico + política de privacidad), botón modal "Consultar política", botón "Continuar →" con validación reactiva.
  - **Paso 2 (Tu nombre)**: Campo de texto con límite de 32 caracteres, tarjeta de vista previa en tiempo real con `"Hola."` o `"Hola, [nombre]."`, botón "Omitir" accesible.
  - **Paso 3 (Tu ciclo)**: Selector de regularidad (3 opciones con iconos), tarjeta expandible de duración habitual con slider (21 a 45 días) o alternativa "No lo sé".
  - **Paso 4 (Último periodo)**: Selector de modo "Elegir fecha" vs "No lo recuerdo". Integración nativa con `DatePickerDialog` de Material 3 y formato localizado en español.
  - **Paso 5 (Preferencias)**: Matriz 2x2 de objetivos ("Entender mi ciclo", "Conocer mi bienestar", "Buscar embarazo", "Evitar embarazo"). Alerta de seguridad automática si se elige evitar embarazo. Selector de anticoncepción hormonal ("Sí", "No", "Prefiero no decir").
  - **Paso 6 (Personalización)**: Slider de duración de sangrado (1 a 10 días), 5 cuadrículas de síntomas de interés (Cólicos, Sueño, Estado de ánimo, Fertilidad, Estrés), acordeón expandible de perfil físico opcional (Edad, Altura en cm, Peso en kg).
  - **Paso 7 (Listo)**: Logo central animado, cuadrícula 2x2 de resumen con botones individuales de `Editar` con salto directo a cada paso específico, y botón primario `"Ver mi inicio"`.
- **Selector de Tema Dinámico**:
  - Píldora interactiva `"◐ Tema: Noir"` / `"◑ Tema: Blush"` en la esquina superior derecha para alternar instantáneamente entre los temas aprobados.

---

## Verificación Visual y Capturas del Emulador

Las capturas fueron tomadas directamente en el emulador Pixel 7 Pro (`1440 x 3120` px) y se encuentran almacenadas en `onboarding-v3-results/screenshots/`:

| Archivo | Descripción |
|---|---|
| `01_step0_bienvenida.png` | Paso 0: Bienvenida con logo grande animado y selección de modo. |
| `02_step1_seguridad_disabled.png` | Paso 1: Tarjeta médica con botón "Continuar" deshabilitado (gating de seguridad). |
| `02_step1_seguridad_enabled.png` | Paso 1: Checkboxes activados y botón "Continuar" habilitado. |
| `02_step1_politica_dialog.png` | Paso 1: Diálogo nativo modal de Política de Privacidad. |
| `03_step2_nombre_empty.png` | Paso 2: Campo de nombre vacío mostrando vista previa "Hola.". |
| `03_step2_nombre_filled.png` | Paso 2: Nombre "Jeremy" ingresado mostrando vista previa en vivo "Hola, Jeremy.". |
| `04_step3_ciclo_initial.png` | Paso 3: Selector de regularidad y estado inicial. |
| `04_step3_ciclo_selected.png` | Paso 3: "Bastante regulares" seleccionado y slider de 28 días desplegado. |
| `05_step4_periodo_no_recuerdo.png` | Paso 4: Opción "No lo recuerdo" con tarjeta explicativa. |
| `05_step4_periodo_elegir_fecha.png` | Paso 4: Opción "Elegir fecha" con botón de selección. |
| `05_step4_datepicker_dialog.png` | Paso 4: Diálogo M3 DatePicker nativo en español. |
| `05_step4_periodo_fecha_seleccionada.png` | Paso 4: Fecha confirmada ("martes, 13 de octubre 2026") y botón Continuar habilitado. |
| `06_step5_preferencias_warning.png` | Paso 5: Alerta visible al elegir objetivo "Evitar embarazo". |
| `06_step5_preferencias_selected.png` | Paso 5: Objetivo "Entender mi ciclo" y anticoncepción hormonal "No". |
| `07_step6_personalizacion_expanded.png` | Paso 6: Slider de sangrado (5 días), síntomas seleccionados y acordeón desplegado. |
| `07_step6_personalizacion_profile_fields.png` | Paso 6: Campos físicos (Edad, Altura, Peso) dentro del acordeón. |
| `08_step7_listo_resumen.png` | Paso 7: Resumen final en tema Noir con cuadrícula 2x2 y botones Editar. |
| `09_step7_blush_theme.png` | Paso 7: Resumen final en tema Blush (modo claro cálido). |
| `10_step_jump_to_name.png` | Navegación: Salto directo hacia Paso 2 al presionar "Editar" en "Tu saludo". |
| `11_home_screen_with_greeting_and_topbar.png` | Pantalla Home: Logo animado en LumiTopBar, saludo "Hola, Jeremy.", ciclo 2 calculado. |
| `12_insights_screen_with_unified_logo.png` | Pantalla Insights: Mismo logo unificado `lumi✦` en la esquina superior izquierda. |
| `13_bonds_screen_with_unified_logo.png` | Pantalla Bonds: Mismo logo unificado `lumi✦` en la barra superior. |

---

## Verificación de Tests Unitarios

Ejecución de suite completa de pruebas:
- **`OnboardingV3VerificationTest.kt`**:
  - `trackMode_hasEightSteps`: Verifica que el flujo de seguimiento contenga los 8 pasos completos (0 a 7).
  - `supportMode_hasFourSteps`: Verifica que el modo acompañante filtre a los 4 pasos relevantes (0, 1, 2, 7).
  - `step1_cannotAdvanceWithoutBothConsents`: Verifica que se requiera tanto el descargo médico como la privacidad.
  - `step3_cannotAdvanceWithoutRegularityChoice`: Verifica que se requiera elegir la regularidad.
  - `step4_dateValidation`: Verifica que si se indica fecha conocida, se exija una fecha válida > 0.
  - `greetingFormat_handlesEmptyAndNonEmpty`: Verifica `"Hola."` para apodo en blanco y `"Hola, Jeremy."` para apodo definido.

**Resultado:** `BUILD SUCCESSFUL in 35s` (todas las pruebas pasan limpiamente).
