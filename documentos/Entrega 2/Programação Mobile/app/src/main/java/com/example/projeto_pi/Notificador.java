package com.example.projeto_pi;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

// Você não ensinou isso em aula, mas solicitou no PI: notificações.
// Guardo a lista de itens que o aluno já viu para só avisar o que for novo.
public class Notificador {

    private static final String CANAL = "avisos";

    public static void marcarVistos(Context contexto, String tipo, String json) {
        JSONArray lista = Json.lista(json);
        JSONArray ids = new JSONArray();
        for (int i = 0; i < lista.length(); i++) {
            JSONObject item = lista.optJSONObject(i);
            if (item != null) {
                ids.put(item.optString("id"));
            }
        }
        Banco.abrir(contexto).salvar("vistos_" + tipo + ":" + Sessao.usuario(contexto), ids.toString());
    }

    public static void verificar(Context contexto, String tipo, String json) {
        String chave = "vistos_" + tipo + ":" + Sessao.usuario(contexto);
        // se nunca guardei nada, é a primeira vez: só salvo a lista, sem avisar
        String salvo = Banco.abrir(contexto).obter(chave);
        JSONArray lista = Json.lista(json);
        if (salvo != null) {
            long agora = System.currentTimeMillis();
            for (int i = 0; i < lista.length(); i++) {
                JSONObject item = lista.optJSONObject(i);
                if (item == null || salvo.contains("\"" + item.optString("id") + "\"")) {
                    continue;
                }
                if (tipo.equals("com")) {
                    mostrar(contexto, item.optString("id"), item.optString("title"), item.optString("message"), ComunicadosActivity.class);
                } else if (tipo.equals("cer")) {
                    JSONObject curso = item.optJSONObject("courses");
                    String nomeCurso = curso == null ? "" : curso.optString("title");
                    mostrar(contexto, item.optString("id"), contexto.getString(R.string.notif_certificado),
                            nomeCurso, CertificadosActivity.class);
                } else if (Util.dataParaMs(item.optString("course_date")) >= agora) {
                    mostrar(contexto, item.optString("id"), contexto.getString(R.string.notif_novo_curso),
                            item.optString("title"), CursosActivity.class);
                }
            }
        }
        marcarVistos(contexto, tipo, json);
    }

    // avisa quando o dia, o horário ou o local de um curso em que o aluno está inscrito mudou
    public static void verificarAgenda(Context contexto, String json) {
        String chave = "vistos_age:" + Sessao.usuario(contexto);
        String salvo = Banco.abrir(contexto).obter(chave);
        JSONArray lista = Json.lista(json);
        JSONArray assinaturas = new JSONArray();
        for (int i = 0; i < lista.length(); i++) {
            JSONObject item = lista.optJSONObject(i);
            if (item == null) {
                continue;
            }
            // a assinatura junta id, data, fim e local: se qualquer um mudar, a assinatura muda
            String id = item.optString("id");
            String assinatura = id + "|" + item.optString("course_date") + "|"
                    + item.optString("course_time_end") + "|" + item.optString("location");
            assinaturas.put(assinatura);
            if (salvo != null && salvo.contains("\"" + id + "|") && !salvo.contains("\"" + assinatura + "\"")) {
                mostrar(contexto, assinatura, contexto.getString(R.string.notif_agenda_alterada),
                        item.optString("title"), AgendaActivity.class);
            }
        }
        Banco.abrir(contexto).salvar(chave, assinaturas.toString());
    }

    private static void mostrar(Context contexto, String id, String titulo, String texto, Class<?> destino) {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // no Android 8 ou mais novo a notificação precisa de um canal
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel canal = new NotificationChannel(CANAL,
                    contexto.getString(R.string.notif_canal), NotificationManager.IMPORTANCE_DEFAULT);
            contexto.getSystemService(NotificationManager.class).createNotificationChannel(canal);
        }
        Intent intent = new Intent(contexto, destino);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        int codigo = id.hashCode();
        PendingIntent toque = PendingIntent.getActivity(contexto, codigo, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder notificacao = new NotificationCompat.Builder(contexto, CANAL)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setContentIntent(toque)
                .setAutoCancel(true);
        NotificationManagerCompat.from(contexto).notify(codigo, notificacao.build());
    }
}
