package gt.com.chn.tarjetas.dto;

import gt.com.chn.tarjetas.entity.MotivoBloqueo;
import gt.com.chn.tarjetas.entity.MotivoCancelacion;
import gt.com.chn.tarjetas.entity.MotivoReposicion;
import gt.com.chn.tarjetas.entity.TipoCargo;
import gt.com.chn.tarjetas.entity.TipoTarjeta;
import java.math.BigDecimal;
import java.util.List;

public record CatalogoTiposTarjetaRespuesta(
        int porcentajeMaximoLimiteSobreIngreso,
        int mesesMinimosEstabilidadLaboral,
        int diaCorte,
        int diaLimitePago,
        BigDecimal comisionReposicionExtravio,
        int porcentajePagoMinimo,
        int tasaInteresMensual,
        List<TipoTarjetaCatalogo> tiposTarjeta,
        List<MotivoBloqueoCatalogo> motivosBloqueo,
        List<MotivoReposicionCatalogo> motivosReposicion,
        List<MotivoCancelacionCatalogo> motivosCancelacion,
        List<TipoCargoCatalogo> tiposCargoManual) {
    public record TipoTarjetaCatalogo(TipoTarjeta codigo, String descripcion, BigDecimal ingresoMinimo) {
    }

    public record MotivoBloqueoCatalogo(MotivoBloqueo codigo, String descripcion, boolean permiteDesbloqueo) {
    }

    public record MotivoReposicionCatalogo(MotivoReposicion codigo, String descripcion) {
    }

    public record MotivoCancelacionCatalogo(MotivoCancelacion codigo, String descripcion) {
    }

    public record TipoCargoCatalogo(TipoCargo codigo, String descripcion) {
    }
}
