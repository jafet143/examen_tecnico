package gt.com.chn.tarjetas.service;

import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.exception.ReglaNegocioException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class ReglasCuenta {
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ReglasCuenta() {
    }

    static void exigirActiva(CuentaCredito cuenta) {
        if (cuenta.estaCancelada()) {
            throw new ReglaNegocioException("La cuenta está cancelada");
        }
    }

    static ReglaNegocioException movimientoYaReversado(LocalDateTime fechaReverso) {
        return new ReglaNegocioException("El movimiento ya fue reversado el " + FORMATO_FECHA_HORA.format(fechaReverso));
    }

    static String formatear(LocalDate fecha) {
        return FORMATO_FECHA.format(fecha);
    }
}
