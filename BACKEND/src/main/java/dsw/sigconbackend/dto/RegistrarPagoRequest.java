package dsw.sigconbackend.dto;

import lombok.Data;

/**
 * HU26 — Payload para registrar el pago de un empleado en una planilla.
 *
 * POST /planillas/{planillaId}/detalles/{detalleId}/pagar
 */
@Data
public class RegistrarPagoRequest {

    /** Opcional: si se omite se usa el sueldoNeto calculado en el detalle */
    private java.math.BigDecimal montoPagado;

    /** Transferencia | Cheque | Efectivo */
    private String metodoPago;

    /** Número de transferencia, cheque u operación bancaria */
    private String numeroOperacion;

    /** Id del usuario que registra el pago (viene del token en producción) */
    private Long registradoPorUsuario;
}
