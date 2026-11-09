do $$ begin
  create type public.app_role as enum ('admin', 'student');
exception when duplicate_object then null; end $$;

create table if not exists public.universities (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  logo_url text,
  description text,
  created_at timestamptz not null default now()
);

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null,
  avatar_url text,
  school text,
  grade text,
  school_year text,
  city text,
  phone text,
  points integer not null default 0,
  level text not null default 'Explorador',
  courses_completed integer not null default 0,
  no_shows integer not null default 0,
  is_blocked boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.user_roles (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  role public.app_role not null,
  unique (user_id, role)
);

create table if not exists public.courses (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  description text,
  banner_url text,
  university_id uuid references public.universities(id),
  location text,
  course_date timestamptz not null,
  course_time_end timestamptz,
  total_spots integer not null default 30 check (total_spots >= 0),
  available_spots integer not null default 30 check (available_spots >= 0),
  points_awarded integer not null default 100,
  has_certificate boolean not null default false,
  category text,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.enrollments (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  course_id uuid not null references public.courses(id) on delete cascade,
  status text not null default 'enrolled'
    check (status in ('enrolled', 'attended', 'no_show', 'cancelled')),
  attended_at timestamptz,
  points_earned integer default 0,
  ticket_code text,
  created_at timestamptz not null default now(),
  unique (user_id, course_id)
);

alter table public.enrollments add column if not exists ticket_code text;
update public.enrollments
  set ticket_code = upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 16))
  where ticket_code is null;
alter table public.enrollments
  alter column ticket_code set default upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 16));
alter table public.enrollments alter column ticket_code set not null;
create unique index if not exists idx_enrollments_ticket on public.enrollments(ticket_code);

create table if not exists public.certificates (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  course_id uuid references public.courses(id) on delete cascade,
  certificate_url text,
  storage_path text,
  issued_at timestamptz not null default now()
);

create table if not exists public.badges (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  description text,
  icon text not null,
  requirement text not null,
  points_required integer default 0,
  created_at timestamptz not null default now()
);

create table if not exists public.user_badges (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  badge_id uuid not null references public.badges(id) on delete cascade,
  earned_at timestamptz not null default now(),
  unique (user_id, badge_id)
);

create table if not exists public.test_results (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  test_type text not null check (test_type in ('vocacional', 'disc', 'bolsas')),
  result_summary text not null,
  result_detail jsonb,
  completed_at timestamptz not null default now()
);

create table if not exists public.test_profiles (
  code text primary key check (code in ('R', 'I', 'A', 'S', 'E', 'C')),
  name text not null,
  tagline text not null,
  description text not null,
  careers text[] not null default '{}',
  sort_order integer not null
);

create table if not exists public.test_questions (
  id uuid primary key default gen_random_uuid(),
  position integer not null unique,
  question_text text not null,
  is_active boolean not null default true
);

create table if not exists public.test_options (
  id uuid primary key default gen_random_uuid(),
  question_id uuid not null references public.test_questions(id) on delete cascade,
  option_text text not null,
  profile_code text not null references public.test_profiles(code),
  sort_order integer not null
);

create table if not exists public.notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  type text not null default 'info',
  title text not null,
  message text,
  link text,
  is_read boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists public.news_posts (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  slug text not null unique,
  excerpt text not null,
  content text not null,
  image_url text,
  category text not null default 'educacao',
  tags text[],
  is_ai_generated boolean not null default false,
  is_published boolean not null default true,
  views integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  video_url text,
  gallery_images text[]
);

create table if not exists public.news_comments (
  id uuid primary key default gen_random_uuid(),
  post_id uuid not null references public.news_posts(id) on delete cascade,
  author_name text not null,
  school text,
  content text not null,
  created_at timestamptz not null default now(),
  user_id uuid
);

create table if not exists public.videos (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  description text,
  video_url text not null,
  thumbnail_url text,
  category text not null default 'palestra',
  year integer not null default extract(year from now())::int,
  university_id uuid references public.universities(id),
  school text,
  program text,
  is_podcast boolean not null default false,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.impact_stats (
  id uuid primary key default gen_random_uuid(),
  year integer not null,
  students_impacted integer not null default 0,
  schools_reached integer not null default 0,
  universities_partners integer not null default 0,
  certificates_issued integer not null default 0,
  courses_offered integer not null default 0,
  created_at timestamptz not null default now()
);

create table if not exists public.ai_knowledge (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  content text not null,
  category text,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.ai_settings (
  id uuid primary key default gen_random_uuid(),
  tone text default 'friendly',
  forbidden_topics text[],
  updated_at timestamptz not null default now()
);

create index if not exists idx_enrollments_user on public.enrollments(user_id);
create index if not exists idx_enrollments_course on public.enrollments(course_id);
create index if not exists idx_notifications_user on public.notifications(user_id, created_at desc);
create index if not exists idx_certificates_user on public.certificates(user_id);
create index if not exists idx_test_results_user on public.test_results(user_id);
create index if not exists idx_courses_date on public.courses(course_date);
create index if not exists idx_test_options_question on public.test_options(question_id);

create sequence if not exists public.student_number_seq;
alter table public.profiles add column if not exists student_number text;
alter table public.profiles alter column student_number
  set default (to_char(now(), 'YY') || lpad(nextval('public.student_number_seq')::text, 4, '0'));
update public.profiles
set student_number = to_char(created_at, 'YY') || lpad(nextval('public.student_number_seq')::text, 4, '0')
where student_number is null;
create unique index if not exists idx_profiles_student_number on public.profiles(student_number);

alter table public.certificates add column if not exists code text;
alter table public.certificates alter column code
  set default ('PE-' || to_char(now(), 'YYYY') || '-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 8)));
update public.certificates
set code = 'PE-' || to_char(issued_at, 'YYYY') || '-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 8))
where code is null;
create unique index if not exists idx_certificates_code on public.certificates(code);

create table if not exists public.announcements (
  id uuid primary key default gen_random_uuid(),
  title text not null check (length(trim(title)) > 0),
  message text not null check (length(trim(message)) > 0),
  course_id uuid references public.courses(id) on delete cascade,
  created_at timestamptz not null default now()
);
create index if not exists idx_announcements_created on public.announcements(created_at desc);

create table if not exists public.support_messages (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  sender text not null default 'student' check (sender in ('student', 'staff', 'auto')),
  body text not null check (length(trim(body)) between 1 and 1000),
  client_id uuid,
  created_at timestamptz not null default now()
);
create index if not exists idx_support_user_created on public.support_messages(user_id, created_at);
create unique index if not exists idx_support_client on public.support_messages(user_id, client_id)
  where client_id is not null;

create or replace function public.has_role(_user_id uuid, _role public.app_role)
returns boolean
language sql stable security definer set search_path = public
as $$
  select exists (select 1 from public.user_roles where user_id = _user_id and role = _role);
$$;

create or replace function public.set_updated_at()
returns trigger language plpgsql set search_path = public
as $$
begin
  new.updated_at := now();
  return new;
end $$;

create or replace function public.profile_before_update()
returns trigger language plpgsql set search_path = public
as $$
begin
  new.updated_at := now();
  new.level := case
    when new.points >= 700 then 'Protagonista'
    when new.points >= 300 then 'Desbravador'
    else 'Explorador'
  end;
  if new.no_shows >= 2 then
    new.is_blocked := true;
  end if;
  return new;
end $$;

create or replace function public.handle_new_user()
returns trigger
language plpgsql security definer set search_path = public
as $$
declare
  m jsonb := coalesce(new.raw_user_meta_data, '{}'::jsonb);
begin
  insert into public.profiles (id, full_name, school, grade, school_year, city, phone)
  values (
    new.id,
    coalesce(nullif(trim(m->>'full_name'), ''), split_part(coalesce(new.email, 'aluno'), '@', 1)),
    nullif(trim(m->>'school'), ''),
    nullif(trim(m->>'grade'), ''),
    nullif(trim(m->>'school_year'), ''),
    nullif(trim(m->>'city'), ''),
    nullif(trim(m->>'phone'), '')
  );
  insert into public.user_roles (user_id, role) values (new.id, 'student');
  insert into public.notifications (user_id, type, title, message)
  values (new.id, 'info', 'Bem-vindo(a) à Próxima Etapa!',
          'Explore os cursos disponíveis e garanta a sua vaga.');
  insert into public.support_messages (user_id, sender, body)
  values (new.id, 'staff',
          'Olá, ' || split_part(coalesce(nullif(trim(m->>'full_name'), ''), 'aluno'), ' ', 1)
          || '! Este é o canal para tirar dúvidas com a equipe da Próxima Etapa.');
  return new;
end $$;

create or replace function public.enrollment_no_show()
returns trigger language plpgsql security definer set search_path = public
as $$
begin
  if new.status = 'no_show' and old.status is distinct from 'no_show' then
    update public.profiles set no_shows = no_shows + 1 where id = new.user_id;
  end if;
  return new;
end $$;

create or replace function public.announcement_notify()
returns trigger language plpgsql security definer set search_path = public
as $$
begin
  insert into public.notifications (user_id, type, title, message, link)
  select r.user_id, 'comunicado', new.title, new.message, 'comunicados'
  from (
    select p.id as user_id from public.profiles p where new.course_id is null
    union
    select e.user_id from public.enrollments e
    where new.course_id is not null and e.course_id = new.course_id and e.status in ('enrolled', 'attended')
  ) r;
  return new;
end $$;

create or replace function public.support_after_insert()
returns trigger language plpgsql security definer set search_path = public
as $$
declare
  v_last_staff timestamptz;
  v_last_auto timestamptz;
begin
  if new.sender = 'student' then
    select max(created_at) filter (where sender = 'staff'), max(created_at) filter (where sender = 'auto')
    into v_last_staff, v_last_auto
    from public.support_messages where user_id = new.user_id and id <> new.id;
    if v_last_auto is null or (v_last_staff is not null and v_last_auto < v_last_staff) then
      insert into public.support_messages (user_id, sender, body)
      values (new.user_id, 'auto', 'Recebemos sua dúvida. Nossa equipe responderá em breve por aqui.');
    end if;
  elsif new.sender = 'staff'
        and exists (select 1 from public.support_messages
                    where user_id = new.user_id and sender = 'student' and id <> new.id) then
    insert into public.notifications (user_id, type, title, message, link)
    values (new.user_id, 'chat', 'Nova resposta da equipe', left(new.body, 140), 'duvidas');
  end if;
  return new;
end $$;

drop trigger if exists trg_announcements_notify on public.announcements;
create trigger trg_announcements_notify after insert on public.announcements
  for each row execute function public.announcement_notify();

drop trigger if exists trg_support_after_insert on public.support_messages;
create trigger trg_support_after_insert after insert on public.support_messages
  for each row execute function public.support_after_insert();

drop trigger if exists trg_profiles_before_update on public.profiles;
create trigger trg_profiles_before_update before update on public.profiles
  for each row execute function public.profile_before_update();

drop trigger if exists trg_courses_updated on public.courses;
create trigger trg_courses_updated before update on public.courses
  for each row execute function public.set_updated_at();

drop trigger if exists trg_news_updated on public.news_posts;
create trigger trg_news_updated before update on public.news_posts
  for each row execute function public.set_updated_at();

drop trigger if exists trg_videos_updated on public.videos;
create trigger trg_videos_updated before update on public.videos
  for each row execute function public.set_updated_at();

drop trigger if exists trg_courses_after_insert on public.courses;
drop function if exists public.course_after_insert();

drop trigger if exists trg_enrollment_no_show on public.enrollments;
create trigger trg_enrollment_no_show after update on public.enrollments
  for each row execute function public.enrollment_no_show();

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users
  for each row execute function public.handle_new_user();

create or replace function public.inscrever_curso(p_course_id uuid)
returns jsonb
language plpgsql security definer set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_course public.courses%rowtype;
  v_enr public.enrollments%rowtype;
  v_blocked boolean;
begin
  if v_uid is null then
    raise exception 'Não autenticado' using errcode = '28000';
  end if;

  select is_blocked into v_blocked from public.profiles where id = v_uid;
  if coalesce(v_blocked, false) then
    return jsonb_build_object('status', 'bloqueado');
  end if;

  select * into v_course from public.courses where id = p_course_id and is_active for update;
  if not found then
    return jsonb_build_object('status', 'curso_inexistente');
  end if;
  if coalesce(v_course.course_time_end, v_course.course_date + interval '3 hours') < now() then
    return jsonb_build_object('status', 'encerrado');
  end if;

  select * into v_enr from public.enrollments where user_id = v_uid and course_id = p_course_id;
  if found and v_enr.status <> 'cancelled' then
    return jsonb_build_object('status', 'ja_inscrito', 'enrollment_id', v_enr.id);
  end if;

  if v_course.available_spots <= 0 then
    return jsonb_build_object('status', 'sem_vagas');
  end if;

  if found then
    update public.enrollments set status = 'enrolled', attended_at = null, points_earned = 0
    where id = v_enr.id returning * into v_enr;
  else
    insert into public.enrollments (user_id, course_id) values (v_uid, p_course_id)
    returning * into v_enr;
  end if;

  update public.courses set available_spots = available_spots - 1 where id = p_course_id;

  insert into public.notifications (user_id, type, title, message, link)
  values (v_uid, 'enrollment', 'Inscrição confirmada',
          'Você está inscrito(a) em ' || v_course.title || '.', '/cursos/' || p_course_id);

  return jsonb_build_object('status', 'inscrito', 'enrollment_id', v_enr.id,
                            'available_spots', v_course.available_spots - 1);
end $$;

create or replace function public.atualizar_perfil(p_nome text, p_telefone text)
returns jsonb
language plpgsql security definer set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_nome text := regexp_replace(trim(coalesce(p_nome, '')), '\s+', ' ', 'g');
  v_tel text := regexp_replace(coalesce(p_telefone, ''), '\D', '', 'g');
begin
  if v_uid is null then
    raise exception 'Não autenticado' using errcode = '28000';
  end if;
  if char_length(v_nome) < 3 or char_length(v_nome) > 120 or char_length(v_tel) not between 10 and 11 then
    return jsonb_build_object('status', 'invalido');
  end if;
  update public.profiles set full_name = v_nome, phone = v_tel where id = v_uid;
  return jsonb_build_object('status', 'ok', 'nome', v_nome);
end $$;

create or replace function public.cancelar_inscricao(p_course_id uuid)
returns jsonb
language plpgsql security definer set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_id uuid;
begin
  if v_uid is null then
    raise exception 'Não autenticado' using errcode = '28000';
  end if;
  update public.enrollments set status = 'cancelled'
  where user_id = v_uid and course_id = p_course_id and status = 'enrolled'
  returning id into v_id;
  if v_id is null then
    return jsonb_build_object('status', 'nao_encontrada');
  end if;
  update public.courses set available_spots = least(total_spots, available_spots + 1)
  where id = p_course_id;
  return jsonb_build_object('status', 'cancelada');
end $$;

drop function if exists public.registrar_presenca(uuid, text);
drop table if exists public.course_checkin cascade;

create or replace function public.registrar_presenca_ingresso(p_course_id uuid, p_code text)
returns jsonb
language plpgsql security definer set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_code text;
  v_enr public.enrollments%rowtype;
  v_course public.courses%rowtype;
  v_nome text;
  v_points integer;
begin
  if v_uid is null then
    raise exception 'Não autenticado' using errcode = '28000';
  end if;
  if not public.has_role(v_uid, 'admin') then
    raise exception 'Sem permissão' using errcode = '42501';
  end if;

  v_code := upper(regexp_replace(coalesce(p_code, ''), '[^A-Za-z0-9]', '', 'g'));
  select * into v_enr from public.enrollments where ticket_code = v_code for update;
  if not found then
    return jsonb_build_object('status', 'invalido');
  end if;

  select * into v_course from public.courses where id = v_enr.course_id;
  select full_name into v_nome from public.profiles where id = v_enr.user_id;

  if v_enr.course_id <> p_course_id then
    return jsonb_build_object('status', 'outro_curso', 'aluno', v_nome, 'curso', v_course.title);
  end if;
  if v_enr.status = 'attended' then
    return jsonb_build_object('status', 'duplicada', 'aluno', v_nome, 'curso', v_course.title);
  end if;
  if v_enr.status <> 'enrolled' then
    return jsonb_build_object('status', 'sem_matricula', 'aluno', v_nome, 'curso', v_course.title);
  end if;

  v_points := coalesce(v_course.points_awarded, 0);

  update public.enrollments
  set status = 'attended', attended_at = now(), points_earned = v_points
  where id = v_enr.id;

  update public.profiles
  set points = points + v_points, courses_completed = courses_completed + 1
  where id = v_enr.user_id;

  if v_course.has_certificate then
    insert into public.certificates (user_id, course_id)
    select v_enr.user_id, v_course.id
    where not exists (
      select 1 from public.certificates where user_id = v_enr.user_id and course_id = v_course.id);
  end if;

  insert into public.user_badges (user_id, badge_id)
  select v_enr.user_id, b.id from public.badges b, public.profiles p
  where p.id = v_enr.user_id and coalesce(b.points_required, 0) > 0 and b.points_required <= p.points
  on conflict do nothing;

  insert into public.notifications (user_id, type, title, message, link)
  values (v_enr.user_id, 'attendance', 'Presença registrada',
          'Presença confirmada em ' || v_course.title || '. +' || v_points || ' pontos.',
          '/cursos/' || v_course.id);

  return jsonb_build_object('status', 'registrada', 'aluno', v_nome, 'curso', v_course.title,
                            'pontos', v_points, 'certificado', v_course.has_certificate);
end $$;

create or replace function public.enviar_teste_vocacional(p_option_ids uuid[])
returns jsonb
language plpgsql security definer set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_total integer;
  v_scores jsonb;
  v_prof public.test_profiles%rowtype;
  v_detail jsonb;
begin
  if v_uid is null then
    raise exception 'Não autenticado' using errcode = '28000';
  end if;

  select count(*) into v_total from public.test_questions where is_active;
  if v_total = 0 then
    return jsonb_build_object('status', 'sem_teste');
  end if;

  if p_option_ids is null
     or cardinality(p_option_ids) <> v_total
     or (select count(distinct x) from unnest(p_option_ids) x) <> v_total
     or (select count(distinct o.question_id)
           from public.test_options o
           join public.test_questions q on q.id = o.question_id and q.is_active
          where o.id = any(p_option_ids)) <> v_total then
    return jsonb_build_object('status', 'invalido');
  end if;

  with s as (
    select o.profile_code as code, count(*) as n
    from public.test_options o where o.id = any(p_option_ids) group by o.profile_code
  )
  select jsonb_object_agg(p.code, coalesce(s.n, 0) order by p.sort_order)
  into v_scores
  from public.test_profiles p left join s on s.code = p.code;

  select p.* into v_prof
  from public.test_profiles p
  order by coalesce((v_scores ->> p.code)::integer, 0) desc, p.sort_order
  limit 1;

  v_detail := jsonb_build_object(
    'codigo', v_prof.code, 'nome', v_prof.name, 'subtitulo', v_prof.tagline,
    'descricao', v_prof.description, 'carreiras', to_jsonb(v_prof.careers),
    'pontuacao', v_scores);

  insert into public.test_results (user_id, test_type, result_summary, result_detail)
  values (v_uid, 'vocacional', 'Perfil ' || v_prof.name, v_detail);

  return v_detail || jsonb_build_object('status', 'ok');
end $$;

create or replace function public.validar_certificado(p_code text)
returns jsonb language sql stable security definer set search_path = public
as $$
  select coalesce(
    (select jsonb_build_object('valido', true, 'aluno', p.full_name, 'curso', c.title,
                               'instituicao', u.name, 'emitido_em', ce.issued_at)
       from public.certificates ce
       join public.profiles p on p.id = ce.user_id
       left join public.courses c on c.id = ce.course_id
       left join public.universities u on u.id = c.university_id
      where ce.code = upper(trim(p_code))),
    jsonb_build_object('valido', false));
$$;

create or replace function public.responder_aluno(p_user_id uuid, p_body text)
returns jsonb language plpgsql security definer set search_path = public
as $$
begin
  if auth.uid() is null or not public.has_role(auth.uid(), 'admin') then
    raise exception 'Sem permissão' using errcode = '42501';
  end if;
  if p_body is null or length(trim(p_body)) = 0 or length(trim(p_body)) > 1000 then
    return jsonb_build_object('status', 'invalido');
  end if;
  if not exists (select 1 from public.profiles where id = p_user_id) then
    return jsonb_build_object('status', 'aluno_inexistente');
  end if;
  insert into public.support_messages (user_id, sender, body) values (p_user_id, 'staff', trim(p_body));
  return jsonb_build_object('status', 'ok');
end $$;

alter table public.universities   enable row level security;
alter table public.profiles       enable row level security;
alter table public.user_roles     enable row level security;
alter table public.courses        enable row level security;
alter table public.enrollments    enable row level security;
alter table public.certificates   enable row level security;
alter table public.badges         enable row level security;
alter table public.user_badges    enable row level security;
alter table public.test_results   enable row level security;
alter table public.test_profiles  enable row level security;
alter table public.test_questions enable row level security;
alter table public.test_options   enable row level security;
alter table public.notifications  enable row level security;
alter table public.announcements  enable row level security;
alter table public.support_messages enable row level security;
alter table public.news_posts     enable row level security;
alter table public.news_comments  enable row level security;
alter table public.videos         enable row level security;
alter table public.impact_stats   enable row level security;
alter table public.ai_knowledge   enable row level security;
alter table public.ai_settings    enable row level security;

revoke all on all tables in schema public from anon, authenticated;
revoke all on all functions in schema public from public, anon, authenticated;
grant usage on schema public to anon, authenticated, service_role;
grant all on all tables in schema public to service_role;
grant all on all functions in schema public to service_role;

grant select on public.universities, public.courses, public.badges,
  public.impact_stats, public.news_posts, public.news_comments, public.videos
  to anon, authenticated;

grant select on public.profiles, public.user_roles, public.enrollments,
  public.certificates, public.user_badges, public.test_results, public.notifications
  to authenticated;

grant update (full_name, avatar_url, school, grade, school_year, city, phone)
  on public.profiles to authenticated;
grant update (is_read) on public.notifications to authenticated;
grant select on public.announcements, public.support_messages to authenticated;
grant insert (user_id, body, client_id) on public.support_messages to authenticated;
grant insert, update, delete on public.announcements to authenticated;
grant select on public.test_profiles, public.test_questions to authenticated;
grant select (id, question_id, option_text, sort_order) on public.test_options to authenticated;
grant insert (post_id, author_name, school, content, user_id) on public.news_comments to authenticated;

grant insert, update, delete on public.test_profiles, public.test_questions, public.test_options
  to authenticated;
grant insert, update, delete on public.universities, public.courses, public.badges,
  public.impact_stats, public.news_posts, public.news_comments, public.videos
  to authenticated;

grant execute on function public.has_role(uuid, public.app_role) to authenticated;
grant execute on function public.inscrever_curso(uuid) to authenticated;
grant execute on function public.cancelar_inscricao(uuid) to authenticated;
grant execute on function public.atualizar_perfil(text, text) to authenticated;
grant execute on function public.enviar_teste_vocacional(uuid[]) to authenticated;
grant execute on function public.validar_certificado(text) to anon, authenticated;
grant execute on function public.responder_aluno(uuid, text) to authenticated;
grant execute on function public.registrar_presenca_ingresso(uuid, text) to authenticated;

do $$
declare r record;
begin
  for r in select policyname, tablename from pg_policies where schemaname = 'public' loop
    execute format('drop policy %I on public.%I', r.policyname, r.tablename);
  end loop;
end $$;

create policy universities_read on public.universities for select to anon, authenticated using (true);
create policy universities_admin on public.universities for all to authenticated
  using (public.has_role((select auth.uid()), 'admin')) with check (public.has_role((select auth.uid()), 'admin'));

create policy courses_read on public.courses for select to anon, authenticated using (is_active);
create policy courses_admin on public.courses for all to authenticated
  using (public.has_role((select auth.uid()), 'admin')) with check (public.has_role((select auth.uid()), 'admin'));

create policy badges_read on public.badges for select to anon, authenticated using (true);
create policy badges_admin on public.badges for all to authenticated
  using (public.has_role((select auth.uid()), 'admin')) with check (public.has_role((select auth.uid()), 'admin'));

create policy impact_read on public.impact_stats for select to anon, authenticated using (true);
create policy impact_admin on public.impact_stats for all to authenticated
  using (public.has_role((select auth.uid()), 'admin')) with check (public.has_role((select auth.uid()), 'admin'));

create policy news_read on public.news_posts for select to anon, authenticated using (is_published);
create policy news_admin on public.news_posts for all to authenticated
  using (public.has_role((select auth.uid()), 'admin')) with check (public.has_role((select auth.uid()), 'admin'));

create policy comments_read on public.news_comments for select to anon, authenticated using (true);
create policy comments_insert on public.news_comments for insert to authenticated
  with check (user_id = (select auth.uid()));
create policy comments_admin on public.news_comments for all to authenticated
  using (public.has_role((select auth.uid()), 'admin')) with check (public.has_role((select auth.uid()), 'admin'));

create policy videos_read on public.videos for select to anon, authenticated using (is_active);
create policy videos_admin on public.videos for all to authenticated
  using (public.has_role((select auth.uid()), 'admin')) with check (public.has_role((select auth.uid()), 'admin'));

create policy profiles_own_read on public.profiles for select to authenticated
  using (id = (select auth.uid()) or public.has_role((select auth.uid()), 'admin'));
create policy profiles_own_update on public.profiles for update to authenticated
  using (id = (select auth.uid())) with check (id = (select auth.uid()));

create policy roles_own_read on public.user_roles for select to authenticated
  using (user_id = (select auth.uid()));

create policy enrollments_read on public.enrollments for select to authenticated
  using (user_id = (select auth.uid()) or public.has_role((select auth.uid()), 'admin'));

create policy certificates_read on public.certificates for select to authenticated
  using (user_id = (select auth.uid()) or public.has_role((select auth.uid()), 'admin'));

create policy user_badges_read on public.user_badges for select to authenticated
  using (user_id = (select auth.uid()));

create policy tests_read on public.test_results for select to authenticated
  using (user_id = (select auth.uid()) or public.has_role((select auth.uid()), 'admin'));
create policy test_profiles_read on public.test_profiles for select to authenticated using (true);
create policy test_questions_read on public.test_questions for select to authenticated using (is_active);
create policy test_options_read on public.test_options for select to authenticated using (true);
create policy test_profiles_admin on public.test_profiles for all to authenticated
  using (public.has_role((select auth.uid()), 'admin')) with check (public.has_role((select auth.uid()), 'admin'));
create policy test_questions_admin on public.test_questions for all to authenticated
  using (public.has_role((select auth.uid()), 'admin')) with check (public.has_role((select auth.uid()), 'admin'));
create policy test_options_admin on public.test_options for all to authenticated
  using (public.has_role((select auth.uid()), 'admin')) with check (public.has_role((select auth.uid()), 'admin'));

create policy notifications_read on public.notifications for select to authenticated
  using (user_id = (select auth.uid()));
create policy notifications_update on public.notifications for update to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

create policy announcements_read on public.announcements for select to authenticated
  using (
    course_id is null
    or exists (select 1 from public.enrollments e
               where e.course_id = announcements.course_id and e.user_id = (select auth.uid())
                 and e.status in ('enrolled', 'attended'))
    or public.has_role((select auth.uid()), 'admin'));
create policy announcements_admin on public.announcements for all to authenticated
  using (public.has_role((select auth.uid()), 'admin')) with check (public.has_role((select auth.uid()), 'admin'));

create policy support_read on public.support_messages for select to authenticated
  using (user_id = (select auth.uid()) or public.has_role((select auth.uid()), 'admin'));
create policy support_insert on public.support_messages for insert to authenticated
  with check (user_id = (select auth.uid()) and sender = 'student');

insert into public.universities (name, description)
select * from (values
  ('FECAP', 'Instituição parceira (exemplo)'),
  ('Universidade Parceira', 'Instituição parceira (exemplo)')
) v(name, description)
where not exists (select 1 from public.universities);

insert into public.courses (title, description, university_id, location, course_date, course_time_end,
                            total_spots, available_spots, points_awarded, has_certificate, category)
select c.title, c.description, u.id, c.location, c.d, c.d + interval '3 hours',
       c.spots, c.spots, c.pts, c.cert, c.cat
from (values
  ('Introdução à Programação', 'Oficina prática de lógica e primeiros códigos.', 'FECAP', 'FECAP – Liberdade, São Paulo',
     date_trunc('day', now()) + interval '9 days 14 hours', 30, 100, true, 'Tecnologia'),
  ('Orientação de Carreira', 'Como escolher curso e carreira após o ensino médio.', 'FECAP', 'FECAP – Liberdade, São Paulo',
     date_trunc('day', now()) + interval '12 days 14 hours', 40, 100, true, 'Carreira'),
  ('Como se Preparar para o Vestibular', 'Estratégias de estudo, redação e inscrição.', 'Universidade Parceira', 'Online',
     date_trunc('day', now()) + interval '16 days 10 hours', 60, 80, false, 'Vestibular'),
  ('Finanças Pessoais para Jovens', 'Primeiros passos para organizar o seu dinheiro.', 'Universidade Parceira', 'Online',
     date_trunc('day', now()) + interval '20 days 19 hours', 50, 80, true, 'Finanças')
) as c(title, description, uni, location, d, spots, pts, cert, cat)
join public.universities u on u.name = c.uni
where not exists (select 1 from public.courses);

insert into public.badges (name, description, icon, requirement, points_required)
select * from (values
  ('Primeiro Passo', 'Concluiu o primeiro curso.', 'footprints', 'Somar 100 pontos', 100),
  ('Desbravador', 'Chegou ao nível Desbravador.', 'compass', 'Somar 300 pontos', 300),
  ('Protagonista', 'Chegou ao nível Protagonista.', 'star', 'Somar 700 pontos', 700)
) v(name, description, icon, requirement, points_required)
where not exists (select 1 from public.badges);

insert into public.support_messages (user_id, sender, body)
select p.id, 'staff', 'Olá, ' || split_part(p.full_name, ' ', 1)
       || '! Este é o canal para tirar dúvidas com a equipe da Próxima Etapa.'
from public.profiles p
where not exists (select 1 from public.support_messages m where m.user_id = p.id);

insert into public.test_profiles (code, name, tagline, description, careers, sort_order) values
  ('R', 'Realista', 'Prático e mão na massa', 'Você gosta de lidar com coisas concretas, resolver problemas técnicos e aprender fazendo. Costuma preferir ferramentas, equipamentos e atividades práticas a longas discussões abstratas, e tem facilidade para transformar uma ideia em algo que funciona.', array['Engenheiro','Mecânico','Policial','Bombeiro','Atleta'], 1),
  ('I', 'Investigativo', 'Curioso, lógico e analítico', 'Você é curioso e gosta de entender como e por que as coisas acontecem. Sente prazer em estudar, investigar problemas científicos ou matemáticos e usar o raciocínio lógico e crítico para chegar a conclusões bem fundamentadas.', array['Cientista','Médico','Professor','Pesquisador','Advogado'], 2),
  ('A', 'Artístico', 'Criativo e expressivo', 'Você é criativo e se expressa bem por ideias, imagens, textos ou sons. Gosta de ambientes livres e imaginativos, de pensar de forma diferente do tradicional e de trabalhar com emoções e novas possibilidades.', array['Músico','Artista','Escritor','Designer','Ator'], 3),
  ('S', 'Social', 'Acolhedor e colaborativo', 'Você gosta de estar com as pessoas, ajudar, ensinar e cuidar. Valoriza a boa comunicação e o entendimento entre as pessoas, e costuma ser quem escuta, apoia e faz o grupo trabalhar bem junto.', array['Psicólogo','Assistente social','Professor','Enfermeiro','Agente de saúde'], 4),
  ('E', 'Empreendedor', 'Líder, estratégico e decidido', 'Você gosta de liderar, tomar iniciativa e assumir riscos para alcançar resultados. Valoriza a independência, sabe influenciar e convencer as pessoas e costuma pensar de forma estratégica e visionária.', array['Empresário','Gerente','Corretor','Advogado','Vendedor'], 5),
  ('C', 'Convencional', 'Organizado e confiável', 'Você é organizado, cuidadoso com os detalhes e gosta de seguir processos claros. Tem afinidade com números, dados e tarefas administrativas, e é a pessoa em quem os outros confiam para manter tudo em ordem.', array['Contador','Secretário','Auditor','Assistente administrativo','Bibliotecário'], 6)
on conflict (code) do nothing;

do $seed$
declare
  v_perguntas jsonb := $json$[{"p": 1, "t": "Em um trabalho em grupo na escola, você prefere:", "o": [{"c": "A", "t": "Criar as ideias e o visual da apresentação"}, {"c": "S", "t": "Cuidar para que todos participem e se sintam bem"}, {"c": "I", "t": "Pesquisar e entender o assunto a fundo"}, {"c": "C", "t": "Organizar prazos, tarefas e o material do grupo"}]}, {"p": 2, "t": "Em uma discussão de grupo, você:", "o": [{"c": "I", "t": "Questiona os argumentos e busca provas"}, {"c": "C", "t": "Anota os pontos e mantém a conversa organizada"}, {"c": "A", "t": "Traz ideias diferentes e fora do comum"}, {"c": "R", "t": "Propõe testar na prática para ver o que funciona"}]}, {"p": 3, "t": "No seu tempo livre, o que mais te atrai?", "o": [{"c": "S", "t": "Encontrar amigos e conversar sobre a vida deles"}, {"c": "R", "t": "Montar, consertar ou construir alguma coisa"}, {"c": "I", "t": "Ler ou assistir conteúdos de ciência e curiosidades"}, {"c": "A", "t": "Desenhar, escrever, tocar ou editar vídeos"}]}, {"p": 4, "t": "Se ganhasse um curso de um dia, qual escolheria?", "o": [{"c": "C", "t": "Organização financeira e planilhas"}, {"c": "A", "t": "Fotografia, desenho ou criação de conteúdo"}, {"c": "S", "t": "Primeiros socorros e acolhimento de pessoas"}, {"c": "R", "t": "Mecânica, marcenaria ou eletrônica básica"}]}, {"p": 5, "t": "Diante de um problema novo, sua primeira reação é:", "o": [{"c": "E", "t": "Montar um plano e assumir a frente para resolver"}, {"c": "S", "t": "Conversar com os envolvidos para entender o que sentem"}, {"c": "R", "t": "Colocar a mão na massa e tentar uma solução concreta"}, {"c": "I", "t": "Estudar as causas antes de agir"}]}, {"p": 6, "t": "Em uma feira de profissões, você pararia primeiro no estande de:", "o": [{"c": "A", "t": "Design, música, cinema ou teatro"}, {"c": "E", "t": "Negócios, vendas e empreendedorismo"}, {"c": "I", "t": "Pesquisa científica e laboratórios"}, {"c": "S", "t": "Saúde, educação e serviço social"}]}, {"p": 7, "t": "Em uma festa da escola, qual tarefa você assumiria?", "o": [{"c": "I", "t": "Calcular o orçamento e comparar as melhores opções"}, {"c": "E", "t": "Convencer patrocinadores e vender os ingressos"}, {"c": "A", "t": "Cuidar da decoração e da identidade visual"}, {"c": "R", "t": "Montar o palco, o som e a estrutura"}]}, {"p": 8, "t": "Quando precisa aprender algo novo, você:", "o": [{"c": "R", "t": "Aprende fazendo, testando com as próprias mãos"}, {"c": "I", "t": "Busca entender a teoria e o porquê de cada passo"}, {"c": "E", "t": "Procura logo um desafio real para aplicar e ver resultado"}, {"c": "C", "t": "Segue um passo a passo e vai anotando tudo"}]}, {"p": 9, "t": "Qual destes trabalhos de férias te agradaria mais?", "o": [{"c": "R", "t": "Ajudar em uma oficina ou em uma reforma"}, {"c": "E", "t": "Vender produtos e fechar negócios em uma feira"}, {"c": "A", "t": "Criar ilustrações ou conteúdos para uma marca"}, {"c": "S", "t": "Cuidar de crianças em uma colônia de férias"}]}, {"p": 10, "t": "Um amigo está desanimado com os estudos. Você:", "o": [{"c": "C", "t": "Ajuda a montar um cronograma de estudos"}, {"c": "I", "t": "Tenta descobrir o que está causando a dificuldade"}, {"c": "S", "t": "Escuta com atenção e oferece apoio"}, {"c": "E", "t": "Incentiva a traçar metas e ir atrás do resultado"}]}, {"p": 11, "t": "Em uma aula, a atividade que mais te prende é:", "o": [{"c": "C", "t": "Resolver exercícios com regras e etapas bem definidas"}, {"c": "A", "t": "Produzir um texto, desenho ou vídeo autoral"}, {"c": "E", "t": "Debater e defender uma posição diante da turma"}, {"c": "S", "t": "Explicar a matéria para um colega com dúvida"}]}, {"p": 12, "t": "Que tipo de conteúdo você escolheria para o fim de semana?", "o": [{"c": "C", "t": "Um guia de organização e produtividade"}, {"c": "I", "t": "Um documentário científico"}, {"c": "S", "t": "Uma história de superação e relações humanas"}, {"c": "R", "t": "Um tutorial de como construir ou consertar algo"}]}, {"p": 13, "t": "Em um projeto, você se destaca por:", "o": [{"c": "R", "t": "Resolver as questões técnicas e práticas"}, {"c": "E", "t": "Tomar a frente e motivar a equipe"}, {"c": "A", "t": "Ter ideias originais"}, {"c": "C", "t": "Cuidar dos detalhes e cumprir o combinado"}]}, {"p": 14, "t": "Se montasse um negócio com colegas, seu papel seria:", "o": [{"c": "A", "t": "Criar a marca e os produtos"}, {"c": "I", "t": "Analisar o mercado e os dados"}, {"c": "C", "t": "Cuidar do controle financeiro e dos registros"}, {"c": "E", "t": "Liderar a equipe e fechar parcerias"}]}, {"p": 15, "t": "Qual destes presentes você mais gostaria de ganhar?", "o": [{"c": "C", "t": "Uma agenda completa com kit de organização"}, {"c": "S", "t": "Um convite para participar de um projeto social"}, {"c": "E", "t": "Uma mentoria com um empreendedor de sucesso"}, {"c": "R", "t": "Um kit de ferramentas ou de eletrônica"}]}, {"p": 16, "t": "Ao planejar uma viagem com amigos, você:", "o": [{"c": "S", "t": "Pensa em como todos vão se divertir e se sentir bem"}, {"c": "R", "t": "Pesquisa trilhas e atividades ao ar livre"}, {"c": "E", "t": "Assume a organização e negocia os melhores preços"}, {"c": "I", "t": "Pesquisa a história e as curiosidades dos lugares"}]}, {"p": 17, "t": "O que você mais valoriza em um ambiente de trabalho?", "o": [{"c": "E", "t": "Autonomia para decidir e crescer rápido"}, {"c": "R", "t": "Atividades práticas e concretas"}, {"c": "S", "t": "Ajudar as pessoas e ter boa convivência"}, {"c": "C", "t": "Regras claras, rotina e organização"}]}, {"p": 18, "t": "Em um dia de voluntariado, você escolheria:", "o": [{"c": "C", "t": "Organizar o cadastro e a lista de doações"}, {"c": "A", "t": "Pintar murais ou criar a comunicação da campanha"}, {"c": "E", "t": "Liderar a mobilização e arrecadar recursos"}, {"c": "I", "t": "Levantar dados sobre as necessidades da comunidade"}]}, {"p": 19, "t": "Quando algo dá errado em um projeto, você:", "o": [{"c": "R", "t": "Arregaça as mangas e conserta"}, {"c": "I", "t": "Analisa o que causou a falha"}, {"c": "C", "t": "Revisa o planejamento e ajusta os processos"}, {"c": "S", "t": "Conversa com a equipe para manter o clima bom"}]}, {"p": 20, "t": "Qual destes desafios te animaria mais?", "o": [{"c": "R", "t": "Construir um protótipo que funcione"}, {"c": "A", "t": "Criar uma campanha ou obra do zero"}, {"c": "S", "t": "Organizar uma ação para ajudar a comunidade"}, {"c": "C", "t": "Planejar cada detalhe de um grande evento"}]}, {"p": 21, "t": "Em uma aula de campo, você gostaria de:", "o": [{"c": "S", "t": "Acompanhar os colegas e ajudar quem precisa"}, {"c": "A", "t": "Registrar fotos e ideias criativas"}, {"c": "E", "t": "Coordenar o grupo durante a visita"}, {"c": "I", "t": "Observar, coletar dados e levantar hipóteses"}]}, {"p": 22, "t": "Como você prefere mostrar o que aprendeu?", "o": [{"c": "I", "t": "Escrevendo uma análise ou relatório"}, {"c": "R", "t": "Construindo um modelo ou uma demonstração prática"}, {"c": "E", "t": "Apresentando para a turma e convencendo"}, {"c": "C", "t": "Entregando um trabalho completo e bem formatado"}]}, {"p": 23, "t": "Quando pedem a sua ajuda, normalmente é para:", "o": [{"c": "C", "t": "Organizar documentos, prazos ou contas"}, {"c": "S", "t": "Escutar e aconselhar"}, {"c": "E", "t": "Tomar uma decisão ou liderar"}, {"c": "A", "t": "Dar ideias criativas"}]}, {"p": 24, "t": "Qual destas profissões você gostaria de experimentar por um dia?", "o": [{"c": "R", "t": "Técnico ou mecânico"}, {"c": "A", "t": "Designer ou ator"}, {"c": "I", "t": "Cientista de laboratório"}, {"c": "E", "t": "Dono de uma empresa"}]}, {"p": 25, "t": "Qual ambiente de trabalho te parece mais agradável?", "o": [{"c": "E", "t": "Um escritório dinâmico, onde as decisões saem rápido"}, {"c": "A", "t": "Um estúdio criativo"}, {"c": "R", "t": "Uma oficina ou um trabalho ao ar livre"}, {"c": "S", "t": "Uma escola ou um posto de saúde"}]}, {"p": 26, "t": "Ao usar o celular ou o computador, você mais gosta de:", "o": [{"c": "I", "t": "Descobrir como as coisas funcionam por dentro"}, {"c": "C", "t": "Organizar arquivos, agenda e planilhas"}, {"c": "R", "t": "Mexer em configurações e equipamentos"}, {"c": "A", "t": "Editar fotos, vídeos e músicas"}]}, {"p": 27, "t": "Em uma competição da escola, você prefere:", "o": [{"c": "A", "t": "Criar a apresentação ou a performance da equipe"}, {"c": "S", "t": "Apoiar o time e cuidar do bem-estar de todos"}, {"c": "I", "t": "Resolver os desafios de lógica"}, {"c": "C", "t": "Cuidar das regras e da pontuação"}]}, {"p": 28, "t": "O que te deixa mais satisfeito ao terminar o dia?", "o": [{"c": "S", "t": "Ter ajudado alguém"}, {"c": "C", "t": "Ter cumprido tudo o que estava na lista"}, {"c": "I", "t": "Ter aprendido ou descoberto algo novo"}, {"c": "E", "t": "Ter alcançado uma meta importante"}]}, {"p": 29, "t": "Se tivesse uma semana livre para um projeto pessoal, você:", "o": [{"c": "R", "t": "Construiria ou consertaria alguma coisa"}, {"c": "A", "t": "Produziria uma obra sua"}, {"c": "I", "t": "Investigaria uma dúvida que você tem"}, {"c": "S", "t": "Organizaria um grupo para apoiar uma causa"}]}, {"p": 30, "t": "Pensando no futuro, o que você mais quer da sua carreira?", "o": [{"c": "E", "t": "Liderar, ter o meu negócio e crescer"}, {"c": "A", "t": "Liberdade para criar e me expressar"}, {"c": "C", "t": "Estabilidade e organização"}, {"c": "R", "t": "Trabalhar fazendo e construindo coisas concretas"}]}]$json$::jsonb;
  q jsonb;
  o jsonb;
  v_id uuid;
  v_ordem integer;
begin
  if exists (select 1 from public.test_questions) then
    return;
  end if;
  for q in select * from jsonb_array_elements(v_perguntas) loop
    insert into public.test_questions (position, question_text)
    values ((q ->> 'p')::integer, q ->> 't') returning id into v_id;
    v_ordem := 0;
    for o in select * from jsonb_array_elements(q -> 'o') loop
      v_ordem := v_ordem + 1;
      insert into public.test_options (question_id, option_text, profile_code, sort_order)
      values (v_id, o ->> 't', o ->> 'c', v_ordem);
    end loop;
  end loop;
end $seed$;
