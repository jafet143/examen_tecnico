package gt.com.chn.tarjetas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.com.chn.tarjetas.dto.AprobacionPeticion;
import gt.com.chn.tarjetas.dto.RechazoPeticion;
import gt.com.chn.tarjetas.dto.SolicitudPeticion;
import gt.com.chn.tarjetas.dto.SolicitudRespuesta;
import gt.com.chn.tarjetas.entity.Cliente;
import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.entity.EstadoCuenta;
import gt.com.chn.tarjetas.entity.EstadoSolicitud;
import gt.com.chn.tarjetas.entity.EstadoTarjeta;
import gt.com.chn.tarjetas.entity.SolicitudTarjeta;
import gt.com.chn.tarjetas.entity.TarjetaCredito;
import gt.com.chn.tarjetas.entity.TipoTarjeta;
import gt.com.chn.tarjetas.exception.RecursoNoEncontradoException;
import gt.com.chn.tarjetas.exception.ReglaNegocioException;
import gt.com.chn.tarjetas.repository.CargoRepository;
import gt.com.chn.tarjetas.repository.ClienteRepository;
import gt.com.chn.tarjetas.repository.CuentaCreditoRepository;
import gt.com.chn.tarjetas.repository.SolicitudTarjetaRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SolicitudServiceTest {

    private static final LocalDate INICIO_LABORAL_ANTIGUO = LocalDate.of(2020, 1, 15);

    @Mock
    private SolicitudTarjetaRepository solicitudTarjetaRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private CuentaCreditoRepository cuentaCreditoRepository;
    @Mock
    private TarjetaCreditoRepository tarjetaCreditoRepository;
    @Mock
    private EstadoPagoService estadoPagoService;
    @Mock
    private CargoRepository cargoRepository;
    @Mock
    private CargoService cargoService;
    @Mock
    private ProcesoInteresesService procesoIntereses;

    private SolicitudService solicitudService;
    private Cliente cliente;

    @BeforeEach
    void prepararServicio() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-09T18:00:00Z"), ZoneId.of("America/Guatemala"));
        TarjetaService tarjetaService = new TarjetaService(
                tarjetaCreditoRepository, cuentaCreditoRepository, new GeneradorNumeroTarjeta(), cargoService,
                procesoIntereses, reloj);
        CuentaService cuentaService = new CuentaService(
                cuentaCreditoRepository, tarjetaCreditoRepository, clienteRepository, cargoRepository,
                estadoPagoService, procesoIntereses, reloj);
        solicitudService = new SolicitudService(solicitudTarjetaRepository, clienteRepository,
                cuentaCreditoRepository, tarjetaCreditoRepository, cuentaService, tarjetaService,
                estadoPagoService, reloj);

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Ana");
        cliente.setApellido("Morales");
        cliente.setNumeroIdentificacion("2456789010101");
    }

    private SolicitudTarjeta crearSolicitud(EstadoSolicitud estado) {
        SolicitudTarjeta solicitud = new SolicitudTarjeta();
        solicitud.setId(10L);
        solicitud.setCliente(cliente);
        solicitud.setTipoTarjeta(TipoTarjeta.ORO);
        solicitud.setIngresoMensual(new BigDecimal("12000.00"));
        solicitud.setNombreEmpleador("Banco Industrial");
        solicitud.setFechaInicioLaboral(INICIO_LABORAL_ANTIGUO);
        solicitud.setFechaSolicitud(LocalDateTime.of(2026, 10, 7, 10, 0));
        solicitud.setEstado(estado);
        return solicitud;
    }

    private SolicitudPeticion peticion(TipoTarjeta tipo, String ingreso, LocalDate inicioLaboral) {
        return new SolicitudPeticion(1L, tipo, new BigDecimal(ingreso), "  Banco Industrial ", inicioLaboral);
    }

    private SolicitudPeticion peticion(TipoTarjeta tipo, String ingreso) {
        return peticion(tipo, ingreso, INICIO_LABORAL_ANTIGUO);
    }

    private CuentaCredito cuentaExistente(TipoTarjeta tipo, String numero) {
        CuentaCredito cuenta = new CuentaCredito();
        cuenta.setId(30L);
        cuenta.setNumeroCuenta(numero);
        cuenta.setTipoTarjeta(tipo);
        return cuenta;
    }

    private void simularGuardadoDeCuentaYTarjeta() {
        when(cuentaCreditoRepository.save(any(CuentaCredito.class))).thenAnswer(invocacion -> {
            CuentaCredito cuenta = invocacion.getArgument(0);
            cuenta.setId(55L);
            return cuenta;
        });
        when(tarjetaCreditoRepository.save(any(TarjetaCredito.class))).thenAnswer(invocacion -> {
            TarjetaCredito tarjeta = invocacion.getArgument(0);
            tarjeta.setId(77L);
            return tarjeta;
        });
    }

    @Test
    @DisplayName("Crear solicitud: queda PENDIENTE con fecha del servidor y guarda empleador y fecha de inicio laboral")
    void crearSolicitudQuedaPendiente() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(solicitudTarjetaRepository.existsByClienteIdAndTipoTarjetaAndEstado(
                1L, TipoTarjeta.ORO, EstadoSolicitud.PENDIENTE)).thenReturn(false);
        when(solicitudTarjetaRepository.save(any(SolicitudTarjeta.class))).thenAnswer(i -> i.getArgument(0));

        SolicitudRespuesta respuesta = solicitudService.crear(peticion(TipoTarjeta.ORO, "12000.00"));

        assertEquals(EstadoSolicitud.PENDIENTE, respuesta.estado());
        assertNotNull(respuesta.fechaSolicitud());
        assertEquals("Banco Industrial", respuesta.nombreEmpleador());
        assertEquals(INICIO_LABORAL_ANTIGUO, respuesta.fechaInicioLaboral());
        assertEquals(80, respuesta.mesesAntiguedadLaboral());
        assertNull(respuesta.idCuenta());
        assertNull(respuesta.numeroCuentaEnmascarado());
        assertNull(respuesta.idTarjeta());
    }

    @Test
    @DisplayName("Crear solicitud con cliente inexistente lanza 404")
    void crearSolicitudClienteInexistente() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> solicitudService.crear(
                new SolicitudPeticion(99L, TipoTarjeta.ORO, new BigDecimal("1000.00"), "Empresa", INICIO_LABORAL_ANTIGUO)));
    }

    @Test
    @DisplayName("Crear solicitud con otra pendiente del mismo tipo lanza regla de negocio")
    void crearSolicitudPendienteDuplicada() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(solicitudTarjetaRepository.existsByClienteIdAndTipoTarjetaAndEstado(
                1L, TipoTarjeta.ORO, EstadoSolicitud.PENDIENTE)).thenReturn(true);

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> solicitudService.crear(peticion(TipoTarjeta.ORO, "12000.00")));

        assertEquals("El cliente ya tiene una solicitud pendiente de tarjeta Oro", excepcion.getMessage());
        verify(solicitudTarjetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("RC-06: 11 meses de antigüedad laboral lanza 422 con el mensaje del contrato")
    void crearSolicitudConOnceMesesDeAntiguedad() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> solicitudService.crear(peticion(TipoTarjeta.ORO, "12000.00", LocalDate.of(2025, 11, 9))));

        assertEquals("Se requiere al menos 1 año de estabilidad laboral. Antigüedad actual: 11 meses",
                excepcion.getMessage());
        verify(solicitudTarjetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("RC-06: exactamente 12 meses de antigüedad laboral es válido")
    void crearSolicitudConDoceMesesDeAntiguedad() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(solicitudTarjetaRepository.save(any(SolicitudTarjeta.class))).thenAnswer(i -> i.getArgument(0));

        SolicitudRespuesta respuesta = solicitudService.crear(
                peticion(TipoTarjeta.ORO, "12000.00", LocalDate.of(2025, 10, 9)));

        assertEquals(EstadoSolicitud.PENDIENTE, respuesta.estado());
        assertEquals(12, respuesta.mesesAntiguedadLaboral());
    }

    @Test
    @DisplayName("RC-06 se valida antes que RC-01: con ambas fallas se informa la estabilidad laboral")
    void estabilidadLaboralSeValidaPrimero() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> solicitudService.crear(peticion(TipoTarjeta.BLACK, "100.00", LocalDate.of(2026, 7, 9))));

        assertTrue(excepcion.getMessage().startsWith("Se requiere al menos 1 año de estabilidad laboral"));
    }

    @Test
    @DisplayName("RC-07: un cliente con cuenta del mismo tipo no puede solicitar otra y se informa el número enmascarado")
    void crearSolicitudConCuentaDelMismoTipo() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(cuentaCreditoRepository.findByClienteIdAndTipoTarjetaAndEstadoCuenta(1L, TipoTarjeta.ORO, EstadoCuenta.ACTIVA))
                .thenReturn(Optional.of(cuentaExistente(TipoTarjeta.ORO, "4200123456789015")));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> solicitudService.crear(peticion(TipoTarjeta.ORO, "12000.00")));

        assertEquals("El cliente ya tiene una cuenta Oro (número **** **** **** 9015). "
                + "Si perdió o dañó la tarjeta, solicite una reposición", excepcion.getMessage());
        verify(solicitudTarjetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("RC-07: con una cuenta de otro tipo sí puede solicitar una categoría nueva")
    void crearSolicitudDeOtroTipoConCuentaExistente() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(solicitudTarjetaRepository.save(any(SolicitudTarjeta.class))).thenAnswer(i -> i.getArgument(0));

        SolicitudRespuesta respuesta = solicitudService.crear(peticion(TipoTarjeta.CLASICA, "5000.00"));

        assertEquals(EstadoSolicitud.PENDIENTE, respuesta.estado());
    }

    @Test
    @DisplayName("RC-07: una cuenta cancelada no cuenta, solo se consulta la cuenta ACTIVA del tipo")
    void crearSolicitudDelTipoDeUnaCuentaCancelada() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(solicitudTarjetaRepository.save(any(SolicitudTarjeta.class))).thenAnswer(i -> i.getArgument(0));

        SolicitudRespuesta respuesta = solicitudService.crear(peticion(TipoTarjeta.CLASICA, "5000.00"));

        assertEquals(EstadoSolicitud.PENDIENTE, respuesta.estado());
        verify(cuentaCreditoRepository).findByClienteIdAndTipoTarjetaAndEstadoCuenta(
                1L, TipoTarjeta.CLASICA, EstadoCuenta.ACTIVA);
    }

    @Test
    @DisplayName("Aprobar una solicitud que no está PENDIENTE lanza regla de negocio")
    void aprobarSolicitudNoPendiente() {
        when(solicitudTarjetaRepository.buscarPorIdConBloqueo(10L))
                .thenReturn(Optional.of(crearSolicitud(EstadoSolicitud.APROBADA)));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class, () -> solicitudService.aprobar(
                10L, new AprobacionPeticion(new BigDecimal("5000.00"), null), "admin", false));

        assertEquals("Solo se pueden aprobar solicitudes en estado PENDIENTE. Estado actual: APROBADA",
                excepcion.getMessage());
        verify(cuentaCreditoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechazar una solicitud que no está PENDIENTE lanza regla de negocio")
    void rechazarSolicitudNoPendiente() {
        when(solicitudTarjetaRepository.buscarPorIdConBloqueo(10L))
                .thenReturn(Optional.of(crearSolicitud(EstadoSolicitud.RECHAZADA)));

        assertThrows(ReglaNegocioException.class, () -> solicitudService.rechazar(
                10L, new RechazoPeticion("Motivo"), "admin"));
    }

    @Test
    @DisplayName("Aprobar crea la cuenta (límite = monto aprobado, saldo cero) y su primera tarjeta con el mismo número")
    void aprobarCreaCuentaYTarjeta() {
        SolicitudTarjeta solicitud = crearSolicitud(EstadoSolicitud.PENDIENTE);
        when(solicitudTarjetaRepository.buscarPorIdConBloqueo(10L)).thenReturn(Optional.of(solicitud));
        simularGuardadoDeCuentaYTarjeta();

        SolicitudRespuesta respuesta = solicitudService.aprobar(
                10L, new AprobacionPeticion(new BigDecimal("18000.00"), "Buen historial"), "admin", false);

        ArgumentCaptor<CuentaCredito> capturaCuenta = ArgumentCaptor.forClass(CuentaCredito.class);
        verify(cuentaCreditoRepository).save(capturaCuenta.capture());
        CuentaCredito cuenta = capturaCuenta.getValue();
        assertEquals(new BigDecimal("18000.00"), cuenta.getLimiteCredito());
        assertEquals(0, BigDecimal.ZERO.compareTo(cuenta.getSaldoActual()));
        assertEquals(TipoTarjeta.ORO, cuenta.getTipoTarjeta());
        assertEquals(solicitud, cuenta.getSolicitud());
        assertEquals(cliente, cuenta.getCliente());
        assertEquals(LocalDateTime.of(2026, 10, 9, 12, 0), cuenta.getFechaApertura());
        assertEquals(16, cuenta.getNumeroCuenta().length());
        assertTrue(cuenta.getNumeroCuenta().startsWith("4200"));
        assertTrue(GeneradorNumeroTarjeta.esNumeroValidoLuhn(cuenta.getNumeroCuenta()));

        ArgumentCaptor<TarjetaCredito> capturaTarjeta = ArgumentCaptor.forClass(TarjetaCredito.class);
        verify(tarjetaCreditoRepository).save(capturaTarjeta.capture());
        TarjetaCredito tarjeta = capturaTarjeta.getValue();
        assertEquals(cuenta.getNumeroCuenta(), tarjeta.getNumeroTarjeta());
        assertEquals(cuenta, tarjeta.getCuenta());
        assertEquals(EstadoTarjeta.ACTIVA, tarjeta.getEstadoTarjeta());
        assertNull(tarjeta.getTarjetaAnterior());
        assertEquals(LocalDate.of(2031, 10, 31), tarjeta.getFechaVencimiento());

        assertEquals(EstadoSolicitud.APROBADA, respuesta.estado());
        assertEquals("admin", respuesta.usuarioDecision());
        assertNotNull(respuesta.fechaDecision());
        assertEquals(55L, respuesta.idCuenta());
        assertEquals(77L, respuesta.idTarjeta());
        assertTrue(respuesta.numeroCuentaEnmascarado().startsWith("**** **** **** "));
        assertEquals(respuesta.numeroCuentaEnmascarado(), respuesta.numeroTarjetaEnmascarado());
        assertNull(respuesta.numeroCuentaCompleto());
        assertNull(respuesta.numeroTarjetaCompleto());
    }

    @Test
    @DisplayName("Con datos sensibles, aprobar devuelve también los números completos de cuenta y tarjeta")
    void aprobarConDatosSensibles() {
        SolicitudTarjeta solicitud = crearSolicitud(EstadoSolicitud.PENDIENTE);
        when(solicitudTarjetaRepository.buscarPorIdConBloqueo(10L)).thenReturn(Optional.of(solicitud));
        simularGuardadoDeCuentaYTarjeta();

        SolicitudRespuesta respuesta = solicitudService.aprobar(
                10L, new AprobacionPeticion(new BigDecimal("18000.00"), null), "admin", true);

        assertEquals(16, respuesta.numeroCuentaCompleto().length());
        assertEquals(respuesta.numeroCuentaCompleto(), respuesta.numeroTarjetaCompleto());
        assertTrue(respuesta.numeroCuentaCompleto().endsWith(respuesta.numeroCuentaEnmascarado().substring(15)));
    }

    @Test
    @DisplayName("Rechazar registra decisión sin monto aprobado y sin crear cuenta")
    void rechazarSolicitudPendiente() {
        SolicitudTarjeta solicitud = crearSolicitud(EstadoSolicitud.PENDIENTE);
        when(solicitudTarjetaRepository.buscarPorIdConBloqueo(10L)).thenReturn(Optional.of(solicitud));

        SolicitudRespuesta respuesta = solicitudService.rechazar(10L, new RechazoPeticion("Ingreso insuficiente"), "admin");

        assertEquals(EstadoSolicitud.RECHAZADA, respuesta.estado());
        assertNull(respuesta.montoAprobado());
        assertEquals("Ingreso insuficiente", respuesta.comentarioDecision());
        verify(cuentaCreditoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Aprobar solicitud inexistente lanza 404")
    void aprobarSolicitudInexistente() {
        when(solicitudTarjetaRepository.buscarPorIdConBloqueo(5L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> solicitudService.aprobar(
                5L, new AprobacionPeticion(new BigDecimal("100.00"), null), "admin", false));
    }

    @Test
    @DisplayName("Obtener y listar resuelven la cuenta y la primera tarjeta en lote")
    void listarSolicitudesConCuentaYPrimeraTarjeta() {
        SolicitudTarjeta aprobada = crearSolicitud(EstadoSolicitud.APROBADA);
        aprobada.setMontoAprobado(new BigDecimal("15000.00"));
        aprobada.setFechaDecision(LocalDateTime.of(2026, 10, 8, 10, 0));
        CuentaCredito cuenta = cuentaExistente(TipoTarjeta.ORO, "4200123456789015");
        ReflectionTestUtils.setField(aprobada, "cuenta", cuenta);
        TarjetaCredito primera = new TarjetaCredito();
        primera.setId(8L);
        primera.setNumeroTarjeta("4200123456789015");
        primera.setCuenta(cuenta);
        when(solicitudTarjetaRepository.buscarPorEstado(null)).thenReturn(List.of(aprobada));
        when(tarjetaCreditoRepository.listarPrimerasPorCuentas(List.of(30L))).thenReturn(List.of(primera));

        List<SolicitudRespuesta> respuestas = solicitudService.listarPorEstado(null, false);

        assertEquals(30L, respuestas.get(0).idCuenta());
        assertEquals("**** **** **** 9015", respuestas.get(0).numeroCuentaEnmascarado());
        assertEquals(8L, respuestas.get(0).idTarjeta());
        assertEquals("**** **** **** 9015", respuestas.get(0).numeroTarjetaEnmascarado());
    }

    @Test
    @DisplayName("RC-01: ingreso menor al mínimo del tipo lanza 422 con el mensaje del contrato")
    void crearSolicitudIngresoBajoElMinimo() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> solicitudService.crear(peticion(TipoTarjeta.BLACK, "29999.99")));

        assertEquals("El ingreso mensual de Q 29999.99 no alcanza el mínimo de Q 30000.00 "
                + "requerido para la tarjeta Black", excepcion.getMessage());
        verify(solicitudTarjetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("RC-01: ingreso exactamente igual al mínimo es válido")
    void crearSolicitudIngresoIgualAlMinimo() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(solicitudTarjetaRepository.save(any(SolicitudTarjeta.class))).thenAnswer(i -> i.getArgument(0));

        SolicitudRespuesta respuesta = solicitudService.crear(peticion(TipoTarjeta.BLACK, "30000.00"));

        assertEquals(EstadoSolicitud.PENDIENTE, respuesta.estado());
    }

    @Test
    @DisplayName("RC-02: cliente con cuentas en mora no puede solicitar")
    void crearSolicitudClienteEnMora() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(estadoPagoService.tieneCuentasEnMora(1L)).thenReturn(true);

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> solicitudService.crear(peticion(TipoTarjeta.ORO, "12000.00")));

        assertEquals("El cliente tiene cuentas en mora. Debe ponerse al día en sus pagos antes de solicitar "
                + "una nueva tarjeta", excepcion.getMessage());
        verify(solicitudTarjetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("RC-03: monto aprobado mayor al 150 % del ingreso lanza 422 y no abre cuenta")
    void aprobarMontoSuperaMaximo() {
        SolicitudTarjeta solicitud = crearSolicitud(EstadoSolicitud.PENDIENTE);
        when(solicitudTarjetaRepository.buscarPorIdConBloqueo(10L)).thenReturn(Optional.of(solicitud));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class, () -> solicitudService.aprobar(
                10L, new AprobacionPeticion(new BigDecimal("18000.01"), null), "admin", false));

        assertEquals("El monto aprobado de Q 18000.01 supera el máximo de Q 18000.00 "
                + "(150 % del ingreso mensual declarado)", excepcion.getMessage());
        assertEquals(EstadoSolicitud.PENDIENTE, solicitud.getEstado());
        verify(cuentaCreditoRepository, never()).save(any());
    }

    @Test
    @DisplayName("RC-03: aprobar exactamente el 150 % del ingreso es válido")
    void aprobarMontoIgualAlMaximo() {
        SolicitudTarjeta solicitud = crearSolicitud(EstadoSolicitud.PENDIENTE);
        when(solicitudTarjetaRepository.buscarPorIdConBloqueo(10L)).thenReturn(Optional.of(solicitud));
        simularGuardadoDeCuentaYTarjeta();

        SolicitudRespuesta respuesta = solicitudService.aprobar(
                10L, new AprobacionPeticion(new BigDecimal("18000.00"), null), "admin", false);

        assertEquals(EstadoSolicitud.APROBADA, respuesta.estado());
    }

    @Test
    @DisplayName("RC-04: al aprobar se revalida la mora del cliente")
    void aprobarClienteEnMora() {
        SolicitudTarjeta solicitud = crearSolicitud(EstadoSolicitud.PENDIENTE);
        when(solicitudTarjetaRepository.buscarPorIdConBloqueo(10L)).thenReturn(Optional.of(solicitud));
        when(estadoPagoService.tieneCuentasEnMora(1L)).thenReturn(true);

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class, () -> solicitudService.aprobar(
                10L, new AprobacionPeticion(new BigDecimal("10000.00"), null), "admin", false));

        assertEquals("El cliente tiene cuentas en mora. Debe ponerse al día en sus pagos antes de solicitar "
                + "una nueva tarjeta", excepcion.getMessage());
        assertEquals(EstadoSolicitud.PENDIENTE, solicitud.getEstado());
        verify(cuentaCreditoRepository, never()).save(any());
    }

    @Test
    @DisplayName("RC-06 al aprobar: una solicitud con menos de 12 meses a la fecha de la solicitud no se puede aprobar")
    void aprobarSolicitudSinEstabilidadLaboral() {
        SolicitudTarjeta solicitud = crearSolicitud(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaInicioLaboral(LocalDate.of(2026, 4, 9));
        when(solicitudTarjetaRepository.buscarPorIdConBloqueo(10L)).thenReturn(Optional.of(solicitud));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class, () -> solicitudService.aprobar(
                10L, new AprobacionPeticion(new BigDecimal("5000.00"), null), "admin", false));

        assertEquals("Se requiere al menos 1 año de estabilidad laboral. Antigüedad actual: 5 meses",
                excepcion.getMessage());
        assertEquals(EstadoSolicitud.PENDIENTE, solicitud.getEstado());
        verify(cuentaCreditoRepository, never()).save(any());
    }

    @Test
    @DisplayName("RC-07 al aprobar: si el cliente ya tiene una cuenta de ese tipo, no se abre otra")
    void aprobarClienteConCuentaDelMismoTipo() {
        SolicitudTarjeta solicitud = crearSolicitud(EstadoSolicitud.PENDIENTE);
        when(solicitudTarjetaRepository.buscarPorIdConBloqueo(10L)).thenReturn(Optional.of(solicitud));
        when(cuentaCreditoRepository.findByClienteIdAndTipoTarjetaAndEstadoCuenta(1L, TipoTarjeta.ORO, EstadoCuenta.ACTIVA))
                .thenReturn(Optional.of(cuentaExistente(TipoTarjeta.ORO, "4200555123409875")));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class, () -> solicitudService.aprobar(
                10L, new AprobacionPeticion(new BigDecimal("10000.00"), null), "admin", false));

        assertEquals("El cliente ya tiene una cuenta Oro (número **** **** **** 9875). "
                + "Si perdió o dañó la tarjeta, solicite una reposición", excepcion.getMessage());
        verify(cuentaCreditoRepository, never()).save(any());
    }
}
