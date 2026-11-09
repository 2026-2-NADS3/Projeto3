package com.example.projeto_pi;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Curso {

    public String id;
    public String titulo;
    public String descricao;
    public String categoria;
    public String local;
    public String data;
    public String horario;
    public int cargaHoraria;
    public int vagas;
    public long inicio;
    public long fim;
    public boolean certificado;
    public boolean inscrito;
    public boolean presente;
    public boolean novo;
    public String ingresso;

    public Curso(JSONObject json, long agora) {
        id = json.optString("id");
        titulo = json.optString("title");
        descricao = json.optString("description");
        categoria = json.optString("category");
        local = json.optString("location");
        vagas = json.optInt("available_spots");
        certificado = json.optBoolean("has_certificate");

        inicio = Util.dataParaMs(json.optString("course_date"));
        long fimInformado = Util.dataParaMs(json.optString("course_time_end"));
        if (fimInformado > inicio) {
            fim = fimInformado;
            cargaHoraria = (int) Math.max(1, Math.round((fim - inicio) / 3600000.0));
            horario = Util.hora(inicio) + " – " + Util.hora(fim);
        } else {
            fim = inicio + 3 * 3600000L;
            cargaHoraria = 0;
            horario = Util.hora(inicio);
        }
        data = Util.diaIso(inicio);

        long criado = Util.dataParaMs(json.optString("created_at"));
        novo = criado > 0 && agora - criado <= 14 * 86400000L;

        String status = json.optString("status");
        presente = status.equals("attended");
        inscrito = presente || status.equals("enrolled");
        if (inscrito) {
            ingresso = json.optString("ticket_code");
        }
    }

    public static List<Curso> lista(String json) {
        List<Curso> cursos = new ArrayList<>();
        if (json == null) {
            return cursos;
        }
        JSONArray array = Json.lista(json);
        long agora = System.currentTimeMillis();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null || Util.dataParaMs(item.optString("course_date")) < 0) {
                continue;
            }
            Curso curso = new Curso(item, agora);
            if (curso.inscrito || curso.fim >= agora) {
                cursos.add(curso);
            }
        }
        return cursos;
    }

    public static List<Curso> doCache(Context contexto) {
        String json = Banco.abrir(contexto).obter("cursos:" + Sessao.usuario(contexto));
        return lista(json);
    }

    public static void atualizar(final Context contexto, final Api.Ouvinte saida) {
        final String usuario = Sessao.usuario(contexto);
        String campos = "id,title,description,location,course_date,course_time_end,"
                + "available_spots,has_certificate,category,created_at";
        Api.get(contexto, "rest/v1/courses?select=" + campos + "&is_active=eq.true&order=course_date.asc",
                new Api.Ouvinte() {
                    @Override
                    public void aoResponder(final String cursos) {
                        Api.get(contexto, "rest/v1/enrollments?select=course_id,status,ticket_code&user_id=eq." + usuario,
                                new Api.Ouvinte() {
                                    @Override
                                    public void aoResponder(String inscricoes) {
                                        String junto = juntar(cursos, inscricoes);
                                        Banco.abrir(contexto).salvar("cursos:" + usuario, junto);
                                        Notificador.marcarVistos(contexto, "cur", cursos);
                                        saida.aoResponder(junto);
                                    }

                                    @Override
                                    public void aoFalhar(int codigo, String mensagem) {
                                        saida.aoFalhar(codigo, mensagem);
                                    }
                                });
                    }

                    @Override
                    public void aoFalhar(int codigo, String mensagem) {
                        saida.aoFalhar(codigo, mensagem);
                    }
                });
    }

    // junta o curso com a inscrição do aluno (status e código do ingresso)
    private static String juntar(String cursos, String inscricoes) {
        JSONArray listaCursos = Json.lista(cursos);
        JSONArray listaInscricoes = Json.lista(inscricoes);
        try {
            for (int i = 0; i < listaCursos.length(); i++) {
                JSONObject curso = listaCursos.getJSONObject(i);
                for (int j = 0; j < listaInscricoes.length(); j++) {
                    JSONObject inscricao = listaInscricoes.getJSONObject(j);
                    if (inscricao.optString("course_id").equals(curso.optString("id"))) {
                        curso.put("status", inscricao.optString("status"));
                        curso.put("ticket_code", inscricao.optString("ticket_code"));
                    }
                }
            }
        } catch (JSONException e) {
            return cursos;
        }
        return listaCursos.toString();
    }
}
