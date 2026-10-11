package gt.com.chn.tarjetas.controller;

import gt.com.chn.tarjetas.config.UsuarioActual;
import gt.com.chn.tarjetas.dto.AprobacionPeticion;
import gt.com.chn.tarjetas.dto.RechazoPeticion;
import gt.com.chn.tarjetas.dto.SolicitudPeticion;
import gt.com.chn.tarjetas.dto.SolicitudRespuesta;
import gt.com.chn.tarjetas.entity.EstadoSolicitud;
import gt.com.chn.tarjetas.service.SolicitudService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/gestion-tarjetas/solicitudes")
public class SolicitudController {
    private final SolicitudService solicitudService;
    private final UsuarioActual usuarioActual;

    public SolicitudController(SolicitudService solicitudService, UsuarioActual usuarioActual) {
        this.solicitudService = solicitudService;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public List<SolicitudRespuesta> listar(@RequestParam(required = false) EstadoSolicitud estado) {
        return solicitudService.listarPorEstado(estado, usuarioActual.puedeVerDatosSensibles());
    }

    @GetMapping("/{idSolicitud}")
    public SolicitudRespuesta obtener(@PathVariable Long idSolicitud) {
        return solicitudService.obtener(idSolicitud, usuarioActual.puedeVerDatosSensibles());
    }

    @PostMapping
    public ResponseEntity<SolicitudRespuesta> crear(@Valid @RequestBody SolicitudPeticion peticion) {
        SolicitudRespuesta creada = solicitudService.crear(peticion);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{idSolicitud}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @PutMapping("/{idSolicitud}/aprobar")
    public SolicitudRespuesta aprobar(@PathVariable Long idSolicitud,
                                      @Valid @RequestBody AprobacionPeticion peticion,
                                      Authentication autenticacion) {
        return solicitudService.aprobar(idSolicitud, peticion, autenticacion.getName(),
                usuarioActual.puedeVerDatosSensibles());
    }

    @PutMapping("/{idSolicitud}/rechazar")
    public SolicitudRespuesta rechazar(@PathVariable Long idSolicitud,
                                       @Valid @RequestBody RechazoPeticion peticion,
                                       Authentication autenticacion) {
        return solicitudService.rechazar(idSolicitud, peticion, autenticacion.getName());
    }
}
