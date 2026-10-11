package gt.com.chn.tarjetas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RechazoPeticion(
        @NotBlank(message = "El comentario del rechazo es obligatorio")
        @Size(max = 500, message = "El comentario no puede exceder 500 caracteres")
        String comentarioDecision) {
}
