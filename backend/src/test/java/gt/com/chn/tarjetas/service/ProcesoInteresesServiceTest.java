package gt.com.chn.tarjetas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.com.chn.tarjetas.entity.Cargo;
import gt.com.chn.tarjetas.entity.Consumo;
import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.entity.MotivoCancelacion;
import gt.com.chn.tarjetas.entity.Pago;
import gt.com.chn.tarjetas.entity.TipoCargo;
import gt.com.chn.tarjetas.repository.CargoRepository;
import gt.com.chn.tarjetas.repository.ConsumoRepository;
import gt.com.chn.tarjetas.repository.CuentaCreditoRepository;
import gt.com.chn.tarjetas.repository.PagoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class ProcesoInteresesServiceTest {
    @Mock
    private CuentaCreditoRepository cuentaCreditoRepository;
    @Mock
    private ConsumoRepository consumoRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private CargoRepository cargoRepository;
    @Mock
    private PlatformTransactionManager administradorTransacciones;

    private ProcesoInteresesService proceso;
    private CuentaCredito cuenta;
    private final List<Consumo> consumos = new ArrayList<>();
    private final List<Pago> pagos = new ArrayList<>();
    private final List<Cargo> cargos = new ArrayList<>();

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-09T18:00:00Z"), ZoneId.of("America/Guatemala"));
        proceso = new ProcesoInteresesService(cuentaCreditoRepository, consumoRepository, pagoRepository,
                cargoRepository, new CalculadoraEstadoPago(), administradorTransacciones, reloj);
        cuenta = new CuentaCredito();
        cuenta.setId(1L);
        cuenta.setLimiteCredito(new BigDecimal("1000.00"));
        cuenta.setSaldoActual(new BigDecimal("1000.00"));
        cuenta.setFechaApertura(LocalDateTime.of(2026, 8, 1, 9, 0));
        org.mockito.Mockito.lenient().when(consumoRepository.findByCuentaId(1L)).thenReturn(consumos);
        org.mockito.Mockito.lenient().when(pagoRepository.findByCuentaId(1L)).thenReturn(pagos);
        org.mockito.Mockito.lenient().when(cargoRepository.findByCuentaId(1L)).thenReturn(cargos);
        org.mockito.Mockito.lenient().when(cargoRepository.save(any(Cargo.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void consumo(String monto, LocalDateTime fecha) {
        Consumo consumo = new Consumo();
        consumo.setCuenta(cuenta);
        consumo.setMonto(new BigDecimal(monto));
        consumo.setFecha(fecha);
        consumos.add(consumo);
    }

    private Pago pago(String monto, LocalDateTime fecha) {
        Pago pago = new Pago();
        pago.setCuenta(cuenta);
        pago.setMonto(new BigDecimal(monto));
        pago.setFecha(fecha);
        pagos.add(pago);
        return pago;
    }

    private List<Cargo> cargosGuardados(int cantidad) {
        ArgumentCaptor<Cargo> captor = ArgumentCaptor.forClass(Cargo.class);
        verify(cargoRepository, times(cantidad)).save(captor.capture());
        return captor.getAllValues();
    }

    private void assertMonto(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " pero fue " + real);
    }

    @Test
    @DisplayName("Solo el pago mínimo: el interés es 5 % de lo no pagado al día 25, fechado el día 26 a las 00:00")
    void soloElMinimoGeneraInteres() {
        consumo("1000.00", LocalDateTime.of(2026, 8, 10, 10, 0));
        pago("100.00", LocalDateTime.of(2026, 9, 10, 10, 0));
        cuenta.setSaldoActual(new BigDecimal("900.00"));

        int generados = proceso.generarInteresesPendientes(cuenta);

        assertEquals(1, generados);
        Cargo interes = cargosGuardados(1).get(0);
        assertEquals(TipoCargo.INTERES, interes.getTipoCargo());
        assertMonto("45.00", interes.getMonto());
        assertEquals(LocalDateTime.of(2026, 9, 26, 0, 0), interes.getFecha());
        assertEquals(LocalDate.of(2026, 9, 5), interes.getCicloCorte());
        assertEquals("sistema", interes.getUsuarioRegistro());
        assertEquals("Intereses del ciclo con corte 05/09/2026 (5 % sobre Q 900.00)", interes.getDescripcion());
        assertMonto("945.00", cuenta.getSaldoActual());
    }

    @Test
    @DisplayName("Pagar el contado completo antes del día 25 no genera interés")
    void contadoCompletoSinInteres() {
        consumo("1000.00", LocalDateTime.of(2026, 8, 10, 10, 0));
        pago("1000.00", LocalDateTime.of(2026, 9, 25, 23, 59, 59));
        cuenta.setSaldoActual(BigDecimal.ZERO);

        assertEquals(0, proceso.generarInteresesPendientes(cuenta));

        verify(cargoRepository, never()).save(any());
        assertMonto("0", cuenta.getSaldoActual());
    }

    @Test
    @DisplayName("Un pago posterior al día 25 no reduce la base del interés")
    void pagoTardioNoReduceLaBase() {
        consumo("1000.00", LocalDateTime.of(2026, 8, 10, 10, 0));
        pago("1000.00", LocalDateTime.of(2026, 9, 26, 0, 0));

        proceso.generarInteresesPendientes(cuenta);

        assertMonto("50.00", cargosGuardados(1).get(0).getMonto());
    }

    @Test
    @DisplayName("El interés se redondea al centavo con HALF_UP")
    void redondeoHalfUp() {
        consumo("100.10", LocalDateTime.of(2026, 8, 10, 10, 0));

        proceso.generarInteresesPendientes(cuenta);

        assertMonto("5.01", cargosGuardados(1).get(0).getMonto());
    }

    @Test
    @DisplayName("Un consumo reversado no forma parte de la base del interés")
    void consumoReversadoNoCuenta() {
        consumo("1000.00", LocalDateTime.of(2026, 8, 10, 10, 0));
        consumos.get(0).reversar("Cargo duplicado por el comercio", LocalDateTime.of(2026, 8, 11, 9, 0), "admin");

        assertEquals(0, proceso.generarInteresesPendientes(cuenta));
    }

    @Test
    @DisplayName("Idempotencia: un ciclo con interés ya generado no se repite")
    void noRegeneraElInteresDeUnCicloYaCobrado() {
        consumo("1000.00", LocalDateTime.of(2026, 8, 10, 10, 0));
        Cargo existente = new Cargo();
        existente.setTipoCargo(TipoCargo.INTERES);
        existente.setMonto(new BigDecimal("50.00"));
        existente.setFecha(LocalDateTime.of(2026, 9, 26, 0, 0));
        existente.setCicloCorte(LocalDate.of(2026, 9, 5));
        cargos.add(existente);

        assertEquals(0, proceso.generarInteresesPendientes(cuenta));

        verify(cargoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Idempotencia: un interés devuelto (reversado) no se vuelve a generar")
    void interesDevueltoNoSeRegenera() {
        consumo("1000.00", LocalDateTime.of(2026, 8, 10, 10, 0));
        Cargo devuelto = new Cargo();
        devuelto.setTipoCargo(TipoCargo.INTERES);
        devuelto.setMonto(new BigDecimal("50.00"));
        devuelto.setFecha(LocalDateTime.of(2026, 9, 26, 0, 0));
        devuelto.setCicloCorte(LocalDate.of(2026, 9, 5));
        devuelto.reversar("Devolución de intereses", LocalDateTime.of(2026, 10, 1, 9, 0), "admin");
        cargos.add(devuelto);
        cuenta.setSaldoActual(new BigDecimal("1000.00"));

        assertEquals(0, proceso.generarInteresesPendientes(cuenta));
        assertEquals(0, proceso.generarInteresesPendientes(cuenta));

        verify(cargoRepository, never()).save(any());
        assertMonto("1000.00", cuenta.getSaldoActual());
    }

    @Test
    @DisplayName("Los ciclos anteriores a la apertura de la cuenta no generan interés")
    void ciclosAnterioresALaAperturaNoGeneranInteres() {
        cuenta.setFechaApertura(LocalDateTime.of(2026, 9, 20, 9, 0));
        consumo("1000.00", LocalDateTime.of(2026, 9, 21, 10, 0));

        assertEquals(0, proceso.generarInteresesPendientes(cuenta));

        verify(cargoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una cuenta cancelada no genera interés y ni siquiera se consultan sus movimientos")
    void cuentaCanceladaSinInteres() {
        consumo("1000.00", LocalDateTime.of(2026, 8, 10, 10, 0));
        cuenta.cancelar(MotivoCancelacion.DECISION_BANCO, null, LocalDateTime.of(2026, 10, 1, 9, 0), "admin");

        assertEquals(0, proceso.generarInteresesPendientes(cuenta));

        verifyNoInteractions(consumoRepository, pagoRepository, cargoRepository);
    }

    @Test
    @DisplayName("Varios ciclos vencidos se procesan en orden y el interés de uno entra en la base del siguiente")
    void ciclosEncadenados() {
        cuenta.setFechaApertura(LocalDateTime.of(2026, 7, 1, 9, 0));
        consumo("1000.00", LocalDateTime.of(2026, 7, 10, 10, 0));

        assertEquals(2, proceso.generarInteresesPendientes(cuenta));

        List<Cargo> guardados = cargosGuardados(2);
        assertEquals(LocalDate.of(2026, 8, 5), guardados.get(0).getCicloCorte());
        assertMonto("50.00", guardados.get(0).getMonto());
        assertEquals(LocalDate.of(2026, 9, 5), guardados.get(1).getCicloCorte());
        assertMonto("52.50", guardados.get(1).getMonto());
        assertMonto("1102.50", cuenta.getSaldoActual());
    }

    @Test
    @DisplayName("Un interés puede sobregirar el límite de crédito")
    void interesPuedeSobregirar() {
        consumo("1000.00", LocalDateTime.of(2026, 8, 10, 10, 0));

        proceso.generarInteresesPendientes(cuenta);

        assertMonto("1050.00", cuenta.getSaldoActual());
        assertTrue(cuenta.getSaldoActual().compareTo(cuenta.getLimiteCredito()) > 0);
        assertMonto("0", cuenta.obtenerCreditoDisponible());
    }

    @Test
    @DisplayName("Al arrancar cada cuenta activa se procesa bloqueada y un error en una no detiene a las demás")
    void procesoParaTodasLasCuentasActivas() {
        CuentaCredito otra = new CuentaCredito();
        otra.setId(2L);
        otra.setSaldoActual(new BigDecimal("500.00"));
        otra.setFechaApertura(LocalDateTime.of(2026, 8, 1, 9, 0));
        when(cuentaCreditoRepository.listarIdsActivas()).thenReturn(List.of(2L, 1L));
        when(cuentaCreditoRepository.buscarPorIdConBloqueo(2L)).thenThrow(new IllegalStateException("falla simulada"));
        when(cuentaCreditoRepository.buscarPorIdConBloqueo(1L)).thenReturn(Optional.of(cuenta));
        consumo("1000.00", LocalDateTime.of(2026, 8, 10, 10, 0));

        proceso.generarParaCuentasActivas();

        assertMonto("1050.00", cuenta.getSaldoActual());
        verify(cuentaCreditoRepository).buscarPorIdConBloqueo(1L);
    }
}
