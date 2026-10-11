package gt.com.chn.tarjetas.dto;

import gt.com.chn.tarjetas.entity.MotivoReposicion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReposicionTarjetaPeticion(
        @NotNull(message = "El motivo de reposición es obligatorio")
        MotivoReposicion motivoReposicion,

        @Size(max = 250, message = "El comentario no puede exceder 250 caracteres")
        String comentario,

        boolean denunciaPresentada,

        boolean tarjetaDeterioradaPresentada) {
}
