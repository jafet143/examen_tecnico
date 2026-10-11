package gt.com.chn.tarjetas.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EstadoPagoCuenta(
        EstadoPago estadoPago,
        LocalDate fechaUltimoCorte,
        LocalDate fechaLimitePago,
        LocalDate fechaProximoCorte,
        BigDecimal saldoAlCorte,
        BigDecimal pagoMinimo,
        BigDecimal pagoMinimoPendiente,
        BigDecimal pagoContado,
        BigDecimal pagoContadoPendiente,
        BigDecimal montoEnMora,
        int diasMora) {
}
