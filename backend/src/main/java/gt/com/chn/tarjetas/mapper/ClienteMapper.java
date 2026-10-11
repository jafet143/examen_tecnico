package gt.com.chn.tarjetas.mapper;

import gt.com.chn.tarjetas.dto.ClientePeticion;
import gt.com.chn.tarjetas.dto.ClienteRespuesta;
import gt.com.chn.tarjetas.entity.Cliente;

public final class ClienteMapper {
    private ClienteMapper() {
    }

    public static ClienteRespuesta aRespuesta(Cliente cliente, boolean tieneCuentasEnMora,
                                           int cantidadProductosActivos) {
        return new ClienteRespuesta(
                cliente.getId(),
                cliente.getCodigoCliente(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getNombreCompleto(),
                cliente.getNumeroIdentificacion(),
                cliente.getFechaNacimiento(),
                cliente.getDireccion(),
                cliente.getCorreoElectronico(),
                cliente.getTelefono(),
                cliente.getFechaCreacion(),
                cliente.getFechaActualizacion(),
                tieneCuentasEnMora,
                cantidadProductosActivos);
    }

    public static Cliente aEntidad(ClientePeticion peticion) {
        Cliente cliente = new Cliente();
        actualizarEntidad(cliente, peticion);
        return cliente;
    }

    public static void actualizarEntidad(Cliente cliente, ClientePeticion peticion) {
        cliente.setNombre(peticion.nombre());
        cliente.setApellido(peticion.apellido());
        cliente.setNumeroIdentificacion(peticion.numeroIdentificacion());
        cliente.setFechaNacimiento(peticion.fechaNacimiento());
        cliente.setDireccion(peticion.direccion());
        cliente.setCorreoElectronico(peticion.correoElectronico());
        cliente.setTelefono(peticion.telefono());
    }
}
