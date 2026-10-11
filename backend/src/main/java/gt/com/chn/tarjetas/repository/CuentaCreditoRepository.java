package gt.com.chn.tarjetas.repository;

import gt.com.chn.tarjetas.entity.CuentaCredito;
import gt.com.chn.tarjetas.entity.EstadoCuenta;
import gt.com.chn.tarjetas.entity.TipoTarjeta;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuentaCreditoRepository extends JpaRepository<CuentaCredito, Long> {
    @Query("select c from CuentaCredito c join fetch c.cliente where (:estado is null or c.estadoCuenta = :estado) "
            + "order by c.fechaApertura desc, c.id desc")
    List<CuentaCredito> listarConCliente(@Param("estado") EstadoCuenta estado);

    @Query("select c from CuentaCredito c join fetch c.cliente where c.cliente.id = :idCliente "
            + "order by c.fechaApertura desc, c.id desc")
    List<CuentaCredito> listarPorCliente(@Param("idCliente") Long idCliente);

    @Query("select c.id from CuentaCredito c where c.estadoCuenta = gt.com.chn.tarjetas.entity.EstadoCuenta.ACTIVA")
    List<Long> listarIdsActivas();

    @Query("select c from CuentaCredito c join fetch c.cliente where c.id = :id")
    Optional<CuentaCredito> buscarDetallePorId(@Param("id") Long id);

    @Query("select c from CuentaCredito c join fetch c.cliente where c.cliente.id in :idsCliente "
            + "and c.estadoCuenta = gt.com.chn.tarjetas.entity.EstadoCuenta.ACTIVA")
    List<CuentaCredito> listarActivasPorClientes(@Param("idsCliente") Collection<Long> idsCliente);

    @Query("select c.cliente.id as idCliente, count(c) as cantidad from CuentaCredito c "
            + "where c.cliente.id in :idsCliente and c.estadoCuenta = gt.com.chn.tarjetas.entity.EstadoCuenta.ACTIVA "
            + "group by c.cliente.id")
    List<CantidadCuentasCliente> contarActivasPorClientes(@Param("idsCliente") Collection<Long> idsCliente);

    long countByClienteIdAndEstadoCuenta(Long idCliente, EstadoCuenta estadoCuenta);

    Optional<CuentaCredito> findByClienteIdAndTipoTarjetaAndEstadoCuenta(Long idCliente, TipoTarjeta tipoTarjeta,
                                                                          EstadoCuenta estadoCuenta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CuentaCredito c where c.id = :id")
    Optional<CuentaCredito> buscarPorIdConBloqueo(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CuentaCredito c where c.cliente.id = :idCliente")
    int eliminarPorIdCliente(@Param("idCliente") Long idCliente);

    interface CantidadCuentasCliente {
        Long getIdCliente();

        Long getCantidad();
    }
}
