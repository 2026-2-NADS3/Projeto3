package com.example.projeto_pi;

import android.content.Context;

import java.util.List;

// Função principal da entrega: sincronizar o que ficou guardado no celular com a API.
// Tudo que o aluno faz sem internet (mensagem do chat e presença) vai para uma fila no SQLite
// e esta classe manda um item de cada vez quando a internet volta.
public class Sincronizador {

    private static volatile boolean enviando = false;

    public static void enviar(Context contexto) {
        enviar(contexto, null);
    }

    public static void enviar(Context contexto, Runnable fim) {
        if (enviando || !Sessao.ativa(contexto)) {
            terminar(fim);
            return;
        }
        Context app = contexto.getApplicationContext();
        List<Banco.ItemFila> itens = Banco.abrir(app).listarFila(Sessao.usuario(app));
        if (itens.isEmpty()) {
            terminar(fim);
            return;
        }
        // enviando evita que duas sincronizações rodem ao mesmo tempo
        enviando = true;
        enviarItem(app, itens, 0, fim);
    }

    public static boolean temPendente(Context contexto) {
        if (!Sessao.ativa(contexto)) {
            return false;
        }
        return !Banco.abrir(contexto).listarFila(Sessao.usuario(contexto)).isEmpty();
    }

    private static void terminar(Runnable fim) {
        if (fim != null) {
            fim.run();
        }
    }

    private static void enviarItem(final Context app, final List<Banco.ItemFila> itens, final int posicao, final Runnable fim) {
        if (posicao >= itens.size()) {
            enviando = false;
            terminar(fim);
            return;
        }
        final Banco.ItemFila item = itens.get(posicao);
        Api.Ouvinte ouvinte = new Api.Ouvinte() {
            @Override
            public void aoResponder(String corpo) {
                Banco.abrir(app).removerDaFila(item.id);
                enviarItem(app, itens, posicao + 1, fim);
            }

            @Override
            public void aoFalhar(int codigo, String mensagem) {
                // sem internet ou erro do servidor: deixo na fila para tentar de novo
                // erro do app (400, 409...): não adianta repetir, então tiro da fila
                boolean tentarDepois = codigo == 0 || codigo == 401 || codigo >= 500;
                if (tentarDepois) {
                    enviando = false;
                    terminar(fim);
                    return;
                }
                Banco.abrir(app).removerDaFila(item.id);
                enviarItem(app, itens, posicao + 1, fim);
            }
        };
        if (item.tipo.equals("chat")) {
            Api.post(app, "rest/v1/support_messages",
                    Json.objeto("user_id", Sessao.usuario(app), "body", item.dado, "client_id", item.chave), ouvinte);
        } else {
            Api.post(app, "rest/v1/rpc/registrar_presenca_ingresso",
                    Json.objeto("p_course_id", item.chave, "p_code", item.dado), ouvinte);
        }
    }
}
