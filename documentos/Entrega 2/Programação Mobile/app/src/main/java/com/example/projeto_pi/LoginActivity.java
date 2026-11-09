package com.example.projeto_pi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etSenha;
    private TextView tvErro;
    private Button btnEntrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etSenha = findViewById(R.id.etSenha);
        tvErro = findViewById(R.id.tvErroLogin);
        btnEntrar = findViewById(R.id.btnEntrar);
        TextView tvIrCadastro = findViewById(R.id.tvIrCadastro);

        btnEntrar.setOnClickListener(v -> entrar());
        tvIrCadastro.setOnClickListener(v -> startActivity(new Intent(this, CadastroActivity.class)));
    }

    private void entrar() {
        final String email = etEmail.getText().toString().trim();
        String senha = etSenha.getText().toString();
        if (email.isEmpty() || senha.isEmpty()) {
            mostrarErro(getString(R.string.erro_campos_login));
            return;
        }
        tvErro.setVisibility(View.GONE);
        btnEntrar.setEnabled(false);
        btnEntrar.setText(R.string.carregando);

        Api.postSemLogin(this, "auth/v1/token?grant_type=password",
                Json.objeto("email", email, "password", senha), new Api.Ouvinte() {
                    @Override
                    public void aoResponder(String corpo) {
                        if (!Autenticacao.guardar(LoginActivity.this, corpo, email, null)) {
                            falhou(getString(R.string.erro_login));
                            return;
                        }
                        Autenticacao.carregarPerfil(LoginActivity.this, () -> irParaHome());
                    }

                    @Override
                    public void aoFalhar(int codigo, String mensagem) {
                        falhou(mensagem);
                    }
                });
    }

    private void falhou(String mensagem) {
        btnEntrar.setEnabled(true);
        btnEntrar.setText(R.string.btn_entrar);
        mostrarErro(mensagem);
    }

    private void irParaHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void mostrarErro(String mensagem) {
        tvErro.setText(mensagem);
        tvErro.setVisibility(View.VISIBLE);
    }
}
