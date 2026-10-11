package gt.com.chn.tarjetas.repository;

import gt.com.chn.tarjetas.entity.Cargo;
import gt.com.chn.tarjetas.entity.TipoCargo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CargoRepository extends JpaRepository<Cargo, Long> {
    List<Cargo> findByCuentaId(Long idCuenta);

    List<Cargo> findByCuentaIdIn(Collection<Long> idsCuenta);

    List<Cargo> findByCuentaIdAndTipoCargoAndReversadoFalse(Long idCuenta, TipoCargo tipoCargo);

    @Query("select distinct c.cuenta.id from Cargo c where c.cuenta.id in :idsCuenta "
            + "and c.tipoCargo = :tipoCargo and c.reversado = false")
    Set<Long> idsCuentasConCargoVigente(@Param("idsCuenta") Collection<Long> idsCuenta,
                                        @Param("tipoCargo") TipoCargo tipoCargo);

    @Query("select c.cuenta.id from Cargo c where c.id = :id")
    Optional<Long> buscarIdCuentaPorId(@Param("id") Long id);
}
