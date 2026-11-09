package com.example.projeto_pi;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CardActivity extends BaseActivity {

    private static final String OUTRAS = "Outras instituições";

    private TextView tvTotal;
    private TextView tvNome;
    private TextView tvEscola;
    private TextView tvMatricula;
    private TextView tvVazio;
    private View contador;
    private LinearLayout lista;
    private final List<String> abertas = new ArrayList<>();
    private boolean carregou = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        abrirTela(R.layout.activity_card, R.id.nav_perfil);
        definirTitulo(R.string.card_titulo);

        tvTotal = findViewById(R.id.tvTotalCard);
        tvNome = findViewById(R.id.tvNomeCard);
        tvEscola = findViewById(R.id.tvEscolaCard);
        tvMatricula = findViewById(R.id.tvMatriculaCard);
        tvVazio = findViewById(R.id.tvVazioCard);
        contador = findViewById(R.id.contadorCard);
        lista = findViewById(R.id.listaInstituicoes);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String usuario = Sessao.usuario(this);
        String perfilSalvo = Banco.abrir(this).obter("card_perfil:" + usuario);
        String certificadosSalvos = Banco.abrir(this).obter("card_certificados:" + usuario);
        if (perfilSalvo != null && certificadosSalvos != null) {
            mostrar(Json.objetoDe(perfilSalvo), Json.lista(certificadosSalvos), false);
        }

        Api.get(this, "rest/v1/profiles?select=full_name,school,student_number&id=eq." + usuario, new Ouvinte() {
            @Override
            protected void aoReceber(final String perfil) {
                Api.get(CardActivity.this, "rest/v1/certificates?select=id,issued_at,courses(title,universities(name))"
                        + "&user_id=eq." + Sessao.usuario(CardActivity.this) + "&order=issued_at.desc", new Ouvinte() {
                    @Override
                    protected void aoReceber(String certificados) {
                        JSONArray listaPerfil = Json.lista(perfil);
                        JSONObject dados = new JSONObject();
                        if (listaPerfil.length() > 0) {
                            dados = listaPerfil.optJSONObject(0);
                        }
                        String usuario = Sessao.usuario(CardActivity.this);
                        Banco.abrir(CardActivity.this).salvar("card_perfil:" + usuario, dados.toString());
                        Banco.abrir(CardActivity.this).salvar("card_certificados:" + usuario, certificados);
                        esconderAviso();
                        mostrar(dados, Json.lista(certificados), true);
                    }

                    @Override
                    public void aoFalhar(int codigo, String mensagem) {
                        falhou(codigo, mensagem);
                    }
                });
            }

            @Override
            public void aoFalhar(int codigo, String mensagem) {
                falhou(codigo, mensagem);
            }
        });
    }

    private void falhou(int codigo, String mensagem) {
        if (isFinishing()) {
            return;
        }
        tratarErro(codigo);
        if (!carregou) {
            tvNome.setText(Sessao.nome(this));
            tvEscola.setText(R.string.card_nao_informado);
            tvMatricula.setText(R.string.card_nao_informado);
        }
    }

    private String textoOu(String texto, String padrao) {
        if (texto == null || texto.trim().isEmpty()) {
            return padrao;
        }
        return texto.trim();
    }

    private void mostrar(JSONObject perfil, JSONArray certificados, boolean definitivo) {
        carregou = true;
        String naoInformado = getString(R.string.card_nao_informado);
        tvNome.setText(textoOu(perfil.optString("full_name"), Sessao.nome(this)));
        tvEscola.setText(textoOu(perfil.optString("school"), naoInformado));
        tvMatricula.setText(textoOu(perfil.optString("student_number"), naoInformado));

        List<String> nomes = new ArrayList<>();
        List<List<String>> cursosPorInstituicao = new ArrayList<>();
        for (int i = 0; i < certificados.length(); i++) {
            JSONObject certificado = certificados.optJSONObject(i);
            if (certificado == null) {
                continue;
            }
            String instituicao = OUTRAS;
            String curso = "Curso";
            JSONObject dadosCurso = certificado.optJSONObject("courses");
            if (dadosCurso != null) {
                if (!dadosCurso.optString("title").trim().isEmpty()) {
                    curso = dadosCurso.optString("title").trim();
                }
                JSONObject universidade = dadosCurso.optJSONObject("universities");
                if (universidade != null && !universidade.optString("name").trim().isEmpty()) {
                    instituicao = universidade.optString("name").trim();
                }
            }
            int posicao = nomes.indexOf(instituicao);
            if (posicao < 0) {
                nomes.add(instituicao);
                cursosPorInstituicao.add(new ArrayList<String>());
                posicao = nomes.size() - 1;
            }
            cursosPorInstituicao.get(posicao).add(curso);
        }

        int total = certificados.length();
        tvTotal.setText(String.valueOf(total));
        contador.setContentDescription(getResources().getQuantityString(R.plurals.card_total_descricao, total, total));
        tvVazio.setVisibility(nomes.isEmpty() && definitivo ? View.VISIBLE : View.GONE);

        lista.removeAllViews();
        while (!nomes.isEmpty()) {
            int escolhida = 0;
            for (int i = 1; i < nomes.size(); i++) {
                if (vemAntes(nomes.get(i), cursosPorInstituicao.get(i).size(),
                        nomes.get(escolhida), cursosPorInstituicao.get(escolhida).size())) {
                    escolhida = i;
                }
            }
            lista.addView(criarInstituicao(nomes.get(escolhida), cursosPorInstituicao.get(escolhida)));
            nomes.remove(escolhida);
            cursosPorInstituicao.remove(escolhida);
        }
    }

    private boolean vemAntes(String nomeA, int qtdA, String nomeB, int qtdB) {
        boolean aOutras = nomeA.equals(OUTRAS);
        boolean bOutras = nomeB.equals(OUTRAS);
        if (aOutras != bOutras) {
            return bOutras;
        }
        if (qtdA != qtdB) {
            return qtdA > qtdB;
        }
        return nomeA.compareToIgnoreCase(nomeB) < 0;
    }

    private View criarInstituicao(final String nome, List<String> cursos) {
        final View item = getLayoutInflater().inflate(R.layout.item_instituicao_card, lista, false);
        final LinearLayout listaCursos = item.findViewById(R.id.listaCursosInstituicao);
        final View seta = item.findViewById(R.id.ivSetaInstituicao);
        final View cabecalho = item.findViewById(R.id.cabecalhoInstituicao);

        final String quantidade = getResources().getQuantityString(R.plurals.card_concluidos, cursos.size(), cursos.size());
        ((TextView) item.findViewById(R.id.tvNomeInstituicao)).setText(nome);
        ((TextView) item.findViewById(R.id.tvQtdInstituicao)).setText(quantidade);

        for (String curso : cursos) {
            TextView linha = (TextView) getLayoutInflater().inflate(R.layout.item_curso_card, listaCursos, false);
            linha.setText(curso);
            listaCursos.addView(linha);
        }

        aplicar(nome, quantidade, abertas.contains(nome), listaCursos, seta, cabecalho);
        cabecalho.setOnClickListener(v -> {
            boolean abrir = !abertas.contains(nome);
            if (abrir) {
                abertas.add(nome);
            } else {
                abertas.remove(nome);
            }
            aplicar(nome, quantidade, abrir, listaCursos, seta, cabecalho);
        });
        return item;
    }

    private void aplicar(String nome, String quantidade, boolean aberta, View cursos, View seta, View cabecalho) {
        cursos.setVisibility(aberta ? View.VISIBLE : View.GONE);
        seta.setRotation(aberta ? -90 : 90);
        int texto = aberta ? R.string.card_instituicao_aberta : R.string.card_instituicao_fechada;
        cabecalho.setContentDescription(getString(texto, nome, quantidade));
    }
}
