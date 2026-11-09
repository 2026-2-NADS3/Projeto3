package com.example.projeto_pi;

import org.json.JSONObject;

public class Certificado {

    public String id;
    public String codigo;
    public String curso = "Curso";
    public String instituicao = "";
    public long emitido;
    public int cargaHoraria = 0;

    public Certificado(JSONObject json) {
        id = json.optString("id");
        codigo = json.optString("code").trim();
        emitido = Util.dataParaMs(json.optString("issued_at"));
        JSONObject dadosCurso = json.optJSONObject("courses");
        if (dadosCurso == null) {
            return;
        }
        if (!dadosCurso.optString("title").trim().isEmpty()) {
            curso = dadosCurso.optString("title").trim();
        }
        JSONObject universidade = dadosCurso.optJSONObject("universities");
        if (universidade != null) {
            instituicao = universidade.optString("name").trim();
        }
        long inicio = Util.dataParaMs(dadosCurso.optString("course_date"));
        long fim = Util.dataParaMs(dadosCurso.optString("course_time_end"));
        if (inicio > 0 && fim > inicio) {
            cargaHoraria = (int) Math.max(1, Math.round((fim - inicio) / 3600000.0));
        }
    }
}
