package gt.com.chn.tarjetas.service;

import gt.com.chn.tarjetas.dto.BloqueoTarjetaPeticion;
import gt.com.chn.tarjetas.dto.ReposicionTarjetaPeticion;
import gt.com.chn.tarjetas.dto.TarjetaRespuesta;
import gt.com.chn.tarjetas.entity.Cargo;
import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.entity.MotivoBloqueo;
import gt.com.chn.tarjetas.entity.MotivoReposicion;
import gt.com.chn.tarjetas.entity.TarjetaCredito;
import gt.com.chn.tarjetas.entity.TipoTarjeta;
import gt.com.chn.tarjetas.exception.RecursoNoEncontradoException;
import gt.com.chn.tarjetas.exception.ReglaNegocioException;
import gt.com.chn.tarjetas.mapper.TarjetaMapper;
import gt.com.chn.tarjetas.repository.CuentaCreditoRepository;
import gt.com.chn.tarjetas.repository.TarjetaCreditoRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TarjetaService {
    private static final int MAXIMO_INTENTOS_NUMERO = 10;
    private static final int DIGITOS_FINALES = 4;

    private final TarjetaCreditoRepository tarjetaCreditoRepository;
    private final CuentaCreditoRepository cuentaCreditoRepository;
    private final GeneradorNumeroTarjeta generadorNumeroTarjeta;
    private final CargoService cargoService;
    private final ProcesoInteresesService procesoIntereses;
    private final Clock reloj;

    public TarjetaService(TarjetaCreditoRepository tarjetaCreditoRepository,
                          CuentaCreditoRepository cuentaCreditoRepository,
                          GeneradorNumeroTarjeta generadorNumeroTarjeta,
                          CargoService cargoService,
                          ProcesoInteresesService procesoIntereses,
                          Clock reloj) {
        this.tarjetaCreditoRepository = tarjetaCreditoRepository;
        this.cuentaCreditoRepository = cuentaCreditoRepository;
        this.generadorNumeroTarjeta = generadorNumeroTarjeta;
        this.cargoService = cargoService;
        this.procesoIntereses = procesoIntereses;
        this.reloj = reloj;
    }

    public String generarNumeroUnico(TipoTarjeta tipo) {
        for (int intento = 0; intento < MAXIMO_INTENTOS_NUMERO; intento++) {
            String numero = generadorNumeroTarjeta.generar(tipo);
            if (!tarjetaCreditoRepository.existsByNumeroTarjeta(numero)) {
                return numero;
            }
        }
        throw new IllegalStateException(
                "No fue posible generar un número de tarjeta único tras " + MAXIMO_INTENTOS_NUMERO + " intentos");
    }

    @Transactional
    public TarjetaCredito emitirTarjeta(CuentaCredito cuenta, String numeroTarjeta, TarjetaCredito anterior) {
        LocalDateTime ahora = LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS);
        TarjetaCredito tarjeta = new TarjetaCredito();
        tarjeta.setCuenta(cuenta);
        tarjeta.setNumeroTarjeta(numeroTarjeta);
        tarjeta.setFechaEmision(ahora);
        tarjeta.setFechaVencimiento(TarjetaCredito.calcularFechaVencimiento(ahora));
        tarjeta.setTarjetaAnterior(anterior);
        return tarjetaCreditoRepository.save(tarjeta);
    }

    @Transactional(readOnly = true)
    public TarjetaRespuesta obtener(Long idTarjeta, boolean incluirDatosSensibles) {
        return tarjetaCreditoRepository.buscarDetallePorId(idTarjeta)
                .map(tarjeta -> TarjetaMapper.aRespuesta(tarjeta, incluirDatosSensibles, LocalDate.now(reloj)))
                .orElseThrow(() -> crearExcepcionNoEncontrada(idTarjeta));
    }

    @Transactional
    public TarjetaRespuesta bloquear(Long idTarjeta, BloqueoTarjetaPeticion peticion, String usuario,
                                     boolean incluirDatosSensibles) {
        TarjetaCredito tarjeta = obtenerConBloqueoDeCuenta(idTarjeta);
        validarNoReemplazada(tarjeta);
        ReglasCuenta.exigirActiva(tarjeta.getCuenta());
        if (tarjeta.estaBloqueada()) {
            throw new ReglaNegocioException("La tarjeta ya se encuentra bloqueada");
        }
        tarjeta.bloquear(peticion.motivoBloqueo(), normalizarComentario(peticion.comentario()),
                ahora(), usuario);
        return TarjetaMapper.aRespuesta(tarjeta, incluirDatosSensibles, LocalDate.now(reloj));
    }

    @Transactional
    public TarjetaRespuesta desbloquear(Long idTarjeta, boolean incluirDatosSensibles) {
        TarjetaCredito tarjeta = obtenerConBloqueoDeCuenta(idTarjeta);
        validarNoReemplazada(tarjeta);
        ReglasCuenta.exigirActiva(tarjeta.getCuenta());
        if (!tarjeta.estaBloqueada()) {
            throw new ReglaNegocioException("La tarjeta no está bloqueada");
        }
        if (!tarjeta.puedeDesbloquearse()) {
            throw new ReglaNegocioException("Una tarjeta bloqueada por "
                    + tarjeta.getMotivoBloqueo().getDescripcion() + " no puede desbloquearse. Solicite una reposición");
        }
        tarjeta.desbloquear();
        return TarjetaMapper.aRespuesta(tarjeta, incluirDatosSensibles, LocalDate.now(reloj));
    }

    @Transactional
    public TarjetaRespuesta reponer(Long idTarjeta, ReposicionTarjetaPeticion peticion, String usuario,
                                    boolean incluirDatosSensibles) {
        MotivoReposicion motivo = peticion.motivoReposicion();
        if (!motivo.esSolicitable()) {
            throw new ReglaNegocioException("El motivo " + motivo.getDescripcion()
                    + " no es válido para una reposición; use la renovación de tarjetas vencidas");
        }
        TarjetaCredito actual = obtenerConBloqueoDeCuenta(idTarjeta);
        validarNoReemplazada(actual);
        CuentaCredito cuenta = actual.getCuenta();
        ReglasCuenta.exigirActiva(cuenta);
        validarRequisitosReposicion(peticion);
        procesoIntereses.generarInteresesPendientes(cuenta);
        String comentario = normalizarComentario(peticion.comentario());
        LocalDateTime ahora = ahora();

        MotivoBloqueo motivoBloqueoPrevio = motivo.getMotivoBloqueoPrevio();
        if (motivoBloqueoPrevio != null && !actual.estaBloqueada()) {
            actual.bloquear(motivoBloqueoPrevio, comentario, ahora, usuario);
        }
        actual.reemplazar(motivo, comentario, ahora, usuario,
                motivo == MotivoReposicion.ROBO ? Boolean.TRUE : null,
                motivo == MotivoReposicion.DETERIORO ? Boolean.TRUE : null);
        TarjetaCredito nueva = reemplazarPorNueva(actual, cuenta);
        if (motivo == MotivoReposicion.EXTRAVIO) {
            Cargo comision = cargoService.registrarComisionReposicion(cuenta, usuario);
            actual.setIdCargoReposicion(comision.getId());
        }
        return TarjetaMapper.aRespuesta(nueva, incluirDatosSensibles, LocalDate.now(reloj));
    }

    @Transactional
    public TarjetaRespuesta renovar(Long idTarjeta, String usuario, boolean incluirDatosSensibles) {
        TarjetaCredito actual = obtenerConBloqueoDeCuenta(idTarjeta);
        validarNoReemplazada(actual);
        CuentaCredito cuenta = actual.getCuenta();
        ReglasCuenta.exigirActiva(cuenta);
        procesoIntereses.generarInteresesPendientes(cuenta);
        LocalDate hoy = LocalDate.now(reloj);
        if (!hoy.isAfter(actual.getFechaVencimiento())) {
            throw new ReglaNegocioException("La tarjeta vence el " + ReglasCuenta.formatear(actual.getFechaVencimiento())
                    + "; solo puede renovarse cuando haya vencido");
        }
        if (actual.estaBloqueada()) {
            throw new ReglaNegocioException("La tarjeta está bloqueada por "
                    + actual.getMotivoBloqueo().getDescripcion() + "; resuelva el bloqueo antes de renovarla");
        }
        actual.reemplazar(MotivoReposicion.RENOVACION, null, ahora(), usuario, null, null);
        return TarjetaMapper.aRespuesta(reemplazarPorNueva(actual, cuenta), incluirDatosSensibles, hoy);
    }

    private TarjetaCredito reemplazarPorNueva(TarjetaCredito reemplazada, CuentaCredito cuenta) {
        tarjetaCreditoRepository.flush();
        return emitirTarjeta(cuenta, generarNumeroUnico(cuenta.getTipoTarjeta()), reemplazada);
    }

    private void validarRequisitosReposicion(ReposicionTarjetaPeticion peticion) {
        if (peticion.motivoReposicion() == MotivoReposicion.ROBO && !peticion.denunciaPresentada()) {
            throw new ReglaNegocioException("Para reponer por robo el cliente debe presentar la denuncia del robo");
        }
        if (peticion.motivoReposicion() == MotivoReposicion.DETERIORO && !peticion.tarjetaDeterioradaPresentada()) {
            throw new ReglaNegocioException(
                    "Para reponer por deterioro el cliente debe presentar la tarjeta deteriorada");
        }
    }

    private void validarNoReemplazada(TarjetaCredito tarjeta) {
        if (!tarjeta.estaReemplazada()) {
            return;
        }
        TarjetaCredito reemplazo = tarjetaCreditoRepository.findByTarjetaAnteriorId(tarjeta.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "La tarjeta " + tarjeta.getId() + " está reemplazada pero no tiene tarjeta sucesora"));
        String numero = reemplazo.getNumeroTarjeta();
        throw new ReglaNegocioException("La tarjeta ya fue reemplazada por la tarjeta terminada en "
                + numero.substring(numero.length() - DIGITOS_FINALES));
    }

    private TarjetaCredito obtenerConBloqueoDeCuenta(Long idTarjeta) {
        Long idCuenta = tarjetaCreditoRepository.buscarIdCuentaPorId(idTarjeta)
                .orElseThrow(() -> crearExcepcionNoEncontrada(idTarjeta));
        cuentaCreditoRepository.buscarPorIdConBloqueo(idCuenta)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la cuenta con id " + idCuenta));
        return obtenerConBloqueo(idTarjeta);
    }

    private TarjetaCredito obtenerConBloqueo(Long idTarjeta) {
        return tarjetaCreditoRepository.buscarPorIdConBloqueo(idTarjeta)
                .orElseThrow(() -> crearExcepcionNoEncontrada(idTarjeta));
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS);
    }

    private String normalizarComentario(String comentario) {
        return comentario == null || comentario.isBlank() ? null : comentario.trim();
    }

    private RecursoNoEncontradoException crearExcepcionNoEncontrada(Long idTarjeta) {
        return new RecursoNoEncontradoException("No se encontró la tarjeta con id " + idTarjeta);
    }
}
