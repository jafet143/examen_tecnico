package gt.com.chn.tarjetas.config;

import gt.com.chn.tarjetas.dto.ErrorRespuesta;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String MENSAJE_NO_AUTENTICADO = "Debe iniciar sesión para acceder a este recurso";
    static final String MENSAJE_SIN_PERMISOS = "No tiene permisos para realizar esta operación";

    private final ObjectMapper objectMapper;

    public ManejadorErroresSeguridad(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest peticion, HttpServletResponse respuesta,
                         AuthenticationException excepcion) throws IOException {
        escribirError(peticion, respuesta, HttpStatus.UNAUTHORIZED, MENSAJE_NO_AUTENTICADO);
    }

    @Override
    public void handle(HttpServletRequest peticion, HttpServletResponse respuesta,
                       AccessDeniedException excepcion) throws IOException {
        escribirError(peticion, respuesta, HttpStatus.FORBIDDEN, MENSAJE_SIN_PERMISOS);
    }

    private void escribirError(HttpServletRequest peticion, HttpServletResponse respuesta,
                               HttpStatus estado, String mensaje) throws IOException {
        ErrorRespuesta error = new ErrorRespuesta(LocalDateTime.now(), estado.value(), estado.getReasonPhrase(),
                mensaje, peticion.getRequestURI(), Map.of());
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(respuesta.getWriter(), error);
    }
}
