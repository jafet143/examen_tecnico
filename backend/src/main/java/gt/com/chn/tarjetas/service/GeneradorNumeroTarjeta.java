package gt.com.chn.tarjetas.service;

import gt.com.chn.tarjetas.entity.TipoTarjeta;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class GeneradorNumeroTarjeta {

    private static final int DIGITOS_ALEATORIOS = 11;
    private static final int LONGITUD_NUMERO = 16;

    private final SecureRandom generadorAleatorio = new SecureRandom();

    public String generar(TipoTarjeta tipo) {
        StringBuilder numeroParcial = new StringBuilder(LONGITUD_NUMERO).append(tipo.getPrefijoNumero());
        for (int posicion = 0; posicion < DIGITOS_ALEATORIOS; posicion++) {
            numeroParcial.append(generadorAleatorio.nextInt(10));
        }
        return numeroParcial.append(calcularDigitoVerificador(numeroParcial.toString())).toString();
    }

    public static boolean esNumeroValidoLuhn(String numero) {
        if (numero == null || numero.isEmpty() || !numero.chars().allMatch(Character::isDigit)) {
            return false;
        }
        return sumarDigitosLuhn(numero, false) % 10 == 0;
    }

    private static int calcularDigitoVerificador(String numeroParcial) {
        int suma = sumarDigitosLuhn(numeroParcial, true);
        return (10 - suma % 10) % 10;
    }

    private static int sumarDigitosLuhn(String numero, boolean duplicarPrimero) {
        int suma = 0;
        boolean duplicar = duplicarPrimero;
        for (int posicion = numero.length() - 1; posicion >= 0; posicion--) {
            int digito = numero.charAt(posicion) - '0';
            if (duplicar) {
                digito *= 2;
                if (digito > 9) {
                    digito -= 9;
                }
            }
            suma += digito;
            duplicar = !duplicar;
        }
        return suma;
    }
}
