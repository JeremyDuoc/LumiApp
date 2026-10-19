# Lumi Studio · Moderación de «Tu voz»

Panel web privado y **separado de Android**. No tiene permisos sobre los datos del ciclo menstrual.

## Funcionalidades
- Revisión de sugerencias: aprobar / rechazar
- Hoja de ruta: Recibida → En revisión → Aceptada → En desarrollo → Disponible
- Alternativa No prevista y botón para ocultar / volver a mostrar una idea
- Respuestas oficiales como Equipo Lumi
- Moderación de comentarios y ocultación de comentarios aprobados
- Reportes y marcado de reportes como Revisado
- Actualización manual y automática de datos públicos en Realtime
- Acceso con Supabase Auth; sin cuenta autorizada no se muestran datos administrativos

## Ya aplicado en Supabase

La migración `lumi_voice_studio_admin` está aplicada en el proyecto
`cochpdozndxoodioftzk`. La tabla `public.voice_admins` comienza sin
ninguna persona autorizada; **nadie tiene privilegios administrativos todavía**.

La migración está en
`supabase/migrations/20261019133000_voice_studio_admin.sql`.

Las funciones administrativas ya no están accesibles a cualquier usuario
autenticado: comprueban desde PostgreSQL que el JWT pertenezca al UID
autorizado y que el correo de la cuenta NO anónima esté confirmado.
No es posible convertirse en administrador modificando JavaScript.

## 1. Crear tu cuenta administrativa (tarea del propietario)

1. Abre https://supabase.com/dashboard/project/cochpdozndxoodioftzk/auth/users
2. Haz clic en «Add user» para añadir **tu cuenta privada de administrador**,
   con correo y contraseña. Confirma ese correo (en Dashboard puedes marcar
   autoconfirmar si está disponible). NO uses tu sesión anónima de Android.
3. Copia el UID / User ID (UUID) de **esa** cuenta, desde Authentication → Users.
4. Abre SQL Editor **desde el Dashboard como propietario** y ejecuta:

```sql
INSERT INTO public.voice_admins (user_id)
VALUES ('UUID_DE_TU_CUENTA_ADMIN'::uuid)
ON CONFLICT (user_id) DO NOTHING;
```

5. Comprueba que está registrado (esta consulta SOLO desde SQL Editor):

```sql
SELECT a.user_id, u.email, u.email_confirmed_at
FROM public.voice_admins a
JOIN auth.users u ON u.id = a.user_id;
```

Mantén la contraseña administrativa privada y activa MFA si está disponible.
Para revocar el acceso ejecuta desde SQL Editor:
`DELETE FROM public.voice_admins WHERE user_id='UUID_DE_LA_CUENTA'::uuid;`
No dejes una cuenta compartida sin responsable.

## 2. Levantar el dashboard en tu PC

Desde la raíz del repositorio:

```bash
git fetch origin
git switch feat/lumi-studio-moderacion
cd lumi-studio
npm install
cp .env.example .env.local
```

Edita `lumi-studio/.env.local` con tu clave **publishable** (no uses
secret ni service_role):

```properties
VITE_SUPABASE_URL=https://cochpdozndxoodioftzk.supabase.co
VITE_SUPABASE_PUBLISHABLE_KEY=sb_publishable_...
```

Luego ejecuta:

```bash
npm run dev
```

Abre la dirección local que te muestre la terminal (normalmente
http://127.0.0.1:5173). Inicia sesión con la cuenta autorizada de Supabase.

`npm run build` genera una versión estática en `dist/`. Se puede desplegar
en Cloudflare Pages sin servidores, pero **NO publiques** el dashboard
hasta validar la autorización y las protecciones en producción.

Nota: la URL y publishable key pueden conocerse públicamente. La protección
real son las restricciones RLS + operaciones SQL con chequeo de administrador.

## 3. Probar el flujo completo
1. Desde Lumi Android, publica una sugerencia anónima.
2. Actualiza Bandeja de entrada: debe mostrarse pendiente.
3. Aprueba la propuesta. Confirma que aparece en el móvil.
4. En Hoja de ruta selecciona «En revisión» y pulsa Guardar estado.
5. Comprueba en Android que se actualiza automáticamente.
6. Pasa por «Aceptada», «En desarrollo» y «Disponible».
7. Abre Responder y publica un mensaje oficial.
8. Envía un comentario anónimo desde otro teléfono, apruébalo.
9. Envía un reporte y prueba ocultar la propuesta y cerrar el reporte.
10. Intenta iniciar sesión con una cuenta normal (no autorizada): debe
    aparecer «Sin acceso de moderación».
11. En red o dispositivo diferente, intenta RPC admin con JWT de persona
    normal: debe responder acceso denegado. No lanzar al público hasta
    completar esta verificación.

## Limitaciones a resolver antes del lanzamiento público

- Las cuentas anónimas pueden ser creadas en masa, por lo que necesitamos
  mitigaciones antiabuso (por ejemplo CAPTCHA, App Check equivalentes).
- Los cambios privados pendientes no se publican en Realtime; el dashboard
  los refresca al recuperar foco, manualmente y cada 45 segundos mientras está abierto.
- Las listas están paginadas a los primeros 250 elementos (prototipo).
  Implementar paginación real si aumenta el tráfico.
- Antes de desplegar un dashboard públicamente configurar política de
  privacidad, retención, eliminación y monitoreo de accesos.
- El panel se crea en rama dedicada: no hacer merge al main hasta completar QA.
