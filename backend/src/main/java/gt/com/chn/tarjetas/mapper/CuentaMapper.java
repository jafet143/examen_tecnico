package gt.com.chn.tarjetas.mapper;

import gt.com.chn.tarjetas.dto.CuentaRespuesta;
import gt.com.chn.tarjetas.dto.TarjetaRespuesta;
import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.entity.EstadoPagoCuenta;
import gt.com.chn.tarjetas.entity.MotivoCancelacion;

public final class CuentaMapper {
    private CuentaMapper() {
    }

    public static CuentaRespuesta aRespuesta(CuentaCredito cuenta, EstadoPagoCuenta estadoPago,
                                             TarjetaRespuesta tarjetaVigente, int cantidadReposiciones,
                                             boolean tieneRecargosMoraPendientes,
                                             boolean incluirDatosSensibles) {
        MotivoCancelacion motivoCancelacion = cuenta.getMotivoCancelacion();
        return new CuentaRespuesta(
                cuenta.getId(),
                TarjetaMapper.enmascararNumero(cuenta.getNumeroCuenta()),
                TarjetaMapper.completarSiAutorizado(cuenta.getNumeroCuenta(), incluirDatosSensibles),
                cuenta.getCliente().getId(),
                cuenta.getCliente().getCodigoCliente(),
                cuenta.getCliente().getNombreCompleto(),
                cuenta.getSolicitud().getId(),
                cuenta.getTipoTarjeta(),
                cuenta.getTipoTarjeta().getDescripcion(),
                cuenta.getLimiteCredito(),
                cuenta.getSaldoActual(),
                cuenta.obtenerCreditoDisponible(),
                cuenta.getFechaApertura(),
                estadoPago.estadoPago(),
                estadoPago.estadoPago().getDescripcion(),
                estadoPago.fechaUltimoCorte(),
                estadoPago.fechaLimitePago(),
                estadoPago.fechaProximoCorte(),
                estadoPago.saldoAlCorte(),
                estadoPago.pagoMinimo(),
                estadoPago.pagoMinimoPendiente(),
                estadoPago.pagoContado(),
                estadoPago.pagoContadoPendiente(),
                estadoPago.montoEnMora(),
                estadoPago.diasMora(),
                tarjetaVigente,
                cantidadReposiciones,
                cuenta.getEstadoCuenta(),
                cuenta.getEstadoCuenta().getDescripcion(),
                motivoCancelacion,
                motivoCancelacion == null ? null : motivoCancelacion.getDescripcion(),
                cuenta.getComentarioCancelacion(),
                cuenta.getFechaCancelacion(),
                cuenta.getUsuarioCancelacion(),
                tieneRecargosMoraPendientes);
    }
}
