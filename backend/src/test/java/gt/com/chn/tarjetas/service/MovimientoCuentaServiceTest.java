package gt.com.chn.tarjetas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.com.chn.tarjetas.dto.MovimientoPeticion;
import gt.com.chn.tarjetas.dto.MovimientoRespuesta;
import gt.com.chn.tarjetas.dto.ReversoPeticion;
import gt.com.chn.tarjetas.entity.Cargo;
import gt.com.chn.tarjetas.entity.Consumo;
import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.entity.EstadoPago;
import gt.com.chn.tarjetas.entity.EstadoPagoCuenta;
import gt.com.chn.tarjetas.entity.MotivoBloqueo;
import gt.com.chn.tarjetas.entity.MotivoCancelacion;
import gt.com.chn.tarjetas.entity.Pago;
import gt.com.chn.tarjetas.entity.TipoCargo;
import gt.com.chn.tarjetas.entity.TarjetaCredito;
import gt.com.chn.tarjetas.exception.RecursoNoEncontradoException;
import gt.com.chn.tarjetas.exception.ReglaNegocioException;
import gt.com.chn.tarjetas.repository.CargoRepository;
import gt.com.chn.tarjetas.repository.ConsumoRepository;
import gt.com.chn.tarjetas.repository.CuentaCreditoRepository;
import gt.com.chn.tarjetas.repository.PagoRepository;
import gt.com.chn.tarjetas.repository.TarjetaCreditoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovimientoCuentaServiceTest {

    @Mock
    private CuentaCreditoRepository cuentaCreditoRepository;
    @Mock
    private TarjetaCreditoRepository tarjetaCreditoRepository;
    @Mock
    private ConsumoRepository consumoRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private CargoRepository cargoRepository;
    @Mock
    private EstadoPagoService estadoPagoService;
    @Mock
    private ProcesoInteresesService procesoIntereses;

    private MovimientoCuentaService movimientoService;

    private CuentaCredito cuenta;
    private TarjetaCredito tarjeta;

    @BeforeEach
    void prepararCuenta() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-09T18:00:00Z"), ZoneId.of("America/Guatemala"));
        movimientoService = new MovimientoCuentaService(cuentaCreditoRepository, tarjetaCreditoRepository,
                consumoRepository, pagoRepository, cargoRepository, estadoPagoService, procesoIntereses, reloj);
        cuenta = new CuentaCredito();
        cuenta.setId(1L);
        cuenta.setLimiteCredito(new BigDecimal("1000.00"));
        cuenta.setSaldoActual(new BigDecimal("400.00"));
        tarjeta = new TarjetaCredito();
        tarjeta.setId(7L);
        tarjeta.setNumeroTarjeta("4200555123409875");
        tarjeta.setCuenta(cuenta);
        tarjeta.setFechaVencimiento(LocalDate.of(2031, 10, 31));
    }

    private void cuentaConBloqueo() {
        when(cuentaCreditoRepository.buscarPorIdConBloqueo(1L)).thenReturn(Optional.of(cuenta));
    }

    private void tarjetaVigente() {
        when(tarjetaCreditoRepository.buscarVigentePorCuenta(1L)).thenReturn(Optional.of(tarjeta));
    }

    private EstadoPagoCuenta estado(EstadoPago estadoPago, String montoEnMora) {
        BigDecimal mora = new BigDecimal(montoEnMora);
        return new EstadoPagoCuenta(estadoPago, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 25),
                LocalDate.of(2026, 11, 5), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, mora, mora.signum() > 0 ? 10 : 0);
    }

    private MovimientoPeticion peticion(String monto) {
        return new MovimientoPeticion(new BigDecimal(monto), "Compra de prueba", null);
    }

    private ReversoPeticion reverso() {
        return new ReversoPeticion("  Cargo duplicado por el comercio ");
    }

    private Consumo consumo(long id, String monto) {
        Consumo consumo = new Consumo();
        consumo.setId(id);
        consumo.setCuenta(cuenta);
        consumo.setTarjeta(tarjeta);
        consumo.setMonto(new BigDecimal(monto));
        consumo.setFecha(LocalDateTime.of(2026, 10, 1, 10, 0));
        return consumo;
    }

    private Pago pago(long id, String monto) {
        Pago pago = new Pago();
        pago.setId(id);
        pago.setCuenta(cuenta);
        pago.setMonto(new BigDecimal(monto));
        pago.setFecha(LocalDateTime.of(2026, 10, 2, 10, 0));
        return pago;
    }

    @Test
    @DisplayName("Consumo que excede el límite lanza 422 con el mensaje del contrato")
    void consumoExcedeLimite() {
        cuentaConBloqueo();
        tarjetaVigente();
        when(estadoPagoService.calcular(cuenta)).thenReturn(estado(EstadoPago.AL_DIA, "0.00"));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> movimientoService.registrarConsumo(1L, peticion("600.01"), "operador"));

        assertEquals("El consumo de Q 600.01 excede el crédito disponible de Q 600.00", excepcion.getMessage());
        assertEquals(new BigDecimal("400.00"), cuenta.getSaldoActual());
        verify(consumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Consumo válido incrementa el saldo de la cuenta y guarda la tarjeta vigente usada")
    void consumoValidoActualizaSaldo() {
        cuentaConBloqueo();
        tarjetaVigente();
        when(estadoPagoService.calcular(cuenta)).thenReturn(estado(EstadoPago.PAGO_PENDIENTE, "0.00"));
        when(consumoRepository.save(any(Consumo.class))).thenAnswer(i -> i.getArgument(0));

        MovimientoRespuesta respuesta = movimientoService.registrarConsumo(1L, peticion("600.00"), "operador");

        assertEquals(0, new BigDecimal("1000.00").compareTo(cuenta.getSaldoActual()));
        assertEquals("CONSUMO", respuesta.tipoMovimiento());
        assertEquals("operador", respuesta.usuarioRegistro());
        assertEquals(1L, respuesta.idCuenta());
        assertEquals(7L, respuesta.idTarjeta());
        assertEquals("**** **** **** 9875", respuesta.numeroTarjetaEnmascarado());
        assertFalse(respuesta.reversado());
    }

    @Test
    @DisplayName("Pago mayor al saldo lanza 422 y no deja saldo negativo")
    void pagoExcedeSaldo() {
        cuentaConBloqueo();

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> movimientoService.registrarPago(1L, peticion("400.01"), "operador"));

        assertEquals("El pago de Q 400.01 excede el saldo actual de Q 400.00", excepcion.getMessage());
        assertEquals(new BigDecimal("400.00"), cuenta.getSaldoActual());
        verify(pagoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pago sobre una cuenta sin saldo lanza 422")
    void pagoSinSaldoPendiente() {
        cuenta.setSaldoActual(new BigDecimal("0.00"));
        cuentaConBloqueo();

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> movimientoService.registrarPago(1L, peticion("10.00"), "operador"));

        assertEquals("La cuenta no tiene saldo pendiente de pago", excepcion.getMessage());
    }

    @Test
    @DisplayName("Pago válido reduce el saldo, incluso hasta cero, y no guarda tarjeta")
    void pagoValidoReduceSaldo() {
        cuentaConBloqueo();
        when(pagoRepository.save(any(Pago.class))).thenAnswer(i -> i.getArgument(0));

        MovimientoRespuesta respuesta = movimientoService.registrarPago(1L, peticion("400.00"), "operador");

        assertEquals(0, BigDecimal.ZERO.compareTo(cuenta.getSaldoActual()));
        assertEquals("PAGO", respuesta.tipoMovimiento());
        assertEquals(null, respuesta.idTarjeta());
    }

    @Test
    @DisplayName("Registrar movimiento en una cuenta inexistente lanza 404")
    void cuentaInexistente() {
        when(cuentaCreditoRepository.buscarPorIdConBloqueo(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> movimientoService.registrarConsumo(9L, peticion("10.00"), "operador"));
    }

    @Test
    @DisplayName("Listar movimientos de una cuenta inexistente lanza 404")
    void listarMovimientosCuentaInexistente() {
        when(cuentaCreditoRepository.existsById(9L)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> movimientoService.listarMovimientos(9L));
    }

    @Test
    @DisplayName("Los movimientos se unifican por fecha descendente e incluyen los reversados")
    void listarMovimientosUnificados() {
        Consumo reversado = consumo(1L, "50.00");
        reversado.reversar("Cargo duplicado por el comercio", LocalDateTime.of(2026, 10, 3, 9, 0), "admin");
        Pago pago = pago(1L, "100.00");
        when(cuentaCreditoRepository.existsById(1L)).thenReturn(true);
        when(consumoRepository.listarPorCuenta(1L)).thenReturn(List.of(reversado));
        when(pagoRepository.findByCuentaId(1L)).thenReturn(List.of(pago));

        List<MovimientoRespuesta> movimientos = movimientoService.listarMovimientos(1L);

        assertEquals(2, movimientos.size());
        assertEquals("PAGO", movimientos.get(0).tipoMovimiento());
        assertEquals("CONSUMO", movimientos.get(1).tipoMovimiento());
        assertTrue(movimientos.get(1).reversado());
        assertEquals("Cargo duplicado por el comercio", movimientos.get(1).motivoReverso());
    }

    private Cargo cargo(long id, TipoCargo tipo, String monto) {
        Cargo cargo = new Cargo();
        cargo.setId(id);
        cargo.setCuenta(cuenta);
        cargo.setTipoCargo(tipo);
        cargo.setMonto(new BigDecimal(monto));
        cargo.setFecha(LocalDateTime.of(2026, 10, 4, 10, 0));
        return cargo;
    }

    @Test
    @DisplayName("Los movimientos unificados incluyen los cargos con su tipo y su descripción")
    void listarMovimientosConCargos() {
        when(cuentaCreditoRepository.existsById(1L)).thenReturn(true);
        when(consumoRepository.listarPorCuenta(1L)).thenReturn(List.of(consumo(1L, "50.00")));
        when(pagoRepository.findByCuentaId(1L)).thenReturn(List.of(pago(1L, "100.00")));
        when(cargoRepository.findByCuentaId(1L)).thenReturn(List.of(cargo(8L, TipoCargo.INTERES, "90.00")));

        List<MovimientoRespuesta> movimientos = movimientoService.listarMovimientos(1L);

        assertEquals(List.of("CARGO", "PAGO", "CONSUMO"),
                movimientos.stream().map(MovimientoRespuesta::tipoMovimiento).toList());
        MovimientoRespuesta cargo = movimientos.get(0);
        assertEquals(TipoCargo.INTERES, cargo.tipoCargo());
        assertEquals("Intereses", cargo.descripcionTipoCargo());
        assertEquals(8L, cargo.id());
        assertEquals(null, cargo.idTarjeta());
        assertFalse(cargo.reversado());
    }

    @Test
    @DisplayName("RNV-04: consumo con la tarjeta vencida lanza 422 con la fecha de vencimiento y los pagos sí se permiten")
    void consumoConTarjetaVencida() {
        tarjeta.setFechaVencimiento(LocalDate.of(2026, 9, 30));
        cuentaConBloqueo();
        tarjetaVigente();

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> movimientoService.registrarConsumo(1L, peticion("10.00"), "operador"));

        assertEquals("La tarjeta venció el 30/09/2026. Debe renovarse antes de registrar consumos",
                excepcion.getMessage());
        verifyNoInteractions(estadoPagoService);
        verify(consumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una tarjeta que vence hoy todavía acepta consumos (vencida es hoy posterior al vencimiento)")
    void consumoElDiaDelVencimiento() {
        tarjeta.setFechaVencimiento(LocalDate.of(2026, 10, 9));
        cuentaConBloqueo();
        tarjetaVigente();
        when(estadoPagoService.calcular(cuenta)).thenReturn(estado(EstadoPago.AL_DIA, "0.00"));
        when(consumoRepository.save(any(Consumo.class))).thenAnswer(i -> i.getArgument(0));

        MovimientoRespuesta respuesta = movimientoService.registrarConsumo(1L, peticion("10.00"), "operador");

        assertEquals("CONSUMO", respuesta.tipoMovimiento());
    }

    @Test
    @DisplayName("Un cargo puede sobregirar el límite y luego el crédito disponible es cero: no se permiten más consumos")
    void consumoConCuentaSobregirada() {
        cuenta.setSaldoActual(new BigDecimal("1045.00"));
        cuentaConBloqueo();
        tarjetaVigente();
        when(estadoPagoService.calcular(cuenta)).thenReturn(estado(EstadoPago.AL_DIA, "0.00"));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> movimientoService.registrarConsumo(1L, peticion("1.00"), "operador"));

        assertEquals("El consumo de Q 1.00 excede el crédito disponible de Q 0.00", excepcion.getMessage());
        assertEquals(0, BigDecimal.ZERO.compareTo(cuenta.obtenerCreditoDisponible()));
    }

    @Test
    @DisplayName("CA-05: una cuenta cancelada rechaza consumos, pagos y reversos con 422 sin generar intereses")
    void cuentaCanceladaNoAdmiteMovimientos() {
        cuenta.cancelar(MotivoCancelacion.SOLICITUD_CLIENTE, null, LocalDateTime.of(2026, 9, 1, 9, 0), "admin");
        cuentaConBloqueo();
        when(consumoRepository.buscarIdCuentaPorId(5L)).thenReturn(Optional.of(1L));
        when(pagoRepository.buscarIdCuentaPorId(3L)).thenReturn(Optional.of(1L));

        for (org.junit.jupiter.api.function.Executable operacion : List.<org.junit.jupiter.api.function.Executable>of(
                () -> movimientoService.registrarConsumo(1L, peticion("10.00"), "operador"),
                () -> movimientoService.registrarPago(1L, peticion("10.00"), "cajero"),
                () -> movimientoService.reversarConsumo(5L, reverso(), "admin"),
                () -> movimientoService.reversarPago(3L, reverso(), "admin"))) {
            ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class, operacion);
            assertEquals("La cuenta está cancelada", excepcion.getMessage());
        }
        verifyNoInteractions(procesoIntereses);
        verify(consumoRepository, never()).save(any());
        verify(pagoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Antes de escribir se generan los intereses pendientes con la cuenta ya bloqueada")
    void generaInteresesAntesDeEscribir() {
        cuentaConBloqueo();
        when(pagoRepository.save(any(Pago.class))).thenAnswer(i -> i.getArgument(0));

        movimientoService.registrarPago(1L, peticion("100.00"), "cajero");

        org.mockito.InOrder orden = org.mockito.Mockito.inOrder(cuentaCreditoRepository, procesoIntereses, pagoRepository);
        orden.verify(cuentaCreditoRepository).buscarPorIdConBloqueo(1L);
        orden.verify(procesoIntereses).generarInteresesPendientes(cuenta);
        orden.verify(pagoRepository).save(any(Pago.class));
    }

    @Test
    @DisplayName("RC-05: consumo en cuenta en mora lanza 422 y no modifica el saldo")
    void consumoEnCuentaEnMora() {
        cuentaConBloqueo();
        tarjetaVigente();
        when(estadoPagoService.calcular(cuenta)).thenReturn(estado(EstadoPago.EN_MORA, "2500"));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> movimientoService.registrarConsumo(1L, peticion("10.00"), "operador"));

        assertEquals("La cuenta está en mora por Q 2500.00. Registre un pago para ponerse al día antes de "
                + "realizar consumos", excepcion.getMessage());
        assertEquals(new BigDecimal("400.00"), cuenta.getSaldoActual());
        verify(consumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("RB-03: consumo con la tarjeta vigente bloqueada lanza 422 y se valida antes que la mora")
    void consumoConTarjetaVigenteBloqueada() {
        tarjeta.bloquear(MotivoBloqueo.SOSPECHA_FRAUDE, null, LocalDateTime.of(2026, 10, 9, 10, 0), "operador");
        cuentaConBloqueo();
        tarjetaVigente();

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> movimientoService.registrarConsumo(1L, peticion("10.00"), "operador"));

        assertEquals("La tarjeta está bloqueada por Sospecha de fraude. No se pueden registrar consumos",
                excepcion.getMessage());
        verifyNoInteractions(estadoPagoService);
        verify(consumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("RB-05: se puede registrar un pago aunque la tarjeta vigente esté bloqueada")
    void pagoConTarjetaBloqueada() {
        tarjeta.bloquear(MotivoBloqueo.ROBO, null, LocalDateTime.of(2026, 10, 9, 10, 0), "operador");
        cuentaConBloqueo();
        when(pagoRepository.save(any(Pago.class))).thenAnswer(i -> i.getArgument(0));

        MovimientoRespuesta respuesta = movimientoService.registrarPago(1L, peticion("100.00"), "operador");

        assertEquals("PAGO", respuesta.tipoMovimiento());
        assertEquals(0, new BigDecimal("300.00").compareTo(cuenta.getSaldoActual()));
        verifyNoInteractions(tarjetaCreditoRepository);
    }

    @Test
    @DisplayName("Pago sin fecha usa la hora del reloj inyectado (America/Guatemala)")
    void pagoSinFechaUsaReloj() {
        cuentaConBloqueo();
        when(pagoRepository.save(any(Pago.class))).thenAnswer(i -> i.getArgument(0));

        MovimientoRespuesta respuesta = movimientoService.registrarPago(1L, peticion("100.00"), "operador");

        assertEquals(LocalDateTime.of(2026, 10, 9, 12, 0), respuesta.fecha());
    }

    @Test
    @DisplayName("RV-02/RV-05: reversar un consumo resta su monto del saldo y lo marca sin borrarlo")
    void reversarConsumo() {
        Consumo consumo = consumo(5L, "150.00");
        when(consumoRepository.buscarIdCuentaPorId(5L)).thenReturn(Optional.of(1L));
        cuentaConBloqueo();
        when(consumoRepository.findById(5L)).thenReturn(Optional.of(consumo));

        MovimientoRespuesta respuesta = movimientoService.reversarConsumo(5L, reverso(), "admin");

        assertEquals(0, new BigDecimal("250.00").compareTo(cuenta.getSaldoActual()));
        assertTrue(respuesta.reversado());
        assertEquals("Cargo duplicado por el comercio", respuesta.motivoReverso());
        assertEquals("admin", respuesta.usuarioReverso());
        assertEquals(LocalDateTime.of(2026, 10, 9, 12, 0), respuesta.fechaReverso());
        verify(consumoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("RV-01: reversar un movimiento ya reversado lanza 422 con la fecha del reverso")
    void reversarYaReversado() {
        Consumo consumo = consumo(5L, "150.00");
        consumo.reversar("Cargo duplicado por el comercio", LocalDateTime.of(2026, 10, 3, 9, 30), "admin");
        when(consumoRepository.buscarIdCuentaPorId(5L)).thenReturn(Optional.of(1L));
        cuentaConBloqueo();
        when(consumoRepository.findById(5L)).thenReturn(Optional.of(consumo));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> movimientoService.reversarConsumo(5L, reverso(), "admin"));

        assertEquals("El movimiento ya fue reversado el 03/10/2026 09:30", excepcion.getMessage());
        assertEquals(new BigDecimal("400.00"), cuenta.getSaldoActual());
    }

    @Test
    @DisplayName("RV-01: un pago ya reversado tampoco se reversa de nuevo")
    void reversarPagoYaReversado() {
        Pago pago = pago(3L, "100.00");
        pago.reversar("Pago registrado por error", LocalDateTime.of(2026, 10, 3, 9, 30), "admin");
        when(pagoRepository.buscarIdCuentaPorId(3L)).thenReturn(Optional.of(1L));
        cuentaConBloqueo();
        when(pagoRepository.findById(3L)).thenReturn(Optional.of(pago));

        assertThrows(ReglaNegocioException.class, () -> movimientoService.reversarPago(3L, reverso(), "admin"));
    }

    @Test
    @DisplayName("RV-02: reversar un consumo que dejaría el saldo negativo lanza 422")
    void reversarConsumoDejaSaldoNegativo() {
        Consumo consumo = consumo(5L, "650.00");
        when(consumoRepository.buscarIdCuentaPorId(5L)).thenReturn(Optional.of(1L));
        cuentaConBloqueo();
        when(consumoRepository.findById(5L)).thenReturn(Optional.of(consumo));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> movimientoService.reversarConsumo(5L, reverso(), "admin"));

        assertEquals("No se puede reversar el consumo: el saldo quedaría en Q -250.00. "
                + "Reverse primero el pago que lo cubrió", excepcion.getMessage());
        assertFalse(consumo.estaReversado());
        assertEquals(new BigDecimal("400.00"), cuenta.getSaldoActual());
    }

    @Test
    @DisplayName("RV-03: reversar un pago suma su monto al saldo")
    void reversarPago() {
        Pago pago = pago(3L, "300.00");
        when(pagoRepository.buscarIdCuentaPorId(3L)).thenReturn(Optional.of(1L));
        cuentaConBloqueo();
        when(pagoRepository.findById(3L)).thenReturn(Optional.of(pago));

        MovimientoRespuesta respuesta = movimientoService.reversarPago(3L, reverso(), "admin");

        assertEquals(0, new BigDecimal("700.00").compareTo(cuenta.getSaldoActual()));
        assertTrue(respuesta.reversado());
        assertEquals("PAGO", respuesta.tipoMovimiento());
    }

    @Test
    @DisplayName("RV-03: reversar un pago que haría superar el límite lanza 422")
    void reversarPagoSuperaLimite() {
        Pago pago = pago(3L, "700.00");
        when(pagoRepository.buscarIdCuentaPorId(3L)).thenReturn(Optional.of(1L));
        cuentaConBloqueo();
        when(pagoRepository.findById(3L)).thenReturn(Optional.of(pago));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> movimientoService.reversarPago(3L, reverso(), "admin"));

        assertEquals("No se puede reversar el pago: el saldo de Q 1100.00 superaría el límite de crédito "
                + "de Q 1000.00", excepcion.getMessage());
        assertFalse(pago.estaReversado());
        assertEquals(new BigDecimal("400.00"), cuenta.getSaldoActual());
    }

    @Test
    @DisplayName("RV-04: el reverso bloquea la cuenta antes de leer el movimiento")
    void reversoBloqueaLaCuentaPrimero() {
        Consumo consumo = consumo(5L, "100.00");
        when(consumoRepository.buscarIdCuentaPorId(5L)).thenReturn(Optional.of(1L));
        cuentaConBloqueo();
        when(consumoRepository.findById(5L)).thenReturn(Optional.of(consumo));

        movimientoService.reversarConsumo(5L, reverso(), "admin");

        org.mockito.InOrder orden = org.mockito.Mockito.inOrder(cuentaCreditoRepository, consumoRepository);
        orden.verify(consumoRepository).buscarIdCuentaPorId(5L);
        orden.verify(cuentaCreditoRepository).buscarPorIdConBloqueo(1L);
        orden.verify(consumoRepository).findById(5L);
    }

    @Test
    @DisplayName("Reversar un consumo o un pago inexistente lanza 404")
    void reversarInexistente() {
        when(consumoRepository.buscarIdCuentaPorId(9L)).thenReturn(Optional.empty());
        when(pagoRepository.buscarIdCuentaPorId(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> movimientoService.reversarConsumo(9L, reverso(), "admin"));
        assertThrows(RecursoNoEncontradoException.class, () -> movimientoService.reversarPago(9L, reverso(), "admin"));
    }
}
