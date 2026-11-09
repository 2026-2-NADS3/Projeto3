Próxima Etapa - App do Aluno

Esse é o aplicativo Android que fizemos para a UC de Programação Mobile (3º semestre de ADS, FECAP), dentro do projeto interdisciplinar da Próxima Etapa. O app é todo em Java, sem Kotlin. Ele serve para o aluno ver os cursos, se inscrever, acompanhar a agenda, mostrar o ingresso em QR Code, ver o histórico de presença, baixar certificados, fazer o teste de perfil e tirar dúvidas com a equipe. O professor usa o mesmo app para ler o QR Code do aluno e registrar a presença e para responder as mensagens.

O que o app usa

O app conversa com um backend no Supabase (banco PostgreSQL com API REST). O login é feito pelo próprio Supabase e o app guarda o token criptografado no celular, usando o Keystore do Android. Para chamar a API a gente usa a biblioteca Volley. No celular existe um banco SQLite que guarda uma cópia das últimas respostas (cache) e uma fila com o que o aluno fez sem internet. Quando a internet volta, o WorkManager manda tudo para a API sozinho. O QR Code é gerado e lido com a biblioteca ZXing. As listas usam RecyclerView com um adapter só, e o detalhe do curso e a tela de privacidade usam Fragments. As notificações são feitas com NotificationCompat e o WorkManager confere a cada 15 minutos se tem coisa nova. O certificado em PDF é desenhado com o PdfDocument do Android.

Como rodar o projeto

Precisa do Android Studio atualizado e de um celular com Android 7.0 ou mais novo (minSdk 24) ou de um emulador. Abra a pasta do projeto no Android Studio, espere o Gradle sincronzar (File > Sync Project with Gradle Files) e depois faça Build > Clean Project. Depois é só clicar em Run. Se o app já estava instalado de uma versao antiga, desinstale antes de rodar de novo, senão o Android pode manter o nome e os dados velhos.

O banco do servidor já está criado no Supabase do grupo. O endereço da API fica no arquivo Api.java. Se precisar criar o banco do zero em outro projeto Supabase, abra o SQL Editor, rode primeiro o arquivo schema-proxima-etapa.sql e depois o usuarios-teste.sql, que ficam na pasat banco. Depois disso troque o endereço e a chave publica no Api.java.

Logins para testar

Aluno: aluno.demo@example.com com a senha Aluno@2026. Essa conta já tem 2 certificados, presenças no histórico, 3 inscrições futuras na agenda, comunicados e mensagens enviadas para a equipe.

Professor: professor.demo@example.com com a senha Professor@2026. Essa conta lê o QR Code do aluno na aba Presença e responde as mensagens em Perfil > Central de Dúvidas.

Todos os dados são fictícios.

Como testar o fluxo principal

Entre como aluno, veja a lista de Cursos, abra um curso e se inscreva. Confira ele na Agenda. Em Presença abra o ingresso para ver o QR Code. Em outro aparelho (ou depois de sair da conta) entre como professor, vá em Presença, escolha o curso e leia o QR Code do aluno, a presença é registrada. Voltnado ao aluno, o histórico mostra a presença e, se o curso dá certificado, ele aparece em Certificados.

Para testar a perda de conexão, ligue o modo avião, mande uma mensagem no chat ou leia um QR Code, e depois desligue o modo avião. O item fica na fila e é enviado sozinho quando a internet volta.

Como gerar o APK

No Android Studio vá em Build > Build Bundle(s) / APK(s) > Build APK(s). Quando terminar, clqiue em locate no aviso que aparece e o arquivo app-debug.apk vai estar na pasta app/build/outputs/apk/debug.

Organização do código

Todo o codigo Java fica em app/src/main/java/com/example/projeto_pi. As telas terminam com Activity (Login, Home, Cursos, Agenda, Presenca, Scanner, Ingresso, Certificados, Card, Teste, Comunicados, Chat, Perfil e outras). Api, Json e Sessao cuidam da comunicação e do login. Banco, Sincronizador e EnvioWorker cuidam do SQLite e do envio offline. Notificador e AvisoWorker cuidam das notificações. Os layouts ficam em app/src/main/res/layout. Nos pontos em que o PI pediu algo que não foi ensinado em aula (ZXing, WorkManager, notificações, Keystore, PDF e RecyclerView) deixamos um comentário avisando.

Testes

Tem testes unitários dos utilitários (telefone, datas, código do ingresso) em app/src/test. Para rodar, clique com o botão direito na pasta test e escolha Run Tests.
