package dsw.sigconbackend.repository;

import dsw.sigconbackend.model.Planilla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanillaRepository extends JpaRepository<Planilla, Long> {

    // Buscar por periodo exacto (ej. "2025-07")
    Optional<Planilla> findByPeriodo(String periodo);

    // Listar todas ordenadas del más reciente al más antiguo
    List<Planilla> findAllByOrderByPeriodoDesc();

    // Listar por estado (Borrador / Validado / Pagado)
    List<Planilla> findByEstadoOrderByPeriodoDesc(String estado);
}
