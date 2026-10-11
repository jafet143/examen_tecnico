package gt.com.chn.tarjetas.repository;

import gt.com.chn.tarjetas.entity.Pago;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByCuentaId(Long idCuenta);

    List<Pago> findByCuentaIdIn(Collection<Long> idsCuenta);

    @Query("select p.cuenta.id from Pago p where p.id = :id")
    Optional<Long> buscarIdCuentaPorId(@Param("id") Long id);
}
