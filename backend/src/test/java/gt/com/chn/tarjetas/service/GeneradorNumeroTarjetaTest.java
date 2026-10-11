package gt.com.chn.tarjetas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gt.com.chn.tarjetas.entity.TipoTarjeta;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class GeneradorNumeroTarjetaTest {

    private final GeneradorNumeroTarjeta generador = new GeneradorNumeroTarjeta();

    @ParameterizedTest
    @EnumSource(TipoTarjeta.class)
    @DisplayName("Genera 16 dígitos, con el prefijo del tipo y dígito verificador Luhn válido")
    void generaNumeroValido(TipoTarjeta tipo) {
        for (int repeticion = 0; repeticion < 200; repeticion++) {
            String numero = generador.generar(tipo);

            assertEquals(16, numero.length());
            assertTrue(numero.startsWith(tipo.getPrefijoNumero()));
            assertTrue(GeneradorNumeroTarjeta.esNumeroValidoLuhn(numero), "Luhn inválido: " + numero);
        }
    }

    @Test
    @DisplayName("Las tarjetas BLACK usan el prefijo 4400")
    void tarjetaBlackUsaPrefijo4400() {
        assertEquals("4400", TipoTarjeta.BLACK.getPrefijoNumero());
        assertTrue(generador.generar(TipoTarjeta.BLACK).startsWith("4400"));
    }

    @Test
    @DisplayName("Genera números distintos entre sí")
    void generaNumerosDistintos() {
        Set<String> numeros = new HashSet<>();
        for (int repeticion = 0; repeticion < 500; repeticion++) {
            numeros.add(generador.generar(TipoTarjeta.ORO));
        }
        assertEquals(500, numeros.size());
    }

    @Test
    @DisplayName("La validación Luhn reconoce números de la semilla y rechaza inválidos")
    void validaLuhn() {
        assertTrue(GeneradorNumeroTarjeta.esNumeroValidoLuhn("4200123456789015"));
        assertTrue(GeneradorNumeroTarjeta.esNumeroValidoLuhn("4300987654321014"));
        assertFalse(GeneradorNumeroTarjeta.esNumeroValidoLuhn("4200123456789016"));
        assertFalse(GeneradorNumeroTarjeta.esNumeroValidoLuhn("42001234abcd9015"));
        assertFalse(GeneradorNumeroTarjeta.esNumeroValidoLuhn(""));
    }
}
