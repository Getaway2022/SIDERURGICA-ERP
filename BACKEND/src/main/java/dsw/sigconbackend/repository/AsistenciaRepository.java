package dsw.sigconbackend.repository;

import dsw.sigconbackend.model.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    // HU21 — listar asistencia de un día específico (por defecto: hoy)
    @Query("SELECT a FROM Asistencia a WHERE a.fecha = :fecha ORDER BY a.empleado.nombre")
    List<Asistencia> findByFecha(LocalDate fecha);

    // Evitar doble registro del mismo empleado el mismo día
    Optional<Asistencia> findByEmpleadoIdAndFecha(Long empleadoId, LocalDate fecha);

    // Historial de un empleado (sirve de base para HU22)
    @Query("SELECT a FROM Asistencia a WHERE a.empleado.id = :empleadoId ORDER BY a.fecha DESC")
    List<Asistencia> findByEmpleadoId(Long empleadoId);

    // ─────────────────────────────────────────
    // HU22 — Registrar tardanzas y faltas
    // ─────────────────────────────────────────

    // Todas las incidencias (Tardanza o Ausente) dentro de un rango de fechas
    @Query("SELECT a FROM Asistencia a WHERE a.fecha BETWEEN :desde AND :hasta " +
           "AND a.estado <> 'Presente' ORDER BY a.fecha DESC, a.empleado.nombre")
    List<Asistencia> findIncidenciasEntreFechas(LocalDate desde, LocalDate hasta);

    // Incidencias de un empleado específico dentro de un rango de fechas
    @Query("SELECT a FROM Asistencia a WHERE a.empleado.id = :empleadoId " +
           "AND a.fecha BETWEEN :desde AND :hasta AND a.estado <> 'Presente' " +
           "ORDER BY a.fecha DESC")
    List<Asistencia> findIncidenciasPorEmpleadoEntreFechas(Long empleadoId, LocalDate desde, LocalDate hasta);

    // Todos los registros (incidencias o no) dentro de un rango, para calcular totales
    @Query("SELECT a FROM Asistencia a WHERE a.fecha BETWEEN :desde AND :hasta")
    List<Asistencia> findEntreFechas(LocalDate desde, LocalDate hasta);
}