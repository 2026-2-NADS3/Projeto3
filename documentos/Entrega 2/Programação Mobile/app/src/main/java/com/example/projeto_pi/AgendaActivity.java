package com.example.projeto_pi;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AgendaActivity extends BaseActivity {

    private ListaAdapter<Curso> adapter;
    private TextView tvVazio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        abrirTela(R.layout.activity_agenda, R.id.nav_agenda);
        definirTitulo(R.string.nav_agenda);
        tvVazio = findViewById(R.id.tvVazio);

        // Você não ensinou isso em aula, mas solicitou no PI: RecyclerView
        RecyclerView lista = findViewById(R.id.listaAgenda);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListaAdapter<>(new ListaAdapter.Ligador<Curso>() {
            @Override
            public int layout(Curso curso) {
                return R.layout.item_agenda;
            }

            @Override
            public void ligar(View item, Curso curso) {
                ligarItemAgenda(item, curso);
            }
        });
        lista.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mostrar(Curso.doCache(this), false);
        Curso.atualizar(this, new Ouvinte() {
            @Override
            protected void aoReceber(String corpo) {
                esconderAviso();
                mostrar(Curso.lista(corpo), true);
            }
        });
    }

    private void mostrar(List<Curso> cursos, boolean definitivo) {
        long agora = System.currentTimeMillis();
        List<Curso> agenda = new ArrayList<>();
        for (Curso curso : cursos) {
            // só entra na agenda o curso em que o aluno está inscrito e que ainda não terminou
            if (curso.inscrito && curso.fim >= agora) {
                agenda.add(curso);
            }
        }
        adapter.trocar(agenda);
        tvVazio.setVisibility(agenda.isEmpty() && definitivo ? View.VISIBLE : View.GONE);
    }
}
