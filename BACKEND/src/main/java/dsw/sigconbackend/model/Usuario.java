package dsw.sigconbackend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Formula;

@Data
@Entity
@Table(name = "usuario", schema = "seguridad")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    // Se acepta al crear/editar, pero el hash nunca se devuelve al frontend.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    // Join con seguridad.rol para traer nombre del rol
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id")
    private Rol rol;

    // Estado real almacenado en seguridad.usuario.
    // Se conserva el id para crear/editar y se expone el nombre legible al frontend.
    @Column(name = "estado_id")
    private Long estadoId = 1L;

    @Formula("(select e.nombre from catalogos.estado e where e.id = estado_id)")
    private String estado;

    // Método auxiliar para Spring Security y AuthService
    public String getRolNombre() {
        return rol != null ? rol.getNombre() : "VENDEDOR";
    }
}
