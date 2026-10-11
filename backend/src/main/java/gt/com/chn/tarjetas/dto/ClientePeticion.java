package gt.com.chn.tarjetas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ClientePeticion(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
        String apellido,

        @NotBlank(message = "El número de identificación es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9-]{5,20}$",
                message = "El número de identificación debe tener entre 5 y 20 caracteres alfanuméricos o guiones")
        String numeroIdentificacion,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
        LocalDate fechaNacimiento,

        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 250, message = "La dirección no puede exceder 250 caracteres")
        String direccion,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico no tiene un formato válido")
        @Size(max = 150, message = "El correo electrónico no puede exceder 150 caracteres")
        String correoElectronico,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^[0-9+()\\- ]{8,20}$",
                message = "El teléfono debe tener entre 8 y 20 caracteres (dígitos, +, paréntesis, guiones o espacios)")
        String telefono) {
}
