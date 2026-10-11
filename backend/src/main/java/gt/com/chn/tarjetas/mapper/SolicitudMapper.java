package gt.com.chn.tarjetas.mapper;

import gt.com.chn.tarjetas.dto.SolicitudRespuesta;
import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.entity.SolicitudTarjeta;
import gt.com.chn.tarjetas.entity.TarjetaCredito;

public final class SolicitudMapper {
    private SolicitudMapper() {
    }

    public static SolicitudRespuesta aRespuesta(SolicitudTarjeta solicitud) {
        return aRespuesta(solicitud, null, null, false);
    }

    public static SolicitudRespuesta aRespuesta(SolicitudTarjeta solicitud, CuentaCredito cuenta,
                                                TarjetaCredito primeraTarjeta, boolean incluirDatosSensibles) {
        return new SolicitudRespuesta(
                solicitud.getId(),
                solicitud.getCliente().getId(),
                solicitud.getCliente().getCodigoCliente(),
                solicitud.getCliente().getNombreCompleto(),
                solicitud.getCliente().getNumeroIdentificacion(),
                solicitud.getTipoTarjeta(),
                solicitud.getTipoTarjeta().getDescripcion(),
                solicitud.getFechaSolicitud(),
                solicitud.getIngresoMensual(),
                solicitud.getNombreEmpleador(),
                solicitud.getFechaInicioLaboral(),
                solicitud.obtenerMesesAntiguedadLaboral(),
                solicitud.getEstado(),
                solicitud.getFechaDecision(),
                solicitud.getComentarioDecision(),
                solicitud.getMontoAprobado(),
                solicitud.getUsuarioDecision(),
                cuenta == null ? null : cuenta.getId(),
                cuenta == null ? null : TarjetaMapper.enmascararNumero(cuenta.getNumeroCuenta()),
                cuenta == null ? null
                        : TarjetaMapper.completarSiAutorizado(cuenta.getNumeroCuenta(), incluirDatosSensibles),
                primeraTarjeta == null ? null : primeraTarjeta.getId(),
                primeraTarjeta == null ? null : TarjetaMapper.enmascararNumero(primeraTarjeta.getNumeroTarjeta()),
                primeraTarjeta == null ? null : TarjetaMapper.completarSiAutorizado(
                        primeraTarjeta.getNumeroTarjeta(), incluirDatosSensibles));
    }
}
