package gt.com.chn.tarjetas.dto;

import gt.com.chn.tarjetas.entity.EstadoCuenta;
import gt.com.chn.tarjetas.entity.EstadoPago;
import gt.com.chn.tarjetas.entity.MotivoCancelacion;
import gt.com.chn.tarjetas.entity.TipoTarjeta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CuentaRespuesta(
        Long id,
        String numeroCuentaEnmascarado,
        String numeroCuentaCompleto,
        Long idCliente,
        String codigoCliente,
        String nombreCliente,
        Long idSolicitud,
        TipoTarjeta tipoTarjeta,
        String descripcionTipoTarjeta,
        BigDecimal limiteCredito,
        BigDecimal saldoActual,
        BigDecimal creditoDisponible,
        LocalDateTime fechaApertura,
        EstadoPago estadoPago,
        String descripcionEstadoPago,
        LocalDate fechaUltimoCorte,
        LocalDate fechaLimitePago,
        LocalDate fechaProximoCorte,
        BigDecimal saldoAlCorte,
        BigDecimal pagoMinimo,
        BigDecimal pagoMinimoPendiente,
        BigDecimal pagoContado,
        BigDecimal pagoContadoPendiente,
        BigDecimal montoEnMora,
        int diasMora,
        TarjetaRespuesta tarjetaVigente,
        int cantidadReposiciones,
        EstadoCuenta estadoCuenta,
        String descripcionEstadoCuenta,
        MotivoCancelacion motivoCancelacion,
        String descripcionMotivoCancelacion,
        String comentarioCancelacion,
        LocalDateTime fechaCancelacion,
        String usuarioCancelacion,
        boolean tieneRecargosMoraPendientes) {
}
