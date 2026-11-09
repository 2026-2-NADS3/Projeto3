create extension if not exists pgcrypto;

do $$
declare
  v_aluno uuid;
  v_prof uuid;
  v_uni uuid;
  v_hoje timestamp := date_trunc('day', now() at time zone 'America/Sao_Paulo');
  r record;
  v_curso uuid;
  v_inicio timestamptz;
begin
  for r in select * from (values
      ('aluno.demo@example.com',     'Aluno@2026',     'Mariana Souza Lima', 'Etec de São Paulo', '2º ano do Ensino Médio', 'São Paulo', '11987654321'),
      ('professor.demo@example.com', 'Professor@2026', 'Prof. Carlos Andrade', 'FECAP', null, 'São Paulo', '11912345678')
    ) as t(email, senha, nome, escola, serie, cidade, fone)
  loop
    if not exists (select 1 from auth.users where lower(email) = r.email) then
      declare v_id uuid := gen_random_uuid();
      begin
        insert into auth.users (
          instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
          raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
          confirmation_token, recovery_token, email_change, email_change_token_new)
        values (
          '00000000-0000-0000-0000-000000000000', v_id, 'authenticated', 'authenticated', r.email,
          crypt(r.senha, gen_salt('bf')), now(),
          '{"provider":"email","providers":["email"]}'::jsonb,
          jsonb_strip_nulls(jsonb_build_object('full_name', r.nome, 'school', r.escola,
            'grade', r.serie, 'city', r.cidade, 'phone', r.fone)),
          now(), now(), '', '', '', '');
        insert into auth.identities (id, user_id, identity_data, provider, provider_id,
                                     last_sign_in_at, created_at, updated_at)
        values (gen_random_uuid(), v_id,
                jsonb_build_object('sub', v_id::text, 'email', r.email, 'email_verified', true),
                'email', v_id::text, now(), now(), now());
      end;
    end if;
  end loop;

  select id into v_aluno from auth.users where lower(email) = 'aluno.demo@example.com';
  select id into v_prof  from auth.users where lower(email) = 'professor.demo@example.com';

  insert into public.user_roles (user_id, role) values (v_prof, 'admin') on conflict do nothing;

  select id into v_uni from public.universities where name = 'Instituição Parceira (demo)';
  if v_uni is null then
    insert into public.universities (name, description)
    values ('Instituição Parceira (demo)', 'Instituição fictícia para demonstração') returning id into v_uni;
  end if;

  for r in select * from (values
      ('Introdução ao Mercado de Trabalho', -10,  9, 3, 'concluido'),
      ('Oficina de Currículo e Entrevista',  -5, 14, 3, 'concluido'),
      ('Empreendedorismo para Jovens',        1, 14, 3, 'inscrito'),
      ('Comunicação e Oratória',              3,  9, 3, 'inscrito'),
      ('Lógica de Programação',               6, 10, 4, 'inscrito'),
      ('Design Thinking na Prática',          8, 14, 3, 'disponivel'),
      ('Introdução a Dados e Planilhas',     10,  9, 3, 'disponivel'),
      ('Preparatório ENEM – Redação',        12, 10, 4, 'disponivel')
    ) as t(titulo, dia, hora, dur, situacao)
  loop
    v_inicio := (v_hoje + make_interval(days => r.dia, hours => r.hora)) at time zone 'America/Sao_Paulo';
    select id into v_curso from public.courses where title = r.titulo;
    if v_curso is null then
      insert into public.courses (title, description, university_id, location, course_date, course_time_end,
                                  total_spots, available_spots, points_awarded, has_certificate, category)
      values (r.titulo, 'Curso fictício para demonstração do app', v_uni,
              'Av. Liberdade, 532 – São Paulo', v_inicio, v_inicio + make_interval(hours => r.dur),
              30, case when r.situacao = 'disponivel' then 30 else 29 end, 100, true, 'Demonstração')
      returning id into v_curso;
    end if;

    if r.situacao = 'concluido' then
      insert into public.enrollments (user_id, course_id, status, attended_at, points_earned)
      values (v_aluno, v_curso, 'attended', v_inicio + interval '10 minutes', 100)
      on conflict (user_id, course_id) do nothing;
      insert into public.certificates (user_id, course_id, issued_at)
      select v_aluno, v_curso, v_inicio + make_interval(hours => r.dur + 1)
      where not exists (select 1 from public.certificates where user_id = v_aluno and course_id = v_curso);
    elsif r.situacao = 'inscrito' then
      insert into public.enrollments (user_id, course_id, status)
      values (v_aluno, v_curso, 'enrolled')
      on conflict (user_id, course_id) do nothing;
    end if;
  end loop;

  update public.profiles
  set points = 200,
      courses_completed = (select count(*) from public.enrollments where user_id = v_aluno and status = 'attended')
  where id = v_aluno;

  insert into public.announcements (title, message)
  select 'Bem-vindos à Próxima Etapa!', 'Explore os cursos na aba Cursos e garanta a sua vaga. Boas-vindas!'
  where not exists (select 1 from public.announcements where title = 'Bem-vindos à Próxima Etapa!');

  insert into public.announcements (title, message, course_id)
  select 'Empreendedorismo para Jovens: leve um documento com foto',
         'Na entrada, apresente o QR Code do seu ingresso (aba Presença) e um documento com foto.',
         (select id from public.courses where title = 'Empreendedorismo para Jovens')
  where not exists (select 1 from public.announcements
                    where title = 'Empreendedorismo para Jovens: leve um documento com foto');

  if not exists (select 1 from public.support_messages where user_id = v_aluno and sender = 'student') then
    update public.support_messages set created_at = now() - interval '1 day'
    where user_id = v_aluno and sender = 'staff';
    insert into public.support_messages (user_id, sender, body, created_at) values
      (v_aluno, 'student', 'Olá! Não encontrei o local da oficina de amanhã. Pode me ajudar?', now() - interval '2 hours'),
      (v_aluno, 'student', 'Também queria saber se o certificado da oficina de currículo já está disponível.', now() - interval '1 hour');
  end if;
end $$;

select
  (select count(*) from public.certificates c join auth.users u on u.id = c.user_id
    where u.email = 'aluno.demo@example.com') as certificados,
  (select count(*) from public.enrollments e join auth.users u on u.id = e.user_id
    where u.email = 'aluno.demo@example.com' and e.status = 'enrolled') as inscricoes_futuras,
  (select count(*) from public.support_messages m join auth.users u on u.id = m.user_id
    where u.email = 'aluno.demo@example.com' and m.sender = 'student') as mensagens_do_aluno,
  (select count(*) from public.user_roles r join auth.users u on u.id = r.user_id
    where u.email = 'professor.demo@example.com' and r.role = 'admin') as professor_admin;
