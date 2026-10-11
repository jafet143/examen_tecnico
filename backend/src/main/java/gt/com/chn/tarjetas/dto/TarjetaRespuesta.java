package gt.com.chn.tarjetas.dto;

import gt.com.chn.tarjetas.entity.EstadoTarjeta;
import gt.com.chn.tarjetas.entity.MotivoBloqueo;
import gt.com.chn.tarjetas.entity.MotivoReposicion;
import gt.com.chn.tarjetas.entity.TipoTarjeta;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TarjetaRespuesta(
        Long id,
        Long idCuenta,
        String numeroCuentaEnmascarado,
        String numeroCuentaCompleto,
        String numeroTarjetaEnmascarado,
        String numeroTarjetaCompleto,
        TipoTarjeta tipoTarjeta,
        String descripcionTipoTarjeta,
        String nombreCliente,
        String codigoCliente,
        LocalDateTime fechaEmision,
        LocalDate fechaVencimiento,
        EstadoTarjeta estadoTarjeta,
        String descripcionEstadoTarjeta,
        MotivoBloqueo motivoBloqueo,
        String descripcionMotivoBloqueo,
        String comentarioBloqueo,
        LocalDateTime fechaBloqueo,
        String usuarioBloqueo,
        Long idTarjetaAnterior,
        MotivoReposicion motivoReposicion,
        String descripcionMotivoReposicion,
        String comentarioReposicion,
        LocalDateTime fechaReposicion,
        String usuarioReposicion,
        boolean puedeDesbloquearse,
        Boolean denunciaPresentada,
        Boolean tarjetaDeterioradaPresentada,
        Long idCargoReposicion,
        boolean vencida,
        boolean puedeRenovarse) {
}
