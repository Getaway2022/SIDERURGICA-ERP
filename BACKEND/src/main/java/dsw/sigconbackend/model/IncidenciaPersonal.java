package dsw.sigconbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * HU23 — Registrar incidencias del personal
 * Mapea: rrhh.incidencia_personal
 *
 * A diferencia de rrhh.asistencia (control diario de entrada/tardanza/falta),
 * esta tabla guarda eventos del legajo del empleado: permisos, licencias,
 * sanciones, accidentes, etc. Cada incidencia pasa por un flujo de aprobación.
 */
@Data
@Entity
@Table(name = "incidencia_personal", schema = "rrhh")
public class IncidenciaPersonal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    // "Permiso" | "Licencia" | "Sancion" | "Accidente" | "Otro"
    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin; // null si es un evento de un solo día

    @Column(nullable = false, length = 500)
    private String descripcion;

    // "Pendiente" | "Aprobado" | "Rechazado"
    @Column(nullable = false, length = 20)
    private String estado = "Pendiente";

    @Column(name = "registrado_por_usuario_id")
    private Long registradoPorUsuarioId;

    @Column(name = "resuelto_por_usuario_id")
    private Long resueltoPorUsuarioId;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(name = "comentario_resolucion", length = 300)
    private String comentarioResolucion; // motivo si fue rechazado, o nota al aprobar
}
