package gt.com.chn.tarjetas.controller;

import gt.com.chn.tarjetas.dto.MovimientoRespuesta;
import gt.com.chn.tarjetas.dto.ReversoPeticion;
import gt.com.chn.tarjetas.service.CargoService;
import gt.com.chn.tarjetas.service.MovimientoCuentaService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gestion-tarjetas")
public class ReversoController {
    private final MovimientoCuentaService movimientoCuentaService;
    private final CargoService cargoService;

    public ReversoController(MovimientoCuentaService movimientoCuentaService, CargoService cargoService) {
        this.movimientoCuentaService = movimientoCuentaService;
        this.cargoService = cargoService;
    }

    @PutMapping("/consumos/{idConsumo}/reversar")
    public MovimientoRespuesta reversarConsumo(@PathVariable Long idConsumo,
                                               @Valid @RequestBody ReversoPeticion peticion,
                                               Authentication autenticacion) {
        return movimientoCuentaService.reversarConsumo(idConsumo, peticion, autenticacion.getName());
    }

    @PutMapping("/pagos/{idPago}/reversar")
    public MovimientoRespuesta reversarPago(@PathVariable Long idPago,
                                            @Valid @RequestBody ReversoPeticion peticion,
                                            Authentication autenticacion) {
        return movimientoCuentaService.reversarPago(idPago, peticion, autenticacion.getName());
    }

    @PutMapping("/cargos/{idCargo}/reversar")
    public MovimientoRespuesta reversarCargo(@PathVariable Long idCargo,
                                             @Valid @RequestBody ReversoPeticion peticion,
                                             Authentication autenticacion) {
        return cargoService.reversar(idCargo, peticion, autenticacion.getName());
    }
}
