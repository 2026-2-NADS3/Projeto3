package com.example.projeto_pi;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class CertificadosActivity extends BaseActivity {

    private List<Certificado> certificados = new ArrayList<>();
    private boolean definitivo = false;
    private Certificado escolhido;
    private EditText etBusca;
    private TextView tvVazio;
    private ListaAdapter<Certificado> adapter;

    // Você não ensinou isso em aula, mas solicitou no PI: baixar o PDF.
    // Abre a tela do Android para o aluno escolher onde salvar o arquivo.
    private final ActivityResultLauncher<String> salvarComo = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/pdf"), uri -> gravar(uri));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        abrirTela(R.layout.activity_certificados, R.id.nav_inicio);
        definirTitulo(R.string.certificados_titulo);

        etBusca = findViewById(R.id.etBuscaCertificados);
        tvVazio = findViewById(R.id.tvVazioCertificados);
        // Você não ensinou isso em aula, mas solicitou no PI: RecyclerView
        RecyclerView lista = findViewById(R.id.listaCertificados);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListaAdapter<>(new ListaAdapter.Ligador<Certificado>() {
            @Override
            public int layout(Certificado certificado) {
                return R.layout.item_certificado;
            }

            @Override
            public void ligar(View item, final Certificado certificado) {
                ((TextView) item.findViewById(R.id.tvCursoCertificado)).setText(certificado.curso);
                TextView tvConclusao = item.findViewById(R.id.tvConclusaoCertificado);
                if (certificado.emitido > 0) {
                    tvConclusao.setText(Util.dataBrMs(certificado.emitido));
                } else {
                    tvConclusao.setText("–");
                }
                View botao = item.findViewById(R.id.btnBaixarCertificado);
                botao.setContentDescription(getString(R.string.certificado_baixar_descricao, certificado.curso));
                botao.setOnClickListener(v -> {
                    escolhido = certificado;
                    salvarComo.launch(nomeDoArquivo(certificado));
                });
            }
        });
        lista.setAdapter(adapter);

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        String usuario = Sessao.usuario(this);
        carregarLista(Banco.abrir(this).obter("certificados:" + usuario));
        mostrar();

        Api.get(this, "rest/v1/certificates?select=id,code,issued_at,courses(title,course_date,course_time_end,universities(name))"
                + "&user_id=eq." + usuario + "&order=issued_at.desc", new Ouvinte() {
            @Override
            protected void aoReceber(String corpo) {
                Banco.abrir(CertificadosActivity.this).salvar("certificados:" + Sessao.usuario(CertificadosActivity.this), corpo);
                Notificador.marcarVistos(CertificadosActivity.this, "cer", corpo);
                esconderAviso();
                carregarLista(corpo);
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

    private void carregarLista(String json) {
        certificados = new ArrayList<>();
        if (json == null) {
            return;
        }
        JSONArray array = Json.lista(json);
        for (int i = 0; i < array.length(); i++) {
            if (array.optJSONObject(i) != null) {
                certificados.add(new Certificado(array.optJSONObject(i)));
            }
        }
    }

    private void mostrar() {
        String consulta = Util.normalizar(etBusca.getText().toString());
        List<Certificado> filtrados = new ArrayList<>();
        for (Certificado certificado : certificados) {
            if (consulta.isEmpty() || Util.normalizar(certificado.curso).contains(consulta)) {
                filtrados.add(certificado);
            }
        }
        adapter.trocar(filtrados);
        if (consulta.isEmpty()) {
            tvVazio.setText(R.string.certificados_vazio);
        } else {
            tvVazio.setText(R.string.vazio_busca);
        }
        boolean vazio = filtrados.isEmpty();
        tvVazio.setVisibility(vazio && definitivo ? View.VISIBLE : View.GONE);
    }

    private String nomeDoArquivo(Certificado c) {
        String base = c.curso.replaceAll("[\\\\/:*?\"<>|]", " ").replaceAll("\\s+", " ").trim();
        if (base.isEmpty()) {
            base = c.codigo;
        }
        return "Certificado - " + base + ".pdf";
    }

    private void gravar(final Uri destino) {
        if (destino == null || escolhido == null) {
            return;
        }
        View raiz = findViewById(R.id.conteudo);
        try {
            OutputStream saida = getContentResolver().openOutputStream(destino);
            CertificadoPdf.gravar(this, escolhido, Sessao.nome(this), saida);
            saida.close();
            Snackbar.make(raiz, R.string.certificado_salvo, Snackbar.LENGTH_LONG)
                    .setAction(R.string.certificado_abrir, v -> abrir(destino))
                    .show();
        } catch (IOException | RuntimeException e) {
            Snackbar.make(raiz, R.string.certificado_erro_salvar, Snackbar.LENGTH_LONG).show();
        }
    }

    private void abrir(Uri arquivo) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(arquivo, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Snackbar.make(findViewById(R.id.conteudo), R.string.certificado_sem_leitor, Snackbar.LENGTH_LONG).show();
        }
    }
}
