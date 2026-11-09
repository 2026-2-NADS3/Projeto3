package com.example.projeto_pi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class UtilTest {

    @Test
    public void telefone_formataConformeQuantidadeDeDigitos() {
        assertEquals("", Util.telefone(""));
        assertEquals("(11", Util.telefone("11"));
        assertEquals("(11) 9", Util.telefone("119"));
        assertEquals("(11) 9999-8", Util.telefone("1199998"));
        assertEquals("(11) 9999-8888", Util.telefone("1199998888"));
        assertEquals("(11) 99999-8888", Util.telefone("11999998888"));
    }

    @Test
    public void dataBr_converteIsoParaPadraoBrasileiro() {
        assertEquals("21/11/2026", Util.dataBr("2026-11-21"));
        assertEquals("", Util.dataBr(null));
    }

    @Test
    public void normalizar_removeAcentosEIgnoraCaixa() {
        assertEquals("preparatorio enem", Util.normalizar("  Preparatório ENEM "));
        assertEquals("", Util.normalizar(null));
    }

    @Test
    public void primeiroNome_retornaApenasOPrimeiroNome() {
        assertEquals("Rodrigo", Util.primeiroNome("Rodrigo Silva"));
        assertEquals("Maria", Util.primeiroNome("  Maria   Souza "));
        assertEquals("", Util.primeiroNome(null));
    }

    @Test
    public void codigoIngresso_aceitaQrOuCodigoDigitado() {
        assertEquals("ABCD1234EFGH5678", Util.codigoIngresso("PEI|ABCD1234EFGH5678"));
        assertEquals("ABCD1234EFGH5678", Util.codigoIngresso(" abcd-1234 efgh-5678 "));
        assertNull(Util.codigoIngresso("ABC"));
        assertNull(Util.codigoIngresso(null));
    }

    @Test
    public void conteudoQr_usaPrefixoDoApp() {
        assertEquals("PEI|ABCD1234EFGH5678", Util.conteudoQr("abcd1234-efgh5678"));
    }

    @Test
    public void dataParaMs_leFormatosDoServidor() {
        long a = Util.dataParaMs("2026-11-21T13:00:00+00:00");
        long b = Util.dataParaMs("2026-11-21T13:00:00.123Z");
        assertEquals(a, b);
        assertEquals(-1, Util.dataParaMs(""));
        assertEquals(-1, Util.dataParaMs("lixo"));
    }
}
