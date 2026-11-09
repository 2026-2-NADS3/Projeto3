package com.example.projeto_pi;

import android.content.Context;
import android.content.SharedPreferences;

public class Sessao {

    private static SharedPreferences prefs(Context contexto) {
        return contexto.getApplicationContext().getSharedPreferences("sessao", Context.MODE_PRIVATE);
    }

    public static void entrar(Context contexto, String token, String refresh, String usuario, String nome, String email) {
        prefs(contexto).edit()
                .putString("token", Cofre.cifrar(token))
                .putString("refresh", Cofre.cifrar(refresh))
                .putString("usuario", usuario)
                .putString("nome", nome)
                .putString("email", email)
                .putBoolean("professor", false)
                .apply();
    }

    public static void guardarTokens(Context contexto, String token, String refresh) {
        prefs(contexto).edit()
                .putString("token", Cofre.cifrar(token))
                .putString("refresh", Cofre.cifrar(refresh))
                .apply();
    }

    public static void definirNome(Context contexto, String nome) {
        prefs(contexto).edit().putString("nome", nome).apply();
    }

    public static void definirProfessor(Context contexto, boolean professor) {
        prefs(contexto).edit().putBoolean("professor", professor).apply();
    }

    public static String token(Context contexto) {
        String guardado = prefs(contexto).getString("token", null);
        if (guardado == null) {
            return null;
        }
        return Cofre.decifrar(guardado);
    }

    public static String refresh(Context contexto) {
        String guardado = prefs(contexto).getString("refresh", null);
        if (guardado == null) {
            return null;
        }
        return Cofre.decifrar(guardado);
    }

    public static String usuario(Context contexto) {
        return prefs(contexto).getString("usuario", null);
    }

    public static String nome(Context contexto) {
        return prefs(contexto).getString("nome", "");
    }

    public static String email(Context contexto) {
        return prefs(contexto).getString("email", "");
    }

    public static boolean professor(Context contexto) {
        return prefs(contexto).getBoolean("professor", false);
    }

    public static boolean ativa(Context contexto) {
        return token(contexto) != null && usuario(contexto) != null;
    }

    public static void encerrar(Context contexto) {
        prefs(contexto).edit().clear().apply();
    }
}
