package dsw.sigconbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "solicitud_reposicion", schema = "compras")
public class SolicitudReposicion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "inventario_id", nullable = false)
    private Long inventarioId;
    private String producto;
    private Integer cantidad;
    private String estado = "PENDIENTE";
    @Column(name = "orden_compra_id")
    private Long ordenCompraId;
    @Column(name = "creada_en")
    private LocalDateTime creadaEn = LocalDateTime.now();
    @Column(name = "actualizada_en")
    private LocalDateTime actualizadaEn = LocalDateTime.now();
    // La solicitud es también el aviso persistente de la bandeja interna.
    private String mensaje;
}
