package gt.com.chn.tarjetas.service;

import gt.com.chn.tarjetas.dto.MovimientoPeticion;
import gt.com.chn.tarjetas.dto.MovimientoRespuesta;
import gt.com.chn.tarjetas.dto.ReversoPeticion;
import gt.com.chn.tarjetas.entity.Consumo;
import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.entity.EstadoPago;
import gt.com.chn.tarjetas.entity.EstadoPagoCuenta;
import gt.com.chn.tarjetas.entity.Pago;
import gt.com.chn.tarjetas.entity.TarjetaCredito;
import gt.com.chn.tarjetas.exception.RecursoNoEncontradoException;
import gt.com.chn.tarjetas.exception.ReglaNegocioException;
import gt.com.chn.tarjetas.mapper.MovimientoMapper;
import gt.com.chn.tarjetas.repository.CargoRepository;
import gt.com.chn.tarjetas.repository.ConsumoRepository;
import gt.com.chn.tarjetas.repository.CuentaCreditoRepository;
import gt.com.chn.tarjetas.repository.PagoRepository;
import gt.com.chn.tarjetas.repository.TarjetaCreditoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovimientoCuentaService {
    private final CuentaCreditoRepository cuentaCreditoRepository;
    private final TarjetaCreditoRepository tarjetaCreditoRepository;
    private final ConsumoRepository consumoRepository;
    private final PagoRepository pagoRepository;
    private final CargoRepository cargoRepository;
    private final EstadoPagoService estadoPagoService;
    private final ProcesoInteresesService procesoIntereses;
    private final Clock reloj;

    public MovimientoCuentaService(CuentaCreditoRepository cuentaCreditoRepository,
                                   TarjetaCreditoRepository tarjetaCreditoRepository,
                                   ConsumoRepository consumoRepository,
                                   PagoRepository pagoRepository,
                                   CargoRepository cargoRepository,
                                   EstadoPagoService estadoPagoService,
                                   ProcesoInteresesService procesoIntereses,
                                   Clock reloj) {
        this.cuentaCreditoRepository = cuentaCreditoRepository;
        this.tarjetaCreditoRepository = tarjetaCreditoRepository;
        this.consumoRepository = consumoRepository;
        this.pagoRepository = pagoRepository;
        this.cargoRepository = cargoRepository;
        this.estadoPagoService = estadoPagoService;
        this.procesoIntereses = procesoIntereses;
        this.reloj = reloj;
    }

    @Transactional
    public MovimientoRespuesta registrarConsumo(Long idCuenta, MovimientoPeticion peticion, String usuario) {
        CuentaCredito cuenta = obtenerCuentaActivaConBloqueo(idCuenta);
        TarjetaCredito vigente = tarjetaCreditoRepository.buscarVigentePorCuenta(idCuenta)
                .orElseThrow(() -> new IllegalStateException("La cuenta " + idCuenta + " no tiene tarjeta vigente"));
        validarSinBloqueoNiMora(cuenta, vigente);
        BigDecimal nuevoSaldo = cuenta.getSaldoActual().add(peticion.monto());
        if (nuevoSaldo.compareTo(cuenta.getLimiteCredito()) > 0) {
            throw new ReglaNegocioException("El consumo de Q " + FormatoMonto.formatear(peticion.monto())
                    + " excede el crédito disponible de Q " + FormatoMonto.formatear(cuenta.obtenerCreditoDisponible()));
        }
        Consumo consumo = new Consumo();
        consumo.setCuenta(cuenta);
        consumo.setTarjeta(vigente);
        consumo.setMonto(peticion.monto());
        consumo.setFecha(peticion.fecha() != null ? peticion.fecha() : LocalDateTime.now(reloj));
        consumo.setDescripcion(peticion.descripcion().trim());
        consumo.setUsuarioRegistro(usuario);
        cuenta.setSaldoActual(nuevoSaldo);
        return MovimientoMapper.aRespuesta(consumoRepository.save(consumo));
    }

    @Transactional
    public MovimientoRespuesta registrarPago(Long idCuenta, MovimientoPeticion peticion, String usuario) {
        CuentaCredito cuenta = obtenerCuentaActivaConBloqueo(idCuenta);
        if (cuenta.getSaldoActual().signum() == 0) {
            throw new ReglaNegocioException("La cuenta no tiene saldo pendiente de pago");
        }
        if (peticion.monto().compareTo(cuenta.getSaldoActual()) > 0) {
            throw new ReglaNegocioException("El pago de Q " + FormatoMonto.formatear(peticion.monto())
                    + " excede el saldo actual de Q " + FormatoMonto.formatear(cuenta.getSaldoActual()));
        }
        Pago pago = new Pago();
        pago.setCuenta(cuenta);
        pago.setMonto(peticion.monto());
        pago.setFecha(peticion.fecha() != null ? peticion.fecha() : LocalDateTime.now(reloj));
        pago.setDescripcion(peticion.descripcion().trim());
        pago.setUsuarioRegistro(usuario);
        cuenta.setSaldoActual(cuenta.getSaldoActual().subtract(peticion.monto()));
        return MovimientoMapper.aRespuesta(pagoRepository.save(pago));
    }

    @Transactional(readOnly = true)
    public List<MovimientoRespuesta> listarMovimientos(Long idCuenta) {
        if (!cuentaCreditoRepository.existsById(idCuenta)) {
            throw crearExcepcionCuentaNoEncontrada(idCuenta);
        }
        List<MovimientoRespuesta> movimientos = new ArrayList<>();
        consumoRepository.listarPorCuenta(idCuenta).forEach(consumo -> movimientos.add(MovimientoMapper.aRespuesta(consumo)));
        pagoRepository.findByCuentaId(idCuenta).forEach(pago -> movimientos.add(MovimientoMapper.aRespuesta(pago)));
        cargoRepository.findByCuentaId(idCuenta).forEach(cargo -> movimientos.add(MovimientoMapper.aRespuesta(cargo)));
        movimientos.sort(Comparator.comparing(MovimientoRespuesta::fecha)
                .thenComparing(MovimientoRespuesta::fechaRegistro)
                .reversed());
        return movimientos;
    }

    @Transactional
    public MovimientoRespuesta reversarConsumo(Long idConsumo, ReversoPeticion peticion, String usuario) {
        Long idCuenta = consumoRepository.buscarIdCuentaPorId(idConsumo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el consumo con id " + idConsumo));
        CuentaCredito cuenta = obtenerCuentaActivaConBloqueo(idCuenta);
        Consumo consumo = consumoRepository.findById(idConsumo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el consumo con id " + idConsumo));
        if (consumo.estaReversado()) {
            throw ReglasCuenta.movimientoYaReversado(consumo.getFechaReverso());
        }
        BigDecimal nuevoSaldo = cuenta.getSaldoActual().subtract(consumo.getMonto());
        if (nuevoSaldo.signum() < 0) {
            throw new ReglaNegocioException("No se puede reversar el consumo: el saldo quedaría en Q "
                    + FormatoMonto.formatear(nuevoSaldo) + ". Reverse primero el pago que lo cubrió");
        }
        consumo.reversar(peticion.motivo().trim(), ahora(), usuario);
        cuenta.setSaldoActual(nuevoSaldo);
        return MovimientoMapper.aRespuesta(consumo);
    }

    @Transactional
    public MovimientoRespuesta reversarPago(Long idPago, ReversoPeticion peticion, String usuario) {
        Long idCuenta = pagoRepository.buscarIdCuentaPorId(idPago)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el pago con id " + idPago));
        CuentaCredito cuenta = obtenerCuentaActivaConBloqueo(idCuenta);
        Pago pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el pago con id " + idPago));
        if (pago.estaReversado()) {
            throw ReglasCuenta.movimientoYaReversado(pago.getFechaReverso());
        }
        BigDecimal nuevoSaldo = cuenta.getSaldoActual().add(pago.getMonto());
        if (nuevoSaldo.compareTo(cuenta.getLimiteCredito()) > 0) {
            throw new ReglaNegocioException("No se puede reversar el pago: el saldo de Q "
                    + FormatoMonto.formatear(nuevoSaldo) + " superaría el límite de crédito de Q "
                    + FormatoMonto.formatear(cuenta.getLimiteCredito()));
        }
        pago.reversar(peticion.motivo().trim(), ahora(), usuario);
        cuenta.setSaldoActual(nuevoSaldo);
        return MovimientoMapper.aRespuesta(pago);
    }

    private void validarSinBloqueoNiMora(CuentaCredito cuenta, TarjetaCredito vigente) {
        if (vigente.estaBloqueada()) {
            throw new ReglaNegocioException("La tarjeta está bloqueada por "
                    + vigente.getMotivoBloqueo().getDescripcion() + ". No se pueden registrar consumos");
        }
        if (vigente.estaVencida(LocalDate.now(reloj))) {
            throw new ReglaNegocioException("La tarjeta venció el " + ReglasCuenta.formatear(vigente.getFechaVencimiento())
                    + ". Debe renovarse antes de registrar consumos");
        }
        EstadoPagoCuenta estadoPago = estadoPagoService.calcular(cuenta);
        if (estadoPago.estadoPago() == EstadoPago.EN_MORA) {
            throw new ReglaNegocioException("La cuenta está en mora por Q "
                    + FormatoMonto.formatear(estadoPago.montoEnMora())
                    + ". Registre un pago para ponerse al día antes de realizar consumos");
        }
    }

    private CuentaCredito obtenerCuentaActivaConBloqueo(Long idCuenta) {
        CuentaCredito cuenta = cuentaCreditoRepository.buscarPorIdConBloqueo(idCuenta)
                .orElseThrow(() -> crearExcepcionCuentaNoEncontrada(idCuenta));
        ReglasCuenta.exigirActiva(cuenta);
        procesoIntereses.generarInteresesPendientes(cuenta);
        return cuenta;
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS);
    }

    private RecursoNoEncontradoException crearExcepcionCuentaNoEncontrada(Long idCuenta) {
        return new RecursoNoEncontradoException("No se encontró la cuenta con id " + idCuenta);
    }
}
