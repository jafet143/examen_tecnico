package gt.com.chn.tarjetas.entity;

import lombok.AccessLevel;
import lombok.Getter;

@Getter
public enum MotivoBloqueo {
    ROBO("Robo", false),
    EXTRAVIO("Extravío", false),
    SOSPECHA_FRAUDE("Sospecha de fraude", false),
    SOLICITUD_CLIENTE("Solicitud del cliente", true);

    private final String descripcion;
    @Getter(AccessLevel.NONE)
    private final boolean permiteDesbloqueo;

    MotivoBloqueo(String descripcion, boolean permiteDesbloqueo) {
        this.descripcion = descripcion;
        this.permiteDesbloqueo = permiteDesbloqueo;
    }

    public boolean permiteDesbloqueo() {
        return permiteDesbloqueo;
    }
}
