package dsw.sigconbackend.repository;

import dsw.sigconbackend.model.PlanillaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanillaDetalleRepository extends JpaRepository<PlanillaDetalle, Long> {

    // Todos los detalles de una planilla
    List<PlanillaDetalle> findByPlanillaId(Long planillaId);

    // Detalle de un empleado en una planilla específica
    Optional<PlanillaDetalle> findByPlanillaIdAndEmpleadoId(Long planillaId, Long empleadoId);

    // ¿Todos los detalles de la planilla están validados? (para HU25)
    @Query("SELECT COUNT(d) = 0 FROM PlanillaDetalle d WHERE d.planilla.id = :planillaId AND d.validado = FALSE")
    boolean todosValidados(Long planillaId);

    // Historial de un empleado en todas las planillas
    @Query("SELECT d FROM PlanillaDetalle d WHERE d.empleado.id = :empleadoId ORDER BY d.planilla.periodo DESC")
    List<PlanillaDetalle> findByEmpleadoId(Long empleadoId);
}
