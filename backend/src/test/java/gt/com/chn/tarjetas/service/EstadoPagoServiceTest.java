package gt.com.chn.tarjetas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.com.chn.tarjetas.entity.Cliente;
import gt.com.chn.tarjetas.entity.Consumo;
import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.repository.CargoRepository;
import gt.com.chn.tarjetas.repository.ConsumoRepository;
import gt.com.chn.tarjetas.repository.CuentaCreditoRepository;
import gt.com.chn.tarjetas.repository.PagoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EstadoPagoServiceTest {

    @Mock
    private CuentaCreditoRepository cuentaCreditoRepository;
    @Mock
    private ConsumoRepository consumoRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private CargoRepository cargoRepository;

    private EstadoPagoService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-09T18:00:00Z"), ZoneId.of("America/Guatemala"));
        servicio = new EstadoPagoService(
                cuentaCreditoRepository, consumoRepository, pagoRepository, cargoRepository, new CalculadoraEstadoPago(),
                reloj);
    }

    private CuentaCredito cuenta(long id, long idCliente) {
        Cliente cliente = new Cliente();
        cliente.setId(idCliente);
        CuentaCredito cuenta = new CuentaCredito();
        cuenta.setId(id);
        cuenta.setCliente(cliente);
        cuenta.setFechaApertura(LocalDateTime.of(2026, 8, 1, 9, 0));
        return cuenta;
    }

    @Test
    @DisplayName("Detecta en lote los clientes con mora usando una consulta de consumos, una de cargos y una de pagos")
    void clientesConMoraEnLote() {
        CuentaCredito enMora = cuenta(1L, 10L);
        CuentaCredito alDia = cuenta(2L, 20L);
        Consumo consumo = new Consumo();
        consumo.setCuenta(enMora);
        consumo.setMonto(new BigDecimal("1000.00"));
        consumo.setFecha(LocalDateTime.of(2026, 8, 20, 10, 0));
        when(cuentaCreditoRepository.listarActivasPorClientes(List.of(10L, 20L))).thenReturn(List.of(enMora, alDia));
        when(consumoRepository.findByCuentaIdIn(List.of(1L, 2L))).thenReturn(List.of(consumo));
        when(pagoRepository.findByCuentaIdIn(List.of(1L, 2L))).thenReturn(List.of());

        Set<Long> idsConMora = servicio.idsClientesConMora(List.of(10L, 20L));

        assertEquals(Set.of(10L), idsConMora);
        verify(consumoRepository, times(1)).findByCuentaIdIn(List.of(1L, 2L));
        verify(cargoRepository, times(1)).findByCuentaIdIn(List.of(1L, 2L));
        verify(pagoRepository, times(1)).findByCuentaIdIn(List.of(1L, 2L));
    }

    @Test
    @DisplayName("Un cliente sin cuentas no tiene mora")
    void clienteSinCuentas() {
        when(cuentaCreditoRepository.listarActivasPorClientes(List.of(5L))).thenReturn(List.of());

        assertFalse(servicio.tieneCuentasEnMora(5L));
        assertTrue(servicio.idsClientesConMora(List.of()).isEmpty());
    }
}
