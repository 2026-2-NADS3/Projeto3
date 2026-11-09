package com.example.projeto_pi;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import java.util.Collections;
import java.util.List;

// Função principal da entrega: registrar a presença lendo o QR Code do aluno.
// Você não ensinou isso em aula, mas solicitou no PI: leitura de QR Code com a biblioteca ZXing.
public class ScannerActivity extends BaseActivity {

    private String cursoId;
    private DecoratedBarcodeView camera;
    private View cardCamera;
    private TextView tvStatusCamera;
    private EditText etCodigo;
    private View btnEnviar;
    private boolean cameraPermitida = false;
    private boolean processando = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        abrirTela(R.layout.activity_scanner, R.id.nav_presenca);
        definirTitulo(R.string.nav_presenca);

        cursoId = getIntent().getStringExtra("cursoId");
        ((TextView) findViewById(R.id.tvCursoScanner)).setText(getIntent().getStringExtra("cursoNome"));

        camera = findViewById(R.id.barcodeView);
        cardCamera = findViewById(R.id.cardCamera);
        tvStatusCamera = findViewById(R.id.tvStatusCamera);
        etCodigo = findViewById(R.id.etCodigo);
        btnEnviar = findViewById(R.id.btnEnviarCodigo);

        // a câmera só precisa ler QR Code, então digo isso para a biblioteca
        camera.setStatusText("");
        camera.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(Collections.singletonList(BarcodeFormat.QR_CODE)));

        btnEnviar.setOnClickListener(v -> {
            InputMethodManager teclado = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            teclado.hideSoftInputFromWindow(etCodigo.getWindowToken(), 0);
            processar(etCodigo.getText().toString());
        });

        // o app só pode usar a câmera se o professor deixar
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraPermitida = true;
        } else {
            tvStatusCamera.setText(R.string.scanner_aguardando_permissao);
            tvStatusCamera.setVisibility(View.VISIBLE);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int codigo, String[] permissoes, int[] resultados) {
        super.onRequestPermissionsResult(codigo, permissoes, resultados);
        if (resultados.length > 0 && resultados[0] == PackageManager.PERMISSION_GRANTED) {
            cameraPermitida = true;
            iniciarCamera();
        } else {
            tvStatusCamera.setText(R.string.scanner_sem_permissao);
            tvStatusCamera.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraPermitida && !processando) {
            iniciarCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        camera.pause();
    }

    private void iniciarCamera() {
        cardCamera.setVisibility(View.VISIBLE);
        tvStatusCamera.setVisibility(View.GONE);
        camera.resume();
        // lê um QR Code e, quando achar, manda o texto para o processar()
        camera.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult resultado) {
                if (resultado != null && resultado.getText() != null) {
                    processar(resultado.getText());
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> pontos) {
            }
        });
    }

    private void processar(String lido) {
        if (processando) {
            return;
        }
        processando = true;
        camera.pause();
        btnEnviar.setEnabled(false);

        // limpa o texto lido (tira o PEI| do começo) e confere se tem 16 caracteres
        final String codigo = Util.codigoIngresso(lido);
        if (codigo == null) {
            terminar("Ingresso inválido. Confira o QR Code ou o código.", false);
            return;
        }

        // manda o código para a API registrar a presença do aluno neste curso
        Api.post(this, "rest/v1/rpc/registrar_presenca_ingresso",
                Json.objeto("p_course_id", cursoId, "p_code", codigo), new Ouvinte() {
                    @Override
                    protected void aoReceber(String corpo) {
                        interpretar(Json.objetoDe(corpo));
                    }

                    @Override
                    public void aoFalhar(int status, String mensagem) {
                        // status 0 = sem internet: guardo na fila e envio depois
                        if (status == 0) {
                            Banco.abrir(ScannerActivity.this).colocarNaFila("presenca", Sessao.usuario(ScannerActivity.this), cursoId, codigo);
                            EnvioWorker.agendar(ScannerActivity.this);
                            terminar("Sem conexão. A presença foi guardada e será enviada quando a internet voltar.", true);
                        } else if (status == 401) {
                            tratarErro(status);
                        } else if (status == 403) {
                            terminar("Apenas professores podem registrar presença.", false);
                        } else {
                            terminar(mensagem, false);
                        }
                    }
                });
    }

    private void interpretar(org.json.JSONObject resposta) {
        String status = resposta.optString("status");
        String aluno = resposta.optString("aluno");
        if (aluno.isEmpty()) {
            aluno = "O aluno";
        }
        if (status.equals("registrada")) {
            terminar("Presença de " + aluno + " registrada.", true);
        } else if (status.equals("duplicada")) {
            terminar(aluno + " já teve a presença registrada neste curso.", false);
        } else if (status.equals("outro_curso")) {
            terminar("O ingresso de " + aluno + " é de outro curso: " + resposta.optString("curso") + ".", false);
        } else if (status.equals("sem_matricula")) {
            terminar("A inscrição de " + aluno + " não está ativa.", false);
        } else if (status.equals("invalido")) {
            terminar("Ingresso inválido. Confira o QR Code ou o código.", false);
        } else {
            terminar("Não foi possível registrar a presença.", false);
        }
    }

    private void terminar(String mensagem, boolean sucesso) {
        processando = false;
        btnEnviar.setEnabled(true);
        etCodigo.setText("");
        int titulo = sucesso ? R.string.scanner_titulo_ok : R.string.scanner_titulo_atencao;
        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(mensagem)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialogo, botao) -> {
                    if (cameraPermitida) {
                        iniciarCamera();
                    }
                })
                .show();
    }
}
