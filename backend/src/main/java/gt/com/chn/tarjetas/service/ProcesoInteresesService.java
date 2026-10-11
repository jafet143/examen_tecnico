package gt.com.chn.tarjetas.service;

import gt.com.chn.tarjetas.entity.Cargo;
import gt.com.chn.tarjetas.entity.CicloFacturacion;
import gt.com.chn.tarjetas.entity.Consumo;
import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.entity.Pago;
import gt.com.chn.tarjetas.entity.Tarifas;
import gt.com.chn.tarjetas.entity.TipoCargo;
import gt.com.chn.tarjetas.repository.CargoRepository;
import gt.com.chn.tarjetas.repository.ConsumoRepository;
import gt.com.chn.tarjetas.repository.CuentaCreditoRepository;
import gt.com.chn.tarjetas.repository.PagoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ProcesoInteresesService {
    public static final String USUARIO_SISTEMA = "sistema";

    private static final Logger registro = LoggerFactory.getLogger(ProcesoInteresesService.class);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final CuentaCreditoRepository cuentaCreditoRepository;
    private final ConsumoRepository consumoRepository;
    private final PagoRepository pagoRepository;
    private final CargoRepository cargoRepository;
    private final CalculadoraEstadoPago calculadora;
    private final TransactionTemplate transaccion;
    private final Clock reloj;

    public ProcesoInteresesService(CuentaCreditoRepository cuentaCreditoRepository,
                                   ConsumoRepository consumoRepository,
                                   PagoRepository pagoRepository,
                                   CargoRepository cargoRepository,
                                   CalculadoraEstadoPago calculadora,
                                   PlatformTransactionManager administradorTransacciones,
                                   Clock reloj) {
        this.cuentaCreditoRepository = cuentaCreditoRepository;
        this.consumoRepository = consumoRepository;
        this.pagoRepository = pagoRepository;
        this.cargoRepository = cargoRepository;
        this.calculadora = calculadora;
        this.transaccion = new TransactionTemplate(administradorTransacciones);
        this.reloj = reloj;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 5 0 * * *", zone = "America/Guatemala")
    public void generarParaCuentasActivas() {
        List<Long> idsCuenta = cuentaCreditoRepository.listarIdsActivas();
        int generados = 0;
        for (Long idCuenta : idsCuenta) {
            try {
                Integer nuevos = transaccion.execute(estado -> cuentaCreditoRepository.buscarPorIdConBloqueo(idCuenta)
                        .map(this::generarInteresesPendientes).orElse(0));
                generados += nuevos == null ? 0 : nuevos;
            } catch (RuntimeException excepcion) {
                registro.error("No se pudieron generar los intereses de la cuenta {}", idCuenta, excepcion);
            }
        }
        registro.info("Proceso de intereses: {} cuentas revisadas, {} intereses generados", idsCuenta.size(), generados);
    }

    public int generarInteresesPendientes(CuentaCredito cuenta) {
        if (cuenta.estaCancelada()) {
            return 0;
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        List<Consumo> consumos = consumoRepository.findByCuentaId(cuenta.getId()).stream()
                .filter(consumo -> !consumo.estaReversado()).toList();
        List<Pago> pagos = pagoRepository.findByCuentaId(cuenta.getId()).stream()
                .filter(pago -> !pago.estaReversado()).toList();
        List<Cargo> todosLosCargos = cargoRepository.findByCuentaId(cuenta.getId());
        Set<LocalDate> ciclosConInteres = todosLosCargos.stream()
                .filter(cargo -> cargo.getTipoCargo() == TipoCargo.INTERES)
                .map(Cargo::getCicloCorte)
                .collect(Collectors.toSet());
        List<Cargo> cargosVigentes = new ArrayList<>(todosLosCargos.stream()
                .filter(cargo -> !cargo.estaReversado()).toList());

        int generados = 0;
        for (LocalDate corte : ciclosVencidosDesdeLaApertura(cuenta, ahora)) {
            if (ciclosConInteres.contains(corte)) {
                continue;
            }
            Cargo interes = crearInteres(cuenta, corte, consumos, cargosVigentes, pagos);
            if (interes != null) {
                cargosVigentes.add(cargoRepository.save(interes));
                cuenta.setSaldoActual(cuenta.getSaldoActual().add(interes.getMonto()));
                generados++;
            }
        }
        return generados;
    }

    private List<LocalDate> ciclosVencidosDesdeLaApertura(CuentaCredito cuenta, LocalDateTime ahora) {
        List<LocalDate> cortes = new ArrayList<>();
        LocalDate corte = CicloFacturacion.ultimoCorteVencido(ahora);
        while (!CicloFacturacion.finDeDia(corte).isBefore(cuenta.getFechaApertura())) {
            cortes.add(0, corte);
            corte = corte.minusMonths(1);
        }
        return cortes;
    }

    private Cargo crearInteres(CuentaCredito cuenta, LocalDate corte, List<Consumo> consumos,
                               List<Cargo> cargos, List<Pago> pagos) {
        BigDecimal saldoAlCorte = calculadora.saldoAlCorte(cuenta, consumos, cargos, pagos, corte);
        LocalDate fechaLimite = CicloFacturacion.fechaLimitePago(corte);
        BigDecimal pagadoATiempo = calculadora.pagosPosteriores(pagos, corte, CicloFacturacion.finDeDia(fechaLimite));
        BigDecimal base = saldoAlCorte.subtract(pagadoATiempo);
        if (base.signum() <= 0) {
            return null;
        }
        BigDecimal monto = base.multiply(BigDecimal.valueOf(Tarifas.TASA_INTERES_MENSUAL))
                .divide(CIEN, 2, RoundingMode.HALF_UP);
        if (monto.signum() <= 0) {
            return null;
        }
        Cargo interes = new Cargo();
        interes.setCuenta(cuenta);
        interes.setTipoCargo(TipoCargo.INTERES);
        interes.setMonto(monto);
        interes.setFecha(fechaLimite.plusDays(1).atStartOfDay());
        interes.setDescripcion("Intereses del ciclo con corte " + FORMATO_FECHA.format(corte) + " ("
                + Tarifas.TASA_INTERES_MENSUAL + " % sobre Q " + FormatoMonto.formatear(base) + ")");
        interes.setUsuarioRegistro(USUARIO_SISTEMA);
        interes.setCicloCorte(corte);
        return interes;
    }
}
