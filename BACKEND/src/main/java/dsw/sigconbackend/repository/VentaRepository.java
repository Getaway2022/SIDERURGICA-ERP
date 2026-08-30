package dsw.sigconbackend.repository;

import dsw.sigconbackend.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findAllByOrderByCreatedAtDesc();

    List<Venta> findByPedidoId(Long pedidoId);

    List<Venta> findByEstadoOrderByCreatedAtDesc(String estado);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(v.codigo, 5) AS int)), 0) FROM Venta v WHERE v.codigo LIKE 'VEN-%'")
    Integer findMaxCodigoNumber();
}