package gt.com.chn.tarjetas.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorRespuesta(
        LocalDateTime fechaHora,
        int estado,
        String error,
        String mensaje,
        String ruta,
        Map<String, String> errores) {
}
