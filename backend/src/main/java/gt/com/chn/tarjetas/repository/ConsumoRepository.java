package gt.com.chn.tarjetas.repository;

import gt.com.chn.tarjetas.entity.Consumo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsumoRepository extends JpaRepository<Consumo, Long> {
    @Query("select c from Consumo c join fetch c.tarjeta where c.cuenta.id = :idCuenta")
    List<Consumo> listarPorCuenta(@Param("idCuenta") Long idCuenta);

    List<Consumo> findByCuentaId(Long idCuenta);

    List<Consumo> findByCuentaIdIn(Collection<Long> idsCuenta);

    @Query("select c.cuenta.id from Consumo c where c.id = :id")
    Optional<Long> buscarIdCuentaPorId(@Param("id") Long id);
}
