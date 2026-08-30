package dsw.sigconbackend.repository;
import dsw.sigconbackend.model.FacturaProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FacturaProveedorRepository extends JpaRepository<FacturaProveedor, Long> {
    List<FacturaProveedor> findByProveedorId(Long proveedorId);
}