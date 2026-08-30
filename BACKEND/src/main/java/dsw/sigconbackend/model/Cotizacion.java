package dsw.sigconbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Entity
@Table(name = "cotizacion", schema = "comercial")
public class Cotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo;
    private String cliente;
    private String ruc;
    private String producto;
    private Integer cantidad;

    @JsonProperty("precio_unitario")
    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;

    private BigDecimal subtotal;

    @JsonProperty("descuento_porcentaje")
    @Column(name = "descuento_porcentaje")
    private Integer descuentoPorcentaje;

    @JsonProperty("descuento_monto")
    @Column(name = "descuento_monto")
    private BigDecimal descuentoMonto;

    private BigDecimal total;
    private String estado;

    @JsonProperty("usuario_id")
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "fecha_registro", insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;
}