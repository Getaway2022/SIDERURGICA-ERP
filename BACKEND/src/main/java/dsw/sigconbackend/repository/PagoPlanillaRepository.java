package dsw.sigconbackend.repository;

import dsw.sigconbackend.model.PagoPlanilla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagoPlanillaRepository extends JpaRepository<PagoPlanilla, Long> {

    // Pago ya registrado para un detalle específico (evita doble pago)
    Optional<PagoPlanilla> findByPlanillaDetalleId(Long planillaDetalleId);

    // Historial de pagos de una planilla completa
    @Query("SELECT pp FROM PagoPlanilla pp WHERE pp.planillaDetalle.planilla.id = :planillaId ORDER BY pp.fechaPago DESC")
    List<PagoPlanilla> findByPlanillaId(Long planillaId);

    // Historial de pagos de un empleado
    @Query("SELECT pp FROM PagoPlanilla pp WHERE pp.empleado.id = :empleadoId ORDER BY pp.fechaPago DESC")
    List<PagoPlanilla> findByEmpleadoId(Long empleadoId);

    // Cuántos pagos se han registrado para una planilla (para saber si está completa)
    @Query("SELECT COUNT(pp) FROM PagoPlanilla pp WHERE pp.planillaDetalle.planilla.id = :planillaId")
    long countByPlanillaId(Long planillaId);
}
