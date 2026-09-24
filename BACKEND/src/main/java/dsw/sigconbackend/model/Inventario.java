package dsw.sigconbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * HU09 — Visualizar stock disponible
 * HU10 — Actualizar inventario
 * HU11 — Alertar bajo stock
 * Mapea: almacen.inventario
 */
@Data
@Entity
@Table(name = "inventario", schema = "almacen")
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String producto;
    private String categoria;
    private Integer stock;

    @Column(name = "stock_reservado", nullable = false)
    private Integer stockReservado = 0;

    @Column(name = "stock_objetivo")
    private Integer stockObjetivo;

    @Transient
    public int getStockDisponible() {
        return (stock == null ? 0 : stock) - (stockReservado == null ? 0 : stockReservado);
    }

    @Column(name = "stock_minimo")
    private Integer stockMinimo;

    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;

    private String unidad;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // HU11 — campo calculado: true si stock <= stock_minimo
    @Transient
    public boolean isBajoStock() {
        return stockMinimo != null && getStockDisponible() <= stockMinimo;
    }
}
