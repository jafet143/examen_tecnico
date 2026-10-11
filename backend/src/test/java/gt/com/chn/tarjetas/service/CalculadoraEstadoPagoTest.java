package gt.com.chn.tarjetas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gt.com.chn.tarjetas.entity.Cargo;
import gt.com.chn.tarjetas.entity.Consumo;
import gt.com.chn.tarjetas.entity.EstadoPago;
import gt.com.chn.tarjetas.entity.EstadoPagoCuenta;
import gt.com.chn.tarjetas.entity.Pago;
import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.entity.TipoCargo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CalculadoraEstadoPagoTest {
    private final CalculadoraEstadoPago calculadora = new CalculadoraEstadoPago();

    private CuentaCredito tarjeta(LocalDateTime apertura) {
        CuentaCredito cuenta = new CuentaCredito();
        cuenta.setId(1L);
        cuenta.setFechaApertura(apertura);
        return cuenta;
    }

    private Consumo consumo(String monto, LocalDateTime fecha) {
        Consumo consumo = new Consumo();
        consumo.setMonto(new BigDecimal(monto));
        consumo.setFecha(fecha);
        return consumo;
    }

    private Cargo cargo(String monto, LocalDateTime fecha) {
        Cargo cargo = new Cargo();
        cargo.setTipoCargo(TipoCargo.INTERES);
        cargo.setMonto(new BigDecimal(monto));
        cargo.setFecha(fecha);
        return cargo;
    }

    private Pago pago(String monto, LocalDateTime fecha) {
        Pago pago = new Pago();
        pago.setMonto(new BigDecimal(monto));
        pago.setFecha(fecha);
        return pago;
    }

    private void assertMonto(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " pero fue " + real);
    }

    private static final LocalDateTime HOY = LocalDateTime.of(2026, 10, 9, 12, 0);
    private static final LocalDateTime APERTURA = LocalDateTime.of(2026, 8, 1, 9, 0);

    private EstadoPagoCuenta calcular(List<Consumo> consumos, List<Cargo> cargos, List<Pago> pagos,
                                      LocalDateTime ahora) {
        return calculadora.calcular(tarjeta(APERTURA), consumos, cargos, pagos, ahora);
    }

    @Test
    @DisplayName("Cuenta sin movimientos está al día y expone las fechas del ciclo")
    void sinMovimientos() {
        EstadoPagoCuenta estado = calcular(List.of(), List.of(), List.of(), HOY);

        assertEquals(EstadoPago.AL_DIA, estado.estadoPago());
        assertEquals(LocalDate.of(2026, 10, 5), estado.fechaUltimoCorte());
        assertEquals(LocalDate.of(2026, 10, 25), estado.fechaLimitePago());
        assertEquals(LocalDate.of(2026, 11, 5), estado.fechaProximoCorte());
        assertMonto("0", estado.saldoAlCorte());
        assertMonto("0", estado.pagoMinimo());
        assertMonto("0", estado.pagoContadoPendiente());
        assertEquals(0, estado.diasMora());
    }

    @Test
    @DisplayName("El pago mínimo es el 10 % del saldo al corte redondeado hacia arriba al centavo")
    void pagoMinimoConRedondeo() {
        assertMonto("100.01", calculadora.pagoMinimo(new BigDecimal("1000.05")));
        assertMonto("1672.58", calculadora.pagoMinimo(new BigDecimal("16725.75")));
        assertMonto("100.00", calculadora.pagoMinimo(new BigDecimal("1000.00")));
        assertMonto("0.01", calculadora.pagoMinimo(new BigDecimal("0.01")));
        assertMonto("0", calculadora.pagoMinimo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Saldo al corte sin pagar dentro del plazo: PAGO_PENDIENTE con mínimo y contado")
    void pagoPendienteDentroDelCiclo() {
        EstadoPagoCuenta estado = calcular(List.of(consumo("1000.00", LocalDateTime.of(2026, 9, 20, 10, 0))),
                List.of(), List.of(), HOY);

        assertEquals(EstadoPago.PAGO_PENDIENTE, estado.estadoPago());
        assertMonto("1000", estado.saldoAlCorte());
        assertMonto("100", estado.pagoMinimo());
        assertMonto("100", estado.pagoMinimoPendiente());
        assertMonto("1000", estado.pagoContado());
        assertMonto("1000", estado.pagoContadoPendiente());
        assertMonto("0", estado.montoEnMora());
    }

    @Test
    @DisplayName("Pagar el mínimo deja la cuenta AL_DIA aunque quede contado pendiente")
    void pagarElMinimoDejaAlDia() {
        EstadoPagoCuenta estado = calcular(List.of(consumo("1000.00", LocalDateTime.of(2026, 9, 20, 10, 0))),
                List.of(), List.of(pago("400.00", LocalDateTime.of(2026, 10, 7, 10, 0))), HOY);

        assertEquals(EstadoPago.AL_DIA, estado.estadoPago());
        assertMonto("0", estado.pagoMinimoPendiente());
        assertMonto("600", estado.pagoContadoPendiente());
    }

    @Test
    @DisplayName("Un pago menor al mínimo reduce el mínimo pendiente y sigue PAGO_PENDIENTE")
    void pagoMenorAlMinimo() {
        EstadoPagoCuenta estado = calcular(List.of(consumo("1000.00", LocalDateTime.of(2026, 9, 20, 10, 0))),
                List.of(), List.of(pago("40.00", LocalDateTime.of(2026, 10, 7, 10, 0))), HOY);

        assertEquals(EstadoPago.PAGO_PENDIENTE, estado.estadoPago());
        assertMonto("60", estado.pagoMinimoPendiente());
        assertMonto("960", estado.pagoContadoPendiente());
    }

    @Test
    @DisplayName("Pago de contado completo: al día sin pendientes")
    void contadoCompleto() {
        EstadoPagoCuenta estado = calcular(List.of(consumo("1000.00", LocalDateTime.of(2026, 9, 20, 10, 0))),
                List.of(), List.of(pago("1000.00", LocalDateTime.of(2026, 10, 7, 10, 0))), HOY);

        assertEquals(EstadoPago.AL_DIA, estado.estadoPago());
        assertMonto("0", estado.pagoMinimoPendiente());
        assertMonto("0", estado.pagoContadoPendiente());
    }

    @Test
    @DisplayName("Mínimo del ciclo vencido sin cubrir después del día 25: EN_MORA por lo que falta del mínimo")
    void moraPorPagoMenorAlMinimo() {
        EstadoPagoCuenta estado = calcular(List.of(consumo("1500.00", LocalDateTime.of(2026, 9, 1, 10, 0))),
                List.of(), List.of(pago("500.00", LocalDateTime.of(2026, 9, 2, 10, 0)),
                        pago("40.00", LocalDateTime.of(2026, 9, 20, 10, 0))), HOY);

        assertEquals(EstadoPago.EN_MORA, estado.estadoPago());
        assertMonto("60", estado.montoEnMora());
        assertEquals(14, estado.diasMora());
    }

    @Test
    @DisplayName("Completar el mínimo vencido saca de la mora aunque quede saldo")
    void completarElMinimoSacaDeLaMora() {
        List<Consumo> consumos = List.of(consumo("1500.00", LocalDateTime.of(2026, 9, 1, 10, 0)));
        List<Pago> previos = List.of(pago("500.00", LocalDateTime.of(2026, 9, 2, 10, 0)),
                pago("40.00", LocalDateTime.of(2026, 9, 20, 10, 0)));
        List<Pago> completo = List.of(pago("500.00", LocalDateTime.of(2026, 9, 2, 10, 0)),
                pago("40.00", LocalDateTime.of(2026, 9, 20, 10, 0)),
                pago("60.00", LocalDateTime.of(2026, 10, 8, 10, 0)));

        assertEquals(EstadoPago.EN_MORA, calcular(consumos, List.of(), previos, HOY).estadoPago());
        EstadoPagoCuenta estado = calcular(consumos, List.of(), completo, HOY);

        assertEquals(EstadoPago.PAGO_PENDIENTE, estado.estadoPago());
        assertMonto("0", estado.montoEnMora());
        assertEquals(0, estado.diasMora());
    }

    @Test
    @DisplayName("Pagar tarde el mínimo vencido vuelve la cuenta a AL_DIA")
    void pagadoTardeVuelveAlDia() {
        EstadoPagoCuenta estado = calcular(List.of(consumo("1000.00", LocalDateTime.of(2026, 8, 20, 10, 0))),
                List.of(), List.of(pago("1000.00", LocalDateTime.of(2026, 10, 2, 10, 0))),
                LocalDateTime.of(2026, 10, 10, 12, 0));

        assertEquals(EstadoPago.AL_DIA, estado.estadoPago());
        assertMonto("0", estado.montoEnMora());
        assertEquals(0, estado.diasMora());
    }

    @Test
    @DisplayName("Pago parcial antes del corte vencido deja mora por el mínimo del saldo restante")
    void pagoParcialTardio() {
        EstadoPagoCuenta estado = calcular(List.of(consumo("1000.00", LocalDateTime.of(2026, 9, 10, 10, 0))),
                List.of(), List.of(pago("300.00", LocalDateTime.of(2026, 9, 29, 10, 0))),
                LocalDateTime.of(2026, 10, 30, 12, 0));

        assertEquals(EstadoPago.EN_MORA, estado.estadoPago());
        assertMonto("70", estado.montoEnMora());
        assertEquals(5, estado.diasMora());
    }

    @Test
    @DisplayName("Tarjeta emitida después del corte: ese corte no cuenta y está al día")
    void emitidaDespuesDelCorte() {
        EstadoPagoCuenta estado = calculadora.calcular(tarjeta(LocalDateTime.of(2026, 10, 7, 9, 0)),
                List.of(consumo("800.00", LocalDateTime.of(2026, 10, 8, 10, 0))),
                List.of(), List.of(), LocalDateTime.of(2026, 10, 30, 12, 0));

        assertEquals(EstadoPago.AL_DIA, estado.estadoPago());
        assertMonto("0", estado.saldoAlCorte());
    }

    @Test
    @DisplayName("Un consumo posterior al corte no entra en el saldo al corte")
    void consumoPosteriorAlCorte() {
        EstadoPagoCuenta estado = calcular(List.of(consumo("500.00", LocalDateTime.of(2026, 10, 6, 0, 0))),
                List.of(), List.of(), HOY);

        assertEquals(EstadoPago.AL_DIA, estado.estadoPago());
        assertMonto("0", estado.saldoAlCorte());
    }

    @Test
    @DisplayName("Frontera del corte: consumo a las 23:59:59 del día 5 entra; a las 23:59:58 el corte aún no ocurre")
    void fronteraDiaCinco() {
        List<Consumo> consumos = List.of(consumo("700.00", LocalDateTime.of(2026, 10, 5, 23, 59, 59)));

        EstadoPagoCuenta enElCorte = calcular(consumos, List.of(), List.of(),
                LocalDateTime.of(2026, 10, 5, 23, 59, 59));
        EstadoPagoCuenta antesDelCorte = calcular(consumos, List.of(), List.of(),
                LocalDateTime.of(2026, 10, 5, 23, 59, 58));

        assertEquals(EstadoPago.PAGO_PENDIENTE, enElCorte.estadoPago());
        assertEquals(LocalDate.of(2026, 10, 5), enElCorte.fechaUltimoCorte());
        assertMonto("700", enElCorte.saldoAlCorte());
        assertEquals(LocalDate.of(2026, 9, 5), antesDelCorte.fechaUltimoCorte());
        assertEquals(EstadoPago.AL_DIA, antesDelCorte.estadoPago());
    }

    @Test
    @DisplayName("Frontera del plazo: a las 23:59:59 del día 25 aún es PAGO_PENDIENTE; al día siguiente es EN_MORA")
    void fronteraDiaVeinticinco() {
        List<Consumo> consumos = List.of(consumo("900.00", LocalDateTime.of(2026, 9, 20, 10, 0)));

        EstadoPagoCuenta ultimoInstante = calcular(consumos, List.of(), List.of(),
                LocalDateTime.of(2026, 10, 25, 23, 59, 59));
        EstadoPagoCuenta diaSiguiente = calcular(consumos, List.of(), List.of(),
                LocalDateTime.of(2026, 10, 26, 0, 0));

        assertEquals(EstadoPago.PAGO_PENDIENTE, ultimoInstante.estadoPago());
        assertEquals(EstadoPago.EN_MORA, diaSiguiente.estadoPago());
        assertEquals(1, diaSiguiente.diasMora());
        assertMonto("90", diaSiguiente.montoEnMora());
    }

    @Test
    @DisplayName("Pasado el día 25 con el mínimo pagado y contado pendiente: AL_DIA (ese saldo genera interés)")
    void pasadoElPlazoConMinimoPagado() {
        EstadoPagoCuenta estado = calcular(List.of(consumo("900.00", LocalDateTime.of(2026, 9, 20, 10, 0))),
                List.of(), List.of(pago("90.00", LocalDateTime.of(2026, 10, 10, 10, 0))),
                LocalDateTime.of(2026, 10, 26, 0, 0));

        assertEquals(EstadoPago.AL_DIA, estado.estadoPago());
        assertMonto("810", estado.pagoContadoPendiente());
    }

    @Test
    @DisplayName("Los cargos cuentan como débitos: entran al saldo al corte y al pago mínimo")
    void cargosCuentanComoDebitos() {
        EstadoPagoCuenta estado = calcular(List.of(consumo("1000.00", LocalDateTime.of(2026, 9, 1, 10, 0))),
                List.of(cargo("300.00", LocalDateTime.of(2026, 9, 26, 0, 0))), List.of(), HOY);

        assertMonto("1300", estado.saldoAlCorte());
        assertMonto("130", estado.pagoMinimo());
        assertMonto("1300", estado.pagoContado());
        assertEquals(EstadoPago.EN_MORA, estado.estadoPago());
        assertMonto("100", estado.montoEnMora());
    }

    @Test
    @DisplayName("Un cargo dentro del corte vencido eleva la mora; uno reversado no cuenta")
    void cargoEnCicloVencidoYReversado() {
        Cargo reversado = cargo("500.00", LocalDateTime.of(2026, 9, 3, 0, 0));
        reversado.reversar("Intereses devueltos", LocalDateTime.of(2026, 9, 4, 0, 0), "admin");

        EstadoPagoCuenta estado = calcular(List.of(consumo("1000.00", LocalDateTime.of(2026, 9, 1, 10, 0))),
                List.of(cargo("200.00", LocalDateTime.of(2026, 9, 2, 0, 0)), reversado), List.of(), HOY);

        assertEquals(EstadoPago.EN_MORA, estado.estadoPago());
        assertMonto("120", estado.montoEnMora());
    }

    private Consumo consumoReversado(String monto, LocalDateTime fecha) {
        Consumo consumo = consumo(monto, fecha);
        consumo.reversar("Cargo duplicado por el comercio", fecha.plusDays(1), "admin");
        return consumo;
    }

    private Pago pagoReversado(String monto, LocalDateTime fecha) {
        Pago pago = pago(monto, fecha);
        pago.reversar("Pago registrado por error", fecha.plusDays(1), "admin");
        return pago;
    }

    @Test
    @DisplayName("Un consumo reversado no cuenta en el saldo al corte ni en la mora")
    void consumoReversadoNoCuenta() {
        EstadoPagoCuenta estado = calcular(
                List.of(consumo("1000.00", LocalDateTime.of(2026, 9, 1, 10, 0)),
                        consumoReversado("800.00", LocalDateTime.of(2026, 9, 1, 11, 0))),
                List.of(), List.of(pago("1000.00", LocalDateTime.of(2026, 9, 10, 10, 0))), HOY);

        assertEquals(EstadoPago.AL_DIA, estado.estadoPago());
        assertMonto("0", estado.montoEnMora());
        assertMonto("0", estado.saldoAlCorte());
    }

    @Test
    @DisplayName("Reversar un consumo ya pagado antes del corte no deja el saldo al corte negativo")
    void saldoAlCorteNuncaNegativo() {
        EstadoPagoCuenta estado = calcular(
                List.of(consumoReversado("1500.00", LocalDateTime.of(2026, 9, 1, 10, 0))),
                List.of(), List.of(pago("1500.00", LocalDateTime.of(2026, 9, 3, 10, 0))), HOY);

        assertEquals(EstadoPago.AL_DIA, estado.estadoPago());
        assertMonto("0", estado.saldoAlCorte());
        assertMonto("0", estado.pagoContadoPendiente());
    }

    @Test
    @DisplayName("Sin el reverso el mismo consumo duplicado dejaría la cuenta en mora")
    void consumoDuplicadoSinReversarGeneraMora() {
        EstadoPagoCuenta estado = calcular(
                List.of(consumo("1000.00", LocalDateTime.of(2026, 9, 1, 10, 0)),
                        consumo("800.00", LocalDateTime.of(2026, 9, 1, 11, 0))),
                List.of(), List.of(pago("1000.00", LocalDateTime.of(2026, 9, 2, 10, 0))), HOY);

        assertEquals(EstadoPago.EN_MORA, estado.estadoPago());
        assertMonto("80", estado.montoEnMora());
    }

    @Test
    @DisplayName("Un pago reversado no cuenta: el saldo vuelve a estar pendiente y en mora")
    void pagoReversadoNoCuenta() {
        EstadoPagoCuenta estado = calcular(List.of(consumo("1000.00", LocalDateTime.of(2026, 9, 1, 10, 0))),
                List.of(), List.of(pagoReversado("1000.00", LocalDateTime.of(2026, 9, 10, 10, 0))), HOY);

        assertEquals(EstadoPago.EN_MORA, estado.estadoPago());
        assertMonto("100", estado.montoEnMora());
    }
}
