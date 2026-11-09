package com.example.projeto_pi;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

public class Autenticacao {

    public static boolean guardar(Context contexto, String corpo, String email, String nomeConhecido) {
        JSONObject json = Json.objetoDe(corpo);
        String token = json.optString("access_token");
        String refresh = json.optString("refresh_token");
        JSONObject usuario = json.optJSONObject("user");
        if (token.isEmpty() || refresh.isEmpty() || usuario == null || usuario.optString("id").isEmpty()) {
            return false;
        }
        String nome = nomeConhecido;
        if (nome == null) {
            JSONObject meta = usuario.optJSONObject("user_metadata");
            if (meta != null) {
                nome = meta.optString("full_name");
            }
        }
        if (nome == null || nome.isEmpty()) {
            nome = email.split("@")[0];
        }
        Banco.abrir(contexto).limparCache();
        Sessao.entrar(contexto, token, refresh, usuario.optString("id"), nome, usuario.optString("email", email));
        return true;
    }

    public static void carregarPerfil(final Context contexto, final Runnable depois) {
        String usuario = Sessao.usuario(contexto);
        Api.get(contexto, "rest/v1/profiles?select=full_name&id=eq." + usuario, new Api.Ouvinte() {
            @Override
            public void aoResponder(String corpo) {
                JSONArray lista = Json.lista(corpo);
                if (lista.length() > 0) {
                    String nome = lista.optJSONObject(0).optString("full_name");
                    if (!nome.isEmpty()) {
                        Sessao.definirNome(contexto, nome);
                    }
                }
                carregarPapel(contexto, depois);
            }

            @Override
            public void aoFalhar(int codigo, String mensagem) {
                depois.run();
            }
        });
    }

    public static void carregarPapel(final Context contexto, final Runnable depois) {
        String usuario = Sessao.usuario(contexto);
        Api.get(contexto, "rest/v1/user_roles?select=role&user_id=eq." + usuario, new Api.Ouvinte() {
            @Override
            public void aoResponder(String corpo) {
                JSONArray lista = Json.lista(corpo);
                boolean professor = false;
                for (int i = 0; i < lista.length(); i++) {
                    if (lista.optJSONObject(i).optString("role").equals("admin")) {
                        professor = true;
                    }
                }
                Sessao.definirProfessor(contexto, professor);
                depois.run();
            }

            @Override
            public void aoFalhar(int codigo, String mensagem) {
                depois.run();
            }
        });
    }
}
