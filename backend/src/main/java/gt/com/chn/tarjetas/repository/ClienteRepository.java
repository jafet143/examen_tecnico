package gt.com.chn.tarjetas.repository;

import gt.com.chn.tarjetas.entity.Cliente;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByNumeroIdentificacion(String numeroIdentificacion);

    boolean existsByNumeroIdentificacionAndIdNot(String numeroIdentificacion, Long id);

    @Query("select c from Cliente c where c.nombre like concat('%', :texto, '%') "
            + "or c.apellido like concat('%', :texto, '%') "
            + "or c.numeroIdentificacion like concat('%', :texto, '%') "
            + "or c.codigoCliente like concat('%', :texto, '%') "
            + "order by c.apellido, c.nombre")
    List<Cliente> buscarPorTexto(@Param("texto") String texto);

    List<Cliente> findAllByOrderByApellidoAscNombreAsc();

    @Query(value = "SELECT NEXT VALUE FOR dbo.seq_codigo_cliente", nativeQuery = true)
    Long siguienteCodigoCliente();
}
