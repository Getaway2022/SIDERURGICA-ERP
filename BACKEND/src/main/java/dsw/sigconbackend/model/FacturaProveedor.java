package dsw.sigconbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "factura_proveedor", schema = "compras")
public class FacturaProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proveedor_id", nullable = false)
    private Long proveedorId;

    @Column(name = "compra_id")
    private Long compraId;

    @Column(name = "numero_factura", nullable = false, length = 50)
    private String numeroFactura;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(nullable = false, length = 20)
    private String estado = "Pendiente";

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}