package gt.com.chn.tarjetas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.com.chn.tarjetas.dto.CargoPeticion;
import gt.com.chn.tarjetas.dto.CuentaRespuesta;
import gt.com.chn.tarjetas.dto.ExoneracionMoraRespuesta;
import gt.com.chn.tarjetas.dto.MovimientoRespuesta;
import gt.com.chn.tarjetas.dto.ReversoPeticion;
import gt.com.chn.tarjetas.entity.Cargo;
import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.entity.MotivoCancelacion;
import gt.com.chn.tarjetas.entity.TipoCargo;
import gt.com.chn.tarjetas.exception.RecursoNoEncontradoException;
import gt.com.chn.tarjetas.exception.ReglaNegocioException;
import gt.com.chn.tarjetas.repository.CargoRepository;
import gt.com.chn.tarjetas.repository.CuentaCreditoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CargoServiceTest {

    @Mock
    private CuentaCreditoRepository cuentaCreditoRepository;
    @Mock
    private CargoRepository cargoRepository;
    @Mock
    private ProcesoInteresesService procesoIntereses;
    @Mock
    private CuentaService cuentaService;

    private CargoService cargoService;
    private CuentaCredito cuenta;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-09T18:00:00Z"), ZoneId.of("America/Guatemala"));
        cargoService = new CargoService(cuentaCreditoRepository, cargoRepository, procesoIntereses, cuentaService,
                reloj);
        cuenta = new CuentaCredito();
        cuenta.setId(1L);
        cuenta.setLimiteCredito(new BigDecimal("1000.00"));
        cuenta.setSaldoActual(new BigDecimal("900.00"));
        org.mockito.Mockito.lenient().when(cuentaCreditoRepository.buscarPorIdConBloqueo(1L))
                .thenReturn(Optional.of(cuenta));
        org.mockito.Mockito.lenient().when(cargoRepository.save(any(Cargo.class))).thenAnswer(i -> i.getArgument(0));
    }

    private CargoPeticion peticion(TipoCargo tipo, String monto) {
        return new CargoPeticion(tipo, new BigDecimal(monto), "  Recargo del ciclo  ", null);
    }

    private ReversoPeticion motivo() {
        return new ReversoPeticion("  Exoneración autorizada ");
    }

    private Cargo cargo(long id, TipoCargo tipo, String monto) {
        Cargo cargo = new Cargo();
        cargo.setId(id);
        cargo.setCuenta(cuenta);
        cargo.setTipoCargo(tipo);
        cargo.setMonto(new BigDecimal(monto));
        cargo.setFecha(LocalDateTime.of(2026, 9, 26, 0, 0));
        return cargo;
    }

    private void assertMonto(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " pero fue " + real);
    }

    @Test
    @DisplayName("Un recargo por mora suma al saldo y puede dejarlo por encima del límite (sobregiro)")
    void registrarRecargoSobregira() {
        MovimientoRespuesta respuesta = cargoService.registrar(1L, peticion(TipoCargo.RECARGO_MORA, "500.00"), "admin");

        assertEquals("CARGO", respuesta.tipoMovimiento());
        assertEquals(TipoCargo.RECARGO_MORA, respuesta.tipoCargo());
        assertEquals("Recargo por mora", respuesta.descripcionTipoCargo());
        assertEquals("Recargo del ciclo", respuesta.descripcion());
        assertEquals("admin", respuesta.usuarioRegistro());
        assertEquals(LocalDateTime.of(2026, 10, 9, 12, 0), respuesta.fecha());
        assertMonto("1400.00", cuenta.getSaldoActual());
        assertMonto("0", cuenta.obtenerCreditoDisponible());
    }

    @Test
    @DisplayName("Antes de escribir se bloquea la cuenta y se generan los intereses pendientes")
    void registrarGeneraInteresesConLaCuentaBloqueada() {
        cargoService.registrar(1L, peticion(TipoCargo.RECARGO_MORA, "10.00"), "admin");

        InOrder orden = inOrder(cuentaCreditoRepository, procesoIntereses, cargoRepository);
        orden.verify(cuentaCreditoRepository).buscarPorIdConBloqueo(1L);
        orden.verify(procesoIntereses).generarInteresesPendientes(cuenta);
        orden.verify(cargoRepository).save(any(Cargo.class));
    }

    @Test
    @DisplayName("Los intereses ya no se registran a mano: 422 con el mensaje del contrato")
    void interesManualNoSePermite() {
        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> cargoService.registrar(1L, peticion(TipoCargo.INTERES, "50.00"), "admin"));

        assertEquals("Los intereses se generan automáticamente al vencer cada ciclo", excepcion.getMessage());
        verify(cargoRepository, never()).save(any());
        assertMonto("900.00", cuenta.getSaldoActual());
    }

    @Test
    @DisplayName("La comisión de reposición tampoco se registra a mano")
    void comisionManualNoSePermite() {
        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> cargoService.registrar(1L, peticion(TipoCargo.COMISION_REPOSICION, "45.00"), "admin"));

        assertEquals("La comisión por reposición se genera automáticamente al reponer una tarjeta por extravío",
                excepcion.getMessage());
        verifyNoInteractions(cuentaCreditoRepository);
    }

    @Test
    @DisplayName("CA-05: no se registran cargos, reversos ni exoneraciones en una cuenta cancelada")
    void cuentaCancelada() {
        cuenta.cancelar(MotivoCancelacion.SOLICITUD_CLIENTE, null, LocalDateTime.of(2026, 9, 1, 9, 0), "admin");
        when(cargoRepository.buscarIdCuentaPorId(4L)).thenReturn(Optional.of(1L));

        for (org.junit.jupiter.api.function.Executable operacion : List.<org.junit.jupiter.api.function.Executable>of(
                () -> cargoService.registrar(1L, peticion(TipoCargo.RECARGO_MORA, "10.00"), "admin"),
                () -> cargoService.reversar(4L, motivo(), "admin"),
                () -> cargoService.exonerarMora(1L, motivo(), "admin", false))) {
            assertEquals("La cuenta está cancelada", assertThrows(ReglaNegocioException.class, operacion).getMessage());
        }
        verifyNoInteractions(procesoIntereses);
        verify(cargoRepository, never()).save(any());
    }

    @Test
    @DisplayName("La comisión por extravío es de Q 45.00, suma al saldo y lleva la descripción del contrato")
    void comisionDeReposicion() {
        Cargo comision = cargoService.registrarComisionReposicion(cuenta, "operador");

        assertEquals(TipoCargo.COMISION_REPOSICION, comision.getTipoCargo());
        assertMonto("45.00", comision.getMonto());
        assertEquals("Comisión por reposición de tarjeta por extravío", comision.getDescripcion());
        assertEquals("operador", comision.getUsuarioRegistro());
        assertMonto("945.00", cuenta.getSaldoActual());
    }

    @Test
    @DisplayName("Devolución de intereses: reversar un cargo resta su monto del saldo y lo marca sin borrarlo")
    void reversarCargo() {
        Cargo interes = cargo(4L, TipoCargo.INTERES, "90.00");
        when(cargoRepository.buscarIdCuentaPorId(4L)).thenReturn(Optional.of(1L));
        when(cargoRepository.findById(4L)).thenReturn(Optional.of(interes));

        MovimientoRespuesta respuesta = cargoService.reversar(4L, motivo(), "admin");

        assertMonto("810.00", cuenta.getSaldoActual());
        assertTrue(respuesta.reversado());
        assertEquals("Exoneración autorizada", respuesta.motivoReverso());
        assertEquals("admin", respuesta.usuarioReverso());
        assertEquals(TipoCargo.INTERES, respuesta.tipoCargo());
        verify(cargoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("RV-01: un cargo ya reversado no se reversa de nuevo")
    void reversarCargoYaReversado() {
        Cargo interes = cargo(4L, TipoCargo.INTERES, "90.00");
        interes.reversar("Devolución", LocalDateTime.of(2026, 10, 3, 9, 30), "admin");
        when(cargoRepository.buscarIdCuentaPorId(4L)).thenReturn(Optional.of(1L));
        when(cargoRepository.findById(4L)).thenReturn(Optional.of(interes));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> cargoService.reversar(4L, motivo(), "admin"));

        assertEquals("El movimiento ya fue reversado el 03/10/2026 09:30", excepcion.getMessage());
        assertMonto("900.00", cuenta.getSaldoActual());
    }

    @Test
    @DisplayName("Reversar un cargo que dejaría el saldo negativo lanza 422")
    void reversarCargoDejaSaldoNegativo() {
        cuenta.setSaldoActual(new BigDecimal("20.00"));
        Cargo comision = cargo(4L, TipoCargo.COMISION_REPOSICION, "45.00");
        when(cargoRepository.buscarIdCuentaPorId(4L)).thenReturn(Optional.of(1L));
        when(cargoRepository.findById(4L)).thenReturn(Optional.of(comision));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> cargoService.reversar(4L, motivo(), "admin"));

        assertEquals("No se puede reversar el cargo: el saldo quedaría en Q -25.00. "
                + "Reverse primero el pago que lo cubrió", excepcion.getMessage());
        assertFalse(comision.estaReversado());
    }

    @Test
    @DisplayName("Reversar un cargo inexistente lanza 404")
    void reversarCargoInexistente() {
        when(cargoRepository.buscarIdCuentaPorId(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> cargoService.reversar(9L, motivo(), "admin"));
    }

    @Test
    @DisplayName("Exoneración de mora: reversa todos los recargos pendientes y devuelve cantidad, monto y cuenta")
    void exonerarMora() {
        cuenta.setSaldoActual(new BigDecimal("1200.00"));
        Cargo primero = cargo(1L, TipoCargo.RECARGO_MORA, "150.00");
        Cargo segundo = cargo(2L, TipoCargo.RECARGO_MORA, "150.50");
        when(cargoRepository.findByCuentaIdAndTipoCargoAndReversadoFalse(1L, TipoCargo.RECARGO_MORA))
                .thenReturn(List.of(primero, segundo));
        CuentaRespuesta cuentaActualizada = org.mockito.Mockito.mock(CuentaRespuesta.class);
        when(cuentaService.obtener(1L, true)).thenReturn(cuentaActualizada);

        ExoneracionMoraRespuesta respuesta = cargoService.exonerarMora(1L, motivo(), "admin", true);

        assertEquals(2, respuesta.cantidadCargosExonerados());
        assertMonto("300.50", respuesta.montoExonerado());
        assertEquals(cuentaActualizada, respuesta.cuenta());
        assertMonto("899.50", cuenta.getSaldoActual());
        assertTrue(primero.estaReversado());
        assertTrue(segundo.estaReversado());
        assertEquals("Exoneración autorizada", primero.getMotivoReverso());
        assertEquals("admin", segundo.getUsuarioReverso());
    }

    @Test
    @DisplayName("Exoneración sin recargos pendientes lanza 422")
    void exonerarMoraSinRecargos() {
        when(cargoRepository.findByCuentaIdAndTipoCargoAndReversadoFalse(1L, TipoCargo.RECARGO_MORA))
                .thenReturn(List.of());

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> cargoService.exonerarMora(1L, motivo(), "admin", false));

        assertEquals("La cuenta no tiene recargos por mora pendientes de exonerar", excepcion.getMessage());
        assertMonto("900.00", cuenta.getSaldoActual());
    }

    @Test
    @DisplayName("La exoneración no deja el saldo negativo")
    void exonerarMoraDejaSaldoNegativo() {
        cuenta.setSaldoActual(new BigDecimal("100.00"));
        Cargo recargo = cargo(1L, TipoCargo.RECARGO_MORA, "150.00");
        when(cargoRepository.findByCuentaIdAndTipoCargoAndReversadoFalse(1L, TipoCargo.RECARGO_MORA))
                .thenReturn(List.of(recargo));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> cargoService.exonerarMora(1L, motivo(), "admin", false));

        assertEquals("No se puede exonerar la mora: el saldo quedaría en Q -50.00. "
                + "Reverse primero el pago que la cubrió", excepcion.getMessage());
        assertFalse(recargo.estaReversado());
    }
}
