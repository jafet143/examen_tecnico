package gt.com.chn.tarjetas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.com.chn.tarjetas.dto.CancelacionCuentaPeticion;
import gt.com.chn.tarjetas.dto.CuentaRespuesta;
import gt.com.chn.tarjetas.dto.TarjetaRespuesta;
import gt.com.chn.tarjetas.entity.Cliente;
import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.entity.EstadoCuenta;
import gt.com.chn.tarjetas.entity.EstadoPago;
import gt.com.chn.tarjetas.entity.EstadoPagoCuenta;
import gt.com.chn.tarjetas.entity.EstadoTarjeta;
import gt.com.chn.tarjetas.entity.MotivoBloqueo;
import gt.com.chn.tarjetas.entity.MotivoCancelacion;
import gt.com.chn.tarjetas.entity.MotivoReposicion;
import gt.com.chn.tarjetas.entity.SolicitudTarjeta;
import gt.com.chn.tarjetas.entity.TarjetaCredito;
import gt.com.chn.tarjetas.entity.TipoCargo;
import gt.com.chn.tarjetas.entity.TipoTarjeta;
import gt.com.chn.tarjetas.exception.RecursoNoEncontradoException;
import gt.com.chn.tarjetas.exception.ReglaNegocioException;
import gt.com.chn.tarjetas.repository.CargoRepository;
import gt.com.chn.tarjetas.repository.ClienteRepository;
import gt.com.chn.tarjetas.repository.CuentaCreditoRepository;
import gt.com.chn.tarjetas.repository.TarjetaCreditoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    @Mock
    private CuentaCreditoRepository cuentaCreditoRepository;
    @Mock
    private TarjetaCreditoRepository tarjetaCreditoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private CargoRepository cargoRepository;
    @Mock
    private EstadoPagoService estadoPagoService;
    @Mock
    private ProcesoInteresesService procesoIntereses;

    private CuentaService cuentaService;
    private Cliente cliente;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-09T18:00:00Z"), ZoneId.of("America/Guatemala"));
        cuentaService = new CuentaService(
                cuentaCreditoRepository, tarjetaCreditoRepository, clienteRepository, cargoRepository,
                estadoPagoService, procesoIntereses, reloj);
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Ana");
        cliente.setCodigoCliente("10000001");
        cliente.setApellido("Morales");
    }

    private CuentaCredito cuenta(long id, String numero) {
        SolicitudTarjeta solicitud = new SolicitudTarjeta();
        solicitud.setId(id * 10);
        CuentaCredito cuenta = new CuentaCredito();
        cuenta.setId(id);
        cuenta.setNumeroCuenta(numero);
        cuenta.setCliente(cliente);
        cuenta.setSolicitud(solicitud);
        cuenta.setTipoTarjeta(TipoTarjeta.ORO);
        cuenta.setLimiteCredito(new BigDecimal("15000.00"));
        cuenta.setSaldoActual(new BigDecimal("1550.00"));
        cuenta.setFechaApertura(LocalDateTime.of(2026, 8, 1, 9, 0));
        return cuenta;
    }

    private TarjetaCredito tarjeta(long id, CuentaCredito cuenta, String numero, LocalDateTime emision) {
        TarjetaCredito tarjeta = new TarjetaCredito();
        tarjeta.setId(id);
        tarjeta.setCuenta(cuenta);
        tarjeta.setNumeroTarjeta(numero);
        tarjeta.setFechaEmision(emision);
        tarjeta.setFechaVencimiento(LocalDate.of(2031, 8, 31));
        return tarjeta;
    }

    private EstadoPagoCuenta alDia() {
        return new EstadoPagoCuenta(EstadoPago.AL_DIA, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 25),
                LocalDate.of(2026, 11, 5), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, 0);
    }

    private EstadoPagoCuenta conEstado(EstadoPago estado) {
        return new EstadoPagoCuenta(estado, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 25),
                LocalDate.of(2026, 11, 5), new BigDecimal("1000.00"), new BigDecimal("100.00"),
                new BigDecimal("100.00"), new BigDecimal("1000.00"), new BigDecimal("1000.00"),
                estado == EstadoPago.EN_MORA ? new BigDecimal("100.00") : BigDecimal.ZERO, 5);
    }

    @Test
    @DisplayName("Abrir cuenta: límite igual al monto aprobado, saldo cero y el número recibido")
    void abrirCuenta() {
        SolicitudTarjeta solicitud = new SolicitudTarjeta();
        solicitud.setCliente(cliente);
        solicitud.setTipoTarjeta(TipoTarjeta.PLATINO);
        solicitud.setMontoAprobado(new BigDecimal("20000.00"));
        when(cuentaCreditoRepository.save(org.mockito.ArgumentMatchers.any(CuentaCredito.class)))
                .thenAnswer(i -> i.getArgument(0));

        CuentaCredito cuenta = cuentaService.abrirCuenta(solicitud, "4300987654321014");

        assertEquals("4300987654321014", cuenta.getNumeroCuenta());
        assertEquals(new BigDecimal("20000.00"), cuenta.getLimiteCredito());
        assertEquals(0, BigDecimal.ZERO.compareTo(cuenta.getSaldoActual()));
        assertEquals(TipoTarjeta.PLATINO, cuenta.getTipoTarjeta());
        assertEquals(LocalDateTime.of(2026, 10, 9, 12, 0), cuenta.getFechaApertura());
    }

    @Test
    @DisplayName("Listar cuentas arma la tarjeta vigente y las reposiciones con una sola consulta de tarjetas")
    void listarCuentasEnLote() {
        CuentaCredito conReposicion = cuenta(1L, "4200123456789015");
        CuentaCredito sinReposicion = cuenta(2L, "4100456789012015");
        TarjetaCredito vieja = tarjeta(10L, conReposicion, "4200123456789015", LocalDateTime.of(2026, 8, 1, 9, 0));
        vieja.reemplazar(MotivoReposicion.ROBO, null, LocalDateTime.of(2026, 9, 1, 9, 0), "operador", true, null);
        TarjetaCredito nueva = tarjeta(11L, conReposicion, "4200555123409875", LocalDateTime.of(2026, 9, 1, 9, 0));
        nueva.setTarjetaAnterior(vieja);
        TarjetaCredito unica = tarjeta(12L, sinReposicion, "4100456789012015", LocalDateTime.of(2026, 8, 2, 9, 0));
        when(cuentaCreditoRepository.listarConCliente(null)).thenReturn(List.of(conReposicion, sinReposicion));
        when(estadoPagoService.calcular(List.of(conReposicion, sinReposicion)))
                .thenReturn(Map.of(1L, alDia(), 2L, alDia()));
        when(tarjetaCreditoRepository.listarPorCuentas(List.of(1L, 2L))).thenReturn(List.of(nueva, unica, vieja));

        List<CuentaRespuesta> respuestas = cuentaService.listarTodas(null, false);

        assertEquals(2, respuestas.size());
        CuentaRespuesta primera = respuestas.get(0);
        assertEquals("**** **** **** 9015", primera.numeroCuentaEnmascarado());
        assertEquals(1, primera.cantidadReposiciones());
        assertEquals(11L, primera.tarjetaVigente().id());
        assertEquals("**** **** **** 9875", primera.tarjetaVigente().numeroTarjetaEnmascarado());
        assertEquals(EstadoTarjeta.ACTIVA, primera.tarjetaVigente().estadoTarjeta());
        assertEquals(0, new BigDecimal("13450.00").compareTo(primera.creditoDisponible()));
        assertEquals("Al día", primera.descripcionEstadoPago());
        assertEquals(EstadoCuenta.ACTIVA, primera.estadoCuenta());
        assertEquals(0, respuestas.get(1).cantidadReposiciones());
        assertEquals(12L, respuestas.get(1).tarjetaVigente().id());
        verify(tarjetaCreditoRepository, times(1)).listarPorCuentas(List.of(1L, 2L));
    }

    @Test
    @DisplayName("Los números completos de la cuenta y de su tarjeta vigente solo se incluyen con datos sensibles")
    void numerosCompletosSegunPermiso() {
        CuentaCredito cuenta = cuenta(1L, "4200123456789015");
        TarjetaCredito vigente = tarjeta(11L, cuenta, "4200555123409875", LocalDateTime.of(2026, 9, 1, 9, 0));
        when(cuentaCreditoRepository.buscarDetallePorId(1L)).thenReturn(Optional.of(cuenta));
        when(estadoPagoService.calcular(List.of(cuenta))).thenReturn(Map.of(1L, alDia()));
        when(tarjetaCreditoRepository.listarPorCuentas(List.of(1L))).thenReturn(List.of(vigente));

        CuentaRespuesta sinPermiso = cuentaService.obtener(1L, false);
        CuentaRespuesta conPermiso = cuentaService.obtener(1L, true);

        assertEquals("10000001", sinPermiso.codigoCliente());
        assertNull(sinPermiso.numeroCuentaCompleto());
        assertNull(sinPermiso.tarjetaVigente().numeroCuentaCompleto());
        assertNull(sinPermiso.tarjetaVigente().numeroTarjetaCompleto());
        assertEquals("4200123456789015", conPermiso.numeroCuentaCompleto());
        assertEquals("4200123456789015", conPermiso.tarjetaVigente().numeroCuentaCompleto());
        assertEquals("4200555123409875", conPermiso.tarjetaVigente().numeroTarjetaCompleto());
    }

    @Test
    @DisplayName("El historial de tarjetas de la cuenta incluye las reemplazadas con su motivo")
    void historialDeTarjetas() {
        CuentaCredito cuenta = cuenta(1L, "4200123456789015");
        TarjetaCredito vieja = tarjeta(10L, cuenta, "4200123456789015", LocalDateTime.of(2026, 8, 1, 9, 0));
        vieja.reemplazar(MotivoReposicion.DETERIORO, null, LocalDateTime.of(2026, 9, 1, 9, 0), "operador", null, true);
        TarjetaCredito nueva = tarjeta(11L, cuenta, "4200555123409875", LocalDateTime.of(2026, 9, 1, 9, 0));
        nueva.setTarjetaAnterior(vieja);
        when(cuentaCreditoRepository.existsById(1L)).thenReturn(true);
        when(tarjetaCreditoRepository.listarPorCuentas(List.of(1L))).thenReturn(List.of(nueva, vieja));

        List<TarjetaRespuesta> historial = cuentaService.listarTarjetas(1L, false);

        assertEquals(List.of(11L, 10L), historial.stream().map(TarjetaRespuesta::id).toList());
        assertEquals(10L, historial.get(0).idTarjetaAnterior());
        assertNull(historial.get(1).idTarjetaAnterior());
        assertEquals("Deterioro", historial.get(1).descripcionMotivoReposicion());
        assertEquals(EstadoTarjeta.REEMPLAZADA, historial.get(1).estadoTarjeta());
    }

    @Test
    @DisplayName("Una cuenta inexistente lanza 404 al consultarla o al listar su historial")
    void cuentaInexistente() {
        when(cuentaCreditoRepository.buscarDetallePorId(9L)).thenReturn(Optional.empty());
        when(cuentaCreditoRepository.existsById(9L)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> cuentaService.obtener(9L, false));
        assertThrows(RecursoNoEncontradoException.class, () -> cuentaService.listarTarjetas(9L, false));
    }

    @Test
    @DisplayName("Listar las cuentas de un cliente inexistente lanza 404")
    void clienteInexistente() {
        when(clienteRepository.existsById(9L)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> cuentaService.listarPorCliente(9L, false));
    }

    @Test
    @DisplayName("tieneRecargosMoraPendientes sale de una sola consulta de cargos para todas las cuentas")
    void recargosMoraPendientesEnLote() {
        CuentaCredito conRecargos = cuenta(1L, "4200123456789015");
        CuentaCredito sinRecargos = cuenta(2L, "4100456789012015");
        when(cuentaCreditoRepository.listarConCliente(EstadoCuenta.ACTIVA)).thenReturn(List.of(conRecargos, sinRecargos));
        when(estadoPagoService.calcular(List.of(conRecargos, sinRecargos)))
                .thenReturn(Map.of(1L, alDia(), 2L, alDia()));
        when(tarjetaCreditoRepository.listarPorCuentas(List.of(1L, 2L))).thenReturn(List.of(
                tarjeta(10L, conRecargos, "4200123456789015", LocalDateTime.of(2026, 8, 1, 9, 0)),
                tarjeta(12L, sinRecargos, "4100456789012015", LocalDateTime.of(2026, 8, 2, 9, 0))));
        when(cargoRepository.idsCuentasConCargoVigente(List.of(1L, 2L), TipoCargo.RECARGO_MORA)).thenReturn(Set.of(1L));

        List<CuentaRespuesta> respuestas = cuentaService.listarTodas(EstadoCuenta.ACTIVA, false);

        assertTrue(respuestas.get(0).tieneRecargosMoraPendientes());
        assertFalse(respuestas.get(1).tieneRecargosMoraPendientes());
        verify(cargoRepository, times(1)).idsCuentasConCargoVigente(List.of(1L, 2L), TipoCargo.RECARGO_MORA);
    }

    private void prepararCancelacion(CuentaCredito cuenta, TarjetaCredito vigente, EstadoPago estado) {
        when(cuentaCreditoRepository.buscarPorIdConBloqueo(1L)).thenReturn(Optional.of(cuenta));
        when(estadoPagoService.calcular(cuenta)).thenReturn(conEstado(estado));
    }

    @Test
    @DisplayName("CA-04: cancelar una cuenta al día y con saldo cero cancela cuenta y tarjeta (aunque esté bloqueada) sin borrar nada")
    void cancelarCuenta() {
        CuentaCredito cuenta = cuenta(1L, "4100456789012015");
        cuenta.setSaldoActual(BigDecimal.ZERO);
        TarjetaCredito vigente = tarjeta(12L, cuenta, "4100456789012015", LocalDateTime.of(2026, 8, 2, 9, 0));
        vigente.bloquear(MotivoBloqueo.SOLICITUD_CLIENTE, null, LocalDateTime.of(2026, 10, 1, 9, 0), "operador");
        prepararCancelacion(cuenta, vigente, EstadoPago.AL_DIA);
        when(tarjetaCreditoRepository.buscarVigentePorCuenta(1L)).thenReturn(Optional.of(vigente));
        when(estadoPagoService.calcular(List.of(cuenta))).thenReturn(Map.of(1L, alDia()));
        when(tarjetaCreditoRepository.listarPorCuentas(List.of(1L))).thenReturn(List.of(vigente));

        CuentaRespuesta respuesta = cuentaService.cancelar(1L,
                new CancelacionCuentaPeticion(MotivoCancelacion.SOLICITUD_CLIENTE, "  Ya no la usa "), "admin", false);

        assertEquals(EstadoCuenta.CANCELADA, respuesta.estadoCuenta());
        assertEquals("Cancelada", respuesta.descripcionEstadoCuenta());
        assertEquals(MotivoCancelacion.SOLICITUD_CLIENTE, respuesta.motivoCancelacion());
        assertEquals("Solicitud del cliente", respuesta.descripcionMotivoCancelacion());
        assertEquals("Ya no la usa", respuesta.comentarioCancelacion());
        assertEquals("admin", respuesta.usuarioCancelacion());
        assertEquals(LocalDateTime.of(2026, 10, 9, 12, 0), respuesta.fechaCancelacion());
        assertEquals(EstadoTarjeta.CANCELADA, respuesta.tarjetaVigente().estadoTarjeta());
        assertFalse(respuesta.tarjetaVigente().puedeDesbloquearse());
        verify(procesoIntereses).generarInteresesPendientes(cuenta);
    }

    @Test
    @DisplayName("CA-01: una cuenta ya cancelada responde 422 con la fecha de cancelación")
    void cancelarCuentaYaCancelada() {
        CuentaCredito cuenta = cuenta(1L, "4100456789012015");
        cuenta.cancelar(MotivoCancelacion.DECISION_BANCO, null, LocalDateTime.of(2026, 7, 4, 10, 0), "admin");
        when(cuentaCreditoRepository.buscarPorIdConBloqueo(1L)).thenReturn(Optional.of(cuenta));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> cuentaService.cancelar(1L,
                new CancelacionCuentaPeticion(MotivoCancelacion.SOLICITUD_CLIENTE, null), "admin", false));

        assertEquals("La cuenta ya fue cancelada el 04/07/2026", error.getMessage());
    }

    @Test
    @DisplayName("CA-02: una cuenta con pago pendiente o en mora no se puede cancelar")
    void cancelarCuentaQueNoEstaAlDia() {
        CuentaCredito cuenta = cuenta(1L, "4100456789012015");
        cuenta.setSaldoActual(BigDecimal.ZERO);
        prepararCancelacion(cuenta, null, EstadoPago.EN_MORA);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> cuentaService.cancelar(1L,
                new CancelacionCuentaPeticion(MotivoCancelacion.SOLICITUD_CLIENTE, null), "admin", false));

        assertEquals("Para cancelar, la cuenta debe estar al día en sus pagos", error.getMessage());
        assertFalse(cuenta.estaCancelada());
    }

    @Test
    @DisplayName("CA-03: una cuenta al día pero con saldo no se puede cancelar")
    void cancelarCuentaConSaldo() {
        CuentaCredito cuenta = cuenta(1L, "4100456789012015");
        cuenta.setSaldoActual(new BigDecimal("275.00"));
        prepararCancelacion(cuenta, null, EstadoPago.AL_DIA);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> cuentaService.cancelar(1L,
                new CancelacionCuentaPeticion(MotivoCancelacion.SOLICITUD_CLIENTE, null), "admin", false));

        assertEquals("Para cancelar, la cuenta debe tener saldo Q 0.00. Saldo actual: Q 275.00", error.getMessage());
        assertFalse(cuenta.estaCancelada());
        verify(tarjetaCreditoRepository, never()).buscarVigentePorCuenta(1L);
    }

    @Test
    @DisplayName("Cancelar una cuenta inexistente lanza 404")
    void cancelarCuentaInexistente() {
        when(cuentaCreditoRepository.buscarPorIdConBloqueo(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> cuentaService.cancelar(9L,
                new CancelacionCuentaPeticion(MotivoCancelacion.SOLICITUD_CLIENTE, null), "admin", false));
    }
}
