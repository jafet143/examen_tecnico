package gt.com.chn.tarjetas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReversoPeticion(
        @NotBlank(message = "El motivo del reverso es obligatorio")
        @Size(min = 5, max = 250, message = "El motivo del reverso debe tener entre 5 y 250 caracteres")
        String motivo) {
}
