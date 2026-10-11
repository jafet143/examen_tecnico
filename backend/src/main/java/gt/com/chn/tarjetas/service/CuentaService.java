package gt.com.chn.tarjetas.service;

import gt.com.chn.tarjetas.dto.CancelacionCuentaPeticion;
import gt.com.chn.tarjetas.dto.CuentaRespuesta;
import gt.com.chn.tarjetas.dto.TarjetaRespuesta;
import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.entity.EstadoCuenta;
import gt.com.chn.tarjetas.entity.EstadoPago;
import gt.com.chn.tarjetas.entity.EstadoPagoCuenta;
import gt.com.chn.tarjetas.entity.TipoCargo;
import gt.com.chn.tarjetas.entity.SolicitudTarjeta;
import gt.com.chn.tarjetas.entity.TarjetaCredito;
import gt.com.chn.tarjetas.exception.RecursoNoEncontradoException;
import gt.com.chn.tarjetas.exception.ReglaNegocioException;
import gt.com.chn.tarjetas.mapper.CuentaMapper;
import gt.com.chn.tarjetas.mapper.TarjetaMapper;
import gt.com.chn.tarjetas.repository.CargoRepository;
import gt.com.chn.tarjetas.repository.ClienteRepository;
import gt.com.chn.tarjetas.repository.CuentaCreditoRepository;
import gt.com.chn.tarjetas.repository.TarjetaCreditoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CuentaService {
    private final CuentaCreditoRepository cuentaCreditoRepository;
    private final TarjetaCreditoRepository tarjetaCreditoRepository;
    private final ClienteRepository clienteRepository;
    private final CargoRepository cargoRepository;
    private final EstadoPagoService estadoPagoService;
    private final ProcesoInteresesService procesoIntereses;
    private final Clock reloj;

    public CuentaService(CuentaCreditoRepository cuentaCreditoRepository,
                         TarjetaCreditoRepository tarjetaCreditoRepository,
                         ClienteRepository clienteRepository,
                         CargoRepository cargoRepository,
                         EstadoPagoService estadoPagoService,
                         ProcesoInteresesService procesoIntereses,
                         Clock reloj) {
        this.cuentaCreditoRepository = cuentaCreditoRepository;
        this.tarjetaCreditoRepository = tarjetaCreditoRepository;
        this.clienteRepository = clienteRepository;
        this.cargoRepository = cargoRepository;
        this.estadoPagoService = estadoPagoService;
        this.procesoIntereses = procesoIntereses;
        this.reloj = reloj;
    }

    @Transactional
    public CuentaCredito abrirCuenta(SolicitudTarjeta solicitud, String numeroCuenta) {
        CuentaCredito cuenta = new CuentaCredito();
        cuenta.setNumeroCuenta(numeroCuenta);
        cuenta.setCliente(solicitud.getCliente());
        cuenta.setSolicitud(solicitud);
        cuenta.setTipoTarjeta(solicitud.getTipoTarjeta());
        cuenta.setLimiteCredito(solicitud.getMontoAprobado());
        cuenta.setSaldoActual(BigDecimal.ZERO.setScale(2));
        cuenta.setFechaApertura(LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS));
        return cuentaCreditoRepository.save(cuenta);
    }

    @Transactional(readOnly = true)
    public List<CuentaRespuesta> listarTodas(EstadoCuenta estadoOpcional, boolean incluirDatosSensibles) {
        return aRespuestas(cuentaCreditoRepository.listarConCliente(estadoOpcional), incluirDatosSensibles);
    }

    @Transactional(readOnly = true)
    public CuentaRespuesta obtener(Long idCuenta, boolean incluirDatosSensibles) {
        CuentaCredito cuenta = cuentaCreditoRepository.buscarDetallePorId(idCuenta)
                .orElseThrow(() -> crearExcepcionNoEncontrada(idCuenta));
        return aRespuestas(List.of(cuenta), incluirDatosSensibles).get(0);
    }

    @Transactional(readOnly = true)
    public List<CuentaRespuesta> listarPorCliente(Long idCliente, boolean incluirDatosSensibles) {
        if (!clienteRepository.existsById(idCliente)) {
            throw new RecursoNoEncontradoException("No se encontró el cliente con id " + idCliente);
        }
        return aRespuestas(cuentaCreditoRepository.listarPorCliente(idCliente), incluirDatosSensibles);
    }

    @Transactional
    public CuentaRespuesta cancelar(Long idCuenta, CancelacionCuentaPeticion peticion, String usuario,
                                    boolean incluirDatosSensibles) {
        CuentaCredito cuenta = cuentaCreditoRepository.buscarPorIdConBloqueo(idCuenta)
                .orElseThrow(() -> crearExcepcionNoEncontrada(idCuenta));
        if (cuenta.estaCancelada()) {
            throw new ReglaNegocioException("La cuenta ya fue cancelada el "
                    + ReglasCuenta.formatear(cuenta.getFechaCancelacion().toLocalDate()));
        }
        procesoIntereses.generarInteresesPendientes(cuenta);
        if (estadoPagoService.calcular(cuenta).estadoPago() != EstadoPago.AL_DIA) {
            throw new ReglaNegocioException("Para cancelar, la cuenta debe estar al día en sus pagos");
        }
        if (cuenta.getSaldoActual().signum() != 0) {
            throw new ReglaNegocioException("Para cancelar, la cuenta debe tener saldo Q 0.00. Saldo actual: Q "
                    + FormatoMonto.formatear(cuenta.getSaldoActual()));
        }
        TarjetaCredito vigente = tarjetaCreditoRepository.buscarVigentePorCuenta(idCuenta)
                .orElseThrow(() -> new IllegalStateException("La cuenta " + idCuenta + " no tiene tarjeta vigente"));
        String comentario = peticion.comentario() == null || peticion.comentario().isBlank()
                ? null : peticion.comentario().trim();
        cuenta.cancelar(peticion.motivoCancelacion(), comentario,
                LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS), usuario);
        vigente.cancelar();
        return aRespuestas(List.of(cuenta), incluirDatosSensibles).get(0);
    }

    @Transactional(readOnly = true)
    public List<TarjetaRespuesta> listarTarjetas(Long idCuenta, boolean incluirDatosSensibles) {
        if (!cuentaCreditoRepository.existsById(idCuenta)) {
            throw crearExcepcionNoEncontrada(idCuenta);
        }
        return tarjetaCreditoRepository.listarPorCuentas(List.of(idCuenta)).stream()
                .map(tarjeta -> TarjetaMapper.aRespuesta(tarjeta, incluirDatosSensibles, LocalDate.now(reloj)))
                .toList();
    }

    private List<CuentaRespuesta> aRespuestas(List<CuentaCredito> cuentas, boolean incluirDatosSensibles) {
        if (cuentas.isEmpty()) {
            return List.of();
        }
        Map<Long, EstadoPagoCuenta> estados = estadoPagoService.calcular(cuentas);
        Map<Long, TarjetaRespuesta> vigentes = new HashMap<>();
        Map<Long, Integer> reposiciones = new HashMap<>();
        List<Long> ids = cuentas.stream().map(CuentaCredito::getId).toList();
        Set<Long> conRecargosMora = cargoRepository.idsCuentasConCargoVigente(ids, TipoCargo.RECARGO_MORA);
        LocalDate hoy = LocalDate.now(reloj);
        for (TarjetaCredito tarjeta : tarjetaCreditoRepository.listarPorCuentas(ids)) {
            Long idCuenta = tarjeta.getCuenta().getId();
            if (tarjeta.estaReemplazada()) {
                reposiciones.merge(idCuenta, 1, Integer::sum);
            } else {
                vigentes.put(idCuenta, TarjetaMapper.aRespuesta(tarjeta, incluirDatosSensibles, hoy));
            }
        }
        return cuentas.stream()
                .map(cuenta -> CuentaMapper.aRespuesta(cuenta, estados.get(cuenta.getId()),
                        vigentes.get(cuenta.getId()), reposiciones.getOrDefault(cuenta.getId(), 0),
                        !cuenta.estaCancelada() && conRecargosMora.contains(cuenta.getId()),
                        incluirDatosSensibles))
                .toList();
    }

    private RecursoNoEncontradoException crearExcepcionNoEncontrada(Long idCuenta) {
        return new RecursoNoEncontradoException("No se encontró la cuenta con id " + idCuenta);
    }
}
