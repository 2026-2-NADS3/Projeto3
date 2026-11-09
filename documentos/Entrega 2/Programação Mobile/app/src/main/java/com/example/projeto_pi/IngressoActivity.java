package com.example.projeto_pi;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class IngressoActivity extends BaseActivity {

    private float brilhoAnterior = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        abrirTela(R.layout.activity_ingresso, R.id.nav_presenca);
        definirTitulo(R.string.nav_presenca);

        String cursoNome = getIntent().getStringExtra("cursoNome");
        final String codigo = getIntent().getStringExtra("codigo");

        ((TextView) findViewById(R.id.tvCursoIngresso)).setText(cursoNome);
        ((TextView) findViewById(R.id.tvCodigoIngresso)).setText(codigo);

        // Você não ensinou isso em aula, mas solicitou no PI: QR Code.
        // O QR leva o código do ingresso, que o professor lê para registrar a presença.
        ImageView ivQr = findViewById(R.id.ivQrIngresso);
        int tamanho = Math.round(224 * getResources().getDisplayMetrics().density);
        try {
            Bitmap qr = new BarcodeEncoder().encodeBitmap(Util.conteudoQr(codigo), BarcodeFormat.QR_CODE, tamanho, tamanho);
            ivQr.setImageBitmap(qr);
        } catch (WriterException e) {
            Toast.makeText(this, R.string.ingresso_erro_qr, Toast.LENGTH_LONG).show();
        }

        // botão para copiar o código caso o professor prefira digitar
        findViewById(R.id.btnCopiarCodigo).setOnClickListener(v -> {
            ClipboardManager area = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            area.setPrimaryClip(ClipData.newPlainText(getString(R.string.ingresso_codigo_rotulo), codigo));
            Toast.makeText(this, R.string.ingresso_copiado, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Window janela = getWindow();
        WindowManager.LayoutParams parametros = janela.getAttributes();
        brilhoAnterior = parametros.screenBrightness;
        parametros.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        janela.setAttributes(parametros);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Window janela = getWindow();
        WindowManager.LayoutParams parametros = janela.getAttributes();
        parametros.screenBrightness = brilhoAnterior;
        janela.setAttributes(parametros);
    }
}
