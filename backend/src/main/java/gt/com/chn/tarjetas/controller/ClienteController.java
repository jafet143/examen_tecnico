package gt.com.chn.tarjetas.controller;

import gt.com.chn.tarjetas.config.UsuarioActual;
import gt.com.chn.tarjetas.dto.ClientePeticion;
import gt.com.chn.tarjetas.dto.ClienteRespuesta;
import gt.com.chn.tarjetas.dto.CuentaRespuesta;
import gt.com.chn.tarjetas.dto.SolicitudRespuesta;
import gt.com.chn.tarjetas.service.ClienteService;
import gt.com.chn.tarjetas.service.CuentaService;
import gt.com.chn.tarjetas.service.SolicitudService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/gestion-tarjetas/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final SolicitudService solicitudService;
    private final CuentaService cuentaService;
    private final UsuarioActual usuarioActual;

    public ClienteController(ClienteService clienteService, SolicitudService solicitudService,
                             CuentaService cuentaService, UsuarioActual usuarioActual) {
        this.clienteService = clienteService;
        this.solicitudService = solicitudService;
        this.cuentaService = cuentaService;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public List<ClienteRespuesta> listar(@RequestParam(required = false) String busqueda) {
        return clienteService.listar(busqueda);
    }

    @GetMapping("/{idCliente}")
    public ClienteRespuesta obtener(@PathVariable Long idCliente) {
        return clienteService.obtener(idCliente);
    }

    @PostMapping
    public ResponseEntity<ClienteRespuesta> crear(@Valid @RequestBody ClientePeticion peticion) {
        ClienteRespuesta creado = clienteService.crear(peticion);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{idCliente}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{idCliente}")
    public ClienteRespuesta actualizar(@PathVariable Long idCliente, @Valid @RequestBody ClientePeticion peticion) {
        return clienteService.actualizar(idCliente, peticion);
    }

    @DeleteMapping("/{idCliente}")
    public ResponseEntity<Void> eliminar(@PathVariable Long idCliente) {
        clienteService.eliminar(idCliente);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{idCliente}/solicitudes")
    public List<SolicitudRespuesta> listarSolicitudes(@PathVariable Long idCliente) {
        return solicitudService.listarPorCliente(idCliente, usuarioActual.puedeVerDatosSensibles());
    }

    @GetMapping("/{idCliente}/cuentas")
    public List<CuentaRespuesta> listarCuentas(@PathVariable Long idCliente) {
        return cuentaService.listarPorCliente(idCliente, usuarioActual.puedeVerDatosSensibles());
    }
}
