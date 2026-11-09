package com.example.projeto_pi;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

// Você não ensinou isso em aula, mas solicitou no PI: Fragment.
// A tela de privacidade é um Fragment que fica dentro da PrivacidadeActivity.
public class PrivacidadeFragment extends Fragment {

    private final int[] titulos = {R.string.privacidade_t1, R.string.privacidade_t2,
            R.string.privacidade_t3, R.string.privacidade_t4, R.string.privacidade_t5};
    private final int[] corpos = {R.string.privacidade_c1, R.string.privacidade_c2,
            R.string.privacidade_c3, R.string.privacidade_c4, R.string.privacidade_c5};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup pai, @Nullable Bundle estado) {
        return inflater.inflate(R.layout.fragment_privacidade, pai, false);
    }

    @Override
    public void onViewCreated(@NonNull View tela, @Nullable Bundle estado) {
        super.onViewCreated(tela, estado);

        LinearLayout lista = tela.findViewById(R.id.listaSecoesPrivacidade);
        for (int i = 0; i < titulos.length; i++) {
            final View secao = getLayoutInflater().inflate(R.layout.item_secao_privacidade, lista, false);
            ((TextView) secao.findViewById(R.id.tvTituloSecao)).setText(titulos[i]);
            ((TextView) secao.findViewById(R.id.tvCorpoSecao)).setText(corpos[i]);
            secao.findViewById(R.id.cabecalhoSecao).setOnClickListener(v -> {
                boolean aberta = secao.findViewById(R.id.tvCorpoSecao).getVisibility() == View.VISIBLE;
                definir(secao, !aberta);
            });
            definir(secao, i == 0);
            lista.addView(secao);
        }
    }

    private void definir(View secao, boolean aberta) {
        secao.findViewById(R.id.tvCorpoSecao).setVisibility(aberta ? View.VISIBLE : View.GONE);
        ImageView seta = secao.findViewById(R.id.ivSetaSecao);
        seta.setRotation(aberta ? 90 : 0);
        int estado = aberta ? R.string.privacidade_expandido : R.string.privacidade_recolhido;
        ViewCompat.setStateDescription(secao.findViewById(R.id.cabecalhoSecao), getString(estado));
    }
}
