package com.example.projeto_pi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Json {

    public static JSONObject objeto(Object... pares) {
        JSONObject json = new JSONObject();
        try {
            for (int i = 0; i + 1 < pares.length; i += 2) {
                json.put((String) pares[i], pares[i + 1]);
            }
        } catch (JSONException e) {
            return new JSONObject();
        }
        return json;
    }

    public static JSONArray lista(String texto) {
        // texto null acontece quando ainda não há nada salvo no cache
        if (texto == null) {
            return new JSONArray();
        }
        try {
            return new JSONArray(texto);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public static JSONObject objetoDe(String texto) {
        if (texto == null) {
            return new JSONObject();
        }
        try {
            return new JSONObject(texto);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }
}
