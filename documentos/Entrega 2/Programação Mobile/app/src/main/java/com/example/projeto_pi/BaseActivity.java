package com.example.projeto_pi;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity {

    private boolean saindo = false;

    protected void abrirTela(int layout, int itemMenu) {
        setContentView(R.layout.activity_base);
        FrameLayout conteudo = findViewById(R.id.conteudo);
        getLayoutInflater().inflate(layout, conteudo, true);

        ViewCompat.setOnApplyWindowInsetsListener(conteudo, (view, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(0, barras.top, 0, 0);
            return insets;
        });

        BottomNavigationView menu = findViewById(R.id.menu);
        menu.setSelectedItemId(itemMenu);
        menu.setOnItemSelectedListener(item -> {
            irParaAba(item.getItemId());
            return true;
        });
        menu.setOnItemReselectedListener(item -> irParaAba(item.getItemId()));

        View voltar = findViewById(R.id.btnVoltar);
        if (voltar != null) {
            voltar.setOnClickListener(v -> finish());
        }
        View avatar = findViewById(R.id.cardAvatar);
        if (avatar != null) {
            avatar.setOnClickListener(v -> irParaAba(R.id.nav_perfil));
        }
    }

    protected void definirTitulo(int texto) {
        TextView titulo = findViewById(R.id.tvTituloTela);
        if (titulo != null) {
            titulo.setText(texto);
        }
    }

    protected View criarItemAgenda(Curso curso, ViewGroup pai) {
        View item = getLayoutInflater().inflate(R.layout.item_agenda, pai, false);
        ligarItemAgenda(item, curso);
        return item;
    }

    protected void ligarItemAgenda(View item, Curso curso) {
        ((TextView) item.findViewById(R.id.tvMes)).setText(Util.mes(curso.inicio));
        ((TextView) item.findViewById(R.id.tvDia)).setText(Util.dia(curso.inicio));
        ((TextView) item.findViewById(R.id.tvTitulo)).setText(curso.titulo);
        ((TextView) item.findViewById(R.id.tvHorario)).setText(curso.horario);
        ((TextView) item.findViewById(R.id.tvLocal)).setText(curso.local);
    }

    protected void irParaAba(int itemId) {
        Class<?> destino = HomeActivity.class;
        if (itemId == R.id.nav_agenda) {
            destino = AgendaActivity.class;
        } else if (itemId == R.id.nav_cursos) {
            destino = CursosActivity.class;
        } else if (itemId == R.id.nav_presenca) {
            destino = PresencaActivity.class;
        } else if (itemId == R.id.nav_perfil) {
            destino = PerfilActivity.class;
        }
        if (destino == getClass()) {
            return;
        }
        Intent intent = new Intent(this, destino);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        if (!(this instanceof HomeActivity)) {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Sincronizador.temPendente(this)) {
            EnvioWorker.agendar(this);
            Sincronizador.enviar(this);
        }
    }

    protected void tratarErro(int codigo) {
        if (codigo == 401) {
            sair(true);
            return;
        }
        TextView aviso = findViewById(R.id.tvAviso);
        if (aviso == null) {
            Toast.makeText(this, R.string.aviso_erro_generico, Toast.LENGTH_SHORT).show();
            return;
        }
        if (codigo == 0) {
            aviso.setText(R.string.aviso_sem_conexao);
        } else {
            aviso.setText(R.string.aviso_erro_generico);
        }
        aviso.setVisibility(View.VISIBLE);
    }

    protected void esconderAviso() {
        TextView aviso = findViewById(R.id.tvAviso);
        if (aviso != null) {
            aviso.setVisibility(View.GONE);
        }
    }

    protected void sair(boolean expirou) {
        if (saindo) {
            return;
        }
        saindo = true;
        if (!expirou) {
            Api.post(this, "auth/v1/logout?scope=local", null, new Api.Ouvinte() {
                @Override
                public void aoResponder(String corpo) {
                }

                @Override
                public void aoFalhar(int codigo, String mensagem) {
                }
            });
        }
        AvisoWorker.cancelar(this);
        Sessao.encerrar(this);
        Banco.abrir(this).limparCache();
        if (expirou) {
            Toast.makeText(this, R.string.sessao_expirada, Toast.LENGTH_LONG).show();
        }
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    protected abstract class Ouvinte implements Api.Ouvinte {

        protected abstract void aoReceber(String corpo);

        @Override
        public final void aoResponder(String corpo) {
            if (!isFinishing()) {
                aoReceber(corpo);
            }
        }

        @Override
        public void aoFalhar(int codigo, String mensagem) {
            if (!isFinishing()) {
                tratarErro(codigo);
            }
        }
    }
}
