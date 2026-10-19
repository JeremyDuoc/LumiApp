-- Lumi · Tu voz · Historial verificable de estados y estimación de disponibilidad.
-- No se rellenan fechas anteriores: su hora exacta no fue registrada.
begin;

alter table public.voice_ideas
  add column if not exists estimated_available_on date;

create table if not exists public.voice_status_history (
  id uuid primary key default gen_random_uuid(),
  idea_id uuid not null references public.voice_ideas(id) on delete cascade,
  from_status text,
  to_status text not null check (to_status in
    ('Recibida','En revisión','Aceptada','En desarrollo','Disponible','No prevista')),
  changed_at_ms bigint not null default (extract(epoch from clock_timestamp()) * 1000)::bigint
);
create index if not exists voice_status_history_idea_time
  on public.voice_status_history(idea_id,changed_at_ms desc);

alter table public.voice_status_history enable row level security;
revoke all on public.voice_status_history from public,anon,authenticated;
grant select on public.voice_status_history to authenticated;
create policy voice_history_public_or_admin
  on public.voice_status_history for select to authenticated
  using (
    exists (
      select 1 from public.voice_ideas as i
      where i.id=idea_id and i.visible=true
    )
    or public.voice_is_admin()
  );

-- INSERT = aprobación de nueva idea; UPDATE de status = transición real.
-- Las actualizaciones de votos, comentarios o ETA no generan hitos ficticios.
create or replace function public.voice_record_status_change()
returns trigger language plpgsql security definer set search_path = ''
as $$
begin
  if TG_OP='INSERT' then
    insert into public.voice_status_history(idea_id,from_status,to_status)
    values(new.id,null,new.status);
  elsif old.status is distinct from new.status then
    insert into public.voice_status_history(idea_id,from_status,to_status)
    values(new.id,old.status,new.status);
  end if;
  return new;
end;
$$;
revoke execute on function public.voice_record_status_change()
  from public,anon,authenticated;

drop trigger if exists voice_status_change_audit on public.voice_ideas;
create trigger voice_status_change_audit
after insert or update of status on public.voice_ideas
for each row execute function public.voice_record_status_change();

-- La vista web guarda estado + estimación juntos en una sola transacción.
-- Los usuarios normales no pueden invocar esta operación, aunque conozcan su URL.
create or replace function public.voice_admin_update_roadmap(
  p_id uuid, p_status text, p_eta date
) returns void language plpgsql security definer set search_path = ''
as $$
declare
  v_previous_status text;
  v_previous_eta date;
begin
  if not public.voice_is_admin() then
    raise exception 'Acceso de administración denegado' using errcode='42501';
  end if;
  if p_status is null or p_status not in
    ('Recibida','En revisión','Aceptada','En desarrollo','Disponible','No prevista')
  then
    raise exception 'Estado no reconocido';
  end if;
  if p_eta is not null and
      (p_eta < current_date or p_eta > (current_date + interval '36 months')::date) then
    raise exception 'La fecha orientativa debe estar entre hoy y los próximos tres años';
  end if;
  if p_status in ('Disponible','No prevista') and p_eta is not null then
    raise exception 'No puede haber fecha prevista cuando la idea está disponible o no prevista';
  end if;
  select status,estimated_available_on into v_previous_status,v_previous_eta
    from public.voice_ideas where id=p_id for update;
  if not found then raise exception 'Propuesta inexistente'; end if;
  if v_previous_status is distinct from p_status or v_previous_eta is distinct from p_eta then
    update public.voice_ideas
      set status=p_status, estimated_available_on=p_eta,
          updated_at_ms=(extract(epoch from clock_timestamp())*1000)::bigint
      where id=p_id;
    if v_previous_status is distinct from p_status then
      update public.voice_submissions
        set status=p_status where id=p_id and is_public=true;
    end if;
  end if;
end;
$$;
revoke execute on function public.voice_admin_update_roadmap(uuid,text,date)
  from public,anon,authenticated;
grant execute on function public.voice_admin_update_roadmap(uuid,text,date)
  to authenticated;

-- Realtime: el estado y la fecha estimada viajan en voice_ideas.
-- Historial no requiere emitir cambios en tiempo real, se consulta al abrir el detalle.
commit;
