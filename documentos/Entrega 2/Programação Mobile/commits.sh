#!/bin/bash
# Roda dentro de 'Entrega 2/Programação Mobile' (pasta onde estão app/, gradle/ e settings.gradle.kts),
# com o repositório da faculdade já clonado.
# Antes, copie os 2 scripts SQL para a pasta banco/ (mkdir banco):
#   schema-proxima-etapa.sql, usuarios-teste.sql   (e dados-demo-card.sql, se quiser)
# Uso:  bash commits.sh
set -e
J=app/src/main/java/com/example/projeto_pi
R=app/src/main/res

git rev-parse --is-inside-work-tree >/dev/null 2>&1 || { echo "ERRO: rode dentro da pasta do repositório clonado"; exit 1; }
git add -A -n . >/dev/null

c() { git commit -q -m "$1" && echo "ok: $1"; }

git add .gitignore app/.gitignore build.gradle.kts settings.gradle.kts gradle.properties gradle gradlew gradlew.bat \
  app/build.gradle.kts app/src/main/keepRules
c "chore: configura projeto Android (Gradle, Java 11, dependências)"

git add $R/values $R/values-land $R/values-night $R/values-v23 $R/values-w600dp $R/values-w1240dp $R/color $R/font $R/xml $R/drawable $R/mipmap* \
  app/src/main/AndroidManifest.xml 2>/dev/null || true
c "feat(ui): tema, cores, fontes, ícones e manifesto do app Próxima Etapa"

git add $J/Json.java $J/Api.java $J/Util.java $J/TelefoneWatcher.java
c "feat(api): cliente Volley para a API REST do Supabase, helpers de JSON e utilitários"

git add $J/Cofre.java $J/Sessao.java $J/Autenticacao.java $J/SplashActivity.java $J/LoginActivity.java $J/CadastroActivity.java \
  $R/layout/activity_splash.xml $R/layout/activity_login.xml $R/layout/activity_cadastro.xml
c "feat(auth): login, cadastro e sessão com token criptografado (Keystore)"

git add $J/BaseActivity.java $J/ListaAdapter.java $R/layout/activity_base.xml $R/layout/view_cabecalho.xml \
  $R/layout/view_cabecalho_avatar.xml $R/menu
c "feat(ui): navegação por abas (BaseActivity) e adapter genérico de RecyclerView"

git add $J/Curso.java $J/CursosActivity.java $J/CursoDetalheFragment.java $J/AgendaActivity.java $J/HomeActivity.java \
  $R/layout/activity_cursos.xml $R/layout/activity_agenda.xml $R/layout/activity_home.xml $R/layout/dialog_curso_detalhe.xml \
  $R/layout/item_curso.xml $R/layout/item_curso_card.xml $R/layout/item_agenda.xml
c "feat(cursos): home, lista de cursos, detalhe em Fragment, inscrição e agenda"

git add $J/Banco.java
c "feat(local): banco SQLite com cache de respostas e fila de envio offline"

git add $J/IngressoActivity.java $J/ScannerActivity.java $J/PresencaActivity.java \
  $R/layout/activity_ingresso.xml $R/layout/activity_scanner.xml $R/layout/activity_presenca.xml $R/layout/item_ingresso.xml
c "feat(presenca): ingresso com QR Code, leitura pelo professor (ZXing) e histórico de presença"

git add $J/Sincronizador.java $J/EnvioWorker.java
c "feat(sync): sincronização da fila offline com a API e reenvio via WorkManager"

git add $J/Notificador.java $J/AvisoWorker.java
c "feat(notificacoes): avisos de comunicado, curso novo, agenda alterada e certificado"

git add $J/Comunicado.java $J/ComunicadosActivity.java $R/layout/activity_comunicados.xml $R/layout/item_comunicado.xml
c "feat(comunicados): lista de comunicados com filtros e marcação de lidos"

git add $J/ChatActivity.java $J/ConversasActivity.java $R/layout/activity_chat.xml $R/layout/activity_conversas.xml \
  $R/layout/item_chat_aluno.xml $R/layout/item_chat_equipe.xml $R/layout/item_conversa.xml
c "feat(chat): central de dúvidas do aluno e respostas do professor"

git add $J/Certificado.java $J/CertificadoPdf.java $J/CertificadosActivity.java $J/CardActivity.java \
  $R/layout/activity_certificados.xml $R/layout/activity_card.xml $R/layout/item_certificado.xml $R/layout/item_instituicao_card.xml
c "feat(certificados): lista de certificados, PDF para download e card digital do aluno"

git add $J/TesteActivity.java $R/layout/activity_teste.xml $R/layout/item_opcao_teste.xml
c "feat(teste): teste de perfil vocacional com perguntas e resultado vindos do banco"

git add $J/PerfilActivity.java $J/PrivacidadeActivity.java $J/PrivacidadeFragment.java \
  $R/layout/activity_perfil.xml $R/layout/activity_privacidade.xml $R/layout/fragment_privacidade.xml \
  $R/layout/dialog_editar_perfil.xml $R/layout/item_secao_privacidade.xml
c "feat(perfil): perfil editável, privacidade em Fragment e logout"

git add app/src/test app/src/androidTest
c "test: testes unitários dos utilitários (telefone, datas, código do ingresso)"

if [ -d banco ]; then
  git add banco
  c "docs(banco): scripts SQL do Supabase (schema, dados fictícios e logins de teste)"
fi

# qualquer arquivo que sobrou
if [ -n "$(git status --porcelain .)" ]; then
  git add -A .
  c "chore: ajustes finais e arquivos restantes"
fi
echo; git log --oneline
