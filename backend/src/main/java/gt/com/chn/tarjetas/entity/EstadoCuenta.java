package gt.com.chn.tarjetas.entity;

import lombok.Getter;

@Getter
public enum EstadoCuenta {
    ACTIVA("Activa"),
    CANCELADA("Cancelada");

    private final String descripcion;

    EstadoCuenta(String descripcion) {
        this.descripcion = descripcion;
    }
}
