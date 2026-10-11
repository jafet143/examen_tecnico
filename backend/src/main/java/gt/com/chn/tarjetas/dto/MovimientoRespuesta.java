package gt.com.chn.tarjetas.dto;

import gt.com.chn.tarjetas.entity.TipoCargo;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoRespuesta(
        Long id,
        Long idCuenta,
        String tipoMovimiento,
        BigDecimal monto,
        LocalDateTime fecha,
        String descripcion,
        LocalDateTime fechaRegistro,
        String usuarioRegistro,
        Long idTarjeta,
        String numeroTarjetaEnmascarado,
        TipoCargo tipoCargo,
        String descripcionTipoCargo,
        boolean reversado,
        String motivoReverso,
        LocalDateTime fechaReverso,
        String usuarioReverso) {
}
