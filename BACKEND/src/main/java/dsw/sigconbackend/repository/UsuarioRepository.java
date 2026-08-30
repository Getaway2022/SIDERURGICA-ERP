package dsw.sigconbackend.repository;

import dsw.sigconbackend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Para Spring Security: carga usuario por username
    Optional<Usuario> findByUsername(String username);

    // Para el response del login: trae id, username, rol, estado
    @Query(value = """
        SELECT u.id, u.username, r.nombre AS rol, e.nombre AS estado
        FROM seguridad.usuario u
        INNER JOIN seguridad.rol r ON u.rol_id = r.id
        INNER JOIN catalogos.estado e ON u.estado_id = e.id
        WHERE u.username = :username
        """, nativeQuery = true)
    List<Object[]> findLoginDataByUsername(@Param("username") String username);

    @Query(value = """
        SELECT u.id, u.username, r.id AS rol_id, r.nombre AS rol,
               e.id AS estado_id, e.nombre AS estado
        FROM seguridad.usuario u
        INNER JOIN seguridad.rol r ON u.rol_id = r.id
        LEFT JOIN catalogos.estado e ON u.estado_id = e.id
        ORDER BY u.id
        """, nativeQuery = true)
    List<Object[]> findAllWithDetails();
}
