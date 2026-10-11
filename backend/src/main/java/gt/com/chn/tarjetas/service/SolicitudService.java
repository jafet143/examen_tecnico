package gt.com.chn.tarjetas.service;

import gt.com.chn.tarjetas.dto.AprobacionPeticion;
import gt.com.chn.tarjetas.dto.RechazoPeticion;
import gt.com.chn.tarjetas.dto.SolicitudPeticion;
import gt.com.chn.tarjetas.dto.SolicitudRespuesta;
import gt.com.chn.tarjetas.entity.Cliente;
import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.entity.EstadoCuenta;
import gt.com.chn.tarjetas.entity.EstadoSolicitud;
import gt.com.chn.tarjetas.entity.SolicitudTarjeta;
import gt.com.chn.tarjetas.entity.TarjetaCredito;
import gt.com.chn.tarjetas.entity.TipoTarjeta;
import gt.com.chn.tarjetas.exception.RecursoNoEncontradoException;
import gt.com.chn.tarjetas.exception.ReglaNegocioException;
import gt.com.chn.tarjetas.mapper.SolicitudMapper;
import gt.com.chn.tarjetas.mapper.TarjetaMapper;
import gt.com.chn.tarjetas.repository.ClienteRepository;
import gt.com.chn.tarjetas.repository.CuentaCreditoRepository;
import gt.com.chn.tarjetas.repository.SolicitudTarjetaRepository;
import gt.com.chn.tarjetas.repository.TarjetaCreditoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SolicitudService {
    public static final int PORCENTAJE_MAXIMO_LIMITE_SOBRE_INGRESO = 150;

    public static final int MESES_MINIMOS_ESTABILIDAD_LABORAL = 12;

    private static final String MENSAJE_CLIENTE_EN_MORA = "El cliente tiene cuentas en mora. "
            + "Debe ponerse al día en sus pagos antes de solicitar una nueva tarjeta";

    private final SolicitudTarjetaRepository solicitudTarjetaRepository;
    private final ClienteRepository clienteRepository;
    private final CuentaCreditoRepository cuentaCreditoRepository;
    private final TarjetaCreditoRepository tarjetaCreditoRepository;
    private final CuentaService cuentaService;
    private final TarjetaService tarjetaService;
    private final EstadoPagoService estadoPagoService;
    private final Clock reloj;

    public SolicitudService(SolicitudTarjetaRepository solicitudTarjetaRepository,
                            ClienteRepository clienteRepository,
                            CuentaCreditoRepository cuentaCreditoRepository,
                            TarjetaCreditoRepository tarjetaCreditoRepository,
                            CuentaService cuentaService,
                            TarjetaService tarjetaService,
                            EstadoPagoService estadoPagoService,
                            Clock reloj) {
        this.solicitudTarjetaRepository = solicitudTarjetaRepository;
        this.clienteRepository = clienteRepository;
        this.cuentaCreditoRepository = cuentaCreditoRepository;
        this.tarjetaCreditoRepository = tarjetaCreditoRepository;
        this.cuentaService = cuentaService;
        this.tarjetaService = tarjetaService;
        this.estadoPagoService = estadoPagoService;
        this.reloj = reloj;
    }

    @Transactional
    public SolicitudRespuesta crear(SolicitudPeticion peticion) {
        Cliente cliente = clienteRepository.findById(peticion.idCliente())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el cliente con id " + peticion.idCliente()));
        validarEstabilidadLaboral(peticion.fechaInicioLaboral(), LocalDate.now(reloj));
        validarIngresoMinimo(peticion);
        validarClienteSinMora(cliente.getId());
        validarSinCuentaDelMismoTipo(cliente.getId(), peticion.tipoTarjeta());
        if (solicitudTarjetaRepository.existsByClienteIdAndTipoTarjetaAndEstado(
                cliente.getId(), peticion.tipoTarjeta(), EstadoSolicitud.PENDIENTE)) {
            throw new ReglaNegocioException("El cliente ya tiene una solicitud pendiente de tarjeta "
                    + peticion.tipoTarjeta().getDescripcion());
        }
        SolicitudTarjeta solicitud = new SolicitudTarjeta();
        solicitud.setCliente(cliente);
        solicitud.setTipoTarjeta(peticion.tipoTarjeta());
        solicitud.setIngresoMensual(peticion.ingresoMensual());
        solicitud.setNombreEmpleador(peticion.nombreEmpleador().trim());
        solicitud.setFechaInicioLaboral(peticion.fechaInicioLaboral());
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaSolicitud(LocalDateTime.now(reloj));
        return SolicitudMapper.aRespuesta(solicitudTarjetaRepository.save(solicitud));
    }

    @Transactional(readOnly = true)
    public SolicitudRespuesta obtener(Long id, boolean incluirDatosSensibles) {
        SolicitudTarjeta solicitud = solicitudTarjetaRepository.findById(id)
                .orElseThrow(() -> crearExcepcionNoEncontrada(id));
        return aRespuestas(List.of(solicitud), incluirDatosSensibles).get(0);
    }

    @Transactional(readOnly = true)
    public List<SolicitudRespuesta> listarPorCliente(Long idCliente, boolean incluirDatosSensibles) {
        if (!clienteRepository.existsById(idCliente)) {
            throw new RecursoNoEncontradoException("No se encontró el cliente con id " + idCliente);
        }
        return aRespuestas(solicitudTarjetaRepository.buscarPorCliente(idCliente), incluirDatosSensibles);
    }

    @Transactional(readOnly = true)
    public List<SolicitudRespuesta> listarPorEstado(EstadoSolicitud estadoOpcional, boolean incluirDatosSensibles) {
        return aRespuestas(solicitudTarjetaRepository.buscarPorEstado(estadoOpcional), incluirDatosSensibles);
    }

    @Transactional
    public SolicitudRespuesta aprobar(Long id, AprobacionPeticion peticion, String usuario,
                                      boolean incluirDatosSensibles) {
        SolicitudTarjeta solicitud = obtenerPendienteConBloqueo(id, "aprobar");
        validarEstabilidadLaboral(solicitud.getFechaInicioLaboral(), solicitud.getFechaSolicitud().toLocalDate());
        validarClienteSinMora(solicitud.getCliente().getId());
        validarMontoAprobado(solicitud, peticion.montoAprobado());
        validarSinCuentaDelMismoTipo(solicitud.getCliente().getId(), solicitud.getTipoTarjeta());
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setFechaDecision(LocalDateTime.now(reloj));
        solicitud.setMontoAprobado(peticion.montoAprobado());
        solicitud.setComentarioDecision(normalizarComentario(peticion.comentarioDecision()));
        solicitud.setUsuarioDecision(usuario);
        String numero = tarjetaService.generarNumeroUnico(solicitud.getTipoTarjeta());
        CuentaCredito cuenta = cuentaService.abrirCuenta(solicitud, numero);
        TarjetaCredito tarjeta = tarjetaService.emitirTarjeta(cuenta, numero, null);
        return SolicitudMapper.aRespuesta(solicitud, cuenta, tarjeta, incluirDatosSensibles);
    }

    @Transactional
    public SolicitudRespuesta rechazar(Long id, RechazoPeticion peticion, String usuario) {
        SolicitudTarjeta solicitud = obtenerPendienteConBloqueo(id, "rechazar");
        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setFechaDecision(LocalDateTime.now(reloj));
        solicitud.setMontoAprobado(null);
        solicitud.setComentarioDecision(normalizarComentario(peticion.comentarioDecision()));
        solicitud.setUsuarioDecision(usuario);
        return SolicitudMapper.aRespuesta(solicitud);
    }

    private void validarEstabilidadLaboral(LocalDate inicioLaboral, LocalDate fechaReferencia) {
        int meses = SolicitudTarjeta.calcularMesesAntiguedad(inicioLaboral, fechaReferencia);
        if (meses < MESES_MINIMOS_ESTABILIDAD_LABORAL) {
            throw new ReglaNegocioException(
                    "Se requiere al menos 1 año de estabilidad laboral. Antigüedad actual: " + meses + " meses");
        }
    }

    private void validarSinCuentaDelMismoTipo(Long idCliente, TipoTarjeta tipo) {
        Optional<CuentaCredito> existente = cuentaCreditoRepository.findByClienteIdAndTipoTarjetaAndEstadoCuenta(
                idCliente, tipo, EstadoCuenta.ACTIVA);
        if (existente.isPresent()) {
            throw new ReglaNegocioException("El cliente ya tiene una cuenta " + tipo.getDescripcion()
                    + " (número " + TarjetaMapper.enmascararNumero(existente.get().getNumeroCuenta())
                    + "). Si perdió o dañó la tarjeta, solicite una reposición");
        }
    }

    private void validarIngresoMinimo(SolicitudPeticion peticion) {
        BigDecimal minimo = peticion.tipoTarjeta().getIngresoMinimo();
        if (peticion.ingresoMensual().compareTo(minimo) < 0) {
            throw new ReglaNegocioException("El ingreso mensual de Q " + FormatoMonto.formatear(peticion.ingresoMensual())
                    + " no alcanza el mínimo de Q " + FormatoMonto.formatear(minimo)
                    + " requerido para la tarjeta " + peticion.tipoTarjeta().getDescripcion());
        }
    }

    private void validarMontoAprobado(SolicitudTarjeta solicitud, BigDecimal montoAprobado) {
        BigDecimal maximo = solicitud.getIngresoMensual()
                .multiply(BigDecimal.valueOf(PORCENTAJE_MAXIMO_LIMITE_SOBRE_INGRESO))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
        if (montoAprobado.compareTo(maximo) > 0) {
            throw new ReglaNegocioException("El monto aprobado de Q " + FormatoMonto.formatear(montoAprobado)
                    + " supera el máximo de Q " + FormatoMonto.formatear(maximo) + " ("
                    + PORCENTAJE_MAXIMO_LIMITE_SOBRE_INGRESO + " % del ingreso mensual declarado)");
        }
    }

    private void validarClienteSinMora(Long idCliente) {
        if (estadoPagoService.tieneCuentasEnMora(idCliente)) {
            throw new ReglaNegocioException(MENSAJE_CLIENTE_EN_MORA);
        }
    }

    private List<SolicitudRespuesta> aRespuestas(List<SolicitudTarjeta> solicitudes, boolean incluirDatosSensibles) {
        List<Long> idsCuenta = solicitudes.stream()
                .map(SolicitudTarjeta::getCuenta)
                .filter(cuenta -> cuenta != null)
                .map(CuentaCredito::getId)
                .toList();
        Map<Long, TarjetaCredito> primerasTarjetas = idsCuenta.isEmpty()
                ? Map.of()
                : tarjetaCreditoRepository.listarPrimerasPorCuentas(idsCuenta).stream()
                        .collect(Collectors.toMap(tarjeta -> tarjeta.getCuenta().getId(), Function.identity()));
        return solicitudes.stream()
                .map(solicitud -> SolicitudMapper.aRespuesta(solicitud, solicitud.getCuenta(),
                        solicitud.getCuenta() == null ? null : primerasTarjetas.get(solicitud.getCuenta().getId()),
                        incluirDatosSensibles))
                .toList();
    }

    private SolicitudTarjeta obtenerPendienteConBloqueo(Long id, String accion) {
        SolicitudTarjeta solicitud = solicitudTarjetaRepository.buscarPorIdConBloqueo(id)
                .orElseThrow(() -> crearExcepcionNoEncontrada(id));
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ReglaNegocioException("Solo se pueden " + accion
                    + " solicitudes en estado PENDIENTE. Estado actual: " + solicitud.getEstado());
        }
        return solicitud;
    }

    private String normalizarComentario(String comentario) {
        if (comentario == null || comentario.isBlank()) {
            return null;
        }
        return comentario.trim();
    }

    private RecursoNoEncontradoException crearExcepcionNoEncontrada(Long id) {
        return new RecursoNoEncontradoException("No se encontró la solicitud con id " + id);
    }
}
