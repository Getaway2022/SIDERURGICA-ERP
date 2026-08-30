package dsw.sigconbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * HU26 — Registrar pago de planilla
 * Mapea: rrhh.pago_planilla
 *
 * Cada fila es un pago individual confirmado a un empleado.
 * Cuando todos los detalles de una planilla tienen su pago registrado,
 * la planilla pasa a estado "Pagado".
 */
@Data
@Entity
@Table(name = "pago_planilla", schema = "rrhh")
public class PagoPlanilla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "planilla_detalle_id", nullable = false)
    private PlanillaDetalle planillaDetalle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Column(name = "monto_pagado", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoPagado;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago = LocalDateTime.now();

    /** Transferencia | Cheque | Efectivo */
    @Column(name = "metodo_pago", nullable = false, length = 30)
    private String metodoPago = "Transferencia";

    @Column(name = "numero_operacion", length = 50)
    private String numeroOperacion;

    @Column(name = "registrado_por_usuario")
    private Long registradoPorUsuario;

    @Column(nullable = false)
    private Boolean confirmado = true;
}
