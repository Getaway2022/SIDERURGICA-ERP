package dsw.sigconbackend.repository;

import dsw.sigconbackend.model.SolicitudReposicion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SolicitudReposicionRepository extends JpaRepository<SolicitudReposicion, Long> {
    List<SolicitudReposicion> findAllByOrderByCreadaEnDesc();
    Optional<SolicitudReposicion> findByInventarioIdAndEstadoIn(Long inventarioId, List<String> estados);
    Optional<SolicitudReposicion> findByOrdenCompraId(Long ordenCompraId);
}
