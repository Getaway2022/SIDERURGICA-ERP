package dsw.sigconbackend.repository;

import dsw.sigconbackend.model.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    @Query("SELECT e FROM Empleado e WHERE e.activo = true ORDER BY e.nombre")
    List<Empleado> findAllActivos();

    @Query("SELECT e FROM Empleado e WHERE e.activo = true AND e.area = :area ORDER BY e.nombre")
    List<Empleado> findByArea(String area);
}
