package gt.com.chn.tarjetas.controller;

import gt.com.chn.tarjetas.config.UsuarioActual;
import gt.com.chn.tarjetas.dto.BloqueoTarjetaPeticion;
import gt.com.chn.tarjetas.dto.ReposicionTarjetaPeticion;
import gt.com.chn.tarjetas.dto.TarjetaRespuesta;
import gt.com.chn.tarjetas.service.TarjetaService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/gestion-tarjetas/tarjetas")
public class TarjetaController {
    private final TarjetaService tarjetaService;
    private final UsuarioActual usuarioActual;

    public TarjetaController(TarjetaService tarjetaService, UsuarioActual usuarioActual) {
        this.tarjetaService = tarjetaService;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping("/{idTarjeta}")
    public TarjetaRespuesta obtener(@PathVariable Long idTarjeta) {
        return tarjetaService.obtener(idTarjeta, usuarioActual.puedeVerDatosSensibles());
    }

    @PutMapping("/{idTarjeta}/bloquear")
    public TarjetaRespuesta bloquear(@PathVariable Long idTarjeta,
                                     @Valid @RequestBody BloqueoTarjetaPeticion peticion,
                                     Authentication autenticacion) {
        return tarjetaService.bloquear(idTarjeta, peticion, autenticacion.getName(),
                usuarioActual.puedeVerDatosSensibles());
    }

    @PutMapping("/{idTarjeta}/desbloquear")
    public TarjetaRespuesta desbloquear(@PathVariable Long idTarjeta) {
        return tarjetaService.desbloquear(idTarjeta, usuarioActual.puedeVerDatosSensibles());
    }

    @PostMapping("/{idTarjeta}/reposiciones")
    public ResponseEntity<TarjetaRespuesta> reponer(@PathVariable Long idTarjeta,
                                                    @Valid @RequestBody ReposicionTarjetaPeticion peticion,
                                                    Authentication autenticacion) {
        TarjetaRespuesta nueva = tarjetaService.reponer(idTarjeta, peticion, autenticacion.getName(),
                usuarioActual.puedeVerDatosSensibles());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/gestion-tarjetas/tarjetas/{idTarjeta}").buildAndExpand(nueva.id()).toUri();
        return ResponseEntity.created(ubicacion).body(nueva);
    }

    @PostMapping("/{idTarjeta}/renovaciones")
    public ResponseEntity<TarjetaRespuesta> renovar(@PathVariable Long idTarjeta, Authentication autenticacion) {
        TarjetaRespuesta nueva = tarjetaService.renovar(idTarjeta, autenticacion.getName(),
                usuarioActual.puedeVerDatosSensibles());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/gestion-tarjetas/tarjetas/{idTarjeta}").buildAndExpand(nueva.id()).toUri();
        return ResponseEntity.created(ubicacion).body(nueva);
    }
}
