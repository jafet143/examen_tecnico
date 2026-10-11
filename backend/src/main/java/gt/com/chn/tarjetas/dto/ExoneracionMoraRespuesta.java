package gt.com.chn.tarjetas.dto;

import java.math.BigDecimal;

public record ExoneracionMoraRespuesta(
        int cantidadCargosExonerados,
        BigDecimal montoExonerado,
        CuentaRespuesta cuenta) {
}
