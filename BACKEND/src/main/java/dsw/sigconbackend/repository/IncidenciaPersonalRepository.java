package dsw.sigconbackend.repository;

import dsw.sigconbackend.model.IncidenciaPersonal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidenciaPersonalRepository extends JpaRepository<IncidenciaPersonal, Long> {

    // Listado general, más reciente primero
    @Query("SELECT i FROM IncidenciaPersonal i ORDER BY i.fechaRegistro DESC")
    List<IncidenciaPersonal> findAllOrdenado();

    // Filtrado por estado (ej. solo Pendientes, para la bandeja de aprobación)
    @Query("SELECT i FROM IncidenciaPersonal i WHERE i.estado = :estado ORDER BY i.fechaRegistro DESC")
    List<IncidenciaPersonal> findByEstado(String estado);

    // Legajo de un empleado específico
    @Query("SELECT i FROM IncidenciaPersonal i WHERE i.empleado.id = :empleadoId ORDER BY i.fechaInicio DESC")
    List<IncidenciaPersonal> findByEmpleadoId(Long empleadoId);

    // Filtrado por tipo (ej. solo Sanciones)
    @Query("SELECT i FROM IncidenciaPersonal i WHERE i.tipo = :tipo ORDER BY i.fechaRegistro DESC")
    List<IncidenciaPersonal> findByTipo(String tipo);
}
