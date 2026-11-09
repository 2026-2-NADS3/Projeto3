package com.example.projeto_pi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import androidx.core.content.res.ResourcesCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.IOException;
import java.io.OutputStream;

// Você não ensinou isso em aula, mas solicitou no PI: baixar o certificado em PDF.
// Eu desenho o certificado numa página usando o PdfDocument do Android.
public class CertificadoPdf {

    private static final int LARGURA = 842;
    private static final int ALTURA = 595;
    private static final int ROXO = Color.parseColor("#643481");
    private static final int PRETO = Color.parseColor("#1A1A1A");
    private static final int LARGURA_TEXTO = 620;

    public static void gravar(Context contexto, Certificado certificado, String aluno, OutputStream saida) throws IOException {
        // uma página A4 deitada: 842 x 595 pontos
        PdfDocument documento = new PdfDocument();
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(LARGURA, ALTURA, 1).create();
        PdfDocument.Page pagina = documento.startPage(info);
        desenhar(contexto, pagina.getCanvas(), certificado, aluno);
        documento.finishPage(pagina);
        documento.writeTo(saida);
        documento.close();
    }

    private static TextPaint pincel(float tamanho, boolean negrito, Typeface fonte) {
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(PRETO);
        p.setTextSize(tamanho);
        if (fonte != null) {
            p.setTypeface(fonte);
        } else if (negrito) {
            p.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        }
        return p;
    }

    private static float bloco(Canvas canvas, String texto, TextPaint pincel, float y) {
        StaticLayout layout = StaticLayout.Builder.obtain(texto, 0, texto.length(), pincel, LARGURA_TEXTO)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build();
        canvas.save();
        canvas.translate((LARGURA - LARGURA_TEXTO) / 2f, y);
        layout.draw(canvas);
        canvas.restore();
        return y + layout.getHeight();
    }

    private static void desenhar(Context contexto, Canvas canvas, Certificado c, String aluno) {
        canvas.drawColor(Color.WHITE);

        Paint moldura = new Paint(Paint.ANTI_ALIAS_FLAG);
        moldura.setStyle(Paint.Style.STROKE);
        moldura.setColor(ROXO);
        moldura.setStrokeWidth(4);
        canvas.drawRect(new RectF(24, 24, LARGURA - 24, ALTURA - 24), moldura);
        moldura.setStrokeWidth(1);
        canvas.drawRect(new RectF(32, 32, LARGURA - 32, ALTURA - 32), moldura);

        Bitmap logo = BitmapFactory.decodeResource(contexto.getResources(), R.drawable.logo_proxima_etapa);
        if (logo != null) {
            float altura = 56;
            float largura = logo.getWidth() * altura / logo.getHeight();
            canvas.drawBitmap(logo, null, new RectF((LARGURA - largura) / 2f, 48, (LARGURA + largura) / 2f, 48 + altura), null);
        }

        Typeface jaro = ResourcesCompat.getFont(contexto, R.font.jaro);
        TextPaint titulo = pincel(40, true, jaro);
        titulo.setColor(ROXO);
        float y = bloco(canvas, "CERTIFICADO", titulo, 116) + 14;

        TextPaint normal = pincel(16, false, null);
        y = bloco(canvas, "Certificamos que", normal, y) + 6;
        y = bloco(canvas, aluno, pincel(28, true, null), y) + 6;
        y = bloco(canvas, "concluiu o curso", normal, y) + 6;
        y = bloco(canvas, c.curso, pincel(24, true, null), y) + 6;
        if (c.cargaHoraria > 0) {
            String unidade = c.cargaHoraria == 1 ? " hora." : " horas.";
            y = bloco(canvas, "com carga horária de " + c.cargaHoraria + unidade, normal, y) + 4;
        }
        if (!c.instituicao.isEmpty()) {
            bloco(canvas, "Curso oferecido por " + c.instituicao + ".", normal, y);
        }

        float lado = 84;
        float topoQr = ALTURA - 32 - 22 - lado - 6;
        desenharQr(canvas, c.codigo, (LARGURA - lado) / 2f, topoQr, lado);
        bloco(canvas, "Código: " + c.codigo, pincel(11, false, null), topoQr + lado + 4);

        if (c.emitido > 0) {
            TextPaint pequeno = pincel(12, false, null);
            canvas.drawText("Emitido em " + Util.dataBrMs(c.emitido), 56, ALTURA - 52, pequeno);
        }
    }

    private static void desenharQr(Canvas canvas, String texto, float x, float y, float lado) {
        if (texto == null || texto.isEmpty()) {
            return;
        }
        try {
            BitMatrix matriz = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, 0, 0);
            float celula = lado / matriz.getWidth();
            Paint tinta = new Paint();
            tinta.setColor(Color.BLACK);
            for (int linha = 0; linha < matriz.getHeight(); linha++) {
                for (int coluna = 0; coluna < matriz.getWidth(); coluna++) {
                    if (matriz.get(coluna, linha)) {
                        canvas.drawRect(x + coluna * celula, y + linha * celula,
                                x + (coluna + 1) * celula + 0.4f, y + (linha + 1) * celula + 0.4f, tinta);
                    }
                }
            }
        } catch (WriterException e) {
            return;
        }
    }
}
