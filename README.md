# LUMI — Kit de arranque para Antigravity (dos agentes)

## Uso
1. Extrae este ZIP en una carpeta segura del equipo. **No sobrescribas con él archivos Kotlin del proyecto Android.** Los Kotlin incluidos son snapshots de referencia.
2. Abre tu proyecto Lumi en Antigravity. Crea una **nueva rama o worktree aislado** para el onboarding.
3. Copia las carpetas `reference/`, `context/`, `.agents/` y `candidates-icons-NOT-FINAL/` dentro de un área de referencia accesible al proyecto (o indica sus rutas absolutas al agente).
4. Envía al agente principal el texto completo de `PROMPT_INICIO_2_AGENTES.md`.
5. Si no reconoce el subagente automáticamente, pídele leer `.agents/agents/lumi-visual-auditor.md` y crear/invocar el subagente utilizando su herramienta `invoke_subagent`. Si no está disponible, usa una segunda conversación como auditor de solo lectura.
6. Concede solo los permisos necesarios. No habilites publicación, borrado de datos, pushes ni merges automáticos.
7. Déjalo trabajando con checkpoints. El Home actual queda fuera de alcance; nosotros lo seguiremos diseñando en paralelo.

## Contenido
- `reference/lumi_onboarding_v7_luz_cromatica.html`: referencia aprobada v7.
- `context/`: historial y decisiones visuales.
- `reference/kotlin-snapshots-READONLY/`: extractos originales de Kotlin que no deben copiarse encima del repo.
- `candidates-icons-NOT-FINAL/`: PNGs candidatos no aprobados; el reemplazo definitivo está pendiente.
- `.agents/agents/lumi-visual-auditor.md`: definición lista para subagente auditor.

## Punto crítico
**No declarar 100% terminado:** el usuario seguirá seleccionando iconos y trabajando el Home. Debe quedar preparado un contrato de integración sin editar HomeScreen.
