package com.example.projeto_pi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

public class HomeActivity extends BaseActivity {

    private TextView tvSaudacao;
    private TextView tvInscritos;
    private TextView tvProximas;
    private TextView tvCertificados;
    private TextView tvAgendaVazia;
    private TextView tvMensagensVazia;
    private View pontoNotificacao;
    private View cardMensagem;
    private LinearLayout listaAgenda;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        abrirTela(R.layout.activity_home, R.id.nav_inicio);

        tvSaudacao = findViewById(R.id.tvGreeting);
        tvInscritos = findViewById(R.id.tvCursosInscritosNumero);
        tvProximas = findViewById(R.id.tvProximasAtividadesNumero);
        tvCertificados = findViewById(R.id.tvCertificadosNumero);
        tvAgendaVazia = findViewById(R.id.tvAgendaVazia);
        tvMensagensVazia = findViewById(R.id.tvMensagensVazia);
        pontoNotificacao = findViewById(R.id.pontoNotificacao);
        cardMensagem = findViewById(R.id.cardMensagem);
        listaAgenda = findViewById(R.id.listaAgenda);
        EditText etBusca = findViewById(R.id.etSearch);

        findViewById(R.id.btnNotificacoes).setOnClickListener(v -> abrir(ComunicadosActivity.class));
        findViewById(R.id.llVerMensagens).setOnClickListener(v -> abrir(ComunicadosActivity.class));
        cardMensagem.setOnClickListener(v -> abrir(ComunicadosActivity.class));
        findViewById(R.id.atalhoCertificados).setOnClickListener(v -> abrir(CertificadosActivity.class));
        findViewById(R.id.atalhoTestes).setOnClickListener(v -> abrir(TesteActivity.class));
        findViewById(R.id.atalhoCard).setOnClickListener(v -> abrir(CardActivity.class));
        findViewById(R.id.atalhoPresenca).setOnClickListener(v -> irParaAba(R.id.nav_presenca));
        findViewById(R.id.llVerTudo).setOnClickListener(v -> irParaAba(R.id.nav_agenda));

        etBusca.setOnEditorActionListener((campo, acao, evento) -> {
            boolean enter = evento != null && evento.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && evento.getAction() == KeyEvent.ACTION_DOWN;
            if (acao == EditorInfo.IME_ACTION_SEARCH || enter) {
                Intent intent = new Intent(this, CursosActivity.class);
                intent.putExtra("busca", campo.getText().toString().trim());
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    // Você não ensinou isso em aula, mas solicitou no PI: notificações.
    // No Android 13 ou mais novo o app precisa pedir permissão para avisar o aluno.
    private void pedirPermissaoDeAvisos() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
        }
    }

    private void abrir(Class<?> tela) {
        startActivity(new Intent(this, tela));
    }

    @Override
    protected void onResume() {
        super.onResume();
        pedirPermissaoDeAvisos();
        AvisoWorker.agendar(this);
        String nome = Util.primeiroNome(Sessao.nome(this));
        if (!nome.isEmpty()) {
            tvSaudacao.setText(getString(R.string.ola_nome, nome));
        }

        mostrarCursos(Curso.doCache(this));
        mostrarComunicados(Comunicado.doCache(this));
        String qtd = Banco.abrir(this).obter("certificados_qtd:" + Sessao.usuario(this));
        tvCertificados.setText(qtd == null ? "0" : qtd);

        Curso.atualizar(this, new Ouvinte() {
            @Override
            protected void aoReceber(String corpo) {
                esconderAviso();
                mostrarCursos(Curso.lista(corpo));
            }

            @Override
            public void aoFalhar(int codigo, String mensagem) {
                super.aoFalhar(codigo, mensagem);
                tvAgendaVazia.setVisibility(listaAgenda.getChildCount() == 0 ? View.VISIBLE : View.GONE);
            }
        });

        Api.get(this, "rest/v1/certificates?select=id&user_id=eq." + Sessao.usuario(this), new Ouvinte() {
            @Override
            protected void aoReceber(String corpo) {
                String total = String.valueOf(Json.lista(corpo).length());
                Banco.abrir(HomeActivity.this).salvar("certificados_qtd:" + Sessao.usuario(HomeActivity.this), total);
                tvCertificados.setText(total);
            }
        });

        Comunicado.atualizar(this, new Ouvinte() {
            @Override
            protected void aoReceber(String corpo) {
                mostrarComunicados(Comunicado.lista(HomeActivity.this, corpo));
            }
        });
    }

    private void mostrarCursos(List<Curso> cursos) {
        long agora = System.currentTimeMillis();
        int inscritos = 0;
        listaAgenda.removeAllViews();
        int proximas = 0;
        for (Curso curso : cursos) {
            if (curso.inscrito) {
                inscritos++;
            }
            if (curso.inscrito && curso.fim >= agora) {
                proximas++;
                if (proximas <= 3) {
                    listaAgenda.addView(criarItemAgenda(curso, listaAgenda));
                }
            }
        }
        tvInscritos.setText(String.valueOf(inscritos));
        tvProximas.setText(String.valueOf(proximas));
        tvAgendaVazia.setVisibility(proximas == 0 ? View.VISIBLE : View.GONE);
    }

    private void mostrarComunicados(List<Comunicado> lista) {
        pontoNotificacao.setVisibility(Comunicado.temNaoLido(lista) ? View.VISIBLE : View.GONE);
        if (lista.isEmpty()) {
            cardMensagem.setVisibility(View.GONE);
            tvMensagensVazia.setVisibility(View.VISIBLE);
            return;
        }
        lista.get(0).preencher(cardMensagem);
        TextView mensagem = cardMensagem.findViewById(R.id.tvMensagemComunicado);
        mensagem.setMaxLines(2);
        mensagem.setEllipsize(TextUtils.TruncateAt.END);
        tvMensagensVazia.setVisibility(View.GONE);
        cardMensagem.setVisibility(View.VISIBLE);
    }
}
