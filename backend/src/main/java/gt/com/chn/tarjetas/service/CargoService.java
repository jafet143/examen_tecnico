package gt.com.chn.tarjetas.service;

import gt.com.chn.tarjetas.dto.CargoPeticion;
import gt.com.chn.tarjetas.dto.ExoneracionMoraRespuesta;
import gt.com.chn.tarjetas.dto.MovimientoRespuesta;
import gt.com.chn.tarjetas.dto.ReversoPeticion;
import gt.com.chn.tarjetas.entity.Cargo;
import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.entity.Tarifas;
import gt.com.chn.tarjetas.entity.TipoCargo;
import gt.com.chn.tarjetas.exception.RecursoNoEncontradoException;
import gt.com.chn.tarjetas.exception.ReglaNegocioException;
import gt.com.chn.tarjetas.mapper.MovimientoMapper;
import gt.com.chn.tarjetas.repository.CargoRepository;
import gt.com.chn.tarjetas.repository.CuentaCreditoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CargoService {
    static final String DESCRIPCION_COMISION_REPOSICION = "Comisión por reposición de tarjeta por extravío";

    private final CuentaCreditoRepository cuentaCreditoRepository;
    private final CargoRepository cargoRepository;
    private final ProcesoInteresesService procesoIntereses;
    private final CuentaService cuentaService;
    private final Clock reloj;

    public CargoService(CuentaCreditoRepository cuentaCreditoRepository,
                        CargoRepository cargoRepository,
                        ProcesoInteresesService procesoIntereses,
                        CuentaService cuentaService,
                        Clock reloj) {
        this.cuentaCreditoRepository = cuentaCreditoRepository;
        this.cargoRepository = cargoRepository;
        this.procesoIntereses = procesoIntereses;
        this.cuentaService = cuentaService;
        this.reloj = reloj;
    }

    @Transactional
    public MovimientoRespuesta registrar(Long idCuenta, CargoPeticion peticion, String usuario) {
        if (!peticion.tipoCargo().permiteRegistroManual()) {
            throw new ReglaNegocioException(peticion.tipoCargo().getMensajeSinRegistroManual());
        }
        CuentaCredito cuenta = obtenerCuentaActivaConBloqueo(idCuenta);
        LocalDateTime fecha = peticion.fecha() != null ? peticion.fecha() : LocalDateTime.now(reloj);
        return MovimientoMapper.aRespuesta(crearCargo(cuenta, peticion.tipoCargo(), peticion.monto(), fecha,
                peticion.descripcion().trim(), usuario));
    }

    @Transactional
    public Cargo registrarComisionReposicion(CuentaCredito cuenta, String usuario) {
        return crearCargo(cuenta, TipoCargo.COMISION_REPOSICION, Tarifas.COMISION_REPOSICION_EXTRAVIO,
                LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS), DESCRIPCION_COMISION_REPOSICION, usuario);
    }

    @Transactional
    public MovimientoRespuesta reversar(Long idCargo, ReversoPeticion peticion, String usuario) {
        Long idCuenta = cargoRepository.buscarIdCuentaPorId(idCargo)
                .orElseThrow(() -> crearExcepcionCargoNoEncontrado(idCargo));
        CuentaCredito cuenta = obtenerCuentaActivaConBloqueo(idCuenta);
        Cargo cargo = cargoRepository.findById(idCargo).orElseThrow(() -> crearExcepcionCargoNoEncontrado(idCargo));
        if (cargo.estaReversado()) {
            throw ReglasCuenta.movimientoYaReversado(cargo.getFechaReverso());
        }
        BigDecimal nuevoSaldo = cuenta.getSaldoActual().subtract(cargo.getMonto());
        if (nuevoSaldo.signum() < 0) {
            throw new ReglaNegocioException("No se puede reversar el cargo: el saldo quedaría en Q "
                    + FormatoMonto.formatear(nuevoSaldo) + ". Reverse primero el pago que lo cubrió");
        }
        cargo.reversar(peticion.motivo().trim(), ahora(), usuario);
        cuenta.setSaldoActual(nuevoSaldo);
        return MovimientoMapper.aRespuesta(cargo);
    }

    @Transactional
    public ExoneracionMoraRespuesta exonerarMora(Long idCuenta, ReversoPeticion peticion, String usuario,
                                                 boolean incluirDatosSensibles) {
        CuentaCredito cuenta = obtenerCuentaActivaConBloqueo(idCuenta);
        List<Cargo> recargos = cargoRepository.findByCuentaIdAndTipoCargoAndReversadoFalse(
                idCuenta, TipoCargo.RECARGO_MORA);
        if (recargos.isEmpty()) {
            throw new ReglaNegocioException("La cuenta no tiene recargos por mora pendientes de exonerar");
        }
        BigDecimal total = recargos.stream().map(Cargo::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal nuevoSaldo = cuenta.getSaldoActual().subtract(total);
        if (nuevoSaldo.signum() < 0) {
            throw new ReglaNegocioException("No se puede exonerar la mora: el saldo quedaría en Q "
                    + FormatoMonto.formatear(nuevoSaldo) + ". Reverse primero el pago que la cubrió");
        }
        String motivo = peticion.motivo().trim();
        LocalDateTime ahora = ahora();
        recargos.forEach(recargo -> recargo.reversar(motivo, ahora, usuario));
        cuenta.setSaldoActual(nuevoSaldo);
        return new ExoneracionMoraRespuesta(recargos.size(), total.setScale(2),
                cuentaService.obtener(idCuenta, incluirDatosSensibles));
    }

    private Cargo crearCargo(CuentaCredito cuenta, TipoCargo tipo, BigDecimal monto, LocalDateTime fecha,
                             String descripcion, String usuario) {
        Cargo cargo = new Cargo();
        cargo.setCuenta(cuenta);
        cargo.setTipoCargo(tipo);
        cargo.setMonto(monto);
        cargo.setFecha(fecha);
        cargo.setDescripcion(descripcion);
        cargo.setUsuarioRegistro(usuario);
        cuenta.setSaldoActual(cuenta.getSaldoActual().add(monto));
        return cargoRepository.save(cargo);
    }

    private CuentaCredito obtenerCuentaActivaConBloqueo(Long idCuenta) {
        CuentaCredito cuenta = cuentaCreditoRepository.buscarPorIdConBloqueo(idCuenta)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la cuenta con id " + idCuenta));
        ReglasCuenta.exigirActiva(cuenta);
        procesoIntereses.generarInteresesPendientes(cuenta);
        return cuenta;
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS);
    }

    private RecursoNoEncontradoException crearExcepcionCargoNoEncontrado(Long idCargo) {
        return new RecursoNoEncontradoException("No se encontró el cargo con id " + idCargo);
    }
}
