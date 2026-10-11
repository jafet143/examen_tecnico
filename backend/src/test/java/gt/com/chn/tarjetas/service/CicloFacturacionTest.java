package gt.com.chn.tarjetas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gt.com.chn.tarjetas.entity.CicloFacturacion;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CicloFacturacionTest {

    @Test
    @DisplayName("Antes del fin del día 5 el último corte es el del mes anterior")
    void antesDelCorteDelMes() {
        assertEquals(LocalDate.of(2026, 9, 5), CicloFacturacion.ultimoCorte(LocalDateTime.of(2026, 10, 5, 23, 59, 58)));
        assertEquals(LocalDate.of(2026, 9, 5), CicloFacturacion.ultimoCorte(LocalDateTime.of(2026, 10, 1, 8, 0)));
    }

    @Test
    @DisplayName("A las 23:59:59 del día 5 el corte ya ocurrió")
    void fronteraDelCorte() {
        assertEquals(LocalDate.of(2026, 10, 5), CicloFacturacion.ultimoCorte(LocalDateTime.of(2026, 10, 5, 23, 59, 59)));
        assertEquals(LocalDate.of(2026, 10, 5), CicloFacturacion.ultimoCorte(LocalDateTime.of(2026, 10, 30, 8, 0)));
    }

    @Test
    @DisplayName("El último corte vencido es el actual solo después del día 25 a las 23:59:59")
    void ultimoCorteVencido() {
        assertEquals(LocalDate.of(2026, 9, 5), CicloFacturacion.ultimoCorteVencido(LocalDateTime.of(2026, 10, 25, 23, 59, 59)));
        assertEquals(LocalDate.of(2026, 10, 5), CicloFacturacion.ultimoCorteVencido(LocalDateTime.of(2026, 10, 26, 0, 0)));
        assertEquals(LocalDate.of(2026, 9, 5), CicloFacturacion.ultimoCorteVencido(LocalDateTime.of(2026, 10, 9, 12, 0)));
    }

    @Test
    @DisplayName("Cambio de año: en enero antes del día 5 el corte es el de diciembre")
    void cambioDeAnio() {
        assertEquals(LocalDate.of(2025, 12, 5), CicloFacturacion.ultimoCorte(LocalDateTime.of(2026, 1, 3, 10, 0)));
        assertEquals(LocalDate.of(2025, 12, 25), CicloFacturacion.fechaLimitePago(LocalDate.of(2025, 12, 5)));
        assertEquals(LocalDate.of(2026, 1, 5), CicloFacturacion.proximoCorte(LocalDate.of(2025, 12, 5)));
    }
}
