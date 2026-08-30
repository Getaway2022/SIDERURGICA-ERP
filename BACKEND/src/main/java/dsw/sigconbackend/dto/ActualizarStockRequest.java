package dsw.sigconbackend.dto;

import lombok.Data;

// Para PATCH /inventario/:id/stock
// Angular envía: { cantidad: 50, tipo: "ENTRADA" o "SALIDA" }
@Data
public class ActualizarStockRequest {
    private Integer cantidad;
    private String tipo; // "ENTRADA" o "SALIDA"
}
