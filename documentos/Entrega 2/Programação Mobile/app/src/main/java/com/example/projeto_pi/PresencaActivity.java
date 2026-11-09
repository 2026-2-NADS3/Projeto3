package com.example.projeto_pi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class PresencaActivity extends BaseActivity {

    private static final int INGRESSOS = 0;
    private static final int HISTORICO = 1;

    private List<Curso> cursos = new ArrayList<>();
    private List<String> pendentes = new ArrayList<>();
    private int aba = INGRESSOS;
    private boolean definitivo = false;
    private boolean professor = false;
    private TextView tvSecao;
    private TextView tvVazio;
    private TextView[] chips;
    private ListaAdapter<Linha> adapter;

    private static class Linha {
        Curso curso;
        String status;
        boolean clicavel;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        abrirTela(R.layout.activity_presenca, R.id.nav_presenca);
        definirTitulo(R.string.nav_presenca);

        tvSecao = findViewById(R.id.tvSecao);
        tvVazio = findViewById(R.id.tvVazio);
        // Você não ensinou isso em aula, mas solicitou no PI: RecyclerView
        RecyclerView lista = findViewById(R.id.listaPresenca);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListaAdapter<>(new ListaAdapter.Ligador<Linha>() {
            @Override
            public int layout(Linha linha) {
                return R.layout.item_ingresso;
            }

            @Override
            public void ligar(View item, final Linha linha) {
                ((TextView) item.findViewById(R.id.tvTituloIngresso)).setText(linha.curso.titulo);
                ((TextView) item.findViewById(R.id.tvStatusIngresso)).setText(linha.status);
                if (linha.clicavel) {
                    item.setOnClickListener(v -> abrirCurso(linha.curso));
                } else {
                    item.setOnClickListener(null);
                    item.setClickable(false);
                }
            }
        });
        lista.setAdapter(adapter);
        chips = new TextView[]{findViewById(R.id.chipIngressos), findViewById(R.id.chipHistorico)};
        for (int i = 0; i < chips.length; i++) {
            final int posicao = i;
            chips[i].setOnClickListener(v -> {
                aba = posicao;
                mostrar();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        professor = Sessao.professor(this);
        cursos = Curso.doCache(this);
        pendentes = new ArrayList<>();
        for (Banco.ItemFila item : Banco.abrir(this).listarFila(Sessao.usuario(this), "presenca")) {
            pendentes.add(item.chave);
        }
        mostrar();

        Autenticacao.carregarPapel(this, () -> {
            if (!isFinishing() && professor != Sessao.professor(this)) {
                professor = Sessao.professor(this);
                mostrar();
            }
        });

        Curso.atualizar(this, new Ouvinte() {
            @Override
            protected void aoReceber(String corpo) {
                esconderAviso();
                cursos = Curso.lista(corpo);
                definitivo = true;
                mostrar();
            }

            @Override
            public void aoFalhar(int codigo, String mensagem) {
                super.aoFalhar(codigo, mensagem);
                definitivo = true;
                mostrar();
            }
        });
    }

    private void mostrar() {
        long agora = System.currentTimeMillis();
        List<Linha> linhas = new ArrayList<>();
        for (final Curso curso : cursos) {
            String status = null;
            boolean clicavel = false;
            if (professor) {
                if (curso.fim >= agora) {
                    status = getString(R.string.professor_item_status, Util.dataBr(curso.data), curso.horario);
                    clicavel = true;
                }
            } else if (aba == INGRESSOS) {
                if (curso.inscrito && !curso.presente && curso.fim >= agora && !curso.ingresso.isEmpty()) {
                    if (pendentes.contains(curso.id)) {
                        status = getString(R.string.ingresso_pendente);
                    } else {
                        status = getString(R.string.ingresso_ativo);
                    }
                    clicavel = true;
                }
            } else if (curso.presente) {
                status = getString(R.string.historico_confirmada, Util.dataBr(curso.data));
            } else if (curso.inscrito && curso.fim < agora) {
                status = getString(R.string.historico_ausente, Util.dataBr(curso.data));
            }
            if (status == null) {
                continue;
            }
            Linha linha = new Linha();
            linha.curso = curso;
            linha.status = status;
            linha.clicavel = clicavel;
            linhas.add(linha);
        }
        adapter.trocar(linhas);

        for (int i = 0; i < chips.length; i++) {
            chips[i].setSelected(i == aba);
            View barra = (View) chips[i].getParent();
            barra.setVisibility(professor ? View.GONE : View.VISIBLE);
        }
        if (professor) {
            tvSecao.setText(R.string.secao_professor);
            tvVazio.setText(R.string.vazio_professor);
        } else if (aba == INGRESSOS) {
            tvSecao.setText(R.string.secao_ativos);
            tvVazio.setText(R.string.vazio_ingressos);
        } else {
            tvSecao.setText(textoHistorico(agora));
            tvVazio.setText(R.string.vazio_historico);
        }
        boolean vazio = linhas.isEmpty();
        tvVazio.setVisibility(vazio && definitivo ? View.VISIBLE : View.GONE);
    }

    private String textoHistorico(long agora) {
        int encerrados = 0;
        int presentes = 0;
        for (Curso curso : cursos) {
            if (curso.presente) {
                encerrados++;
                presentes++;
            } else if (curso.inscrito && curso.fim < agora) {
                encerrados++;
            }
        }
        String titulo = getString(R.string.secao_historico);
        if (encerrados == 0) {
            return titulo;
        }
        int percentual = Math.round(100f * presentes / encerrados);
        return titulo + "\n" + getString(R.string.historico_frequencia, percentual, presentes, encerrados);
    }

    private void abrirCurso(Curso curso) {
        Intent intent;
        if (professor) {
            intent = new Intent(this, ScannerActivity.class);
            intent.putExtra("cursoId", curso.id);
            intent.putExtra("cursoNome", curso.titulo);
        } else {
            intent = new Intent(this, IngressoActivity.class);
            intent.putExtra("cursoNome", curso.titulo);
            intent.putExtra("codigo", curso.ingresso);
        }
        startActivity(intent);
    }
}
