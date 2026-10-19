-- Lumi / Tu voz. Supabase PostgreSQL - NUEVO proyecto de foro.
-- NO ejecuta migraciones de datos del ciclo ni toca Firestore.
-- Ejecutar en un proyecto Supabase NUEVO o tras revisar conflictos.
begin;

create table if not exists public.voice_ideas (
  id uuid primary key default gen_random_uuid(),
  title text not null check (char_length(title) between 8 and 95),
  description text not null check (char_length(description) between 20 and 650),
  category text not null check (category in
    ('Calendario','Registro diario','Apariencia','Privacidad','Recordatorios','Otro')),
  status text not null default 'Recibida' check (status in
    ('Recibida','En revisión','Aceptada','En desarrollo','Disponible','No prevista')),
  visible boolean not null default true,
  votes_count integer not null default 0 check (votes_count >= 0),
  comments_count integer not null default 0 check (comments_count >= 0),
  created_at_ms bigint not null default (extract(epoch from now()) * 1000)::bigint,
  updated_at_ms bigint not null default (extract(epoch from now()) * 1000)::bigint
);

create table if not exists public.voice_submissions (
  id uuid primary key default gen_random_uuid(),
  owner_uid uuid not null references auth.users(id) on delete cascade,
  title text not null check (char_length(title) between 8 and 95),
  description text not null check (char_length(description) between 20 and 650),
  category text not null check (category in
    ('Calendario','Registro diario','Apariencia','Privacidad','Recordatorios','Otro')),
  status text not null default 'Pendiente de revisión',
  is_public boolean not null default false,
  created_at_ms bigint not null default (extract(epoch from now()) * 1000)::bigint
);
create index if not exists voice_submissions_owner_created
  on public.voice_submissions(owner_uid,created_at_ms desc);

create table if not exists public.voice_votes (
  owner_uid uuid not null references auth.users(id) on delete cascade,
  idea_id uuid not null references public.voice_ideas(id) on delete cascade,
  primary key(owner_uid,idea_id)
);
create index if not exists voice_votes_by_idea on public.voice_votes(idea_id);

create table if not exists public.voice_comments (
  id uuid primary key default gen_random_uuid(),
  idea_id uuid not null references public.voice_ideas(id) on delete cascade,
  body text not null check (char_length(body) between 4 and 420),
  is_team boolean not null default false,
  created_at_ms bigint not null default (extract(epoch from now()) * 1000)::bigint
);
create index if not exists voice_comments_by_idea
  on public.voice_comments(idea_id,created_at_ms asc);

create table if not exists public.voice_pending_comments (
  id uuid primary key default gen_random_uuid(),
  idea_id uuid not null references public.voice_ideas(id) on delete cascade,
  owner_uid uuid not null references auth.users(id) on delete cascade,
  body text not null check (char_length(body) between 4 and 420),
  created_at_ms bigint not null default (extract(epoch from now()) * 1000)::bigint
);
create index if not exists voice_pending_comments_owner_time
  on public.voice_pending_comments(owner_uid,created_at_ms desc);

create table if not exists public.voice_reports (
  id uuid primary key default gen_random_uuid(),
  idea_id uuid not null references public.voice_ideas(id) on delete cascade,
  owner_uid uuid not null references auth.users(id) on delete cascade,
  reason text not null check (char_length(reason) between 4 and 160),
  created_at_ms bigint not null default (extract(epoch from now()) * 1000)::bigint,
  unique(idea_id,owner_uid)
);

-- Privilegios reales: ninguna escritura directa desde aplicaciones.
-- La clave publicable nunca puede aprobar ideas ni recontar votos.
alter table public.voice_ideas enable row level security;
alter table public.voice_submissions enable row level security;
alter table public.voice_votes enable row level security;
alter table public.voice_comments enable row level security;
alter table public.voice_pending_comments enable row level security;
alter table public.voice_reports enable row level security;

revoke all on table public.voice_ideas,public.voice_submissions,public.voice_votes,
  public.voice_comments,public.voice_pending_comments,public.voice_reports
  from public,anon,authenticated;
grant select on public.voice_ideas,public.voice_comments,
  public.voice_submissions,public.voice_votes to authenticated;

create policy "voice_public_ideas" on public.voice_ideas for select to authenticated
  using (visible = true);
create policy "voice_public_comments" on public.voice_comments for select to authenticated
  using (exists (select 1 from public.voice_ideas i where i.id=idea_id and i.visible=true));
create policy "voice_own_submissions" on public.voice_submissions for select to authenticated
  using ((select auth.uid()) = owner_uid);
create policy "voice_own_votes" on public.voice_votes for select to authenticated
  using ((select auth.uid()) = owner_uid);
-- Las colas de comentarios y denuncias no tienen políticas de lectura de cliente.

create or replace function public.voice_submit_idea(
  p_title text, p_description text, p_category text
) returns uuid language plpgsql security definer set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_id uuid;
  v_title text := pg_catalog.btrim(p_title);
  v_description text := pg_catalog.btrim(p_description);
begin
  if v_uid is null then raise exception 'Debes iniciar sesión en Tu voz'; end if;
  if v_title is null or pg_catalog.char_length(v_title) not between 8 and 95
    or v_description is null or pg_catalog.char_length(v_description) not between 20 and 650
    or p_category not in ('Calendario','Registro diario','Apariencia','Privacidad','Recordatorios','Otro')
  then raise exception 'Propuesta inválida'; end if;
  if v_title ~* '(https?://|www[.]|[^ ]+@[^ ]+)'
    or v_description ~* '(https?://|www[.]|[^ ]+@[^ ]+)'
  then raise exception 'No incluyas enlaces ni correos personales'; end if;
  perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtext(v_uid::text)::bigint);
  if exists (select 1 from public.voice_submissions
       where owner_uid=v_uid and created_at_ms > (extract(epoch FROM pg_catalog.now()-interval '5 minutes')*1000)::bigint)
  then raise exception 'Espera cinco minutos antes de enviar otra propuesta'; end if;
  insert into public.voice_submissions(owner_uid,title,description,category)
    values(v_uid,v_title,v_description,p_category) returning id into v_id;
  return v_id;
end;
$$;

create or replace function public.voice_toggle_vote(p_idea_id uuid)
returns boolean language plpgsql security definer set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_existing boolean;
begin
  if v_uid is null then raise exception 'Inicia sesión en Tu voz'; end if;
  -- El bloqueo de la propuesta serializa las votaciones concurrentes.
  perform 1 from public.voice_ideas where id=p_idea_id and visible=true for update;
  if not found then raise exception 'La propuesta no está disponible'; end if;
  delete from public.voice_votes where owner_uid=v_uid and idea_id=p_idea_id returning true into v_existing;
  if v_existing is true then
    update public.voice_ideas set votes_count=votes_count-1,
      updated_at_ms=(extract(epoch FROM pg_catalog.now())*1000)::bigint
      where id=p_idea_id and votes_count>0;
    if not found then raise exception 'Inconsistencia de votos'; end if;
    return false;
  end if;
  insert into public.voice_votes(owner_uid,idea_id) values(v_uid,p_idea_id);
  update public.voice_ideas set votes_count=votes_count+1,
    updated_at_ms=(extract(epoch FROM pg_catalog.now())*1000)::bigint where id=p_idea_id;
  return true;
end;
$$;

create or replace function public.voice_submit_comment(p_idea_id uuid,p_body text)
returns uuid language plpgsql security definer set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_body text := pg_catalog.btrim(p_body);
  v_id uuid;
begin
  if v_uid is null then raise exception 'Inicia sesión en Tu voz'; end if;
  if v_body is null or pg_catalog.char_length(v_body) not between 4 and 420
    or v_body ~* '(https?://|www[.]|[^ ]+@[^ ]+)'
  then raise exception 'Comentario inválido o con datos de contacto'; end if;
  if not exists(select 1 from public.voice_ideas where id=p_idea_id and visible=true)
  then raise exception 'La propuesta no está disponible'; end if;
  perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtext(v_uid::text)::bigint);
  if exists(select 1 from public.voice_pending_comments
      where owner_uid=v_uid and created_at_ms>(extract(epoch FROM pg_catalog.now()-interval '1 minute')*1000)::bigint)
  then raise exception 'Espera un minuto para publicar otro comentario'; end if;
  insert into public.voice_pending_comments(idea_id,owner_uid,body)
    values(p_idea_id,v_uid,v_body) returning id into v_id;
  return v_id;
end;
$$;

create or replace function public.voice_report(p_idea_id uuid,p_reason text)
returns void language plpgsql security definer set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_reason text := pg_catalog.btrim(p_reason);
begin
  if v_uid is null then raise exception 'Inicia sesión en Tu voz'; end if;
  if v_reason is null or pg_catalog.char_length(v_reason) not between 4 and 160
  then raise exception 'Motivo inválido'; end if;
  if not exists(select 1 from public.voice_ideas where id=p_idea_id and visible=true)
  then raise exception 'La propuesta no está disponible'; end if;
  insert into public.voice_reports(idea_id,owner_uid,reason)
  values(p_idea_id,v_uid,v_reason)
  on conflict (idea_id,owner_uid) do nothing;
end;
$$;

-- ADMINISTRACIÓN: solo SQL Editor / cuenta de confianza, jamás Android.
create or replace function public.voice_admin_review_idea(p_id uuid,p_approve boolean)
returns void language plpgsql security definer set search_path = ''
as $$
declare v public.voice_submissions%rowtype;
begin
  select * into v from public.voice_submissions where id=p_id for update;
  if not found or v.status <> 'Pendiente de revisión' then
    raise exception 'Propuesta no pendiente o inexistente'; end if;
  if p_approve then
    insert into public.voice_ideas(id,title,description,category,created_at_ms)
      values(v.id,v.title,v.description,v.category,v.created_at_ms);
  end if;
  update public.voice_submissions
    set status=case when p_approve then 'Recibida' else 'No publicada' end,
        is_public=p_approve where id=p_id;
end;
$$;

create or replace function public.voice_admin_review_comment(p_id uuid,p_approve boolean)
returns void language plpgsql security definer set search_path = ''
as $$
declare v public.voice_pending_comments%rowtype;
begin
  select * into v from public.voice_pending_comments where id=p_id for update;
  if not found then raise exception 'Comentario pendiente inexistente'; end if;
  if p_approve then
    perform 1 from public.voice_ideas where id=v.idea_id and visible=true for update;
    if not found then raise exception 'La propuesta no es pública'; end if;
    insert into public.voice_comments(id,idea_id,body,created_at_ms)
      values(v.id,v.idea_id,v.body,v.created_at_ms);
    update public.voice_ideas set comments_count=comments_count+1,
      updated_at_ms=(extract(epoch FROM pg_catalog.now())*1000)::bigint
      where id=v.idea_id;
  end if;
  delete from public.voice_pending_comments where id=p_id;
end;
$$;

create or replace function public.voice_admin_set_status(p_id uuid,p_status text)
returns void language plpgsql security definer set search_path = ''
as $$
begin
  if p_status not in ('Recibida','En revisión','Aceptada','En desarrollo','Disponible','No prevista')
  then raise exception 'Estado inválido'; end if;
  update public.voice_ideas set status=p_status,
    updated_at_ms=(extract(epoch FROM pg_catalog.now())*1000)::bigint where id=p_id;
  if not found then raise exception 'Propuesta inexistente'; end if;
  update public.voice_submissions set status=p_status where id=p_id and is_public=true;
end;
$$;

create or replace function public.voice_admin_reply(p_idea_id uuid,p_body text)
returns uuid language plpgsql security definer set search_path = ''
as $$
declare v_id uuid;
begin
  if p_body is null or pg_catalog.char_length(pg_catalog.btrim(p_body)) not between 4 and 420
  then raise exception 'Respuesta inválida'; end if;
  perform 1 from public.voice_ideas where id=p_idea_id and visible=true for update;
  if not found then raise exception 'Propuesta no disponible'; end if;
  insert into public.voice_comments(idea_id,body,is_team)
    values(p_idea_id,pg_catalog.btrim(p_body),true) returning id into v_id;
  update public.voice_ideas set comments_count=comments_count+1,
    updated_at_ms=(extract(epoch FROM pg_catalog.now())*1000)::bigint where id=p_idea_id;
  return v_id;
end;
$$;

-- Un RPC SECURITY DEFINER requiere privilegios explícitos: sin grant admin a móviles.
revoke execute on function
  public.voice_submit_idea(text,text,text),public.voice_toggle_vote(uuid),
  public.voice_submit_comment(uuid,text),public.voice_report(uuid,text),
  public.voice_admin_review_idea(uuid,boolean),public.voice_admin_review_comment(uuid,boolean),
  public.voice_admin_set_status(uuid,text),public.voice_admin_reply(uuid,text)
  from public,anon,authenticated;
grant execute on function public.voice_submit_idea(text,text,text),
  public.voice_toggle_vote(uuid),public.voice_submit_comment(uuid,text),
  public.voice_report(uuid,text) to authenticated;

-- Activar Realtime sin exponer tablas privadas ni UID.
-- La publicación ya existe en proyectos Supabase gestionados.
do $$
begin
  if exists (select 1 from pg_catalog.pg_publication where pubname='supabase_realtime') then
    if not exists (select 1 from pg_catalog.pg_publication_tables
                   where pubname='supabase_realtime' and schemaname='public' and tablename='voice_ideas') then
      alter publication supabase_realtime add table public.voice_ideas;
    end if;
    if not exists (select 1 from pg_catalog.pg_publication_tables
                   where pubname='supabase_realtime' and schemaname='public' and tablename='voice_comments') then
      alter publication supabase_realtime add table public.voice_comments;
    end if;
  end if;
end $$;

commit;
