package com.example.projeto_pi;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.core.view.ViewCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TesteActivity extends BaseActivity {

    private static final int INTRO = 0;
    private static final int PERGUNTAS = 1;
    private static final int RESULTADO = 2;

    private int tela = INTRO;
    private List<JSONObject> perguntas = null;
    private String[] respostas = new String[0];
    private int indice = 0;
    private boolean carregando = false;
    private boolean enviando = false;
    private boolean aguardando = false;
    private String erroCarregar = null;
    private String erroEnvio = null;
    private JSONObject resultado = null;
    private JSONObject ultimo = null;

    private View painelIntro;
    private View painelPerguntas;
    private View painelResultado;
    private TextView tvTempo;
    private TextView tvQtdPerguntas;
    private TextView tvEstado;
    private TextView btnIniciar;
    private View btnVerUltimo;
    private TextView tvEnunciado;
    private LinearLayout listaOpcoes;
    private TextView tvErroEnvio;
    private View btnReenviar;
    private View progresso;
    private View tvEnviando;
    private View btnAnterior;
    private TextView tvPerfil;
    private TextView tvSubtitulo;
    private TextView tvDescricao;
    private ChipGroup chips;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        abrirTela(R.layout.activity_teste, R.id.nav_inicio);
        definirTitulo(R.string.teste_titulo);

        painelIntro = findViewById(R.id.painelIntro);
        painelPerguntas = findViewById(R.id.painelPerguntas);
        painelResultado = findViewById(R.id.painelResultado);
        tvTempo = findViewById(R.id.tvTempoTeste);
        tvQtdPerguntas = findViewById(R.id.tvQtdPerguntasTeste);
        tvEstado = findViewById(R.id.tvEstadoTeste);
        btnIniciar = findViewById(R.id.btnIniciarTeste);
        btnVerUltimo = findViewById(R.id.btnVerUltimoTeste);
        tvEnunciado = findViewById(R.id.tvEnunciadoTeste);
        listaOpcoes = findViewById(R.id.listaOpcoesTeste);
        tvErroEnvio = findViewById(R.id.tvErroEnvioTeste);
        btnReenviar = findViewById(R.id.btnReenviarTeste);
        progresso = findViewById(R.id.progressoTeste);
        tvEnviando = findViewById(R.id.tvEnviandoTeste);
        btnAnterior = findViewById(R.id.btnPerguntaAnterior);
        tvPerfil = findViewById(R.id.tvPerfilTeste);
        tvSubtitulo = findViewById(R.id.tvSubtituloTeste);
        tvDescricao = findViewById(R.id.tvDescricaoTeste);
        chips = findViewById(R.id.chipsCarreirasTeste);

        btnIniciar.setOnClickListener(v -> {
            if (perguntas == null) {
                carregarPerguntas();
            } else {
                iniciar();
            }
        });
        btnVerUltimo.setOnClickListener(v -> {
            if (ultimo != null) {
                resultado = ultimo;
                tela = RESULTADO;
                desenhar();
            }
        });
        btnAnterior.setOnClickListener(v -> anterior());
        btnReenviar.setOnClickListener(v -> enviar());
        findViewById(R.id.btnRefazerTeste).setOnClickListener(v -> voltarParaIntro());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (tela == PERGUNTAS) {
                    anterior();
                } else if (tela == RESULTADO) {
                    voltarParaIntro();
                } else {
                    finish();
                }
            }
        });

        String salvo = Banco.abrir(this).obter("teste_ultimo:" + Sessao.usuario(this));
        if (salvo != null && !salvo.isEmpty()) {
            ultimo = Json.objetoDe(salvo);
        }
        carregarPerguntas();
        carregarUltimo();
        desenhar();
    }

    private void carregarPerguntas() {
        if (carregando) {
            return;
        }
        carregando = true;
        erroCarregar = null;
        desenhar();
        Api.get(this, "rest/v1/test_questions?select=id,position,question_text,test_options(id,option_text,sort_order)"
                + "&is_active=eq.true&order=position.asc&test_options.order=sort_order.asc", new Ouvinte() {
            @Override
            protected void aoReceber(String corpo) {
                carregando = false;
                List<JSONObject> lista = new ArrayList<>();
                JSONArray array = Json.lista(corpo);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject pergunta = array.optJSONObject(i);
                    if (pergunta != null && pergunta.optJSONArray("test_options") != null
                            && pergunta.optJSONArray("test_options").length() >= 2) {
                        lista.add(pergunta);
                    }
                }
                if (lista.isEmpty()) {
                    erroCarregar = "O teste ainda não está disponível. Tente novamente mais tarde.";
                } else {
                    perguntas = lista;
                }
                desenhar();
            }

            @Override
            public void aoFalhar(int codigo, String mensagem) {
                carregando = false;
                if (codigo == 401) {
                    super.aoFalhar(codigo, mensagem);
                    return;
                }
                erroCarregar = mensagem;
                desenhar();
            }
        });
    }

    private void carregarUltimo() {
        Api.get(this, "rest/v1/test_results?select=result_detail&user_id=eq." + Sessao.usuario(this)
                + "&test_type=eq.vocacional&order=completed_at.desc&limit=1", new Ouvinte() {
            @Override
            protected void aoReceber(String corpo) {
                JSONArray array = Json.lista(corpo);
                ultimo = null;
                if (array.length() > 0) {
                    JSONObject detalhe = array.optJSONObject(0).optJSONObject("result_detail");
                    if (detalhe != null && !detalhe.optString("nome").isEmpty()) {
                        ultimo = detalhe;
                    }
                }
                guardarUltimo();
                desenhar();
            }

            @Override
            public void aoFalhar(int codigo, String mensagem) {
                if (codigo == 401) {
                    super.aoFalhar(codigo, mensagem);
                }
            }
        });
    }

    private void guardarUltimo() {
        String texto = ultimo == null ? "" : ultimo.toString();
        Banco.abrir(this).salvar("teste_ultimo:" + Sessao.usuario(this), texto);
    }

    private void iniciar() {
        respostas = new String[perguntas.size()];
        indice = 0;
        erroEnvio = null;
        tela = PERGUNTAS;
        desenhar();
    }

    private void voltarParaIntro() {
        tela = INTRO;
        erroEnvio = null;
        desenhar();
    }

    private void anterior() {
        if (enviando) {
            return;
        }
        erroEnvio = null;
        if (indice > 0) {
            indice--;
            desenhar();
        } else {
            voltarParaIntro();
        }
    }

    private void enviar() {
        if (enviando) {
            return;
        }
        enviando = true;
        erroEnvio = null;
        desenhar();
        JSONArray ids = new JSONArray();
        for (String r : respostas) {
            ids.put(r);
        }
        Api.post(this, "rest/v1/rpc/enviar_teste_vocacional", Json.objeto("p_option_ids", ids), new Ouvinte() {
            @Override
            protected void aoReceber(String corpo) {
                enviando = false;
                JSONObject json = Json.objetoDe(corpo);
                String status = json.optString("status");
                if (status.equals("ok") && !json.optString("nome").isEmpty()) {
                    resultado = json;
                    ultimo = json;
                    guardarUltimo();
                    tela = RESULTADO;
                } else if (status.equals("invalido")) {
                    perguntas = null;
                    erroCarregar = "O teste foi atualizado enquanto você respondia. Comece novamente.";
                    tela = INTRO;
                } else {
                    erroEnvio = "Não foi possível calcular o seu perfil agora. Tente novamente.";
                }
                desenhar();
            }

            @Override
            public void aoFalhar(int codigo, String mensagem) {
                enviando = false;
                if (codigo == 401) {
                    super.aoFalhar(codigo, mensagem);
                    return;
                }
                erroEnvio = mensagem;
                desenhar();
            }
        });
    }

    private void desenhar() {
        painelIntro.setVisibility(tela == INTRO ? View.VISIBLE : View.GONE);
        painelPerguntas.setVisibility(tela == PERGUNTAS ? View.VISIBLE : View.GONE);
        painelResultado.setVisibility(tela == RESULTADO ? View.VISIBLE : View.GONE);
        if (tela == INTRO) {
            desenharIntro();
        } else if (tela == PERGUNTAS) {
            desenharPergunta();
        } else {
            desenharResultado();
        }
    }

    private void desenharIntro() {
        if (perguntas != null) {
            int total = perguntas.size();
            int minutos = Math.max(1, Math.round(total / 3f));
            tvTempo.setText(getResources().getQuantityString(R.plurals.teste_minutos, minutos, minutos));
            tvQtdPerguntas.setText(getResources().getQuantityString(R.plurals.teste_qtd_perguntas, total, total));
        } else {
            tvTempo.setText("–");
            tvQtdPerguntas.setText("–");
        }
        if (carregando) {
            tvEstado.setText(R.string.teste_carregando);
            tvEstado.setVisibility(View.VISIBLE);
        } else if (erroCarregar != null && perguntas == null) {
            tvEstado.setText(erroCarregar + "\n" + getString(R.string.teste_tentar_de_novo));
            tvEstado.setVisibility(View.VISIBLE);
        } else {
            tvEstado.setVisibility(View.GONE);
        }
        btnIniciar.setEnabled(!carregando);
        btnIniciar.setAlpha(carregando ? 0.5f : 1f);
        btnVerUltimo.setVisibility(ultimo != null ? View.VISIBLE : View.GONE);
    }

    private void desenharPergunta() {
        aguardando = false;
        JSONObject pergunta = perguntas.get(indice);
        tvEnunciado.setText(getString(R.string.teste_pergunta_numero, indice + 1, pergunta.optString("question_text")));
        String marcada = respostas[indice];

        listaOpcoes.removeAllViews();
        JSONArray opcoes = pergunta.optJSONArray("test_options");
        for (int i = 0; i < opcoes.length(); i++) {
            final JSONObject opcao = opcoes.optJSONObject(i);
            final TextView item = (TextView) getLayoutInflater().inflate(R.layout.item_opcao_teste, listaOpcoes, false);
            item.setText(opcao.optString("option_text"));
            boolean selecionada = opcao.optString("id").equals(marcada);
            item.setSelected(selecionada);
            ViewCompat.setStateDescription(item, selecionada ? getString(R.string.teste_opcao_selecionada) : null);
            item.setEnabled(!enviando);
            item.setOnClickListener(v -> escolher(item, opcao.optString("id")));
            listaOpcoes.addView(item);
        }

        btnAnterior.setVisibility(indice > 0 && !enviando ? View.VISIBLE : View.GONE);
        progresso.setVisibility(enviando ? View.VISIBLE : View.GONE);
        tvEnviando.setVisibility(enviando ? View.VISIBLE : View.GONE);
        boolean erro = erroEnvio != null && !enviando;
        tvErroEnvio.setVisibility(erro ? View.VISIBLE : View.GONE);
        btnReenviar.setVisibility(erro ? View.VISIBLE : View.GONE);
        if (erro) {
            tvErroEnvio.setText(erroEnvio);
        }
    }

    private void escolher(final TextView item, final String opcaoId) {
        if (enviando || aguardando) {
            return;
        }
        aguardando = true;
        for (int i = 0; i < listaOpcoes.getChildCount(); i++) {
            listaOpcoes.getChildAt(i).setSelected(listaOpcoes.getChildAt(i) == item);
        }
        item.postDelayed(() -> {
            aguardando = false;
            if (isFinishing() || tela != PERGUNTAS) {
                return;
            }
            respostas[indice] = opcaoId;
            erroEnvio = null;
            if (indice < perguntas.size() - 1) {
                indice++;
                desenhar();
            } else {
                enviar();
            }
        }, 250);
    }

    private void desenharResultado() {
        tvPerfil.setText(getString(R.string.teste_perfil, resultado.optString("nome")));
        String subtitulo = resultado.optString("subtitulo");
        tvSubtitulo.setText(subtitulo);
        tvSubtitulo.setVisibility(subtitulo.isEmpty() ? View.GONE : View.VISIBLE);
        tvDescricao.setText(resultado.optString("descricao"));

        chips.removeAllViews();
        JSONArray carreiras = resultado.optJSONArray("carreiras");
        if (carreiras == null) {
            return;
        }
        for (int i = 0; i < carreiras.length(); i++) {
            Chip chip = new Chip(this);
            chip.setText(carreiras.optString(i));
            chip.setClickable(false);
            chip.setCheckable(false);
            chip.setFocusable(false);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setTextSize(13);
            chip.setChipBackgroundColorResource(R.color.white);
            chip.setChipStrokeColorResource(R.color.text_primary);
            chip.setChipStrokeWidth(getResources().getDisplayMetrics().density);
            chip.setTextColor(getColor(R.color.text_primary));
            chips.addView(chip);
        }
    }
}
