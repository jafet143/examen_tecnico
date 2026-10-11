package gt.com.chn.tarjetas.repository;

import gt.com.chn.tarjetas.entity.EstadoSolicitud;
import gt.com.chn.tarjetas.entity.SolicitudTarjeta;
import gt.com.chn.tarjetas.entity.TipoTarjeta;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudTarjetaRepository extends JpaRepository<SolicitudTarjeta, Long> {

    @Query("select s from SolicitudTarjeta s join fetch s.cliente left join fetch s.cuenta "
            + "where s.cliente.id = :idCliente order by s.fechaSolicitud desc")
    List<SolicitudTarjeta> buscarPorCliente(@Param("idCliente") Long idCliente);

    @Query("select s from SolicitudTarjeta s join fetch s.cliente left join fetch s.cuenta "
            + "where (:estado is null or s.estado = :estado) order by s.fechaSolicitud asc")
    List<SolicitudTarjeta> buscarPorEstado(@Param("estado") EstadoSolicitud estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitudTarjeta s where s.id = :id")
    Optional<SolicitudTarjeta> buscarPorIdConBloqueo(@Param("id") Long id);

    boolean existsByClienteIdAndTipoTarjetaAndEstado(Long idCliente, TipoTarjeta tipoTarjeta, EstadoSolicitud estado);
}
