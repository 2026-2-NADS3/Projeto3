package com.example.projeto_pi;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ComunicadosActivity extends BaseActivity {

    private static final int TODOS = 0;
    private static final int LIDOS = 1;
    private static final int NAO_LIDOS = 2;

    private List<Comunicado> comunicados = new ArrayList<>();
    private boolean definitivo = false;
    private int filtro = TODOS;
    private TextView[] chips;
    private ListaAdapter<Comunicado> adapter;
    private TextView tvVazio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        abrirTela(R.layout.activity_comunicados, R.id.nav_inicio);
        definirTitulo(R.string.comunicados_titulo);

        // Você não ensinou isso em aula, mas solicitou no PI: RecyclerView
        RecyclerView lista = findViewById(R.id.listaComunicados);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListaAdapter<>(new ListaAdapter.Ligador<Comunicado>() {
            @Override
            public int layout(Comunicado comunicado) {
                return R.layout.item_comunicado;
            }

            @Override
            public void ligar(final View card, final Comunicado comunicado) {
                comunicado.preencher(card);
                card.setOnClickListener(v -> {
                    // ao tocar no comunicado ele passa a ser considerado lido
                    if (!comunicado.lido) {
                        comunicado.lido = true;
                        Comunicado.marcarLido(ComunicadosActivity.this, comunicado.id);
                        comunicado.preencher(card);
                    }
                });
            }
        });
        lista.setAdapter(adapter);
        tvVazio = findViewById(R.id.tvVazioComunicados);
        chips = new TextView[]{
                findViewById(R.id.chipTodosComunicados),
                findViewById(R.id.chipLidos),
                findViewById(R.id.chipNaoLidos)};
        for (int i = 0; i < chips.length; i++) {
            final int posicao = i;
            chips[i].setOnClickListener(v -> {
                filtro = posicao;
                mostrar();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        comunicados = Comunicado.doCache(this);
        mostrar();
        Comunicado.atualizar(this, new Ouvinte() {
            @Override
            protected void aoReceber(String corpo) {
                esconderAviso();
                comunicados = Comunicado.lista(ComunicadosActivity.this, corpo);
                definitivo = true;
                mostrar();
            }
        });
    }

    private void mostrar() {
        for (int i = 0; i < chips.length; i++) {
            chips[i].setSelected(i == filtro);
        }
        List<Comunicado> filtrados = new ArrayList<>();
        for (Comunicado comunicado : comunicados) {
            if (filtro == TODOS || (filtro == LIDOS) == comunicado.lido) {
                filtrados.add(comunicado);
            }
        }
        adapter.trocar(filtrados);
        if (filtro == LIDOS) {
            tvVazio.setText(R.string.comunicados_vazio_lidas);
        } else if (filtro == NAO_LIDOS) {
            tvVazio.setText(R.string.comunicados_vazio_nao_lidas);
        } else {
            tvVazio.setText(R.string.comunicados_vazio);
        }
        boolean vazio = filtrados.isEmpty();
        tvVazio.setVisibility(vazio && definitivo ? View.VISIBLE : View.GONE);
    }
}
