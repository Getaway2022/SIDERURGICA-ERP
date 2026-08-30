package dsw.sigconbackend.repository;
import dsw.sigconbackend.model.Presupuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {
    Optional<Presupuesto> findByPeriodo(String periodo);
}