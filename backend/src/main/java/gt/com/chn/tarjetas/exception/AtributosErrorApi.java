package gt.com.chn.tarjetas.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

@Component
public class AtributosErrorApi extends DefaultErrorAttributes {
    private static final String CLAVE_ESTADO = "status";
    private static final String CLAVE_RUTA = "path";

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest peticion, ErrorAttributeOptions opciones) {
        Map<String, Object> atributosOriginales = super.getErrorAttributes(peticion, ErrorAttributeOptions.defaults());
        HttpStatus estado = resolverEstado(atributosOriginales.get(CLAVE_ESTADO));

        Map<String, Object> atributos = new LinkedHashMap<>();
        atributos.put("fechaHora", LocalDateTime.now());
        atributos.put("estado", estado.value());
        atributos.put("error", estado.getReasonPhrase());
        atributos.put("mensaje", obtenerMensaje(estado));
        atributos.put("ruta", atributosOriginales.get(CLAVE_RUTA));
        atributos.put("errores", Map.of());
        return atributos;
    }

    private HttpStatus resolverEstado(Object codigoEstado) {
        if (codigoEstado instanceof Integer codigo) {
            HttpStatus estado = HttpStatus.resolve(codigo);
            if (estado != null) {
                return estado;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String obtenerMensaje(HttpStatus estado) {
        return switch (estado) {
            case BAD_REQUEST -> "La petición no es válida o la ruta está mal formada";
            case NOT_FOUND -> "El recurso solicitado no existe";
            case METHOD_NOT_ALLOWED -> "El método HTTP no está permitido para este recurso";
            default -> estado.is5xxServerError()
                    ? "Ocurrió un error interno. Intente nuevamente más tarde."
                    : "No fue posible procesar la petición";
        };
    }
}
