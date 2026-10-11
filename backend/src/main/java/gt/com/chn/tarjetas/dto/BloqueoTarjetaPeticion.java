package gt.com.chn.tarjetas.dto;

import gt.com.chn.tarjetas.entity.MotivoBloqueo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BloqueoTarjetaPeticion(
        @NotNull(message = "El motivo de bloqueo es obligatorio")
        MotivoBloqueo motivoBloqueo,

        @Size(max = 250, message = "El comentario no puede exceder 250 caracteres")
        String comentario) {
}
