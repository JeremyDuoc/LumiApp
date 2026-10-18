# Contrato de Integración y Handoff de Transición: Onboarding v7 ➔ HomeScreen

> **Estado:** Aprobado para integración no invasiva.  
> **Fecha:** Octubre 2026  
> **Referencia visual:** `reference/lumi_onboarding_v7_luz_cromatica.html` (V7 Timing & Handoff).  
> **Restricción estricta:** `app/src/main/java/com/jeremy/lumi/ui/screens/home/HomeScreen.kt` **NO ha sido modificado** para no interferir con su rediseño independiente.

---

## 1. Resumen Ejecutivo

El Onboarding v7 de Lumi culmina con una secuencia atmosférica ceremonial de **~11.5 segundos** (reducida a salto inmediato cuando `prefers-reduced-motion` está activo) que culmina con el **vuelo continuo del logotipo de marca (~590 ms)** desde el centro de la pantalla hacia la posición de la cabecera superior de la aplicación (`LumiTopBar`).

Para preservar la total independencia entre el equipo de Onboarding y el equipo de Rediseño de Home, este contrato define:
1. **La API pública no invasiva** existente (`onComplete: () -> Unit`).
2. **Las garantías de persistencia atómica previa** (sin pérdida de datos ni dobles envíos).
3. **La geometría y cinemática exacta del vuelo del logo** (`LumiBrandLockup`).
4. **La guía de adopción opcional** para el nuevo `HomeScreen.kt` cuando esté listo.

---

## 2. Garantías de Persistencia Previa al Handoff

Antes de iniciar la animación ceremonial y antes de invocar `onComplete()`:
1. **Prevención de doble clic:** `isCompleting` bloquea reentradas concurrentes en `OnboardingViewModel`.
2. **Guardado en DataStore:**
   - Modo seguimiento (`mode = "track"`): Nombre/apodo, regularidad de ciclo, duración estimada, duración de sangrado, objetivo primario (`UserGoal`), objetivos secundarios (`SecondaryGoal`), anticoncepción hormonal y perfil físico opcional.
   - Modo acompañante (`mode = "support"`): Perfil configurado con bandera `isObserver = true`.
3. **Persistencia en Room:**
   - Si la usuaria seleccionó una fecha válida de último periodo (`lastPeriodDate > 0L`), se crea e inserta atómicamente el primer ciclo en `CycleRepository`.
4. **Manejo de errores:**
   - Si ocurre una excepción de I/O o base de datos, la secuencia atmosférica **no se inicia**. Se muestra el error visualmente en pantalla (`saveError`) y el botón principal permite reintentar sin pérdida del progreso capturado.

---

## 3. Especificación Cinemática y Geometría del Logotipo

La animación utiliza el componente oficial unificado [`LumiBrandLockup`](file:///app/src/main/java/com/jeremy/lumi/ui/components/LumiTopBar.kt#L181).

```
   ┌──────────────────────────────────────────────┐
   │ [LumiBrandLockup (Destino)]                 │  <-- Final Scale: 0.52x, X: 20dp, Y: StatusBars + 14dp
   │                                              │
   │                                              │
   │                                              │
   │            [LumiBrandLockup (Centro)]        │  <-- Center Scale: 1.17x, Y: 39.7% altura pantalla
   │                   (Respiración)              │      Resplandor cromático sin halos grises
   │                                              │
   │                                              │
   │            [LumiBrandLockup (Origen)]        │  <-- Initial Scale: 1.0x, Step 7 Hero
   └──────────────────────────────────────────────┘
```

### Tabla de Parámetros Cinemáticos

| Tramo | Ventana Temporal | Posición Y | Escala | Easing | Descripción |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **1. Llegada al centro** | 0 .. 900 ms | Step 7 ➔ 39.7% | 1.00x ➔ 1.17x | `cubic-bezier(.16, .89, .24, 1)` | Ascenso/acomodo ceremonial del logo hacia el centro de atención. |
| **2. Contemplación** | 900 .. 10760 ms | 39.7% (fijo) | 1.17x (±0.02x) | Onda senoidal (3.2 s) | Respiración orgánica mientras se presentan los 5 mensajes. |
| **3. Vuelo hacia cabecera** | 10760 .. 11350 ms | 39.7% ➔ Cabecera | 1.17x ➔ 0.52x | `cubic-bezier(.17, .79, .21, 1)` | **Vuelo estelar continuo de ~590 ms** hacia la posición de `LumiTopBar`. |
| **4. Revelación Home** | 10400 .. 11520 ms | Destino | 0.52x (1.0x cabecera) | Lineal | Desvanecimiento de mensajes y velo. Llamada a `onComplete()`. |

### Resplandor Cromático (Sin Halos Grises/Blancos)
- Gradiente: `radial-gradient(ellipse 90% 72% at 50% 44%, tokens.glow, tokens.veil1(35%), Color.Transparent 83%)`.
- Filtro: `blur(22.dp)`.
- Opacidad pico: `0.66f` (escalada orgánicamente de 0.0 a 1.0 entre 0% y 37% del tiempo, descendiendo a 0.0 en el vuelo).

---

## 4. Los 5 Mensajes Contemplativos Secuenciales

Presentados durante el tramo de contemplación central:

1. **`1300 ms`**: *“Tu espacio empieza aquí”* — *“Un momento para hacerlo tuyo”*
2. **`3450 ms`**: *“Conociendo tus preferencias”* — *“Solo lo que decidiste compartir”*
3. **`5550 ms`**: *“Un lugar a tu manera”* — *“Sin prisas, sin expectativas”*
4. **`7780 ms`**: *“Preparando tu bienvenida”* — *“Tus elecciones estarán a mano”*
5. **`9770 ms`**: *“Todo está listo”* — *“Bienvenida a Lumi”*

Transición entre mensajes:
- Salida del mensaje previo: `translateY: 0 ➔ -13dp`, `alpha: 1 ➔ 0` en 400 ms.
- Entrada del nuevo mensaje: `translateY: 15dp ➔ 0`, `alpha: 0 ➔ 1` en 680 ms con `cubic-bezier(.18, 1, .3, 1)`.

---

## 5. Control de Usuario y Accesibilidad

1. **Botón Omitir (`skipEntrance`):**
   - Aparece suavemente a los **850 ms** (`fadeIn` de 350 ms) en la esquina superior derecha.
   - Al presionarlo, cancela inmediatamente la animación en curso y ejecuta `onComplete()`.
2. **Preferencia de Movimiento Reducido (`prefers-reduced-motion`):**
   - Detectado automáticamente vía escala de animación del sistema (`Settings.Global.TRANSITION_ANIMATION_SCALE == 0f` o `ANIMATOR_DURATION_SCALE == 0f`).
   - Omite de inmediato la secuencia de 11.5 s para usuarias con sensibilidad vestibular.

---

## 6. Guía de Integración para el Rediseño de HomeScreen

El nuevo `HomeScreen.kt` **no requiere ningún cambio obligatorio**. Al recibir la llamada `onComplete()`, el navegador Compose (`NavHost`) cambia el destino a Home:

```kotlin
// LumiAppNavHost.kt (actual)
composable(Routes.ONBOARDING) {
    OnboardingScreen(
        onComplete = {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.ONBOARDING) { inclusive = true }
            }
        }
    )
}
```

### Opción Avanzada (Transición Compartida con Compose 1.7+):
Si el nuevo `HomeScreen.kt` desea sincronizar exactamente el fotograma del logo, puede usar la clave de elemento compartido:
- Origen: `Modifier.sharedElement(state = rememberSharedContentState(key = "lumi_brand_lockup"), ...)`
- Destino en `LumiTopBar.kt`: `LumiBrandLockup(flowerSize = 21, modifier = Modifier.sharedElement(...))`

---

**Autor:** Agente Implementador Lumi  
**Aprobación requerida:** Subagente Auditor Visual (`lumi_visual_auditor`)
