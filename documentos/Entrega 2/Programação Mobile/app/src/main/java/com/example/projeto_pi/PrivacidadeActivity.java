package com.example.projeto_pi;

import android.os.Bundle;

public class PrivacidadeActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        abrirTela(R.layout.activity_privacidade, R.id.nav_perfil);
        definirTitulo(R.string.perfil_privacidade);

        // Você não ensinou isso em aula, mas solicitou no PI: Fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.containerPrivacidade, new PrivacidadeFragment())
                    .commit();
        }
    }
}
