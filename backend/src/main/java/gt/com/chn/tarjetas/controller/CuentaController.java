package gt.com.chn.tarjetas.controller;

import gt.com.chn.tarjetas.config.UsuarioActual;
import gt.com.chn.tarjetas.dto.CancelacionCuentaPeticion;
import gt.com.chn.tarjetas.dto.CargoPeticion;
import gt.com.chn.tarjetas.dto.CuentaRespuesta;
import gt.com.chn.tarjetas.dto.ExoneracionMoraRespuesta;
import gt.com.chn.tarjetas.dto.MovimientoPeticion;
import gt.com.chn.tarjetas.dto.MovimientoRespuesta;
import gt.com.chn.tarjetas.dto.ReversoPeticion;
import gt.com.chn.tarjetas.dto.TarjetaRespuesta;
import gt.com.chn.tarjetas.entity.EstadoCuenta;
import gt.com.chn.tarjetas.service.CargoService;
import gt.com.chn.tarjetas.service.CuentaService;
import gt.com.chn.tarjetas.service.MovimientoCuentaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/gestion-tarjetas/cuentas")
public class CuentaController {
    private final CuentaService cuentaService;
    private final MovimientoCuentaService movimientoCuentaService;
    private final CargoService cargoService;
    private final UsuarioActual usuarioActual;

    public CuentaController(CuentaService cuentaService, MovimientoCuentaService movimientoCuentaService,
                            CargoService cargoService, UsuarioActual usuarioActual) {
        this.cuentaService = cuentaService;
        this.movimientoCuentaService = movimientoCuentaService;
        this.cargoService = cargoService;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public List<CuentaRespuesta> listar(@RequestParam(required = false) EstadoCuenta estado) {
        return cuentaService.listarTodas(estado, usuarioActual.puedeVerDatosSensibles());
    }

    @GetMapping("/{idCuenta}")
    public CuentaRespuesta obtener(@PathVariable Long idCuenta) {
        return cuentaService.obtener(idCuenta, usuarioActual.puedeVerDatosSensibles());
    }

    @GetMapping("/{idCuenta}/tarjetas")
    public List<TarjetaRespuesta> listarTarjetas(@PathVariable Long idCuenta) {
        return cuentaService.listarTarjetas(idCuenta, usuarioActual.puedeVerDatosSensibles());
    }

    @GetMapping("/{idCuenta}/movimientos")
    public List<MovimientoRespuesta> listarMovimientos(@PathVariable Long idCuenta) {
        return movimientoCuentaService.listarMovimientos(idCuenta);
    }

    @PostMapping("/{idCuenta}/consumos")
    public ResponseEntity<MovimientoRespuesta> registrarConsumo(@PathVariable Long idCuenta,
                                                                @Valid @RequestBody MovimientoPeticion peticion,
                                                                Authentication autenticacion) {
        return crearRespuesta(movimientoCuentaService.registrarConsumo(idCuenta, peticion, autenticacion.getName()));
    }

    @PostMapping("/{idCuenta}/pagos")
    public ResponseEntity<MovimientoRespuesta> registrarPago(@PathVariable Long idCuenta,
                                                             @Valid @RequestBody MovimientoPeticion peticion,
                                                             Authentication autenticacion) {
        return crearRespuesta(movimientoCuentaService.registrarPago(idCuenta, peticion, autenticacion.getName()));
    }

    @PostMapping("/{idCuenta}/cargos")
    public ResponseEntity<MovimientoRespuesta> registrarCargo(@PathVariable Long idCuenta,
                                                              @Valid @RequestBody CargoPeticion peticion,
                                                              Authentication autenticacion) {
        return crearRespuesta(cargoService.registrar(idCuenta, peticion, autenticacion.getName()));
    }

    @PostMapping("/{idCuenta}/exoneracion-mora")
    public ExoneracionMoraRespuesta exonerarMora(@PathVariable Long idCuenta,
                                                 @Valid @RequestBody ReversoPeticion peticion,
                                                 Authentication autenticacion) {
        return cargoService.exonerarMora(idCuenta, peticion, autenticacion.getName(),
                usuarioActual.puedeVerDatosSensibles());
    }

    @PutMapping("/{idCuenta}/cancelar")
    public CuentaRespuesta cancelar(@PathVariable Long idCuenta,
                                    @Valid @RequestBody CancelacionCuentaPeticion peticion,
                                    Authentication autenticacion) {
        return cuentaService.cancelar(idCuenta, peticion, autenticacion.getName(),
                usuarioActual.puedeVerDatosSensibles());
    }

    private ResponseEntity<MovimientoRespuesta> crearRespuesta(MovimientoRespuesta movimiento) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movimiento);
    }
}
