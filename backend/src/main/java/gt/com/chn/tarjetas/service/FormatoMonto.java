package gt.com.chn.tarjetas.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class FormatoMonto {
    private FormatoMonto() {
    }

    static String formatear(BigDecimal monto) {
        return monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
