package gt.com.chn.tarjetas.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class CicloFacturacion {
    public static final int DIA_CORTE = 5;
    public static final int DIA_LIMITE_PAGO = 25;

    private static final LocalTime FIN_DE_DIA = LocalTime.of(23, 59, 59);

    private CicloFacturacion() {
    }

    public static LocalDateTime finDeDia(LocalDate fecha) {
        return fecha.atTime(FIN_DE_DIA);
    }

    public static LocalDate ultimoCorte(LocalDateTime ahora) {
        LocalDate corteDelMes = ahora.toLocalDate().withDayOfMonth(DIA_CORTE);
        return ahora.isBefore(finDeDia(corteDelMes)) ? corteDelMes.minusMonths(1) : corteDelMes;
    }

    public static LocalDate ultimoCorteVencido(LocalDateTime ahora) {
        LocalDate corte = ultimoCorte(ahora);
        return ahora.isAfter(finDeDia(fechaLimitePago(corte))) ? corte : corte.minusMonths(1);
    }

    public static LocalDate fechaLimitePago(LocalDate corte) {
        return corte.withDayOfMonth(DIA_LIMITE_PAGO);
    }

    public static LocalDate proximoCorte(LocalDate corte) {
        return corte.plusMonths(1);
    }
}
