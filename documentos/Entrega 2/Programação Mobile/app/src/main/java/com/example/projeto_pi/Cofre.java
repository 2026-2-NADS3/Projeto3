package com.example.projeto_pi;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

// Você não ensinou isso em aula, mas solicitou no PI: guardar o token de login de forma segura.
// A chave fica dentro do Android Keystore e o token só é salvo criptografado.
public class Cofre {

    private static final String PROVEDOR = "AndroidKeyStore";
    private static final String ALIAS = "proxima_etapa_token";
    private static final String TRANSFORMACAO = "AES/GCM/NoPadding";

    private static SecretKey chave() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(PROVEDOR);
        keyStore.load(null);
        if (!keyStore.containsAlias(ALIAS)) {
            KeyGenerator gerador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEDOR);
            gerador.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build());
            return gerador.generateKey();
        }
        return (SecretKey) keyStore.getKey(ALIAS, null);
    }

    public static String cifrar(String texto) {
        try {
            // transforma o texto em um código embaralhado que só este app consegue desfazer
            Cipher cifra = Cipher.getInstance(TRANSFORMACAO);
            cifra.init(Cipher.ENCRYPT_MODE, chave());
            byte[] iv = cifra.getIV();
            byte[] dados = cifra.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(dados, Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    public static String decifrar(String guardado) {
        try {
            String[] partes = guardado.split(":");
            byte[] iv = Base64.decode(partes[0], Base64.NO_WRAP);
            byte[] dados = Base64.decode(partes[1], Base64.NO_WRAP);
            Cipher cifra = Cipher.getInstance(TRANSFORMACAO);
            cifra.init(Cipher.DECRYPT_MODE, chave(), new GCMParameterSpec(128, iv));
            return new String(cifra.doFinal(dados), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
