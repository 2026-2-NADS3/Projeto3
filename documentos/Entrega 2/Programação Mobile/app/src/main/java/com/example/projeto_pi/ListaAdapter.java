package com.example.projeto_pi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

// Você não ensinou isso em aula, mas solicitou no PI: RecyclerView com Adapter.
// Eu fiz um adapter só para todas as listas do app, assim não repito código.
public class ListaAdapter<T> extends RecyclerView.Adapter<ListaAdapter.Linha> {

    public interface Ligador<T> {
        int layout(T dado);

        void ligar(View item, T dado);
    }

    public static class Linha extends RecyclerView.ViewHolder {
        public Linha(View item) {
            super(item);
        }
    }

    private final List<T> dados = new ArrayList<>();
    private final Ligador<T> ligador;

    public ListaAdapter(Ligador<T> ligador) {
        this.ligador = ligador;
    }

    public void trocar(List<T> novos) {
        dados.clear();
        dados.addAll(novos);
        notifyDataSetChanged();
    }

    public int quantidade() {
        return dados.size();
    }

    @Override
    public int getItemViewType(int posicao) {
        return ligador.layout(dados.get(posicao));
    }

    @NonNull
    @Override
    public Linha onCreateViewHolder(@NonNull ViewGroup pai, int layout) {
        View item = LayoutInflater.from(pai.getContext()).inflate(layout, pai, false);
        return new Linha(item);
    }

    @Override
    public void onBindViewHolder(@NonNull Linha linha, int posicao) {
        ligador.ligar(linha.itemView, dados.get(posicao));
    }

    @Override
    public int getItemCount() {
        return dados.size();
    }
}
