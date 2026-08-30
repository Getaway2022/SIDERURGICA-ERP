package dsw.sigconbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "proveedor", schema = "compras")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 150)
    private String origen;

    @Column(unique = true, nullable = false, length = 20)
    private String ruc;

    @Column(length = 150)
    private String contacto;

    @Column(length = 30)
    private String telefono;

    @Column(length = 100)
    private String categoria;

    private Double calificacion = 4.0;

    @Column(nullable = false, length = 20)
    private String estado = "Activo";

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}