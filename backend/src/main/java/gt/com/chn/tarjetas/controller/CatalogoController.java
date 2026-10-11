package gt.com.chn.tarjetas.controller;

import gt.com.chn.tarjetas.dto.CatalogoTiposTarjetaRespuesta;
import gt.com.chn.tarjetas.dto.CatalogoTiposTarjetaRespuesta.MotivoBloqueoCatalogo;
import gt.com.chn.tarjetas.dto.CatalogoTiposTarjetaRespuesta.MotivoCancelacionCatalogo;
import gt.com.chn.tarjetas.dto.CatalogoTiposTarjetaRespuesta.MotivoReposicionCatalogo;
import gt.com.chn.tarjetas.dto.CatalogoTiposTarjetaRespuesta.TipoCargoCatalogo;
import gt.com.chn.tarjetas.dto.CatalogoTiposTarjetaRespuesta.TipoTarjetaCatalogo;
import gt.com.chn.tarjetas.entity.CicloFacturacion;
import gt.com.chn.tarjetas.entity.MotivoBloqueo;
import gt.com.chn.tarjetas.entity.MotivoCancelacion;
import gt.com.chn.tarjetas.entity.MotivoReposicion;
import gt.com.chn.tarjetas.entity.Tarifas;
import gt.com.chn.tarjetas.entity.TipoCargo;
import gt.com.chn.tarjetas.entity.TipoTarjeta;
import gt.com.chn.tarjetas.service.SolicitudService;
import java.util.Arrays;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gestion-tarjetas/catalogos")
public class CatalogoController {

    @GetMapping("/tipos-tarjeta")
    public CatalogoTiposTarjetaRespuesta obtenerTiposTarjeta() {
        return new CatalogoTiposTarjetaRespuesta(
                SolicitudService.PORCENTAJE_MAXIMO_LIMITE_SOBRE_INGRESO,
                SolicitudService.MESES_MINIMOS_ESTABILIDAD_LABORAL,
                CicloFacturacion.DIA_CORTE,
                CicloFacturacion.DIA_LIMITE_PAGO,
                Tarifas.COMISION_REPOSICION_EXTRAVIO,
                Tarifas.PORCENTAJE_PAGO_MINIMO,
                Tarifas.TASA_INTERES_MENSUAL,
                Arrays.stream(TipoTarjeta.values())
                        .map(tipo -> new TipoTarjetaCatalogo(tipo, tipo.getDescripcion(), tipo.getIngresoMinimo()))
                        .toList(),
                Arrays.stream(MotivoBloqueo.values())
                        .map(motivo -> new MotivoBloqueoCatalogo(
                                motivo, motivo.getDescripcion(), motivo.permiteDesbloqueo()))
                        .toList(),
                Arrays.stream(MotivoReposicion.values())
                        .filter(MotivoReposicion::esSolicitable)
                        .map(motivo -> new MotivoReposicionCatalogo(motivo, motivo.getDescripcion()))
                        .toList(),
                Arrays.stream(MotivoCancelacion.values())
                        .map(motivo -> new MotivoCancelacionCatalogo(motivo, motivo.getDescripcion()))
                        .toList(),
                Arrays.stream(TipoCargo.values())
                        .filter(TipoCargo::permiteRegistroManual)
                        .map(tipo -> new TipoCargoCatalogo(tipo, tipo.getDescripcion()))
                        .toList());
    }
}
