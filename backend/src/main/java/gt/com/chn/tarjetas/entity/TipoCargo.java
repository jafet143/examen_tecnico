package gt.com.chn.tarjetas.entity;

import lombok.Getter;

@Getter
public enum TipoCargo {
    COMISION_REPOSICION("Comisión por reposición",
            "La comisión por reposición se genera automáticamente al reponer una tarjeta por extravío"),
    INTERES("Intereses", "Los intereses se generan automáticamente al vencer cada ciclo"),
    RECARGO_MORA("Recargo por mora", null);

    private final String descripcion;
    private final String mensajeSinRegistroManual;

    TipoCargo(String descripcion, String mensajeSinRegistroManual) {
        this.descripcion = descripcion;
        this.mensajeSinRegistroManual = mensajeSinRegistroManual;
    }

    public boolean permiteRegistroManual() {
        return mensajeSinRegistroManual == null;
    }
}
