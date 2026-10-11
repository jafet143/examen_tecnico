package gt.com.chn.tarjetas.entity;

import lombok.Getter;

@Getter
public enum MotivoCancelacion {
    SOLICITUD_CLIENTE("Solicitud del cliente"),
    DECISION_BANCO("Decisión del banco");

    private final String descripcion;

    MotivoCancelacion(String descripcion) {
        this.descripcion = descripcion;
    }
}
