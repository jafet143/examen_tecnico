package gt.com.chn.tarjetas.entity;

import java.math.BigDecimal;
import lombok.Getter;

@Getter
public enum TipoTarjeta {
    CLASICA("Clásica", new BigDecimal("3000.00"), "4100"),
    ORO("Oro", new BigDecimal("8000.00"), "4200"),
    PLATINO("Platino", new BigDecimal("15000.00"), "4300"),
    BLACK("Black", new BigDecimal("30000.00"), "4400");

    private final String descripcion;
    private final BigDecimal ingresoMinimo;
    private final String prefijoNumero;

    TipoTarjeta(String descripcion, BigDecimal ingresoMinimo, String prefijoNumero) {
        this.descripcion = descripcion;
        this.ingresoMinimo = ingresoMinimo;
        this.prefijoNumero = prefijoNumero;
    }
}
