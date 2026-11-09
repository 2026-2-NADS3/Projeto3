package com.example.projeto_pi;

import android.text.Editable;
import android.text.TextWatcher;

public class TelefoneWatcher implements TextWatcher {

    private boolean editando = false;

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (editando) {
            return;
        }
        editando = true;
        String digitos = s.toString().replaceAll("\\D", "");
        if (digitos.length() > 11) {
            digitos = digitos.substring(0, 11);
        }
        String formatado = Util.telefone(digitos);
        s.replace(0, s.length(), formatado);
        editando = false;
    }
}
