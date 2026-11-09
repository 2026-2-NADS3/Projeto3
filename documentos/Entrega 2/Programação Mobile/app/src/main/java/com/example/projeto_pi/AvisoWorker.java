package com.example.projeto_pi;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// Você não ensinou isso em aula, mas solicitou no PI: WorkManager e notificações.
// A cada 15 minutos ele pergunta para a API se tem comunicado ou curso novo.
public class AvisoWorker extends Worker {

    public AvisoWorker(@NonNull Context contexto, @NonNull WorkerParameters parametros) {
        super(contexto, parametros);
    }

    public static void agendar(Context contexto) {
        Constraints regras = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest pedido = new PeriodicWorkRequest.Builder(AvisoWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(regras)
                .build();
        WorkManager.getInstance(contexto).enqueueUniquePeriodicWork("avisos", ExistingPeriodicWorkPolicy.KEEP, pedido);
    }

    public static void cancelar(Context contexto) {
        WorkManager.getInstance(contexto).cancelUniqueWork("avisos");
    }

    @NonNull
    @Override
    public Result doWork() {
        final Context contexto = getApplicationContext();
        if (!Sessao.ativa(contexto)) {
            return Result.success();
        }
        // espera as duas consultas (comunicados e cursos) terminarem
        final CountDownLatch espera = new CountDownLatch(1);
        Api.get(contexto, "rest/v1/announcements?select=id,title,message,course_id,created_at&order=created_at.desc&limit=100",
                new Api.Ouvinte() {
                    @Override
                    public void aoResponder(String comunicados) {
                        Notificador.verificar(contexto, "com", comunicados);
                        buscarCursos(contexto, espera);
                    }

                    @Override
                    public void aoFalhar(int codigo, String mensagem) {
                        espera.countDown();
                    }
                });
        try {
            espera.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return Result.retry();
        }
        return Result.success();
    }

    private void buscarCursos(final Context contexto, final CountDownLatch espera) {
        Api.get(contexto, "rest/v1/courses?select=id,title,course_date,course_time_end,location&is_active=eq.true&order=course_date.asc",
                new Api.Ouvinte() {
                    @Override
                    public void aoResponder(String cursos) {
                        Notificador.verificar(contexto, "cur", cursos);
                        buscarInscricoes(contexto, cursos, espera);
                    }

                    @Override
                    public void aoFalhar(int codigo, String mensagem) {
                        espera.countDown();
                    }
                });
    }

    // só avisa mudança de agenda dos cursos em que o aluno está inscrito
    private void buscarInscricoes(final Context contexto, final String cursos, final CountDownLatch espera) {
        Api.get(contexto, "rest/v1/enrollments?select=course_id", new Api.Ouvinte() {
            @Override
            public void aoResponder(String inscricoes) {
                JSONArray meus = new JSONArray();
                JSONArray todos = Json.lista(cursos);
                for (int i = 0; i < todos.length(); i++) {
                    JSONObject curso = todos.optJSONObject(i);
                    if (curso != null && inscricoes.contains(curso.optString("id"))) {
                        meus.put(curso);
                    }
                }
                Notificador.verificarAgenda(contexto, meus.toString());
                buscarCertificados(contexto, espera);
            }

            @Override
            public void aoFalhar(int codigo, String mensagem) {
                buscarCertificados(contexto, espera);
            }
        });
    }

    private void buscarCertificados(final Context contexto, final CountDownLatch espera) {
        Api.get(contexto, "rest/v1/certificates?select=id,courses(title)", new Api.Ouvinte() {
            @Override
            public void aoResponder(String certificados) {
                Notificador.verificar(contexto, "cer", certificados);
                espera.countDown();
            }

            @Override
            public void aoFalhar(int codigo, String mensagem) {
                espera.countDown();
            }
        });
    }
}
