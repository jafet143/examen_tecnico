package gt.com.chn.tarjetas.dto;

import gt.com.chn.tarjetas.entity.TipoTarjeta;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SolicitudPeticion(
        @NotNull(message = "El cliente es obligatorio")
        @Positive(message = "El identificador del cliente debe ser positivo")
        Long idCliente,

        @NotNull(message = "El tipo de tarjeta es obligatorio")
        TipoTarjeta tipoTarjeta,

        @NotNull(message = "El ingreso mensual es obligatorio")
        @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
        @Digits(integer = 16, fraction = 2, message = "El ingreso mensual admite hasta 16 enteros y 2 decimales")
        BigDecimal ingresoMensual,

        @NotBlank(message = "El nombre del empleador es obligatorio")
        @Size(min = 2, max = 150, message = "El nombre del empleador debe tener entre 2 y 150 caracteres")
        String nombreEmpleador,

        @NotNull(message = "La fecha de inicio laboral es obligatoria")
        @PastOrPresent(message = "La fecha de inicio laboral no puede ser futura")
        LocalDate fechaInicioLaboral) {
}
