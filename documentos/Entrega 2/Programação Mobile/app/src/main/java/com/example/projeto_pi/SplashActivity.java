package com.example.projeto_pi;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Class<?> destino = LoginActivity.class;
                if (Sessao.ativa(SplashActivity.this)) {
                    destino = HomeActivity.class;
                }
                startActivity(new Intent(SplashActivity.this, destino));
                finish();
            }
        }, 1500);
    }
}
