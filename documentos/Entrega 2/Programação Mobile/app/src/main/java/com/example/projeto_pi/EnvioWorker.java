package com.example.projeto_pi;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// Você não ensinou isso em aula, mas solicitou no PI: WorkManager.
// Ele roda em segundo plano e só começa quando o celular tem internet.
public class EnvioWorker extends Worker {

    public EnvioWorker(@NonNull Context contexto, @NonNull WorkerParameters parametros) {
        super(contexto, parametros);
    }

    public static void agendar(Context contexto) {
        Constraints regras = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest pedido = new OneTimeWorkRequest.Builder(EnvioWorker.class)
                .setConstraints(regras)
                .build();
        WorkManager.getInstance(contexto).enqueueUniqueWork("envio_pendentes", ExistingWorkPolicy.KEEP, pedido);
    }

    @NonNull
    @Override
    public Result doWork() {
        // o Worker precisa esperar a internet responder antes de terminar
        final CountDownLatch espera = new CountDownLatch(1);
        Sincronizador.enviar(getApplicationContext(), espera::countDown);
        try {
            espera.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return Result.retry();
        }
        // se ainda sobrou coisa na fila, tenta de novo mais tarde
        if (Sincronizador.temPendente(getApplicationContext())) {
            return Result.retry();
        }
        return Result.success();
    }
}
