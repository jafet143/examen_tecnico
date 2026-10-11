package gt.com.chn.tarjetas.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClienteRespuesta(
        Long id,
        String codigoCliente,
        String nombre,
        String apellido,
        String nombreCompleto,
        String numeroIdentificacion,
        LocalDate fechaNacimiento,
        String direccion,
        String correoElectronico,
        String telefono,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion,
        boolean tieneCuentasEnMora,
        int cantidadProductosActivos) {
}
