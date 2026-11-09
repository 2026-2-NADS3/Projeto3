package com.example.projeto_pi;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatActivity extends BaseActivity {

    private static class Mensagem {
        String texto;
        boolean doAluno;
        boolean pendente;
    }

    private final Handler relogio = new Handler(Looper.getMainLooper());
    private final Runnable atualizar = new Runnable() {
        @Override
        public void run() {
            Sincronizador.enviar(ChatActivity.this);
            buscar();
            relogio.postDelayed(this, 5000);
        }
    };

    private RecyclerView lista;
    private ListaAdapter<Mensagem> adapter;
    private EditText etMensagem;
    private TextView tvStatus;
    private TextView tvVazio;
    private View pontoStatus;
    // quando o professor abre a conversa de um aluno, alunoId guarda quem é o aluno
    private String alunoId = null;
    private boolean online = true;
    private boolean recebeu = false;
    private int total = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        abrirTela(R.layout.activity_chat, R.id.nav_perfil);
        alunoId = getIntent().getStringExtra("aluno_id");

        // Você não ensinou isso em aula, mas solicitou no PI: RecyclerView
        // Aqui a lista usa dois layouts: um para o aluno e outro para a equipe.
        lista = findViewById(R.id.listaChat);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListaAdapter<>(new ListaAdapter.Ligador<Mensagem>() {
            @Override
            public int layout(Mensagem m) {
                return m.doAluno ? R.layout.item_chat_aluno : R.layout.item_chat_equipe;
            }

            @Override
            public void ligar(View item, Mensagem m) {
                ligarMensagem(item, m);
            }
        });
        lista.setAdapter(adapter);
        etMensagem = findViewById(R.id.etMensagemChat);
        tvStatus = findViewById(R.id.tvStatusChat);
        tvVazio = findViewById(R.id.tvVazioChat);
        pontoStatus = findViewById(R.id.pontoStatusChat);

        findViewById(R.id.btnEnviarChat).setOnClickListener(v -> enviar());
        etMensagem.setOnEditorActionListener((v, acao, evento) -> {
            if (acao == EditorInfo.IME_ACTION_SEND) {
                enviar();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mostrar();
        relogio.post(atualizar);
    }

    @Override
    protected void onPause() {
        super.onPause();
        relogio.removeCallbacks(atualizar);
    }

    private void buscar() {
        String usuario = dono();
        Api.get(this, "rest/v1/support_messages?select=id,sender,body,client_id,created_at&user_id=eq." + usuario
                + "&order=created_at.asc&limit=300", new Ouvinte() {
            @Override
            protected void aoReceber(String corpo) {
                Banco.abrir(ChatActivity.this).salvar("mensagens:" + dono(), corpo);
                recebeu = true;
                definirOnline(true);
                mostrar();
            }

            @Override
            public void aoFalhar(int codigo, String mensagem) {
                if (codigo == 0) {
                    recebeu = true;
                    definirOnline(false);
                    mostrar();
                } else {
                    super.aoFalhar(codigo, mensagem);
                }
            }
        });
    }

    private void enviar() {
        String texto = etMensagem.getText().toString().trim();
        if (texto.isEmpty() || texto.length() > 1000) {
            return;
        }
        etMensagem.setText("");
        if (alunoId != null) {
            responderAluno(texto);
            return;
        }
        String clienteId = UUID.randomUUID().toString();
        Banco.abrir(this).colocarNaFila("chat", Sessao.usuario(this), clienteId, texto);
        mostrar();
        EnvioWorker.agendar(this);
        Sincronizador.enviar(this);
        relogio.postDelayed(this::buscar, 1500);
    }

    // dono da conversa: o aluno logado ou, para o professor, o aluno escolhido
    private String dono() {
        return alunoId != null ? alunoId : Sessao.usuario(this);
    }

    // professor responde direto pela API (sem fila): a função responder_aluno só aceita admin
    private void responderAluno(final String texto) {
        Api.post(this, "rest/v1/rpc/responder_aluno",
                Json.objeto("p_user_id", alunoId, "p_body", texto), new Ouvinte() {
                    @Override
                    protected void aoReceber(String corpo) {
                        buscar();
                    }

                    @Override
                    public void aoFalhar(int codigo, String mensagem) {
                        etMensagem.setText(texto);
                        super.aoFalhar(codigo, mensagem);
                    }
                });
    }

    private void definirOnline(boolean valor) {
        online = valor;
        if (valor) {
            tvStatus.setText(R.string.chat_online);
            pontoStatus.setBackgroundResource(R.drawable.bg_ponto_online);
            esconderAviso();
        } else {
            tvStatus.setText(R.string.chat_offline);
            pontoStatus.setBackgroundResource(R.drawable.bg_ponto_offline);
            tratarErro(0);
        }
    }

    private void ligarMensagem(View item, Mensagem m) {
        ((TextView) item.findViewById(R.id.tvTextoChat)).setText(m.texto);
        String autor = getString(m.doAluno ? R.string.chat_msg_voce
                : (alunoId != null ? R.string.chat_msg_aluno : R.string.chat_msg_equipe));
        String descricao = autor + ": " + m.texto;
        TextView status = item.findViewById(R.id.tvStatusMensagem);
        if (status != null) {
            if (m.pendente) {
                status.setText(online ? R.string.chat_enviando : R.string.chat_aguardando_conexao);
                status.setVisibility(View.VISIBLE);
                descricao += ". " + status.getText();
            } else {
                status.setVisibility(View.GONE);
            }
        }
        item.setContentDescription(descricao);
    }

    private void mostrar() {
        List<Mensagem> mensagens = new ArrayList<>();
        List<String> conhecidos = new ArrayList<>();

        String cache = Banco.abrir(this).obter("mensagens:" + dono());
        JSONArray servidor = Json.lista(cache);
        for (int i = 0; i < servidor.length(); i++) {
            JSONObject json = servidor.optJSONObject(i);
            if (json == null) {
                continue;
            }
            Mensagem m = new Mensagem();
            m.texto = json.optString("body");
            // para o professor, "minha" mensagem é a da equipe
            m.doAluno = json.optString("sender").equals(alunoId != null ? "staff" : "student");
            m.pendente = false;
            mensagens.add(m);
            conhecidos.add(json.optString("client_id"));
        }
        List<Banco.ItemFila> fila = alunoId != null ? new ArrayList<Banco.ItemFila>()
                : Banco.abrir(this).listarFila(Sessao.usuario(this), "chat");
        for (Banco.ItemFila item : fila) {
            if (!conhecidos.contains(item.chave)) {
                Mensagem m = new Mensagem();
                m.texto = item.dado;
                m.doAluno = true;
                m.pendente = true;
                mensagens.add(m);
            }
        }

        adapter.trocar(mensagens);
        if (mensagens.size() > total) {
            lista.scrollToPosition(mensagens.size() - 1);
        }
        total = mensagens.size();
        tvVazio.setVisibility(total == 0 && recebeu ? View.VISIBLE : View.GONE);
    }
}
