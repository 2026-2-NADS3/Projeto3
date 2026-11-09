package com.example.projeto_pi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

// Tela só do professor: mostra os alunos que mandaram mensagem na Central de Dúvidas.
public class ConversasActivity extends BaseActivity {

    private static class Conversa {
        String alunoId;
        String nome;
        String ultima;
    }

    private ListaAdapter<Conversa> adapter;
    private TextView tvVazio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        abrirTela(R.layout.activity_conversas, R.id.nav_perfil);
        definirTitulo(R.string.conversas_titulo);
        tvVazio = findViewById(R.id.tvVazio);

        RecyclerView lista = findViewById(R.id.listaConversas);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListaAdapter<>(new ListaAdapter.Ligador<Conversa>() {
            @Override
            public int layout(Conversa c) {
                return R.layout.item_conversa;
            }

            @Override
            public void ligar(View item, final Conversa c) {
                ((TextView) item.findViewById(R.id.tvNomeConversa)).setText(c.nome);
                ((TextView) item.findViewById(R.id.tvUltimaConversa)).setText(c.ultima);
                item.setContentDescription(c.nome + ": " + c.ultima);
                item.setOnClickListener(v -> {
                    Intent intent = new Intent(ConversasActivity.this, ChatActivity.class);
                    intent.putExtra("aluno_id", c.alunoId);
                    intent.putExtra("aluno_nome", c.nome);
                    startActivity(intent);
                });
            }
        });
        lista.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        buscar();
    }

    private void buscar() {
        // primeiro pego os nomes dos alunos, depois as mensagens mais novas primeiro
        Api.get(this, "rest/v1/profiles?select=id,full_name&limit=500", new Ouvinte() {
            @Override
            protected void aoReceber(final String perfis) {
                Api.get(ConversasActivity.this, "rest/v1/support_messages?select=user_id,sender,body"
                        + "&sender=eq.student&order=created_at.desc&limit=300", new Ouvinte() {
                    @Override
                    protected void aoReceber(String mensagens) {
                        esconderAviso();
                        mostrar(perfis, mensagens);
                    }
                });
            }
        });
    }

    private void mostrar(String perfis, String mensagens) {
        JSONArray nomes = Json.lista(perfis);
        JSONArray msgs = Json.lista(mensagens);
        List<Conversa> conversas = new ArrayList<>();
        List<String> jaVistos = new ArrayList<>();
        for (int i = 0; i < msgs.length(); i++) {
            JSONObject m = msgs.optJSONObject(i);
            if (m == null || jaVistos.contains(m.optString("user_id"))) {
                continue;
            }
            // a primeira mensagem de cada aluno é a mais nova, então ela vira o resumo
            Conversa c = new Conversa();
            c.alunoId = m.optString("user_id");
            c.ultima = m.optString("body");
            c.nome = getString(R.string.chat_msg_aluno);
            for (int j = 0; j < nomes.length(); j++) {
                JSONObject perfil = nomes.optJSONObject(j);
                if (perfil != null && perfil.optString("id").equals(c.alunoId)
                        && !perfil.optString("full_name").trim().isEmpty()) {
                    c.nome = perfil.optString("full_name").trim();
                }
            }
            jaVistos.add(c.alunoId);
            conversas.add(c);
        }
        adapter.trocar(conversas);
        tvVazio.setVisibility(conversas.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
