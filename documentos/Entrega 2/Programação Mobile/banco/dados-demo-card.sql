do $$
declare
  v_email text := 'SEU_EMAIL';
  v_uid uuid;
begin
  select id into v_uid from auth.users where lower(email) = lower(v_email);
  if v_uid is null then
    raise exception 'Conta não encontrada: %', v_email;
  end if;

  insert into public.universities (name, description)
  select n, 'Instituição parceira (exemplo)'
  from unnest(array['ESPM','PUC-SP','FAAP','INSPER','FGU','INTELI','SÃO CAMILO']) n
  where not exists (select 1 from public.universities u where u.name = n);

  insert into public.courses (title, description, university_id, location, course_date, course_time_end,
                              total_spots, available_spots, points_awarded, has_certificate, category)
  select c.title, 'Curso de exemplo (dados fictícios)', u.id, 'São Paulo',
         now() - interval '40 days', now() - interval '40 days' + interval '3 hours', 30, 30, 100, true, 'Exemplo'
  from (values
    ('FECAP','Carreira 2.0'), ('FECAP','Democracia e Consciência Social'), ('FECAP','Empreendedorismo'),
    ('ESPM','Marketing Pessoal'), ('ESPM','Comunicação e Oratória'),
    ('PUC-SP','Direitos Humanos'),
    ('FAAP','Design Thinking'),
    ('INSPER','Introdução a Dados'),
    ('FGU','Gestão de Projetos'),
    ('INTELI','Lógica de Programação'),
    ('SÃO CAMILO','Saúde e Bem-Estar')
  ) as c(uni, title)
  join public.universities u on u.name = c.uni
  where not exists (select 1 from public.courses x where x.title = c.title);

  insert into public.enrollments (user_id, course_id, status, attended_at, points_earned)
  select v_uid, c.id, 'attended', c.course_date, 100
  from public.courses c
  where c.description = 'Curso de exemplo (dados fictícios)'
    and not exists (select 1 from public.enrollments e where e.user_id = v_uid and e.course_id = c.id);

  insert into public.certificates (user_id, course_id, issued_at)
  select v_uid, c.id, c.course_date + interval '1 day'
  from public.courses c
  where c.description = 'Curso de exemplo (dados fictícios)'
    and not exists (select 1 from public.certificates x where x.user_id = v_uid and x.course_id = c.id);

  update public.profiles
  set courses_completed = (select count(*) from public.enrollments where user_id = v_uid and status = 'attended'),
      school = coalesce(school, 'Etec de São Paulo')
  where id = v_uid;
end $$;

insert into public.announcements (title, message)
select t, m from (values
  ('Inscrições Abertas para o ENEM!', 'As inscrições para o Preparatório ENEM 2026 estão abertas! Garanta sua vaga acessando a aba Cursos. As aulas começam em 01/10.'),
  ('Novo curso disponível!', 'Um novo curso de Empreendedorismo está com inscrições abertas. Confira na aba Cursos.'),
  ('Seu certificado está disponível', 'Parabéns! Seu certificado já está disponível para download na tela Certificados.')
) v(t, m)
where not exists (select 1 from public.announcements a where a.title = v.t);
