package gt.com.chn.tarjetas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoPeticion(
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        @Digits(integer = 16, fraction = 2, message = "El monto admite hasta 16 enteros y 2 decimales")
        BigDecimal monto,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 250, message = "La descripción no puede exceder 250 caracteres")
        String descripcion,

        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDateTime fecha) {
}
