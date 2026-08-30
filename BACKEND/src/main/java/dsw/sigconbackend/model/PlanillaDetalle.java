package dsw.sigconbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * HU24/HU25 — Detalle de planilla por empleado
 * Mapea: rrhh.planilla_detalle
 *
 * Cada fila representa un empleado dentro de una planilla mensual.
 * Contiene los días de asistencia, los cálculos de descuentos y el sueldo neto.
 */
@Data
@Entity
@Table(name = "planilla_detalle", schema = "rrhh",
        uniqueConstraints = @UniqueConstraint(columnNames = {"planilla_id", "empleado_id"}))
public class PlanillaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "planilla_id", nullable = false)
    private Planilla planilla;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    // ─── Asistencia del período ───────────────────────────────────────────────

    @Column(name = "dias_laborables", nullable = false)
    private Integer diasLaborables = 0;

    @Column(name = "dias_presentes", nullable = false)
    private Integer diasPresentes = 0;

    @Column(name = "dias_tardanza", nullable = false)
    private Integer diasTardanza = 0;

    @Column(name = "dias_ausente", nullable = false)
    private Integer diasAusente = 0;

    @Column(name = "total_minutos_tardanza", nullable = false)
    private Integer totalMinutosTardanza = 0;

    // ─── Cálculos HU24 ───────────────────────────────────────────────────────

    @Column(name = "salario_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal salarioBase = BigDecimal.ZERO;

    /** Descuento proporcional por días ausentes: (salarioBase / diasLaborables) × diasAusente */
    @Column(name = "descuento_faltas", nullable = false, precision = 10, scale = 2)
    private BigDecimal descuentoFaltas = BigDecimal.ZERO;

    /** Descuento proporcional por minutos de tardanza acumulados */
    @Column(name = "descuento_tardanzas", nullable = false, precision = 10, scale = 2)
    private BigDecimal descuentoTardanzas = BigDecimal.ZERO;

    /** AFP: 10 % del salario base */
    @Column(name = "descuento_afp", nullable = false, precision = 10, scale = 2)
    private BigDecimal descuentoAfp = BigDecimal.ZERO;

    /** EsSalud (informativo, lo paga el empleador): 9 % del salario base */
    @Column(name = "descuento_essalud", nullable = false, precision = 10, scale = 2)
    private BigDecimal descuentoEssalud = BigDecimal.ZERO;

    /** Bonificaciones adicionales (bono de productividad, etc.) */
    @Column(name = "bonificacion", nullable = false, precision = 10, scale = 2)
    private BigDecimal bonificacion = BigDecimal.ZERO;

    /** Suma de todos los descuentos al trabajador */
    @Column(name = "total_descuentos", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalDescuentos = BigDecimal.ZERO;

    /** salarioBase − totalDescuentos + bonificacion */
    @Column(name = "sueldo_neto", nullable = false, precision = 10, scale = 2)
    private BigDecimal sueldoNeto = BigDecimal.ZERO;

    // ─── HU25 — Validación ───────────────────────────────────────────────────

    @Column(nullable = false)
    private Boolean validado = false;

    @Column(name = "observacion_validacion", length = 300)
    private String observacionValidacion;
}
