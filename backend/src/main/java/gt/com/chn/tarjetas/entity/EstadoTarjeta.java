package gt.com.chn.tarjetas.entity;

import lombok.Getter;

@Getter
public enum EstadoTarjeta {
    ACTIVA("Activa"),
    BLOQUEADA("Bloqueada"),
    REEMPLAZADA("Reemplazada"),
    CANCELADA("Cancelada");

    private final String descripcion;

    EstadoTarjeta(String descripcion) {
        this.descripcion = descripcion;
    }
}
