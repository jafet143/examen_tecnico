package gt.com.chn.tarjetas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AprobacionPeticion(
        @NotNull(message = "El monto aprobado es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto aprobado debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "El monto aprobado admite hasta 10 enteros y 2 decimales")
        BigDecimal montoAprobado,

        @Size(max = 500, message = "El comentario no puede exceder 500 caracteres")
        String comentarioDecision) {
}
