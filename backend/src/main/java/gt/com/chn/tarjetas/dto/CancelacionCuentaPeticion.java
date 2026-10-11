package gt.com.chn.tarjetas.dto;

import gt.com.chn.tarjetas.entity.MotivoCancelacion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CancelacionCuentaPeticion(
        @NotNull(message = "El motivo de cancelación es obligatorio")
        MotivoCancelacion motivoCancelacion,

        @Size(max = 250, message = "El comentario no puede exceder 250 caracteres")
        String comentario) {
}
