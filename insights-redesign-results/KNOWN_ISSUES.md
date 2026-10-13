# Lumi — Consideraciones Técnicas, Casos Borde y Notas de Concurrencia

**Fecha:** 13 de Octubre de 2026  
**Rama:** `feature/lumi-insights-redesign` (Worktree: `.worktrees/insights`)

---

## 1. Concurrencia y Coordinación con Otros Agentes

- **Agente de Bonds / Seguridad:**
  - Existe trabajo activo en la rama `test/bonds-local-emulator` enfocado en Firebase Local Emulator y la arquitectura de Bonds.
  - Para proteger el repositorio y evitar colisiones de Git o archivos de configuración, todo el desarrollo de Insights se realizó en un worktree completamente aislado en `.worktrees/insights`.
  - No se modificaron archivos de Firebase Emulator, Cloud Functions, Firestore Rules ni la lógica de seguridad de Bonds.
  - No se realiza merge automático a la rama `main`. La integración final será gestionada centralizadamente por el agente de Bonds/Release o el mantenedor del proyecto.

---

## 2. Casos Borde y Comportamiento Clínico Prudente

1. **Usuarios Nuevos sin Ciclos Registrados:**
   - **Comportamiento:** La pantalla muestra `-- días` de media, `-- días` de periodo y `-- días` de rango observado, con la etiqueta `"Sin registros"`.
   - **Garantía:** No se aplica el valor predeterminado ficticio de 28 días que pudiera sesgar las expectativas del usuario o simular un historial biológico inexistente.

2. **Detección de Subida Térmica (Temperatura Basal - BBT):**
   - **Comportamiento:** Si se detecta un patrón de elevación térmica bifásica sostenida, el algoritmo lo etiqueta estrictamente como `"Posible cambio térmico"` con fecha estimada.
   - **Garantía:** Se prohíbe categóricamente presentarlo como "ovulación confirmada" o como método anticonceptivo confiable, incluyendo una nota al pie clínica explicativa.

3. **Cálculo de Distribución de Fases:**
   - **Comportamiento:** Si no existen registros suficientes para individualizar las fases folicular y lútea, el gráfico del acordeón se presenta como un modelo biológico ilustrativo general, advirtiendo con transparencia que la fase ovulatoria real no es una ventana fija de 3 días para todas las usuarias.

4. **Tratamiento de Fechas y Zonas Horarias:**
   - **Comportamiento:** Los cálculos de duración de ciclo se basan en `ChronoUnit.DAYS.between(startDate, endDate)` sobre fechas locales (`LocalDate`), evitando desfases de medianoche producidos por conversiones horarias UTC/local erróneas.

5. **Exportación de Informes PDF:**
   - **Comportamiento:** Se mantiene íntegro el generador nativo en Kotlin y Android (`PdfDocument`) conectado al ActivityResultContract nativo `CreateDocument("application/pdf")`. No se depende de impresión web ni motores externos.
