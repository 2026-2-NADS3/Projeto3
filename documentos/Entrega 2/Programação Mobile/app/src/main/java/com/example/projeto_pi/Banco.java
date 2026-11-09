package com.example.projeto_pi;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class Banco extends SQLiteOpenHelper {

    private static Banco instancia;

    public static class ItemFila {
        public long id;
        public String tipo;
        public String chave;
        public String dado;
    }

    public static synchronized Banco abrir(Context contexto) {
        if (instancia == null) {
            instancia = new Banco(contexto.getApplicationContext());
        }
        return instancia;
    }

    private Banco(Context contexto) {
        super(contexto, "proxima_etapa.db", null, 9);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE cache (chave TEXT PRIMARY KEY, json TEXT)");
        db.execSQL("CREATE TABLE fila (id INTEGER PRIMARY KEY AUTOINCREMENT, tipo TEXT, usuario TEXT, chave TEXT, dado TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int antiga, int nova) {
        db.execSQL("DROP TABLE IF EXISTS cache");
        db.execSQL("DROP TABLE IF EXISTS fila");
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int antiga, int nova) {
        onUpgrade(db, antiga, nova);
    }

    public void salvar(String chave, String json) {
        ContentValues valores = new ContentValues();
        valores.put("chave", chave);
        valores.put("json", json);
        getWritableDatabase().insertWithOnConflict("cache", null, valores, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String obter(String chave) {
        Cursor cursor = getReadableDatabase().rawQuery("SELECT json FROM cache WHERE chave = ?", new String[]{chave});
        String json = null;
        if (cursor.moveToFirst()) {
            json = cursor.getString(0);
        }
        cursor.close();
        return json;
    }

    public void limparCache() {
        getWritableDatabase().delete("cache", null, null);
    }

    // a fila guarda o que ainda não foi enviado para a API
    public void colocarNaFila(String tipo, String usuario, String chave, String dado) {
        ContentValues valores = new ContentValues();
        valores.put("tipo", tipo);
        valores.put("usuario", usuario);
        valores.put("chave", chave);
        valores.put("dado", dado);
        getWritableDatabase().insert("fila", null, valores);
    }

    public List<ItemFila> listarFila(String usuario) {
        List<ItemFila> lista = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, tipo, chave, dado FROM fila WHERE usuario = ? ORDER BY id", new String[]{usuario});
        while (cursor.moveToNext()) {
            ItemFila item = new ItemFila();
            item.id = cursor.getLong(0);
            item.tipo = cursor.getString(1);
            item.chave = cursor.getString(2);
            item.dado = cursor.getString(3);
            lista.add(item);
        }
        cursor.close();
        return lista;
    }

    public List<ItemFila> listarFila(String usuario, String tipo) {
        List<ItemFila> lista = new ArrayList<>();
        for (ItemFila item : listarFila(usuario)) {
            if (item.tipo.equals(tipo)) {
                lista.add(item);
            }
        }
        return lista;
    }

    public void removerDaFila(long id) {
        getWritableDatabase().delete("fila", "id = ?", new String[]{String.valueOf(id)});
    }
}
