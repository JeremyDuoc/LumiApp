-- Lumi Studio: administración restringida de Tu voz.
-- La tabla de administradores se configura desde SQL Editor. NO concede acceso por defecto.
begin;

create table if not exists public.voice_admins (
  user_id uuid primary key references auth.users(id) on delete cascade,
  added_at timestamptz not null default now()
);
alter table public.voice_admins enable row level security;
revoke all on public.voice_admins from public, anon, authenticated;

create or replace function public.voice_is_admin()
returns boolean language sql stable security definer set search_path = ''
as $$
  select coalesce((auth.jwt()->>'is_anonymous')::boolean, true) = false
    and exists (
      select 1 from public.voice_admins a
      join auth.users u on u.id = a.user_id
      where a.user_id = (select auth.uid())
        and u.email_confirmed_at is not null
    );
$$;
revoke execute on function public.voice_is_admin() from public, anon, authenticated;
grant execute on function public.voice_is_admin() to authenticated;

-- RLS sólo para cuenta de administración autorizada (UID + correo verificado).
create policy voice_admin_submissions on public.voice_submissions
  for select to authenticated using (public.voice_is_admin());
create policy voice_admin_ideas on public.voice_ideas
  for select to authenticated using (public.voice_is_admin());
create policy voice_admin_pending_comments on public.voice_pending_comments
  for select to authenticated using (public.voice_is_admin());
create policy voice_admin_reports on public.voice_reports
  for select to authenticated using (public.voice_is_admin());
create policy voice_admin_comments on public.voice_comments
  for select to authenticated using (public.voice_is_admin());

grant select on public.voice_pending_comments,public.voice_reports to authenticated;

-- Ocultar comentario tras denuncia, sin eliminar historial privado de moderación.
alter table public.voice_comments add column if not exists visible boolean not null default true;
drop policy if exists voice_public_comments on public.voice_comments;
create policy voice_public_comments on public.voice_comments for select to authenticated
  using (visible=true and exists (select 1 from public.voice_ideas i
    where i.id=idea_id and i.visible=true));

create or replace function public.voice_admin_review_idea(p_id uuid,p_approve boolean)
returns void language plpgsql security definer set search_path = ''
as $$
declare v public.voice_submissions%rowtype;
begin
  if not public.voice_is_admin() then raise exception 'Acceso de administración denegado' using errcode='42501'; end if;
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
  if not public.voice_is_admin() then raise exception 'Acceso de administración denegado' using errcode='42501'; end if;
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
  if not public.voice_is_admin() then raise exception 'Acceso de administración denegado' using errcode='42501'; end if;
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
  if not public.voice_is_admin() then raise exception 'Acceso de administración denegado' using errcode='42501'; end if;
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

create or replace function public.voice_admin_set_visibility(p_id uuid,p_visible boolean)
returns void language plpgsql security definer set search_path = ''
as $$
begin
  if not public.voice_is_admin() then raise exception 'No autorizado' using errcode='42501'; end if;
  if p_visible is null then raise exception 'Visibilidad inválida'; end if;
  update public.voice_ideas set visible=p_visible,
    updated_at_ms=(extract(epoch from pg_catalog.now())*1000)::bigint where id=p_id;
  if not found then raise exception 'Propuesta inexistente'; end if;
end;
$$;

create or replace function public.voice_admin_hide_comment(p_id uuid)
returns void language plpgsql security definer set search_path = ''
as $$
declare v_idea_id uuid;
begin
  if not public.voice_is_admin() then raise exception 'No autorizado' using errcode='42501'; end if;
  update public.voice_comments set visible=false where id=p_id and visible=true
    returning idea_id into v_idea_id;
  if v_idea_id is null then raise exception 'Comentario inexistente u oculto'; end if;
  update public.voice_ideas set comments_count=greatest(0,comments_count-1),
    updated_at_ms=(extract(epoch from pg_catalog.now())*1000)::bigint where id=v_idea_id;
end;
$$;

alter table public.voice_reports add column if not exists status text not null default 'Abierto'
  check (status in ('Abierto','Revisado','Descartado'));
alter table public.voice_reports add column if not exists reviewed_at_ms bigint;

create or replace function public.voice_admin_review_report(p_id uuid,p_status text)
returns void language plpgsql security definer set search_path = ''
as $$
begin
  if not public.voice_is_admin() then raise exception 'No autorizado' using errcode='42501'; end if;
  if p_status not in ('Revisado','Descartado','Abierto') then raise exception 'Estado inválido'; end if;
  update public.voice_reports set status=p_status,
    reviewed_at_ms=case when p_status='Abierto' then null else
      (extract(epoch from pg_catalog.now())*1000)::bigint end
  where id=p_id;
  if not found then raise exception 'Reporte inexistente'; end if;
end;
$$;

-- Restringir operaciones de administración a JWT autenticado y chequeo interno UID.
revoke execute on function
  public.voice_admin_review_idea(uuid,boolean), public.voice_admin_review_comment(uuid,boolean),
  public.voice_admin_set_status(uuid,text), public.voice_admin_reply(uuid,text),
  public.voice_admin_set_visibility(uuid,boolean),public.voice_admin_hide_comment(uuid),
  public.voice_admin_review_report(uuid,text)
  from public,anon,authenticated;
grant execute on function
  public.voice_admin_review_idea(uuid,boolean), public.voice_admin_review_comment(uuid,boolean),
  public.voice_admin_set_status(uuid,text), public.voice_admin_reply(uuid,text),
  public.voice_admin_set_visibility(uuid,boolean),public.voice_admin_hide_comment(uuid),
  public.voice_admin_review_report(uuid,text)
  to authenticated;
commit;
