# Tu voz: historial real de etapas y disponibilidad estimada

## Implementado en Supabase (19/10/2026)

Migración aplicada: lumi_voice_roadmap_dates.
Archivo: supabase/migrations/20261019160000_voice_roadmap_dates.sql

- voice_status_history almacena idea_id, estado anterior, nuevo estado y fecha/hora del servidor.
- Un trigger añade eventos de aprobaciones nuevas y cambios de estado reales.
- Votos, comentarios o cambios de la estimación NO generan falsos eventos de progreso.
- voice_ideas.estimated_available_on almacena una fecha estimada de disponibilidad OPCIONAL.
- voice_admin_update_roadmap(p_id,p_status,p_eta) actualiza estado y estimación atómicamente y valida la cuenta moderadora.
- Historial solo legible para ideas públicas y administradores autorizados; ningún móvil puede alterarlo.

NO SE INVENTAN FECHAS ANTERIORES: antes de esta migración no se guardaba el historial.
No derivar fechas antiguas de updated_at_ms (puede cambiar por votos) ni de
created_at_ms (antes era la fecha del envío, no de aprobación).

## Lumi Studio
Hoja de ruta > Cambiar estado:
- Stepper vertical con fechas comprobadas y selección de etapas.
- Campo opcional de fecha estimada (hoy hasta tres años en el futuro).
- Guardar cambios actualiza estado y fecha en la misma transacción.
- Cerrar sin guardar no modifica la base.
- Si el estado es Disponible o No prevista, la estimación futura desaparece.
- Estimación siempre marcada como ORIENTATIVA, no compromiso de entrega.

## Lumi Android
Tu voz > Ver avance:
- Fecha y hora reales de etapas registradas.
- Fecha anterior no registrada para hitos sin evidencia histórica.
- Disponibilidad estimada, si fue definida por el administrador.
- Los datos clínicos permanecen en el dispositivo, como antes.

## Pruebas
1. Idea antigua: no deben aparecer fechas ficticias anteriores.
2. Cambio de estado desde dashboard: ver fecha nueva en Android.
3. Guardar ETA: aparece como estimada, sin generar un nuevo evento histórico.
4. Retirar ETA: no genera evento histórico.
5. Disponible / No prevista: no debe quedar ETA visible.
6. Votos/comentarios no cambian historial.
7. Repetir pruebas en dos dispositivos.
8. Cuenta normal/anónima no puede alterar estados ni escribir historial.

El SQL ha pasado una prueba de transición dentro de una transacción revertida sin
dejar datos de prueba. Aún se requiere compilar Android y web del commit final.
