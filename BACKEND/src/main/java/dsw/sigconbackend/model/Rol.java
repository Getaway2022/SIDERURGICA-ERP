package dsw.sigconbackend.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Mapea: seguridad.rol
 */
@Data
@Entity
@Table(name = "rol", schema = "seguridad")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;  // "ADMIN", "VENDEDOR", etc.
}
