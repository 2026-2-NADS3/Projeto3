package com.example.projeto_pi;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CursosActivity extends BaseActivity {

    private static final int DISPONIVEIS = 0;
    private static final int INSCRITOS = 1;
    private static final int NOVOS = 2;

    private List<Curso> cursos = new ArrayList<>();
    private int filtro = DISPONIVEIS;
    private boolean definitivo = false;
    private EditText etBusca;
    private TextView tvVazio;
    private TextView[] chips;
    private ListaAdapter<Curso> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        abrirTela(R.layout.activity_cursos, R.id.nav_cursos);
        definirTitulo(R.string.nav_cursos);

        etBusca = findViewById(R.id.etBuscaCursos);
        tvVazio = findViewById(R.id.tvVazio);
        chips = new TextView[]{
                findViewById(R.id.chipDisponiveis),
                findViewById(R.id.chipInscritos),
                findViewById(R.id.chipNovos)};

        // Você não ensinou isso em aula, mas solicitou no PI: RecyclerView
        RecyclerView lista = findViewById(R.id.listaCursos);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListaAdapter<>(new ListaAdapter.Ligador<Curso>() {
            @Override
            public int layout(Curso curso) {
                return R.layout.item_curso;
            }

            @Override
            public void ligar(View item, final Curso curso) {
                ((TextView) item.findViewById(R.id.tvCategoria)).setText(curso.categoria);
                ((TextView) item.findViewById(R.id.tvNomeCurso)).setText(curso.titulo);
                ((TextView) item.findViewById(R.id.tvDescricaoCurso)).setText(curso.descricao);
                ((TextView) item.findViewById(R.id.tvLocalCurso)).setText(curso.local);
                TextView tvCarga = item.findViewById(R.id.tvCargaCurso);
                if (curso.cargaHoraria > 0) {
                    tvCarga.setText(getString(R.string.carga_horaria_valor, curso.cargaHoraria));
                } else {
                    tvCarga.setText(R.string.carga_indefinida);
                }
                item.setOnClickListener(v -> {
                    // Você não ensinou isso em aula, mas solicitou no PI: Fragment
                    CursoDetalheFragment.novo(curso.id).show(getSupportFragmentManager(), "detalhe");
                });
            }
        });
        lista.setAdapter(adapter);

        for (int i = 0; i < chips.length; i++) {
            final int posicao = i;
            chips[i].setOnClickListener(v -> {
                filtro = posicao;
                if (etBusca.getText().toString().isEmpty()) {
                    mostrar();
                } else {
                    etBusca.setText("");
                }
            });
        }

        etBusca.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mostrar();
            }
        });

        String busca = getIntent().getStringExtra("busca");
        if (busca != null && !busca.isEmpty()) {
            etBusca.setText(busca);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cursos = Curso.doCache(this);
        mostrar();
        carregar();
    }

    public void aoInscrever() {
        filtro = INSCRITOS;
        etBusca.setText("");
        carregar();
    }

    private void carregar() {
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
        String consulta = Util.normalizar(etBusca.getText().toString());
        boolean buscando = !consulta.isEmpty();
        List<Curso> filtrados = new ArrayList<>();
        for (Curso curso : cursos) {
            if (buscando) {
                // a busca ignora acento e maiúscula
                String alvo = Util.normalizar(curso.titulo + " " + curso.descricao + " " + curso.categoria);
                if (alvo.contains(consulta)) {
                    filtrados.add(curso);
                }
            } else if (passaNoFiltro(curso)) {
                filtrados.add(curso);
            }
        }
        adapter.trocar(filtrados);

        for (int i = 0; i < chips.length; i++) {
            chips[i].setSelected(!buscando && i == filtro);
        }
        if (buscando) {
            tvVazio.setText(R.string.vazio_busca);
        } else if (filtro == INSCRITOS) {
            tvVazio.setText(R.string.vazio_inscritos);
        } else if (filtro == NOVOS) {
            tvVazio.setText(R.string.vazio_novos);
        } else {
            tvVazio.setText(R.string.vazio_disponiveis);
        }
        tvVazio.setVisibility(filtrados.isEmpty() && definitivo ? View.VISIBLE : View.GONE);
    }

    private boolean passaNoFiltro(Curso curso) {
        if (filtro == INSCRITOS) {
            return curso.inscrito;
        }
        if (filtro == NOVOS) {
            return curso.novo;
        }
        return !curso.inscrito;
    }
}
