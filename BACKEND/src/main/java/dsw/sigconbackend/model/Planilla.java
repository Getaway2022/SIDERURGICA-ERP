package dsw.sigconbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * HU24/HU25/HU26 — Gestión de Planillas
 * Cabecera de planilla mensual.
 * Mapea: rrhh.planilla
 *
 * Estados:
 *  Borrador  → recién generada, aún sin validar
 *  Validado  → el encargado revisó y aprobó cada detalle (HU25)
 *  Pagado    → todos los empleados recibieron su pago (HU26)
 */
@Data
@Entity
@Table(name = "planilla", schema = "rrhh")
public class Planilla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Periodo en formato YYYY-MM, ej. "2025-07". Único. */
    @Column(nullable = false, length = 7, unique = true)
    private String periodo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    /** Borrador | Validado | Pagado */
    @Column(nullable = false, length = 20)
    private String estado = "Borrador";

    @Column(name = "creado_por_usuario")
    private Long creadoPorUsuario;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @Column(name = "validado_por_usuario")
    private Long validadoPorUsuario;

    @Column(length = 300)
    private String observacion;
}
