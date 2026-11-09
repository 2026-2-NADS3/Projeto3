package com.example.projeto_pi;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONArray;

public class PerfilActivity extends BaseActivity {

    private TextView tvNome;
    private TextView tvEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        abrirTela(R.layout.activity_perfil, R.id.nav_perfil);
        definirTitulo(R.string.nav_perfil);

        tvNome = findViewById(R.id.tvNomePerfil);
        tvEmail = findViewById(R.id.tvEmailPerfil);

        findViewById(R.id.itemEditarPerfil).setOnClickListener(v -> editarPerfil());
        findViewById(R.id.itemCard).setOnClickListener(v -> startActivity(new Intent(this, CardActivity.class)));
        findViewById(R.id.itemNotificacoes).setOnClickListener(v -> startActivity(new Intent(this, ComunicadosActivity.class)));
        findViewById(R.id.itemDuvidas).setOnClickListener(v -> startActivity(new Intent(this,
                Sessao.professor(this) ? ConversasActivity.class : ChatActivity.class)));
        findViewById(R.id.itemPrivacidade).setOnClickListener(v -> startActivity(new Intent(this, PrivacidadeActivity.class)));
        findViewById(R.id.btnSair).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.sair_titulo)
                .setMessage(R.string.sair_mensagem)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.sair_confirmar, (dialogo, botao) -> sair(false))
                .show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mostrarDados();
    }

    private void mostrarDados() {
        tvNome.setText(Sessao.nome(this));
        tvEmail.setText(Sessao.email(this));
    }

    private void editarPerfil() {
        View tela = LayoutInflater.from(this).inflate(R.layout.dialog_editar_perfil, null);
        final EditText etNome = tela.findViewById(R.id.etNomePerfil);
        final EditText etTelefone = tela.findViewById(R.id.etTelefonePerfil);
        final TextView tvErro = tela.findViewById(R.id.tvErroPerfil);
        etTelefone.addTextChangedListener(new TelefoneWatcher());
        etNome.setText(Sessao.nome(this));
        etTelefone.setEnabled(false);

        final AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle(R.string.editar_perfil_titulo)
                .setView(tela)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.salvar, null)
                .create();
        dialogo.show();

        Api.get(this, "rest/v1/profiles?select=phone&id=eq." + Sessao.usuario(this), new Api.Ouvinte() {
            @Override
            public void aoResponder(String corpo) {
                JSONArray lista = Json.lista(corpo);
                if (lista.length() > 0) {
                    etTelefone.setText(lista.optJSONObject(0).optString("phone"));
                }
                etTelefone.setEnabled(true);
            }

            @Override
            public void aoFalhar(int codigo, String mensagem) {
                etTelefone.setEnabled(true);
                tvErro.setText(mensagem);
                tvErro.setVisibility(View.VISIBLE);
            }
        });

        final Button salvar = dialogo.getButton(AlertDialog.BUTTON_POSITIVE);
        salvar.setOnClickListener(v -> {
            String nome = etNome.getText().toString().trim().replaceAll("\\s+", " ");
            String telefone = etTelefone.getText().toString().replaceAll("\\D", "");
            if (nome.length() < 3) {
                tvErro.setText(R.string.erro_nome);
                tvErro.setVisibility(View.VISIBLE);
                return;
            }
            if (telefone.length() < 10 || telefone.length() > 11) {
                tvErro.setText(R.string.erro_telefone);
                tvErro.setVisibility(View.VISIBLE);
                return;
            }
            salvar.setEnabled(false);
            tvErro.setVisibility(View.GONE);
            final String nomeNovo = nome;
            Api.post(this, "rest/v1/rpc/atualizar_perfil", Json.objeto("p_nome", nome, "p_telefone", telefone),
                    new Api.Ouvinte() {
                        @Override
                        public void aoResponder(String corpo) {
                            if (Json.objetoDe(corpo).optString("status").equals("ok")) {
                                Sessao.definirNome(PerfilActivity.this, nomeNovo);
                                mostrarDados();
                                dialogo.dismiss();
                                return;
                            }
                            salvar.setEnabled(true);
                            tvErro.setText("Confira o nome e o telefone informados.");
                            tvErro.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void aoFalhar(int codigo, String mensagem) {
                            if (codigo == 401) {
                                dialogo.dismiss();
                                tratarErro(codigo);
                                return;
                            }
                            salvar.setEnabled(true);
                            tvErro.setText(mensagem);
                            tvErro.setVisibility(View.VISIBLE);
                        }
                    });
        });
    }
}
