package gt.com.chn.tarjetas.service;

import gt.com.chn.tarjetas.dto.ClientePeticion;
import gt.com.chn.tarjetas.dto.ClienteRespuesta;
import gt.com.chn.tarjetas.entity.Cliente;
import gt.com.chn.tarjetas.entity.EstadoCuenta;
import gt.com.chn.tarjetas.exception.RecursoDuplicadoException;
import gt.com.chn.tarjetas.exception.RecursoNoEncontradoException;
import gt.com.chn.tarjetas.exception.ReglaNegocioException;
import gt.com.chn.tarjetas.mapper.ClienteMapper;
import gt.com.chn.tarjetas.repository.ClienteRepository;
import gt.com.chn.tarjetas.repository.CuentaCreditoRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {
    private static final int EDAD_MINIMA = 18;

    private final ClienteRepository clienteRepository;
    private final CuentaCreditoRepository cuentaCreditoRepository;
    private final EstadoPagoService estadoPagoService;
    private final Clock reloj;

    public ClienteService(ClienteRepository clienteRepository,
                          CuentaCreditoRepository cuentaCreditoRepository,
                          EstadoPagoService estadoPagoService,
                          Clock reloj) {
        this.clienteRepository = clienteRepository;
        this.cuentaCreditoRepository = cuentaCreditoRepository;
        this.estadoPagoService = estadoPagoService;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<ClienteRespuesta> listar(String busqueda) {
        List<Cliente> clientes = busqueda == null || busqueda.isBlank()
                ? clienteRepository.findAllByOrderByApellidoAscNombreAsc()
                : clienteRepository.buscarPorTexto(busqueda.trim());
        Set<Long> idsConMora = estadoPagoService.idsClientesConMora(clientes.stream().map(Cliente::getId).toList());
        Map<Long, Integer> productosActivos = contarProductosActivos(clientes.stream().map(Cliente::getId).toList());
        return clientes.stream()
                .map(cliente -> ClienteMapper.aRespuesta(cliente, idsConMora.contains(cliente.getId()),
                        productosActivos.getOrDefault(cliente.getId(), 0)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteRespuesta obtener(Long id) {
        Cliente cliente = buscarEntidad(id);
        return ClienteMapper.aRespuesta(cliente, estadoPagoService.tieneCuentasEnMora(id),
                contarProductosActivos(List.of(id)).getOrDefault(id, 0));
    }

    @Transactional
    public ClienteRespuesta crear(ClientePeticion peticion) {
        ClientePeticion datos = normalizar(peticion);
        validarMayoriaDeEdad(datos);
        if (clienteRepository.existsByNumeroIdentificacion(datos.numeroIdentificacion())) {
            throw crearExcepcionDuplicado(datos.numeroIdentificacion());
        }
        Cliente nuevo = ClienteMapper.aEntidad(datos);
        nuevo.setCodigoCliente(String.valueOf(clienteRepository.siguienteCodigoCliente()));
        Cliente cliente = clienteRepository.save(nuevo);
        return ClienteMapper.aRespuesta(cliente, false, 0);
    }

    @Transactional
    public ClienteRespuesta actualizar(Long id, ClientePeticion peticion) {
        Cliente cliente = buscarEntidad(id);
        ClientePeticion datos = normalizar(peticion);
        validarMayoriaDeEdad(datos);
        if (clienteRepository.existsByNumeroIdentificacionAndIdNot(datos.numeroIdentificacion(), id)) {
            throw crearExcepcionDuplicado(datos.numeroIdentificacion());
        }
        ClienteMapper.actualizarEntidad(cliente, datos);
        Cliente actualizado = clienteRepository.saveAndFlush(cliente);
        return ClienteMapper.aRespuesta(actualizado, estadoPagoService.tieneCuentasEnMora(id),
                contarProductosActivos(List.of(id)).getOrDefault(id, 0));
    }

    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = buscarEntidad(id);
        long cuentasActivas = cuentaCreditoRepository.countByClienteIdAndEstadoCuenta(id, EstadoCuenta.ACTIVA);
        if (cuentasActivas > 0) {
            throw new ReglaNegocioException("No se puede eliminar el cliente: tiene " + cuentasActivas
                    + (cuentasActivas == 1 ? " cuenta activa" : " cuentas activas")
                    + ". Cancele primero sus cuentas");
        }
        cuentaCreditoRepository.eliminarPorIdCliente(id);
        clienteRepository.delete(cliente);
    }

    private Map<Long, Integer> contarProductosActivos(List<Long> idsCliente) {
        if (idsCliente.isEmpty()) {
            return Map.of();
        }
        return cuentaCreditoRepository.contarActivasPorClientes(idsCliente).stream()
                .collect(Collectors.toMap(CuentaCreditoRepository.CantidadCuentasCliente::getIdCliente, conteo -> conteo.getCantidad().intValue()));
    }

    private Cliente buscarEntidad(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el cliente con id " + id));
    }

    private ClientePeticion normalizar(ClientePeticion peticion) {
        return new ClientePeticion(
                peticion.nombre().trim(),
                peticion.apellido().trim(),
                peticion.numeroIdentificacion().trim(),
                peticion.fechaNacimiento(),
                peticion.direccion().trim(),
                peticion.correoElectronico().trim(),
                peticion.telefono().trim());
    }

    private void validarMayoriaDeEdad(ClientePeticion datos) {
        int edad = Period.between(datos.fechaNacimiento(), LocalDate.now(reloj)).getYears();
        if (edad < EDAD_MINIMA) {
            throw new ReglaNegocioException("El cliente debe ser mayor de edad (18 años o más)");
        }
    }

    private RecursoDuplicadoException crearExcepcionDuplicado(String numeroIdentificacion) {
        return new RecursoDuplicadoException(
                "Ya existe un cliente con el número de identificación " + numeroIdentificacion);
    }
}
