package gt.com.chn.tarjetas.repository;

import gt.com.chn.tarjetas.entity.TarjetaCredito;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TarjetaCreditoRepository extends JpaRepository<TarjetaCredito, Long> {
    @Query("select t from TarjetaCredito t join fetch t.cuenta c join fetch c.cliente where t.id = :id")
    Optional<TarjetaCredito> buscarDetallePorId(@Param("id") Long id);

    @Query("select t from TarjetaCredito t join fetch t.cuenta c join fetch c.cliente "
            + "where c.id in :idsCuenta order by t.fechaEmision desc, t.id desc")
    List<TarjetaCredito> listarPorCuentas(@Param("idsCuenta") Collection<Long> idsCuenta);

    @Query("select t from TarjetaCredito t where t.cuenta.id in :idsCuenta and t.tarjetaAnterior is null")
    List<TarjetaCredito> listarPrimerasPorCuentas(@Param("idsCuenta") Collection<Long> idsCuenta);

    @Query("select t from TarjetaCredito t where t.cuenta.id = :idCuenta and t.estadoTarjeta <> gt.com.chn.tarjetas.entity.EstadoTarjeta.REEMPLAZADA")
    Optional<TarjetaCredito> buscarVigentePorCuenta(@Param("idCuenta") Long idCuenta);

    Optional<TarjetaCredito> findByTarjetaAnteriorId(Long idTarjetaAnterior);

    @Query("select t.cuenta.id from TarjetaCredito t where t.id = :id")
    Optional<Long> buscarIdCuentaPorId(@Param("id") Long id);

    boolean existsByNumeroTarjeta(String numeroTarjeta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TarjetaCredito t where t.id = :id")
    Optional<TarjetaCredito> buscarPorIdConBloqueo(@Param("id") Long id);
}
