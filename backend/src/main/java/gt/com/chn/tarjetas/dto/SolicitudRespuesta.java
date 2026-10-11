package gt.com.chn.tarjetas.dto;

import gt.com.chn.tarjetas.entity.EstadoSolicitud;
import gt.com.chn.tarjetas.entity.TipoTarjeta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SolicitudRespuesta(
        Long id,
        Long idCliente,
        String codigoCliente,
        String nombreCliente,
        String numeroIdentificacionCliente,
        TipoTarjeta tipoTarjeta,
        String descripcionTipoTarjeta,
        LocalDateTime fechaSolicitud,
        BigDecimal ingresoMensual,
        String nombreEmpleador,
        LocalDate fechaInicioLaboral,
        int mesesAntiguedadLaboral,
        EstadoSolicitud estado,
        LocalDateTime fechaDecision,
        String comentarioDecision,
        BigDecimal montoAprobado,
        String usuarioDecision,
        Long idCuenta,
        String numeroCuentaEnmascarado,
        String numeroCuentaCompleto,
        Long idTarjeta,
        String numeroTarjetaEnmascarado,
        String numeroTarjetaCompleto) {
}
