package gt.com.chn.tarjetas.mapper;

import gt.com.chn.tarjetas.dto.TarjetaRespuesta;
import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.entity.MotivoBloqueo;
import gt.com.chn.tarjetas.entity.MotivoReposicion;
import gt.com.chn.tarjetas.entity.TarjetaCredito;
import java.time.LocalDate;

public final class TarjetaMapper {
    private static final String PREFIJO_ENMASCARADO = "**** **** **** ";
    private static final int DIGITOS_VISIBLES = 4;

    private TarjetaMapper() {
    }

    public static String enmascararNumero(String numero) {
        int inicio = Math.max(0, numero.length() - DIGITOS_VISIBLES);
        return PREFIJO_ENMASCARADO + numero.substring(inicio);
    }

    public static String completarSiAutorizado(String numero, boolean incluirDatosSensibles) {
        return incluirDatosSensibles ? numero : null;
    }

    public static TarjetaRespuesta aRespuesta(TarjetaCredito tarjeta, boolean incluirDatosSensibles, LocalDate hoy) {
        CuentaCredito cuenta = tarjeta.getCuenta();
        MotivoBloqueo motivoBloqueo = tarjeta.getMotivoBloqueo();
        MotivoReposicion motivoReposicion = tarjeta.getMotivoReposicion();
        TarjetaCredito anterior = tarjeta.getTarjetaAnterior();
        return new TarjetaRespuesta(
                tarjeta.getId(),
                cuenta.getId(),
                enmascararNumero(cuenta.getNumeroCuenta()),
                completarSiAutorizado(cuenta.getNumeroCuenta(), incluirDatosSensibles),
                enmascararNumero(tarjeta.getNumeroTarjeta()),
                completarSiAutorizado(tarjeta.getNumeroTarjeta(), incluirDatosSensibles),
                cuenta.getTipoTarjeta(),
                cuenta.getTipoTarjeta().getDescripcion(),
                cuenta.getCliente().getNombreCompleto(),
                cuenta.getCliente().getCodigoCliente(),
                tarjeta.getFechaEmision(),
                tarjeta.getFechaVencimiento(),
                tarjeta.getEstadoTarjeta(),
                tarjeta.getEstadoTarjeta().getDescripcion(),
                motivoBloqueo,
                motivoBloqueo == null ? null : motivoBloqueo.getDescripcion(),
                tarjeta.getComentarioBloqueo(),
                tarjeta.getFechaBloqueo(),
                tarjeta.getUsuarioBloqueo(),
                anterior == null ? null : anterior.getId(),
                motivoReposicion,
                motivoReposicion == null ? null : motivoReposicion.getDescripcion(),
                tarjeta.getComentarioReposicion(),
                tarjeta.getFechaReposicion(),
                tarjeta.getUsuarioReposicion(),
                tarjeta.puedeDesbloquearse(),
                tarjeta.getDenunciaPresentada(),
                tarjeta.getTarjetaDeterioradaPresentada(),
                tarjeta.getIdCargoReposicion(),
                tarjeta.estaVencida(hoy),
                tarjeta.puedeRenovarse(hoy));
    }
}
