package dsw.sigconbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "despacho", schema = "comercial")
public class Despacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String cliente;

    @Column(length = 20)
    private String ruc;

    @Column(nullable = false, length = 200)
    private String producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(length = 300)
    private String direccion;

    @Column(length = 100)
    private String transportista;

    @Column(length = 50)
    private String peso;

    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(name = "venta_id")
    private Long ventaId;

    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "fecha_despacho")
    private LocalDateTime fechaDespacho;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(length = 300)
private String comprobante;

@Column(name = "comprobante_validado")
private Boolean comprobanteValidado = false;

@Column(name = "fecha_validacion_comprobante")
private LocalDateTime fechaValidacionComprobante;

@Column(name = "fecha_entrega")
private LocalDateTime fechaEntrega;
    @PrePersist
    protected void onCreate() {
        if (fechaDespacho == null) fechaDespacho = LocalDateTime.now();
        if (estado == null) estado = "PENDIENTE";
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}