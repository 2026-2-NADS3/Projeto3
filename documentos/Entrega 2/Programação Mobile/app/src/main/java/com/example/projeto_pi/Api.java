package com.example.projeto_pi;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Api {

    public static final String URL = "https://kmezmmnuqcfotvmlqdeg.supabase.co/";
    private static final String CHAVE = "sb_publishable_MH-BESwQfyQQzRbgB6VU4w_TJKk9M8m";

    private static RequestQueue fila;

    public interface Ouvinte {
        void aoResponder(String corpo);

        void aoFalhar(int codigo, String mensagem);
    }

    private static RequestQueue fila(Context contexto) {
        if (fila == null) {
            fila = Volley.newRequestQueue(contexto.getApplicationContext());
        }
        return fila;
    }

    public static void get(Context contexto, String caminho, Ouvinte ouvinte) {
        chamar(contexto, Request.Method.GET, caminho, null, true, false, ouvinte);
    }

    public static void post(Context contexto, String caminho, JSONObject corpo, Ouvinte ouvinte) {
        chamar(contexto, Request.Method.POST, caminho, corpo, true, false, ouvinte);
    }

    public static void postSemLogin(Context contexto, String caminho, JSONObject corpo, Ouvinte ouvinte) {
        chamar(contexto, Request.Method.POST, caminho, corpo, false, false, ouvinte);
    }

    // todas as telas usam este método para falar com a API (Supabase)
    private static void chamar(final Context contexto, final int metodo, final String caminho,
                               final JSONObject corpo, final boolean comLogin,
                               final boolean jaRenovou, final Ouvinte ouvinte) {
        final String tokenUsado = comLogin ? Sessao.token(contexto) : null;

        Response.Listener<String> aoSucesso = new Response.Listener<String>() {
            @Override
            public void onResponse(String resposta) {
                ouvinte.aoResponder(resposta);
            }
        };

        Response.ErrorListener aoErro = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError erro) {
                int codigo = 0;
                String texto = "";
                if (erro.networkResponse != null) {
                    codigo = erro.networkResponse.statusCode;
                    if (erro.networkResponse.data != null) {
                        texto = new String(erro.networkResponse.data, StandardCharsets.UTF_8);
                    }
                }
                // 401 = token vencido: renovo o token uma vez e repito o pedido
                if (codigo == 401 && comLogin && !jaRenovou) {
                    renovarSessao(contexto, metodo, caminho, corpo, tokenUsado, ouvinte);
                    return;
                }
                ouvinte.aoFalhar(codigo, traduzir(codigo, texto));
            }
        };

        StringRequest pedido = new StringRequest(metodo, URL + caminho, aoSucesso, aoErro) {
            @Override
            public byte[] getBody() {
                if (corpo == null) {
                    return null;
                }
                return corpo.toString().getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> cabecalhos = new HashMap<>();
                cabecalhos.put("apikey", CHAVE);
                if (tokenUsado != null) {
                    cabecalhos.put("Authorization", "Bearer " + tokenUsado);
                }
                return cabecalhos;
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse resposta) {
                String texto = new String(resposta.data, StandardCharsets.UTF_8);
                return Response.success(texto, null);
            }
        };
        pedido.setShouldCache(false);
        pedido.setRetryPolicy(new DefaultRetryPolicy(15000, 0, 1f));
        fila(contexto).add(pedido);
    }

    private static void renovarSessao(final Context contexto, final int metodo, final String caminho,
                                      final JSONObject corpo, String tokenUsado, final Ouvinte ouvinte) {
        String atual = Sessao.token(contexto);
        if (atual != null && tokenUsado != null && !atual.equals(tokenUsado)) {
            chamar(contexto, metodo, caminho, corpo, true, true, ouvinte);
            return;
        }
        final String refresh = Sessao.refresh(contexto);
        if (refresh == null) {
            ouvinte.aoFalhar(401, traduzir(401, ""));
            return;
        }
        JSONObject pedido = Json.objeto("refresh_token", refresh);
        chamar(contexto, Request.Method.POST, "auth/v1/token?grant_type=refresh_token", pedido, false, true,
                new Ouvinte() {
                    @Override
                    public void aoResponder(String resposta) {
                        try {
                            JSONObject json = new JSONObject(resposta);
                            String novoToken = json.getString("access_token");
                            String novoRefresh = json.optString("refresh_token", refresh);
                            Sessao.guardarTokens(contexto, novoToken, novoRefresh);
                            chamar(contexto, metodo, caminho, corpo, true, true, ouvinte);
                        } catch (JSONException e) {
                            ouvinte.aoFalhar(401, traduzir(401, ""));
                        }
                    }

                    @Override
                    public void aoFalhar(int codigo, String mensagem) {
                        if (codigo == 0) {
                            ouvinte.aoFalhar(0, mensagem);
                        } else {
                            ouvinte.aoFalhar(401, traduzir(401, ""));
                        }
                    }
                });
    }

    private static String traduzir(int codigo, String texto) {
        String t = texto == null ? "" : texto.toLowerCase();
        if (codigo == 0) {
            return "Sem conexão com a internet.";
        }
        if (t.contains("invalid_credentials") || t.contains("invalid login credentials")) {
            return "E-mail ou senha inválidos.";
        }
        if (t.contains("user_already_exists") || t.contains("already registered") || t.contains("email_exists")) {
            return "Este e-mail já está cadastrado.";
        }
        if (t.contains("weak_password")) {
            return "Senha muito fraca. Use pelo menos 6 caracteres.";
        }
        if (t.contains("email_not_confirmed")) {
            return "Confirme seu e-mail antes de entrar.";
        }
        if (codigo == 429) {
            return "Muitas tentativas. Aguarde alguns minutos e tente de novo.";
        }
        if (codigo == 401) {
            return "Sua sessão expirou. Entre novamente.";
        }
        if (codigo == 403) {
            return "Você não tem permissão para esta ação.";
        }
        if (codigo >= 500) {
            return "Servidor indisponível no momento. Tente mais tarde.";
        }
        if (codigo == 400 || codigo == 422) {
            return "Confira os dados informados.";
        }
        return "Erro inesperado (" + codigo + ").";
    }
}
