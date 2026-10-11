package gt.com.chn.tarjetas.service;

import gt.com.chn.tarjetas.entity.Cargo;
import gt.com.chn.tarjetas.entity.CicloFacturacion;
import gt.com.chn.tarjetas.entity.Consumo;
import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.entity.EstadoPago;
import gt.com.chn.tarjetas.entity.EstadoPagoCuenta;
import gt.com.chn.tarjetas.entity.Pago;
import gt.com.chn.tarjetas.entity.Tarifas;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CalculadoraEstadoPago {
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2);
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    public EstadoPagoCuenta calcular(CuentaCredito cuenta, List<Consumo> consumos, List<Cargo> cargos,
                                     List<Pago> pagos, LocalDateTime ahora) {
        List<Consumo> consumosVigentes = consumos.stream().filter(consumo -> !consumo.estaReversado()).toList();
        List<Cargo> cargosVigentes = cargos.stream().filter(cargo -> !cargo.estaReversado()).toList();
        List<Pago> pagosVigentes = pagos.stream().filter(pago -> !pago.estaReversado()).toList();

        LocalDate corteActual = CicloFacturacion.ultimoCorte(ahora);
        BigDecimal saldoAlCorte = saldoAlCorte(cuenta, consumosVigentes, cargosVigentes, pagosVigentes, corteActual);
        BigDecimal pagoMinimo = pagoMinimo(saldoAlCorte);
        BigDecimal pagadoEnElCiclo = pagosPosteriores(pagosVigentes, corteActual, ahora);
        BigDecimal pagoMinimoPendiente = pagoMinimo.subtract(pagadoEnElCiclo).max(CERO);
        BigDecimal pagoContadoPendiente = saldoAlCorte.subtract(pagadoEnElCiclo).max(CERO);

        LocalDate corteVencido = CicloFacturacion.ultimoCorteVencido(ahora);
        BigDecimal minimoVencido = pagoMinimo(
                saldoAlCorte(cuenta, consumosVigentes, cargosVigentes, pagosVigentes, corteVencido));
        BigDecimal faltanteMinimoVencido = minimoVencido
                .subtract(pagosPosteriores(pagosVigentes, corteVencido, ahora)).max(CERO);

        EstadoPago estado;
        BigDecimal montoEnMora = CERO;
        int diasMora = 0;
        if (faltanteMinimoVencido.signum() > 0) {
            estado = EstadoPago.EN_MORA;
            montoEnMora = faltanteMinimoVencido;
            diasMora = (int) ChronoUnit.DAYS.between(
                    CicloFacturacion.fechaLimitePago(corteVencido), ahora.toLocalDate());
        } else if (pagoMinimoPendiente.signum() > 0
                && !ahora.isAfter(CicloFacturacion.finDeDia(CicloFacturacion.fechaLimitePago(corteActual)))) {
            estado = EstadoPago.PAGO_PENDIENTE;
        } else {
            estado = EstadoPago.AL_DIA;
        }
        return new EstadoPagoCuenta(estado, corteActual, CicloFacturacion.fechaLimitePago(corteActual),
                CicloFacturacion.proximoCorte(corteActual), saldoAlCorte, pagoMinimo, pagoMinimoPendiente,
                saldoAlCorte, pagoContadoPendiente, montoEnMora, diasMora);
    }

    public BigDecimal pagoMinimo(BigDecimal saldoAlCorte) {
        return saldoAlCorte.multiply(BigDecimal.valueOf(Tarifas.PORCENTAJE_PAGO_MINIMO))
                .divide(CIEN, 2, RoundingMode.CEILING);
    }

    public BigDecimal saldoAlCorte(CuentaCredito cuenta, List<Consumo> consumos, List<Cargo> cargos,
                                   List<Pago> pagos, LocalDate corte) {
        LocalDateTime finCorte = CicloFacturacion.finDeDia(corte);
        if (finCorte.isBefore(cuenta.getFechaApertura())) {
            return CERO;
        }
        BigDecimal totalConsumos = consumos.stream()
                .filter(consumo -> !consumo.getFecha().isAfter(finCorte))
                .map(Consumo::getMonto)
                .reduce(CERO, BigDecimal::add);
        BigDecimal totalCargos = cargos.stream()
                .filter(cargo -> !cargo.getFecha().isAfter(finCorte))
                .map(Cargo::getMonto)
                .reduce(CERO, BigDecimal::add);
        BigDecimal totalPagos = pagos.stream()
                .filter(pago -> !pago.getFecha().isAfter(finCorte))
                .map(Pago::getMonto)
                .reduce(CERO, BigDecimal::add);
        return totalConsumos.add(totalCargos).subtract(totalPagos).max(CERO);
    }

    public BigDecimal pagosPosteriores(List<Pago> pagos, LocalDate corte, LocalDateTime hasta) {
        LocalDateTime finCorte = CicloFacturacion.finDeDia(corte);
        return pagos.stream()
                .filter(pago -> pago.getFecha().isAfter(finCorte) && !pago.getFecha().isAfter(hasta))
                .map(Pago::getMonto)
                .reduce(CERO, BigDecimal::add);
    }
}
