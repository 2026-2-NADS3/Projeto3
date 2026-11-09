package com.example.projeto_pi;

import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Util {

    private static final String[] MESES = {"JAN", "FEV", "MAR", "ABR", "MAI", "JUN", "JUL", "AGO", "SET", "OUT", "NOV", "DEZ"};

    public static long dataParaMs(String iso) {
        if (iso == null || iso.trim().isEmpty()) {
            return -1;
        }
        String s = iso.trim().replace(' ', 'T').replaceFirst("\\.\\d+", "");
        if (s.endsWith("Z")) {
            s = s.substring(0, s.length() - 1) + "+00:00";
        } else if (s.matches(".*[+-]\\d{2}$")) {
            s = s + ":00";
        }
        try {
            Date data = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ROOT).parse(s);
            if (data == null) {
                return -1;
            }
            return data.getTime();
        } catch (ParseException e) {
            return -1;
        }
    }

    public static String dataBr(String iso) {
        if (iso == null) {
            return "";
        }
        if (iso.length() < 10) {
            return iso;
        }
        return iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4);
    }

    public static String dataBrMs(long ms) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(new Date(ms));
    }

    public static String diaIso(long ms) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date(ms));
    }

    public static String hora(long ms) {
        return new SimpleDateFormat("HH:mm", Locale.ROOT).format(new Date(ms));
    }

    public static String mes(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        return MESES[c.get(Calendar.MONTH)];
    }

    public static String dia(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        return String.format(Locale.ROOT, "%02d", c.get(Calendar.DAY_OF_MONTH));
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).trim();
    }

    public static String primeiroNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            return "";
        }
        return nome.trim().split("\\s+")[0];
    }

    public static String telefone(String digitos) {
        int n = digitos.length();
        if (n == 0) {
            return "";
        }
        if (n <= 2) {
            return "(" + digitos;
        }
        if (n <= 6) {
            return "(" + digitos.substring(0, 2) + ") " + digitos.substring(2);
        }
        if (n <= 10) {
            return "(" + digitos.substring(0, 2) + ") " + digitos.substring(2, 6) + "-" + digitos.substring(6);
        }
        return "(" + digitos.substring(0, 2) + ") " + digitos.substring(2, 7) + "-" + digitos.substring(7);
    }

    public static String limparCodigo(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    public static String codigoIngresso(String lido) {
        if (lido == null) {
            return null;
        }
        String texto = lido.trim();
        if (texto.toUpperCase(Locale.ROOT).startsWith("PEI|")) {
            texto = texto.substring(4);
        }
        String codigo = limparCodigo(texto);
        if (codigo.length() != 16) {
            return null;
        }
        return codigo;
    }

    public static String conteudoQr(String codigo) {
        return "PEI|" + limparCodigo(codigo);
    }

    public static String dataComunicado(long ms) {
        if (diaIso(ms).equals(diaIso(System.currentTimeMillis()))) {
            return "HOJE";
        }
        return dataBrMs(ms);
    }
}
