package gt.com.chn.tarjetas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.com.chn.tarjetas.dto.ClientePeticion;
import gt.com.chn.tarjetas.dto.ClienteRespuesta;
import gt.com.chn.tarjetas.entity.Cliente;
import gt.com.chn.tarjetas.entity.EstadoCuenta;
import gt.com.chn.tarjetas.exception.RecursoDuplicadoException;
import gt.com.chn.tarjetas.exception.RecursoNoEncontradoException;
import gt.com.chn.tarjetas.exception.ReglaNegocioException;
import gt.com.chn.tarjetas.repository.ClienteRepository;
import gt.com.chn.tarjetas.repository.CuentaCreditoRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private CuentaCreditoRepository cuentaCreditoRepository;
    @Mock
    private EstadoPagoService estadoPagoService;
    private ClienteService clienteService;

    private static final Clock RELOJ_FIJO =
            Clock.fixed(Instant.parse("2026-10-09T18:00:00Z"), ZoneId.of("America/Guatemala"));

    @BeforeEach
    void crearServicio() {
        clienteService = new ClienteService(clienteRepository, cuentaCreditoRepository, estadoPagoService, RELOJ_FIJO);
    }

    private ClientePeticion crearPeticion(LocalDate fechaNacimiento) {
        return new ClientePeticion("  Ana ", "Morales", " 2456789010101 ", fechaNacimiento,
                "Zona 1", "ana@correo.com", "5555-1234");
    }

    @Test
    @DisplayName("Crear con identificación duplicada lanza 409")
    void crearIdentificacionDuplicada() {
        when(clienteRepository.existsByNumeroIdentificacion("2456789010101")).thenReturn(true);

        RecursoDuplicadoException excepcion = assertThrows(RecursoDuplicadoException.class,
                () -> clienteService.crear(crearPeticion(LocalDate.of(1990, 4, 15))));

        assertEquals("Ya existe un cliente con el número de identificación 2456789010101", excepcion.getMessage());
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear de un menor de edad lanza regla de negocio")
    void crearMenorDeEdad() {
        assertThrows(ReglaNegocioException.class,
                () -> clienteService.crear(crearPeticion(LocalDate.now(RELOJ_FIJO).minusYears(17))));
    }

    @Test
    @DisplayName("Crear aplica trim a los textos y guarda el cliente")
    void crearAplicaTrim() {
        when(clienteRepository.existsByNumeroIdentificacion("2456789010101")).thenReturn(false);
        when(clienteRepository.siguienteCodigoCliente()).thenReturn(10000007L);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        ClienteRespuesta respuesta = clienteService.crear(crearPeticion(LocalDate.of(1990, 4, 15)));

        assertEquals("10000007", respuesta.codigoCliente());
        assertEquals("Ana", respuesta.nombre());
        assertEquals("2456789010101", respuesta.numeroIdentificacion());
        assertEquals("Ana Morales", respuesta.nombreCompleto());
    }

    @Test
    @DisplayName("El código de cliente sale de la secuencia, una sola vez por cliente nuevo")
    void crearUsaElSiguienteValorDeLaSecuencia() {
        when(clienteRepository.siguienteCodigoCliente()).thenReturn(10000008L);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        clienteService.crear(crearPeticion(LocalDate.of(1990, 4, 15)));

        ArgumentCaptor<Cliente> guardado = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(guardado.capture());
        assertEquals("10000008", guardado.getValue().getCodigoCliente());
        verify(clienteRepository, times(1)).siguienteCodigoCliente();
    }

    @Test
    @DisplayName("Actualizar no cambia el código de cliente ni consume la secuencia")
    void actualizarConservaElCodigo() {
        Cliente existente = new Cliente();
        existente.setId(3L);
        existente.setCodigoCliente("10000003");
        when(clienteRepository.findById(3L)).thenReturn(Optional.of(existente));
        when(clienteRepository.saveAndFlush(existente)).thenReturn(existente);

        ClienteRespuesta respuesta = clienteService.actualizar(3L, crearPeticion(LocalDate.of(1990, 4, 15)));

        assertEquals("10000003", respuesta.codigoCliente());
        verify(clienteRepository, never()).siguienteCodigoCliente();
    }

    @Test
    @DisplayName("Actualizar verifica unicidad excluyendo al propio cliente")
    void actualizarIdentificacionDuplicada() {
        when(clienteRepository.findById(3L)).thenReturn(Optional.of(new Cliente()));
        when(clienteRepository.existsByNumeroIdentificacionAndIdNot("2456789010101", 3L)).thenReturn(true);

        assertThrows(RecursoDuplicadoException.class,
                () -> clienteService.actualizar(3L, crearPeticion(LocalDate.of(1990, 4, 15))));
    }

    @Test
    @DisplayName("Eliminar un cliente sin cuentas activas borra primero las cuentas y luego el cliente")
    void eliminarEnOrden() {
        Cliente cliente = new Cliente();
        when(clienteRepository.findById(4L)).thenReturn(Optional.of(cliente));
        when(cuentaCreditoRepository.countByClienteIdAndEstadoCuenta(4L, EstadoCuenta.ACTIVA)).thenReturn(0L);

        clienteService.eliminar(4L);

        InOrder orden = inOrder(cuentaCreditoRepository, clienteRepository);
        orden.verify(cuentaCreditoRepository).eliminarPorIdCliente(4L);
        orden.verify(clienteRepository).delete(cliente);
    }

    @Test
    @DisplayName("CL-01: no se puede eliminar un cliente con cuentas activas y no se borra nada")
    void eliminarConCuentasActivas() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(new Cliente()));
        when(cuentaCreditoRepository.countByClienteIdAndEstadoCuenta(1L, EstadoCuenta.ACTIVA)).thenReturn(2L);

        ReglaNegocioException excepcion = assertThrows(ReglaNegocioException.class, () -> clienteService.eliminar(1L));

        assertEquals("No se puede eliminar el cliente: tiene 2 cuentas activas. Cancele primero sus cuentas", excepcion.getMessage());
        verify(cuentaCreditoRepository, never()).eliminarPorIdCliente(any());
        verify(clienteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Eliminar cliente inexistente lanza 404")
    void eliminarInexistente() {
        when(clienteRepository.findById(8L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> clienteService.eliminar(8L));
        verify(cuentaCreditoRepository, never()).eliminarPorIdCliente(any());
    }

    @Test
    @DisplayName("Listar calcula tieneCuentasEnMora de todos los clientes con una sola consulta en lote")
    void listarMarcaClientesEnMora() {
        Cliente enMora = new Cliente();
        enMora.setId(1L);
        Cliente alDia = new Cliente();
        alDia.setId(2L);
        when(clienteRepository.findAllByOrderByApellidoAscNombreAsc()).thenReturn(List.of(enMora, alDia));
        when(estadoPagoService.idsClientesConMora(List.of(1L, 2L))).thenReturn(Set.of(1L));

        List<ClienteRespuesta> respuestas = clienteService.listar(null);

        assertTrue(respuestas.get(0).tieneCuentasEnMora());
        assertFalse(respuestas.get(1).tieneCuentasEnMora());
        verify(estadoPagoService, never()).tieneCuentasEnMora(any());
    }

    @Test
    @DisplayName("El listado trae la cantidad de productos activos de todos los clientes con una sola consulta")
    void listarConCantidadDeProductosActivos() {
        Cliente conProductos = new Cliente();
        conProductos.setId(1L);
        conProductos.setNombre("Ana");
        conProductos.setApellido("Morales");
        Cliente sinProductos = new Cliente();
        sinProductos.setId(2L);
        sinProductos.setNombre("Jorge");
        sinProductos.setApellido("Pineda");
        when(clienteRepository.findAllByOrderByApellidoAscNombreAsc()).thenReturn(List.of(conProductos, sinProductos));
        when(estadoPagoService.idsClientesConMora(List.of(1L, 2L))).thenReturn(Set.of());
        CuentaCreditoRepository.CantidadCuentasCliente conteo = new CuentaCreditoRepository.CantidadCuentasCliente() {
            @Override
            public Long getIdCliente() {
                return 1L;
            }

            @Override
            public Long getCantidad() {
                return 2L;
            }
        };
        when(cuentaCreditoRepository.contarActivasPorClientes(List.of(1L, 2L))).thenReturn(List.of(conteo));

        List<ClienteRespuesta> respuestas = clienteService.listar(null);

        assertEquals(2, respuestas.get(0).cantidadProductosActivos());
        assertEquals(0, respuestas.get(1).cantidadProductosActivos());
        verify(cuentaCreditoRepository, times(1)).contarActivasPorClientes(List.of(1L, 2L));
    }

    @Test
    @DisplayName("Un cliente recién creado no tiene productos activos")
    void clienteNuevoSinProductos() {
        when(clienteRepository.siguienteCodigoCliente()).thenReturn(10000008L);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        ClienteRespuesta respuesta = clienteService.crear(crearPeticion(LocalDate.of(1990, 4, 15)));

        assertEquals(0, respuesta.cantidadProductosActivos());
    }
}
