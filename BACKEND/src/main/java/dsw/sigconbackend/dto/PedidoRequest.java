package dsw.sigconbackend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PedidoRequest {
    private String cliente;
    private String ruc;
    private String producto;
    private Integer cantidad;
    private BigDecimal precio_unitario;
    private Long usuario_id;
}
