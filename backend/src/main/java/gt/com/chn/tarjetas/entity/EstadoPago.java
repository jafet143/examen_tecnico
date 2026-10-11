package gt.com.chn.tarjetas.entity;

import lombok.Getter;

@Getter
public enum EstadoPago {
    AL_DIA("Al día"),
    PAGO_PENDIENTE("Pago pendiente"),
    EN_MORA("En mora");

    private final String descripcion;

    EstadoPago(String descripcion) {
        this.descripcion = descripcion;
    }
}
