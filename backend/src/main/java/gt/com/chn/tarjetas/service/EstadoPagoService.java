package gt.com.chn.tarjetas.service;

import gt.com.chn.tarjetas.entity.Cargo;
import gt.com.chn.tarjetas.entity.Consumo;
import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.entity.EstadoPago;
import gt.com.chn.tarjetas.entity.EstadoPagoCuenta;
import gt.com.chn.tarjetas.entity.Pago;
import gt.com.chn.tarjetas.repository.CargoRepository;
import gt.com.chn.tarjetas.repository.ConsumoRepository;
import gt.com.chn.tarjetas.repository.CuentaCreditoRepository;
import gt.com.chn.tarjetas.repository.PagoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstadoPagoService {
    private final CuentaCreditoRepository cuentaCreditoRepository;
    private final ConsumoRepository consumoRepository;
    private final PagoRepository pagoRepository;
    private final CargoRepository cargoRepository;
    private final CalculadoraEstadoPago calculadora;
    private final Clock reloj;

    public EstadoPagoService(CuentaCreditoRepository cuentaCreditoRepository,
                             ConsumoRepository consumoRepository,
                             PagoRepository pagoRepository,
                             CargoRepository cargoRepository,
                             CalculadoraEstadoPago calculadora,
                             Clock reloj) {
        this.cuentaCreditoRepository = cuentaCreditoRepository;
        this.consumoRepository = consumoRepository;
        this.pagoRepository = pagoRepository;
        this.cargoRepository = cargoRepository;
        this.calculadora = calculadora;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public EstadoPagoCuenta calcular(CuentaCredito cuenta) {
        return calcular(List.of(cuenta)).get(cuenta.getId());
    }

    @Transactional(readOnly = true)
    public Map<Long, EstadoPagoCuenta> calcular(Collection<CuentaCredito> cuentas) {
        if (cuentas.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = cuentas.stream().map(CuentaCredito::getId).toList();
        Map<Long, List<Consumo>> consumos = consumoRepository.findByCuentaIdIn(ids).stream()
                .collect(Collectors.groupingBy(consumo -> consumo.getCuenta().getId()));
        Map<Long, List<Cargo>> cargos = cargoRepository.findByCuentaIdIn(ids).stream()
                .collect(Collectors.groupingBy(cargo -> cargo.getCuenta().getId()));
        Map<Long, List<Pago>> pagos = pagoRepository.findByCuentaIdIn(ids).stream()
                .collect(Collectors.groupingBy(pago -> pago.getCuenta().getId()));
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Map<Long, EstadoPagoCuenta> estados = new HashMap<>();
        for (CuentaCredito cuenta : cuentas) {
            estados.put(cuenta.getId(), calculadora.calcular(
                    cuenta,
                    consumos.getOrDefault(cuenta.getId(), List.of()),
                    cargos.getOrDefault(cuenta.getId(), List.of()),
                    pagos.getOrDefault(cuenta.getId(), List.of()),
                    ahora));
        }
        return estados;
    }

    @Transactional(readOnly = true)
    public boolean tieneCuentasEnMora(Long idCliente) {
        return !idsClientesConMora(List.of(idCliente)).isEmpty();
    }

    @Transactional(readOnly = true)
    public Set<Long> idsClientesConMora(Collection<Long> idsCliente) {
        if (idsCliente.isEmpty()) {
            return Set.of();
        }
        List<CuentaCredito> cuentas = cuentaCreditoRepository.listarActivasPorClientes(idsCliente);
        Map<Long, EstadoPagoCuenta> estados = calcular(cuentas);
        return cuentas.stream()
                .filter(cuenta -> estados.get(cuenta.getId()).estadoPago() == EstadoPago.EN_MORA)
                .map(cuenta -> cuenta.getCliente().getId())
                .collect(Collectors.toSet());
    }
}
