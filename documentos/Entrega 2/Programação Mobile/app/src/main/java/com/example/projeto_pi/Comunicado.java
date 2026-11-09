package com.example.projeto_pi;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Comunicado {

    public String id;
    public String titulo;
    public String mensagem;
    public String cursoId;
    public long criado;
    public boolean lido;

    public Comunicado(JSONObject json) {
        id = json.optString("id");
        titulo = json.optString("title");
        mensagem = json.optString("message");
        if (json.isNull("course_id")) {
            cursoId = null;
        } else {
            cursoId = json.optString("course_id");
        }
        criado = Util.dataParaMs(json.optString("created_at"));
    }

    public static List<Comunicado> lista(Context contexto, String json) {
        List<Comunicado> lista = new ArrayList<>();
        if (json == null) {
            return lista;
        }
        String lidos = Banco.abrir(contexto).obter("lidos:" + Sessao.usuario(contexto));
        if (lidos == null) {
            lidos = "[]";
        }
        JSONArray array = Json.lista(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item != null) {
                Comunicado c = new Comunicado(item);
                c.lido = lidos.contains("\"" + c.id + "\"");
                lista.add(c);
            }
        }
        return lista;
    }

    public static List<Comunicado> doCache(Context contexto) {
        return lista(contexto, Banco.abrir(contexto).obter("comunicados:" + Sessao.usuario(contexto)));
    }

    public static void atualizar(final Context contexto, final Api.Ouvinte saida) {
        Api.get(contexto, "rest/v1/announcements?select=id,title,message,course_id,created_at&order=created_at.desc&limit=100",
                new Api.Ouvinte() {
                    @Override
                    public void aoResponder(String corpo) {
                        Banco.abrir(contexto).salvar("comunicados:" + Sessao.usuario(contexto), corpo);
                        Notificador.marcarVistos(contexto, "com", corpo);
                        saida.aoResponder(corpo);
                    }

                    @Override
                    public void aoFalhar(int codigo, String mensagem) {
                        saida.aoFalhar(codigo, mensagem);
                    }
                });
    }

    public static void marcarLido(Context contexto, String id) {
        String chave = "lidos:" + Sessao.usuario(contexto);
        JSONArray lidos = Json.lista(Banco.abrir(contexto).obter(chave));
        lidos.put(id);
        Banco.abrir(contexto).salvar(chave, lidos.toString());
    }

    public static boolean temNaoLido(List<Comunicado> lista) {
        for (Comunicado c : lista) {
            if (!c.lido) {
                return true;
            }
        }
        return false;
    }

    public void preencher(View card) {
        ImageView icone = card.findViewById(R.id.ivIconeComunicado);
        String t = Util.normalizar(titulo);
        if (t.contains("certificado")) {
            icone.setImageResource(R.drawable.ic_certificado);
        } else if (t.contains("curso") || cursoId != null) {
            icone.setImageResource(R.drawable.ic_capelo);
        } else {
            icone.setImageResource(R.drawable.ic_chat);
        }
        ((TextView) card.findViewById(R.id.tvTituloComunicado)).setText(titulo);
        ((TextView) card.findViewById(R.id.tvMensagemComunicado)).setText(mensagem);
        String data = Util.dataComunicado(criado);
        ((TextView) card.findViewById(R.id.tvDataComunicado)).setText(data);
        String descricao = titulo + ". " + data;
        if (!lido) {
            descricao = card.getContext().getString(R.string.comunicado_nao_lido) + ". " + descricao;
        }
        card.setContentDescription(descricao);
        card.findViewById(R.id.pontoComunicado).setVisibility(lido ? View.GONE : View.VISIBLE);
    }
}
