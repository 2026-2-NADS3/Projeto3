package com.example.projeto_pi;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CadastroActivity extends AppCompatActivity {

    private EditText etNome;
    private EditText etEmail;
    private EditText etTelefone;
    private EditText etSenha;
    private EditText etConfirmaSenha;
    private TextView tvErro;
    private Button btnCadastrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        etNome = findViewById(R.id.etNome);
        etEmail = findViewById(R.id.etEmailCadastro);
        etTelefone = findViewById(R.id.etTelefone);
        etSenha = findViewById(R.id.etSenhaCadastro);
        etConfirmaSenha = findViewById(R.id.etConfirmaSenha);
        tvErro = findViewById(R.id.tvErroCadastro);
        btnCadastrar = findViewById(R.id.btnCadastrar);
        TextView tvFacaLogin = findViewById(R.id.tvFacaLogin);

        etTelefone.addTextChangedListener(new TelefoneWatcher());
        btnCadastrar.setOnClickListener(v -> cadastrar());
        tvFacaLogin.setOnClickListener(v -> finish());
    }

    private void cadastrar() {
        final String nome = etNome.getText().toString().trim().replaceAll("\\s+", " ");
        final String email = etEmail.getText().toString().trim();
        String telefone = etTelefone.getText().toString().replaceAll("\\D", "");
        String senha = etSenha.getText().toString();
        String confirma = etConfirmaSenha.getText().toString();

        String erro = validar(nome, email, telefone, senha, confirma);
        if (erro != null) {
            mostrarErro(erro);
            return;
        }
        tvErro.setVisibility(View.GONE);
        btnCadastrar.setEnabled(false);
        btnCadastrar.setText(R.string.carregando);

        Api.postSemLogin(this, "auth/v1/signup", Json.objeto(
                "email", email,
                "password", senha,
                "data", Json.objeto("full_name", nome, "phone", telefone)), new Api.Ouvinte() {
            @Override
            public void aoResponder(String corpo) {
                if (!Autenticacao.guardar(CadastroActivity.this, corpo, email, nome)) {
                    falhou("Cadastro criado. Confirme seu e-mail para entrar.");
                    return;
                }
                Intent intent = new Intent(CadastroActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void aoFalhar(int codigo, String mensagem) {
                falhou(mensagem);
            }
        });
    }

    private void falhou(String mensagem) {
        btnCadastrar.setEnabled(true);
        btnCadastrar.setText(R.string.btn_cadastrar);
        mostrarErro(mensagem);
    }

    private String validar(String nome, String email, String telefone, String senha, String confirma) {
        if (nome.length() < 3) {
            return getString(R.string.erro_nome);
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return getString(R.string.erro_email);
        }
        if (telefone.length() < 10 || telefone.length() > 11) {
            return getString(R.string.erro_telefone);
        }
        if (senha.length() < 6) {
            return getString(R.string.erro_senha_curta);
        }
        if (!senha.equals(confirma)) {
            return getString(R.string.erro_senhas_diferentes);
        }
        return null;
    }

    private void mostrarErro(String mensagem) {
        tvErro.setText(mensagem);
        tvErro.setVisibility(View.VISIBLE);
    }
}
