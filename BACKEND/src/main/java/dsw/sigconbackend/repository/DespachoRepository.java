package dsw.sigconbackend.repository;

import dsw.sigconbackend.model.Despacho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DespachoRepository extends JpaRepository<Despacho, Long> {

    List<Despacho> findAllByOrderByCreatedAtDesc();

    List<Despacho> findByVentaId(Long ventaId);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(d.codigo, 5) AS int)), 0) FROM Despacho d WHERE d.codigo LIKE 'DES-%'")
    Integer findMaxCodigoNumber();
}