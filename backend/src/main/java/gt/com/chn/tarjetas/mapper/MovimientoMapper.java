package gt.com.chn.tarjetas.mapper;

import gt.com.chn.tarjetas.dto.MovimientoRespuesta;
import gt.com.chn.tarjetas.entity.Cargo;
import gt.com.chn.tarjetas.entity.Consumo;
import gt.com.chn.tarjetas.entity.Pago;

public final class MovimientoMapper {
    public static final String TIPO_CONSUMO = "CONSUMO";
    public static final String TIPO_PAGO = "PAGO";
    public static final String TIPO_CARGO = "CARGO";

    private MovimientoMapper() {
    }

    public static MovimientoRespuesta aRespuesta(Consumo consumo) {
        return new MovimientoRespuesta(
                consumo.getId(),
                consumo.getCuenta().getId(),
                TIPO_CONSUMO,
                consumo.getMonto(),
                consumo.getFecha(),
                consumo.getDescripcion(),
                consumo.getFechaRegistro(),
                consumo.getUsuarioRegistro(),
                consumo.getTarjeta().getId(),
                TarjetaMapper.enmascararNumero(consumo.getTarjeta().getNumeroTarjeta()),
                null,
                null,
                consumo.estaReversado(),
                consumo.getMotivoReverso(),
                consumo.getFechaReverso(),
                consumo.getUsuarioReverso());
    }

    public static MovimientoRespuesta aRespuesta(Pago pago) {
        return new MovimientoRespuesta(
                pago.getId(),
                pago.getCuenta().getId(),
                TIPO_PAGO,
                pago.getMonto(),
                pago.getFecha(),
                pago.getDescripcion(),
                pago.getFechaRegistro(),
                pago.getUsuarioRegistro(),
                null,
                null,
                null,
                null,
                pago.estaReversado(),
                pago.getMotivoReverso(),
                pago.getFechaReverso(),
                pago.getUsuarioReverso());
    }

    public static MovimientoRespuesta aRespuesta(Cargo cargo) {
        return new MovimientoRespuesta(
                cargo.getId(),
                cargo.getCuenta().getId(),
                TIPO_CARGO,
                cargo.getMonto(),
                cargo.getFecha(),
                cargo.getDescripcion(),
                cargo.getFechaRegistro(),
                cargo.getUsuarioRegistro(),
                null,
                null,
                cargo.getTipoCargo(),
                cargo.getTipoCargo().getDescripcion(),
                cargo.estaReversado(),
                cargo.getMotivoReverso(),
                cargo.getFechaReverso(),
                cargo.getUsuarioReverso());
    }
}
