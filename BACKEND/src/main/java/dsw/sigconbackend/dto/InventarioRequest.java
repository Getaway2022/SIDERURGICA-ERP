package dsw.sigconbackend.dto;

import lombok.Data;
import java.math.BigDecimal;

// Para POST /inventario y PUT /inventario/:id
@Data
public class InventarioRequest {
    private String producto;
    private String categoria;
    private Integer stock;
    private Integer stockMinimo;
    private BigDecimal precioUnitario;
    private String unidad;
}
