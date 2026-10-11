package gt.com.chn.tarjetas.entity;

import lombok.AccessLevel;
import lombok.Getter;

@Getter
public enum MotivoReposicion {
    ROBO("Robo", MotivoBloqueo.ROBO, true),
    EXTRAVIO("Extravío", MotivoBloqueo.EXTRAVIO, true),
    DETERIORO("Deterioro", null, true),
    SOSPECHA_FRAUDE("Sospecha de fraude", MotivoBloqueo.SOSPECHA_FRAUDE, true),
    RENOVACION("Renovación por vencimiento", null, false);

    private final String descripcion;
    private final MotivoBloqueo motivoBloqueoPrevio;
    @Getter(AccessLevel.NONE)
    private final boolean solicitable;

    MotivoReposicion(String descripcion, MotivoBloqueo motivoBloqueoPrevio, boolean solicitable) {
        this.descripcion = descripcion;
        this.motivoBloqueoPrevio = motivoBloqueoPrevio;
        this.solicitable = solicitable;
    }

    public boolean esSolicitable() {
        return solicitable;
    }
}
