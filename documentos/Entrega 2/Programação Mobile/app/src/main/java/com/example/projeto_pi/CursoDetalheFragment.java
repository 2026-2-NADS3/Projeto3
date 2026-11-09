package com.example.projeto_pi;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

// Você não ensinou isso em aula, mas solicitou no PI: Fragment.
// Este fragment é a janelinha que sobe com os detalhes do curso.
public class CursoDetalheFragment extends BottomSheetDialogFragment {

    private Curso curso;
    private Button btnInscrever;
    private TextView tvErro;

    public static CursoDetalheFragment novo(String cursoId) {
        CursoDetalheFragment fragment = new CursoDetalheFragment();
        Bundle argumentos = new Bundle();
        argumentos.putString("cursoId", cursoId);
        fragment.setArguments(argumentos);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup pai, @Nullable Bundle estado) {
        return inflater.inflate(R.layout.dialog_curso_detalhe, pai, false);
    }

    @Override
    public void onViewCreated(@NonNull View tela, @Nullable Bundle estado) {
        super.onViewCreated(tela, estado);

        // procura o curso na lista que já está guardada no aparelho
        String cursoId = getArguments().getString("cursoId");
        for (Curso c : Curso.doCache(requireContext())) {
            if (c.id.equals(cursoId)) {
                curso = c;
            }
        }
        if (curso == null) {
            dismiss();
            return;
        }

        ((TextView) tela.findViewById(R.id.tvCategoria)).setText(curso.categoria);
        ((TextView) tela.findViewById(R.id.tvNomeCurso)).setText(curso.titulo);
        ((TextView) tela.findViewById(R.id.tvDescricaoCurso)).setText(curso.descricao);
        ((TextView) tela.findViewById(R.id.tvPeriodo)).setText(
                getString(R.string.detalhe_data, Util.dataBr(curso.data), curso.horario));
        ((TextView) tela.findViewById(R.id.tvLocal)).setText(getString(R.string.detalhe_local, curso.local));

        TextView tvCarga = tela.findViewById(R.id.tvCarga);
        if (curso.cargaHoraria > 0) {
            tvCarga.setText(getString(R.string.detalhe_carga, curso.cargaHoraria));
        } else {
            tvCarga.setText(R.string.detalhe_carga_indefinida);
        }

        TextView tvFrequencia = tela.findViewById(R.id.tvFrequencia);
        if (curso.certificado) {
            tvFrequencia.setText(R.string.detalhe_com_certificado);
        } else {
            tvFrequencia.setText(R.string.detalhe_sem_certificado);
        }

        TextView tvParticipacao = tela.findViewById(R.id.tvParticipacao);
        btnInscrever = tela.findViewById(R.id.btnInscrever);
        tvErro = tela.findViewById(R.id.tvErro);

        if (curso.inscrito) {
            tvParticipacao.setVisibility(View.VISIBLE);
            if (curso.presente) {
                tvParticipacao.setText(R.string.detalhe_presenca_ok);
            } else {
                tvParticipacao.setText(R.string.detalhe_presenca_pendente);
            }
            btnInscrever.setVisibility(View.GONE);
        } else {
            tvParticipacao.setVisibility(View.GONE);
            btnInscrever.setVisibility(View.VISIBLE);
            if (curso.vagas <= 0) {
                btnInscrever.setEnabled(false);
                btnInscrever.setText(R.string.detalhe_sem_vagas);
            } else {
                btnInscrever.setOnClickListener(v -> inscrever());
            }
        }
    }

    private void inscrever() {
        btnInscrever.setEnabled(false);
        btnInscrever.setText(R.string.carregando);
        tvErro.setVisibility(View.GONE);

        Api.post(requireContext(), "rest/v1/rpc/inscrever_curso", Json.objeto("p_course_id", curso.id),
                new Api.Ouvinte() {
                    @Override
                    public void aoResponder(String corpo) {
                        if (!isAdded()) {
                            return;
                        }
                        String status = Json.objetoDe(corpo).optString("status");
                        if (status.equals("inscrito") || status.equals("ja_inscrito")) {
                            CursosActivity tela = (CursosActivity) requireActivity();
                            dismiss();
                            tela.aoInscrever();
                            return;
                        }
                        mostrarErro(mensagemInscricao(status));
                    }

                    @Override
                    public void aoFalhar(int codigo, String mensagem) {
                        if (!isAdded()) {
                            return;
                        }
                        if (codigo == 401) {
                            BaseActivity tela = (BaseActivity) requireActivity();
                            dismiss();
                            tela.tratarErro(codigo);
                            return;
                        }
                        mostrarErro(mensagem);
                    }
                });
    }

    private void mostrarErro(String mensagem) {
        btnInscrever.setEnabled(true);
        btnInscrever.setText(R.string.btn_inscrever);
        tvErro.setText(mensagem);
        tvErro.setVisibility(View.VISIBLE);
    }

    private String mensagemInscricao(String status) {
        if (status.equals("sem_vagas")) {
            return "Não há mais vagas neste curso.";
        }
        if (status.equals("bloqueado")) {
            return "Sua conta está bloqueada por faltas. Procure a equipe da ONG.";
        }
        if (status.equals("encerrado")) {
            return "Este curso já foi encerrado.";
        }
        return "Curso indisponível.";
    }
}
