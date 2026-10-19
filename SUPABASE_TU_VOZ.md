# Lumi · Migración de Tu voz a Supabase

Proyecto destino: cochpdozndxoodioftzk
URL pública: https://cochpdozndxoodioftzk.supabase.co
Alcance: exclusivamente Tu voz; datos de salud siguen locales, Firebase de otros módulos no cambia.

ESTADO: código y migración SQL preparados en GitHub. NO se aplicó el SQL al proyecto Supabase. No listo para producción sin validación.

## 1. Autenticación
Dashboard -> Authentication -> Providers -> Anonymous Sign-ins -> Enable.
La cuenta anónima Supabase es distinta a Firebase Anonymous Auth.
Reinstalar/borrar datos pierde la sesión y los votos asociados al UID.
Activar protección antiabuso antes del lanzamiento público.

## 2. Crear tablas con permisos RLS
En Supabase SQL Editor inspeccionar primero las tablas actuales.
Revisar y ejecutar el script:
supabase/migrations/20261019010000_voice.sql

Crea las tablas de propuestas y comentarios públicos aprobados, pendientes privados,
votos privados, reportes y funciones transaccionales. El cliente solo puede
consultar sus propias propuestas/votos y contenido público, más invocar RPC
limitados. Funciones de administración SIN permisos de llamada de cliente.
Realtime habilitado solo en propuestas y comentarios públicos.

NO introducir claves secretas ni service_role en el APK.

## 3. Configurar Kotlin/Android
Dashboard -> Settings -> API Keys -> copia la Publishable key de cliente (NO secret).
En el archivo ~/.gradle/gradle.properties del PC (FUERA de Git), añadir:
LUMI_SUPABASE_PUBLISHABLE_KEY=sb_publishable_...
Esta clave está destinada a ser pública; la seguridad depende de RLS, no de
mantenerla secreta.
Gradle Sync y compilar de nuevo.

## 4. Moderar desde Supabase SQL Editor
Solicitudes:
SELECT id,title,description,category FROM public.voice_submissions
WHERE status='Pendiente de revisión' ORDER BY created_at_ms DESC;

Aprobar idea (sustituir UUID):
SELECT public.voice_admin_review_idea('UUID-DE-IDEA'::uuid,true);

Rechazar idea:
SELECT public.voice_admin_review_idea('UUID-DE-IDEA'::uuid,false);

Ver comentarios pendientes:
SELECT id,idea_id,body FROM public.voice_pending_comments ORDER BY created_at_ms DESC;

Aprobar comentario:
SELECT public.voice_admin_review_comment('UUID-DE-COMENTARIO'::uuid,true);

Cambiar estado:
SELECT public.voice_admin_set_status('UUID-DE-IDEA'::uuid,'En desarrollo');

Responder como equipo Lumi:
SELECT public.voice_admin_reply('UUID-DE-IDEA'::uuid,'Estamos trabajando en ello.');

Reportes:
SELECT * FROM public.voice_reports ORDER BY created_at_ms DESC;

Ocultar idea:
UPDATE public.voice_ideas SET visible=false WHERE id='UUID-DE-IDEA'::uuid;

Todos los comandos de administración son exclusivos del panel/rol de confianza,
nunca de Android.

## 5. Prueba en dos dispositivos
1. Ambos dispositivos abren Tu voz. Vacío sin ideas aprobadas es normal.
2. A propone una idea: queda privada pendiente. B no la ve.
3. Moderador aprueba en SQL Editor. A y B la ven mediante Realtime.
4. A vota +1 y retira -1; B vota +1, se refleja en ambos.
5. A comenta: el comentario no aparece antes de moderación.
6. Moderador aprueba comentario, actualiza estado y escribe respuesta oficial.
7. B denuncia idea y el reporte permanece privado.
8. Revisar errores al desconectarse, 8 temas, accesibilidad y rotación.
9. Comprobar con JWT de usuario anónimo que no puede escribir directamente
   en tablas públicas ni invocar funciones de administración.

## Advertencias
- Tu voz sí envía texto de propuestas, comentarios, votos y UIDs anónimos
  a Supabase. El resto de datos privados no debe salir del dispositivo.
- No garantiza una persona = un voto, porque un atacante puede crear
  varias cuentas anónimas; CAPTCHA/rate limiting y moderación son necesarias.
- Plan gratuito tiene cuotas y posible pausa por inactividad.
- Mantener las reglas Firestore desplegadas: otros módulos pueden necesitarlas.
- No conectar datos médicos a este cliente ni publicar hasta auditar RLS
  y hacer pruebas de punta a punta.
