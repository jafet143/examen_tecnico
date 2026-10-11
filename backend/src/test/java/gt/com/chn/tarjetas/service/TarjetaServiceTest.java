package gt.com.chn.tarjetas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.com.chn.tarjetas.dto.BloqueoTarjetaPeticion;
import gt.com.chn.tarjetas.dto.ReposicionTarjetaPeticion;
import gt.com.chn.tarjetas.dto.TarjetaRespuesta;
import gt.com.chn.tarjetas.entity.Cargo;
import gt.com.chn.tarjetas.entity.Cliente;
import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.entity.EstadoTarjeta;
import gt.com.chn.tarjetas.entity.MotivoBloqueo;
import gt.com.chn.tarjetas.entity.MotivoCancelacion;
import gt.com.chn.tarjetas.entity.MotivoReposicion;
import gt.com.chn.tarjetas.entity.SolicitudTarjeta;
import gt.com.chn.tarjetas.entity.TarjetaCredito;
import gt.com.chn.tarjetas.entity.TipoTarjeta;
import gt.com.chn.tarjetas.exception.ReglaNegocioException;
import gt.com.chn.tarjetas.repository.CuentaCreditoRepository;
import gt.com.chn.tarjetas.repository.TarjetaCreditoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TarjetaServiceTest {
    @Mock
    private TarjetaCreditoRepository tarjetaCreditoRepository;
    @Mock
    private CuentaCreditoRepository cuentaCreditoRepository;
    @Mock
    private CargoService cargoService;
    @Mock
    private ProcesoInteresesService procesoIntereses;

    private TarjetaService tarjetaService;
    private CuentaCredito cuenta;
    private TarjetaCredito tarjeta;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-09T18:00:00Z"), ZoneId.of("America/Guatemala"));
        tarjetaService = new TarjetaService(
                tarjetaCreditoRepository, cuentaCreditoRepository, new GeneradorNumeroTarjeta(), cargoService,
                procesoIntereses, reloj);

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Ana");
        cliente.setApellido("Morales");
        cliente.setCodigoCliente("10000001");
        SolicitudTarjeta solicitud = new SolicitudTarjeta();
        solicitud.setId(10L);
        cuenta = new CuentaCredito();
        cuenta.setId(3L);
        cuenta.setNumeroCuenta("4200123456789015");
        cuenta.setCliente(cliente);
        cuenta.setSolicitud(solicitud);
        cuenta.setTipoTarjeta(TipoTarjeta.ORO);
        cuenta.setLimiteCredito(new BigDecimal("10000.00"));
        cuenta.setSaldoActual(new BigDecimal("2500.00"));
        cuenta.setFechaApertura(LocalDateTime.of(2026, 8, 1, 9, 0));
        tarjeta = new TarjetaCredito();
        tarjeta.setId(5L);
        tarjeta.setNumeroTarjeta("4200123456789015");
        tarjeta.setCuenta(cuenta);
        tarjeta.setFechaEmision(LocalDateTime.of(2026, 8, 1, 9, 0));
        tarjeta.setFechaVencimiento(LocalDate.of(2031, 8, 31));
        lenient().when(tarjetaCreditoRepository.buscarPorIdConBloqueo(5L)).thenReturn(Optional.of(tarjeta));
        lenient().when(tarjetaCreditoRepository.buscarIdCuentaPorId(5L)).thenReturn(Optional.of(3L));
        lenient().when(cuentaCreditoRepository.buscarPorIdConBloqueo(3L)).thenReturn(Optional.of(cuenta));
        Cargo comision = new Cargo();
        comision.setId(77L);
        lenient().when(cargoService.registrarComisionReposicion(any(CuentaCredito.class), any())).thenReturn(comision);
        lenient().when(tarjetaCreditoRepository.save(any(TarjetaCredito.class))).thenAnswer(i -> i.getArgument(0));
    }

    private ReposicionTarjetaPeticion reposicion(MotivoReposicion motivo) {
        return new ReposicionTarjetaPeticion(motivo, "  Reportado por la clienta ", true, true);
    }

    @Test
    @DisplayName("Bloquear una tarjeta activa registra motivo, comentario, fecha y usuario")
    void bloquearTarjetaActiva() {
        TarjetaRespuesta respuesta = tarjetaService.bloquear(
                5L, new BloqueoTarjetaPeticion(MotivoBloqueo.SOSPECHA_FRAUDE, "  Compra inusual  "), "operador", false);

        assertEquals(EstadoTarjeta.BLOQUEADA, respuesta.estadoTarjeta());
        assertEquals("Bloqueada", respuesta.descripcionEstadoTarjeta());
        assertEquals(MotivoBloqueo.SOSPECHA_FRAUDE, respuesta.motivoBloqueo());
        assertEquals("Sospecha de fraude", respuesta.descripcionMotivoBloqueo());
        assertEquals("Compra inusual", respuesta.comentarioBloqueo());
        assertEquals("operador", respuesta.usuarioBloqueo());
        assertEquals(LocalDateTime.of(2026, 10, 9, 12, 0), respuesta.fechaBloqueo());
        assertFalse(respuesta.puedeDesbloquearse());
        assertEquals(3L, respuesta.idCuenta());
    }

    @Test
    @DisplayName("RB-01: bloquear una tarjeta ya bloqueada lanza 422")
    void bloquearTarjetaYaBloqueada() {
        tarjeta.bloquear(MotivoBloqueo.ROBO, null, LocalDateTime.of(2026, 10, 8, 9, 0), "operador");

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class, () -> tarjetaService.bloquear(
                5L, new BloqueoTarjetaPeticion(MotivoBloqueo.EXTRAVIO, null), "operador", false));

        assertEquals("La tarjeta ya se encuentra bloqueada", excepcion.getMessage());
        assertEquals(MotivoBloqueo.ROBO, tarjeta.getMotivoBloqueo());
    }

    @Test
    @DisplayName("RB-02: desbloquear una tarjeta activa lanza 422")
    void desbloquearTarjetaActiva() {
        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> tarjetaService.desbloquear(5L, false));

        assertEquals("La tarjeta no está bloqueada", excepcion.getMessage());
    }

    @Test
    @DisplayName("RB-04: tarjeta bloqueada por robo, extravío o sospecha de fraude no puede desbloquearse")
    void desbloquearPorMotivoSinDesbloqueo() {
        for (MotivoBloqueo motivo : new MotivoBloqueo[] {
                MotivoBloqueo.ROBO, MotivoBloqueo.EXTRAVIO, MotivoBloqueo.SOSPECHA_FRAUDE}) {
            tarjeta.bloquear(motivo, null, LocalDateTime.of(2026, 10, 8, 9, 0), "operador");

            ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                    () -> tarjetaService.desbloquear(5L, false));

            assertEquals("Una tarjeta bloqueada por " + motivo.getDescripcion()
                    + " no puede desbloquearse. Solicite una reposición",
                    excepcion.getMessage());
            assertTrue(tarjeta.estaBloqueada());
        }
    }

    @Test
    @DisplayName("RB-04: solo la solicitud del cliente permite desbloquear y se limpian los datos del bloqueo")
    void desbloquearPorSolicitudDelCliente() {
        tarjeta.bloquear(MotivoBloqueo.SOLICITUD_CLIENTE, "Revisión", LocalDateTime.of(2026, 10, 8, 9, 0), "operador");

        TarjetaRespuesta respuesta = tarjetaService.desbloquear(5L, false);

        assertEquals(EstadoTarjeta.ACTIVA, respuesta.estadoTarjeta());
        assertNull(respuesta.motivoBloqueo());
        assertNull(respuesta.comentarioBloqueo());
        assertNull(respuesta.fechaBloqueo());
        assertNull(respuesta.usuarioBloqueo());
        assertFalse(respuesta.puedeDesbloquearse());
    }

    @Test
    @DisplayName("La respuesta trae los números completos solo con datos sensibles")
    void numerosCompletosSegunPermiso() {
        when(tarjetaCreditoRepository.buscarDetallePorId(5L)).thenReturn(Optional.of(tarjeta));

        TarjetaRespuesta sinPermiso = tarjetaService.obtener(5L, false);
        TarjetaRespuesta conPermiso = tarjetaService.obtener(5L, true);

        assertNull(sinPermiso.numeroCuentaCompleto());
        assertNull(sinPermiso.numeroTarjetaCompleto());
        assertEquals("**** **** **** 9015", sinPermiso.numeroTarjetaEnmascarado());
        assertEquals("4200123456789015", conPermiso.numeroCuentaCompleto());
        assertEquals("4200123456789015", conPermiso.numeroTarjetaCompleto());
    }

    @Test
    @DisplayName("RR-02/RR-03/RR-04: reponer por robo bloquea la activa, la reemplaza y emite una nueva en la misma cuenta")
    void reponerPorRoboBloqueaYReemplaza() {
        TarjetaRespuesta nueva = tarjetaService.reponer(5L, reposicion(MotivoReposicion.ROBO), "operador", false);

        assertEquals(EstadoTarjeta.REEMPLAZADA, tarjeta.getEstadoTarjeta());
        assertEquals(MotivoBloqueo.ROBO, tarjeta.getMotivoBloqueo());
        assertEquals(LocalDateTime.of(2026, 10, 9, 12, 0), tarjeta.getFechaBloqueo());
        assertEquals("operador", tarjeta.getUsuarioBloqueo());
        assertEquals(MotivoReposicion.ROBO, tarjeta.getMotivoReposicion());
        assertEquals("Reportado por la clienta", tarjeta.getComentarioReposicion());
        assertEquals(LocalDateTime.of(2026, 10, 9, 12, 0), tarjeta.getFechaReposicion());
        assertEquals("operador", tarjeta.getUsuarioReposicion());
        assertEquals(EstadoTarjeta.ACTIVA, nueva.estadoTarjeta());
        assertEquals(3L, nueva.idCuenta());
        assertEquals("**** **** **** 9015", nueva.numeroCuentaEnmascarado());
        assertNotEquals("**** **** **** 9015", nueva.numeroTarjetaEnmascarado());
        assertEquals(5L, nueva.idTarjetaAnterior());
        assertEquals(LocalDateTime.of(2026, 10, 9, 12, 0), nueva.fechaEmision());
        assertEquals(LocalDate.of(2031, 10, 31), nueva.fechaVencimiento());
        assertEquals("Ana Morales", nueva.nombreCliente());
        assertEquals(0, new BigDecimal("2500.00").compareTo(cuenta.getSaldoActual()));
        assertEquals("4200123456789015", cuenta.getNumeroCuenta());
    }

    @Test
    @DisplayName("RR-02: la anterior se persiste como REEMPLAZADA antes de insertar la nueva (índice de vigente única)")
    void reponerGuardaLaAnteriorAntesDeLaNueva() {
        tarjetaService.reponer(5L, reposicion(MotivoReposicion.EXTRAVIO), "operador", false);

        InOrder orden = inOrder(tarjetaCreditoRepository);
        orden.verify(tarjetaCreditoRepository).flush();
        orden.verify(tarjetaCreditoRepository).save(any(TarjetaCredito.class));
    }

    @Test
    @DisplayName("RR-02: reponer por extravío o sospecha de fraude también bloquea con ese motivo")
    void reponerBloqueaConElMotivoCorrespondiente() {
        tarjetaService.reponer(5L, reposicion(MotivoReposicion.SOSPECHA_FRAUDE), "operador", false);

        assertEquals(MotivoBloqueo.SOSPECHA_FRAUDE, tarjeta.getMotivoBloqueo());
    }

    @Test
    @DisplayName("Reponer por deterioro no bloquea la tarjeta anterior")
    void reponerPorDeterioroNoBloquea() {
        tarjetaService.reponer(5L, new ReposicionTarjetaPeticion(MotivoReposicion.DETERIORO, null, false, true), "operador", false);

        assertEquals(EstadoTarjeta.REEMPLAZADA, tarjeta.getEstadoTarjeta());
        assertNull(tarjeta.getMotivoBloqueo());
        assertNull(tarjeta.getFechaBloqueo());
        assertEquals(MotivoReposicion.DETERIORO, tarjeta.getMotivoReposicion());
    }

    @Test
    @DisplayName("Reponer una tarjeta ya bloqueada conserva su bloqueo original")
    void reponerTarjetaBloqueadaConservaElBloqueo() {
        tarjeta.bloquear(MotivoBloqueo.SOLICITUD_CLIENTE, "Pedido por la clienta",
                LocalDateTime.of(2026, 10, 8, 9, 0), "admin");

        tarjetaService.reponer(5L, reposicion(MotivoReposicion.ROBO), "operador", false);

        assertEquals(MotivoBloqueo.SOLICITUD_CLIENTE, tarjeta.getMotivoBloqueo());
        assertEquals("admin", tarjeta.getUsuarioBloqueo());
        assertEquals(EstadoTarjeta.REEMPLAZADA, tarjeta.getEstadoTarjeta());
    }

    @Test
    @DisplayName("RR-04: la nueva tarjeta tiene un número de tarjeta único con el prefijo del tipo de la cuenta")
    void reponerGeneraNumeroUnico() {
        tarjetaService.reponer(5L, reposicion(MotivoReposicion.ROBO), "operador", false);

        ArgumentCaptor<TarjetaCredito> captor = ArgumentCaptor.forClass(TarjetaCredito.class);
        verify(tarjetaCreditoRepository).save(captor.capture());
        TarjetaCredito nueva = captor.getValue();
        assertTrue(nueva.getNumeroTarjeta().startsWith("4200"));
        assertTrue(GeneradorNumeroTarjeta.esNumeroValidoLuhn(nueva.getNumeroTarjeta()));
        assertSame(cuenta, nueva.getCuenta());
        assertSame(tarjeta, nueva.getTarjetaAnterior());
    }

    @Test
    @DisplayName("RR-01: reponer una tarjeta REEMPLAZADA lanza 422 con los últimos 4 dígitos de la sucesora")
    void reponerTarjetaYaReemplazada() {
        tarjeta.reemplazar(MotivoReposicion.ROBO, null, LocalDateTime.of(2026, 10, 8, 9, 0), "operador", null, null);
        TarjetaCredito sucesora = new TarjetaCredito();
        sucesora.setNumeroTarjeta("4200555123409875");
        org.mockito.Mockito.when(tarjetaCreditoRepository.findByTarjetaAnteriorId(5L))
                .thenReturn(Optional.of(sucesora));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> tarjetaService.reponer(5L, reposicion(MotivoReposicion.EXTRAVIO), "operador", false));

        assertEquals("La tarjeta ya fue reemplazada por la tarjeta terminada en 9875", excepcion.getMessage());
        verify(tarjetaCreditoRepository, never()).save(any());
    }

    @Test
    @DisplayName("RR-01: una tarjeta REEMPLAZADA tampoco se bloquea ni se desbloquea")
    void tarjetaReemplazadaNoAdmiteBloqueoNiDesbloqueo() {
        tarjeta.reemplazar(MotivoReposicion.DETERIORO, null, LocalDateTime.of(2026, 10, 8, 9, 0), "operador", null, null);
        TarjetaCredito sucesora = new TarjetaCredito();
        sucesora.setNumeroTarjeta("4200555123409875");
        org.mockito.Mockito.when(tarjetaCreditoRepository.findByTarjetaAnteriorId(5L))
                .thenReturn(Optional.of(sucesora));

        assertThrows(ReglaNegocioException.class, () -> tarjetaService.bloquear(
                5L, new BloqueoTarjetaPeticion(MotivoBloqueo.ROBO, null), "operador", false));
        assertThrows(ReglaNegocioException.class, () -> tarjetaService.desbloquear(5L, false));
    }

    @Test
    @DisplayName("RR-05: la mora no impide la reposición (el servicio no consulta el estado de pago)")
    void reponerEnCuentaConSaldoVencido() {
        cuenta.setSaldoActual(new BigDecimal("9999.00"));

        TarjetaRespuesta nueva = tarjetaService.reponer(5L, reposicion(MotivoReposicion.ROBO), "operador", false);

        assertEquals(EstadoTarjeta.ACTIVA, nueva.estadoTarjeta());
    }

    @Test
    @DisplayName("Una tarjeta reemplazada no puede desbloquearse según la respuesta")
    void puedeDesbloquearseFalsoSiReemplazada() {
        tarjeta.bloquear(MotivoBloqueo.SOSPECHA_FRAUDE, null, LocalDateTime.of(2026, 10, 8, 9, 0), "operador");
        tarjeta.reemplazar(MotivoReposicion.SOSPECHA_FRAUDE, null, LocalDateTime.of(2026, 10, 8, 10, 0), "operador", null, null);

        assertFalse(tarjeta.puedeDesbloquearse());
    }

    private ReposicionTarjetaPeticion reposicion(MotivoReposicion motivo, boolean denuncia, boolean deteriorada) {
        return new ReposicionTarjetaPeticion(motivo, null, denuncia, deteriorada);
    }

    @Test
    @DisplayName("Reponer por robo sin la denuncia presentada lanza 422 y no cambia nada")
    void reponerPorRoboSinDenuncia() {
        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class, () -> tarjetaService.reponer(
                5L, reposicion(MotivoReposicion.ROBO, false, true), "operador", false));

        assertEquals("Para reponer por robo el cliente debe presentar la denuncia del robo", excepcion.getMessage());
        assertEquals(EstadoTarjeta.ACTIVA, tarjeta.getEstadoTarjeta());
        verify(tarjetaCreditoRepository, never()).save(any());
        verifyNoInteractions(cargoService);
    }

    @Test
    @DisplayName("Reponer por robo con la denuncia presentada guarda la evidencia y no cobra comisión")
    void reponerPorRoboConDenuncia() {
        TarjetaRespuesta nueva = tarjetaService.reponer(5L, reposicion(MotivoReposicion.ROBO, true, false),
                "operador", false);

        assertEquals(EstadoTarjeta.ACTIVA, nueva.estadoTarjeta());
        assertEquals(Boolean.TRUE, tarjeta.getDenunciaPresentada());
        assertNull(tarjeta.getTarjetaDeterioradaPresentada());
        assertNull(tarjeta.getIdCargoReposicion());
        verifyNoInteractions(cargoService);
        verify(procesoIntereses).generarInteresesPendientes(cuenta);
    }

    @Test
    @DisplayName("Reponer por deterioro sin presentar la tarjeta deteriorada lanza 422")
    void reponerPorDeterioroSinTarjeta() {
        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class, () -> tarjetaService.reponer(
                5L, reposicion(MotivoReposicion.DETERIORO, true, false), "operador", false));

        assertEquals("Para reponer por deterioro el cliente debe presentar la tarjeta deteriorada",
                excepcion.getMessage());
        assertEquals(EstadoTarjeta.ACTIVA, tarjeta.getEstadoTarjeta());
    }

    @Test
    @DisplayName("Reponer por deterioro con la tarjeta presentada guarda la evidencia sin costo")
    void reponerPorDeterioroConTarjeta() {
        tarjetaService.reponer(5L, reposicion(MotivoReposicion.DETERIORO, false, true), "operador", false);

        assertEquals(Boolean.TRUE, tarjeta.getTarjetaDeterioradaPresentada());
        assertNull(tarjeta.getDenunciaPresentada());
        verifyNoInteractions(cargoService);
    }

    @Test
    @DisplayName("Reponer por extravío cobra la comisión en la misma transacción y enlaza el cargo con la tarjeta reemplazada")
    void reponerPorExtravioCobraComision() {
        TarjetaRespuesta nueva = tarjetaService.reponer(5L, reposicion(MotivoReposicion.EXTRAVIO, false, false),
                "operador", false);

        assertEquals(EstadoTarjeta.ACTIVA, nueva.estadoTarjeta());
        assertEquals(MotivoBloqueo.EXTRAVIO, tarjeta.getMotivoBloqueo());
        assertEquals(77L, tarjeta.getIdCargoReposicion());
        verify(cargoService).registrarComisionReposicion(cuenta, "operador");
    }

    @Test
    @DisplayName("Reponer por sospecha de fraude no cobra comisión")
    void reponerPorFraudeSinComision() {
        tarjetaService.reponer(5L, reposicion(MotivoReposicion.SOSPECHA_FRAUDE, false, false), "operador", false);

        verifyNoInteractions(cargoService);
        assertNull(tarjeta.getIdCargoReposicion());
    }

    @Test
    @DisplayName("La renovación por vencimiento no se acepta como motivo de reposición")
    void reponerConMotivoRenovacion() {
        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class, () -> tarjetaService.reponer(
                5L, reposicion(MotivoReposicion.RENOVACION, true, true), "operador", false));

        assertEquals("El motivo Renovación por vencimiento no es válido para una reposición; "
                + "use la renovación de tarjetas vencidas", excepcion.getMessage());
        verify(tarjetaCreditoRepository, never()).buscarPorIdConBloqueo(any());
    }

    private void cancelarCuenta() {
        cuenta.cancelar(MotivoCancelacion.SOLICITUD_CLIENTE, null, LocalDateTime.of(2026, 9, 1, 9, 0), "admin");
        tarjeta.cancelar();
    }

    @Test
    @DisplayName("CA-05: en una cuenta cancelada no se puede bloquear, desbloquear, reponer ni renovar")
    void cuentaCanceladaNoAdmiteOperacionesDeTarjeta() {
        cancelarCuenta();
        String mensaje = "La cuenta está cancelada";

        assertEquals(mensaje, assertThrows(ReglaNegocioException.class, () -> tarjetaService.bloquear(
                5L, new BloqueoTarjetaPeticion(MotivoBloqueo.ROBO, null), "operador", false)).getMessage());
        assertEquals(mensaje, assertThrows(ReglaNegocioException.class,
                () -> tarjetaService.desbloquear(5L, false)).getMessage());
        assertEquals(mensaje, assertThrows(ReglaNegocioException.class, () -> tarjetaService.reponer(
                5L, reposicion(MotivoReposicion.ROBO), "operador", false)).getMessage());
        assertEquals(mensaje, assertThrows(ReglaNegocioException.class,
                () -> tarjetaService.renovar(5L, "operador", false)).getMessage());
        verify(tarjetaCreditoRepository, never()).save(any());
        verifyNoInteractions(procesoIntereses);
    }

    @Test
    @DisplayName("La respuesta marca vencida y puedeRenovarse solo para la tarjeta vigente, activa y vencida")
    void vencidaYPuedeRenovarse() {
        tarjeta.setFechaVencimiento(LocalDate.of(2026, 9, 30));
        when(tarjetaCreditoRepository.buscarDetallePorId(5L)).thenReturn(Optional.of(tarjeta));

        TarjetaRespuesta vencida = tarjetaService.obtener(5L, false);
        tarjeta.bloquear(MotivoBloqueo.SOLICITUD_CLIENTE, null, LocalDateTime.of(2026, 10, 1, 9, 0), "operador");
        TarjetaRespuesta vencidaYBloqueada = tarjetaService.obtener(5L, false);

        assertTrue(vencida.vencida());
        assertTrue(vencida.puedeRenovarse());
        assertTrue(vencidaYBloqueada.vencida());
        assertFalse(vencidaYBloqueada.puedeRenovarse());
    }

    @Test
    @DisplayName("RNV-01: renovar una tarjeta no vencida lanza 422 con su fecha de vencimiento")
    void renovarTarjetaNoVencida() {
        tarjeta.setFechaVencimiento(LocalDate.of(2026, 10, 9));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> tarjetaService.renovar(5L, "operador", false));

        assertEquals("La tarjeta vence el 09/10/2026; solo puede renovarse cuando haya vencido",
                excepcion.getMessage());
        assertEquals(EstadoTarjeta.ACTIVA, tarjeta.getEstadoTarjeta());
    }

    @Test
    @DisplayName("RNV-02: renovar una tarjeta vencida pero bloqueada lanza 422")
    void renovarTarjetaBloqueada() {
        tarjeta.setFechaVencimiento(LocalDate.of(2026, 9, 30));
        tarjeta.bloquear(MotivoBloqueo.SOLICITUD_CLIENTE, null, LocalDateTime.of(2026, 10, 1, 9, 0), "operador");

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> tarjetaService.renovar(5L, "operador", false));

        assertEquals("La tarjeta está bloqueada por Solicitud del cliente; resuelva el bloqueo antes de renovarla",
                excepcion.getMessage());
    }

    @Test
    @DisplayName("RNV-03: renovar emite una tarjeta nueva en la misma cuenta, sin costo, y reemplaza la vieja con motivo RENOVACION")
    void renovarTarjetaVencida() {
        tarjeta.setFechaVencimiento(LocalDate.of(2026, 9, 30));

        TarjetaRespuesta nueva = tarjetaService.renovar(5L, "operador", false);

        assertEquals(EstadoTarjeta.REEMPLAZADA, tarjeta.getEstadoTarjeta());
        assertEquals(MotivoReposicion.RENOVACION, tarjeta.getMotivoReposicion());
        assertEquals("operador", tarjeta.getUsuarioReposicion());
        assertNull(tarjeta.getMotivoBloqueo());
        assertEquals(EstadoTarjeta.ACTIVA, nueva.estadoTarjeta());
        assertEquals(3L, nueva.idCuenta());
        assertEquals(5L, nueva.idTarjetaAnterior());
        assertEquals(LocalDate.of(2031, 10, 31), nueva.fechaVencimiento());
        assertFalse(nueva.vencida());
        assertFalse(nueva.puedeRenovarse());
        assertEquals(0, new BigDecimal("2500.00").compareTo(cuenta.getSaldoActual()));
        verifyNoInteractions(cargoService);
        InOrder orden = inOrder(tarjetaCreditoRepository);
        orden.verify(tarjetaCreditoRepository).flush();
        orden.verify(tarjetaCreditoRepository).save(any(TarjetaCredito.class));
    }

    @Test
    @DisplayName("RR-01: renovar una tarjeta ya reemplazada lanza 422")
    void renovarTarjetaReemplazada() {
        tarjeta.reemplazar(MotivoReposicion.ROBO, null, LocalDateTime.of(2026, 10, 8, 9, 0), "operador", true, null);
        TarjetaCredito sucesora = new TarjetaCredito();
        sucesora.setNumeroTarjeta("4200555123409875");
        when(tarjetaCreditoRepository.findByTarjetaAnteriorId(5L)).thenReturn(Optional.of(sucesora));

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class,
                () -> tarjetaService.renovar(5L, "operador", false));

        assertEquals("La tarjeta ya fue reemplazada por la tarjeta terminada en 9875", excepcion.getMessage());
    }
}
