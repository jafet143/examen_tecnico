package gt.com.chn.tarjetas.exception;

import gt.com.chn.tarjetas.dto.ErrorRespuesta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {
    private static final Logger registro = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);
    private static final String MENSAJE_DATOS_INVALIDOS = "La petición contiene datos inválidos";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> manejarValidacion(MethodArgumentNotValidException excepcion,
                                                            HttpServletRequest peticion) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError errorCampo : excepcion.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(errorCampo.getField(), errorCampo.getDefaultMessage());
        }
        return responder(HttpStatus.BAD_REQUEST, MENSAJE_DATOS_INVALIDOS, peticion, errores);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorRespuesta> manejarViolacionRestricciones(ConstraintViolationException excepcion,
                                                                        HttpServletRequest peticion) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (ConstraintViolation<?> violacion : excepcion.getConstraintViolations()) {
            String ruta = violacion.getPropertyPath().toString();
            errores.putIfAbsent(ruta.substring(ruta.lastIndexOf('.') + 1), violacion.getMessage());
        }
        return responder(HttpStatus.BAD_REQUEST, MENSAJE_DATOS_INVALIDOS, peticion, errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> manejarCuerpoIlegible(HttpMessageNotReadableException excepcion,
                                                                HttpServletRequest peticion) {
        return responder(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición no es válido o tiene un formato incorrecto", peticion, Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorRespuesta> manejarTipoParametroInvalido(MethodArgumentTypeMismatchException excepcion,
                                                                       HttpServletRequest peticion) {
        return responder(HttpStatus.BAD_REQUEST,
                "El parámetro '" + excepcion.getName() + "' tiene un valor inválido", peticion, Map.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorRespuesta> manejarParametroFaltante(MissingServletRequestParameterException excepcion,
                                                                   HttpServletRequest peticion) {
        return responder(HttpStatus.BAD_REQUEST,
                "Falta el parámetro obligatorio '" + excepcion.getParameterName() + "'", peticion, Map.of());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> manejarNoEncontrado(RecursoNoEncontradoException excepcion,
                                                              HttpServletRequest peticion) {
        return responder(HttpStatus.NOT_FOUND, excepcion.getMessage(), peticion, Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorRespuesta> manejarRutaInexistente(NoResourceFoundException excepcion,
                                                                 HttpServletRequest peticion) {
        return responder(HttpStatus.NOT_FOUND, "El recurso solicitado no existe", peticion, Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorRespuesta> manejarMetodoNoPermitido(HttpRequestMethodNotSupportedException excepcion,
                                                                   HttpServletRequest peticion) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "El método HTTP no está permitido para este recurso",
                peticion, Map.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorRespuesta> manejarTipoContenidoNoSoportado(
            HttpMediaTypeNotSupportedException excepcion, HttpServletRequest peticion) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El tipo de contenido no está soportado",
                peticion, Map.of());
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErrorRespuesta> manejarDuplicado(RecursoDuplicadoException excepcion,
                                                           HttpServletRequest peticion) {
        return responder(HttpStatus.CONFLICT, excepcion.getMessage(), peticion, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorRespuesta> manejarIntegridadDatos(DataIntegrityViolationException excepcion,
                                                                 HttpServletRequest peticion) {
        registro.warn("Violación de integridad de datos en {}: {}", peticion.getRequestURI(),
                excepcion.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "La operación viola una restricción de integridad de los datos",
                peticion, Map.of());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorRespuesta> manejarReglaNegocio(ReglaNegocioException excepcion,
                                                              HttpServletRequest peticion) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, excepcion.getMessage(), peticion, Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorRespuesta> manejarAccesoDenegado(AccessDeniedException excepcion,
                                                                HttpServletRequest peticion) {
        return responder(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación", peticion, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> manejarErrorInterno(Exception excepcion, HttpServletRequest peticion) {
        registro.error("Error interno no controlado en {}", peticion.getRequestURI(), excepcion);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno. Intente nuevamente más tarde.", peticion, Map.of());
    }

    private ResponseEntity<ErrorRespuesta> responder(HttpStatus estado, String mensaje,
                                                     HttpServletRequest peticion, Map<String, String> errores) {
        ErrorRespuesta cuerpo = new ErrorRespuesta(LocalDateTime.now(), estado.value(), estado.getReasonPhrase(),
                mensaje, peticion.getRequestURI(), errores);
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
