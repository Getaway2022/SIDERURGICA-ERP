package dsw.sigconbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * HU21 — Registrar asistencia del personal
 * Mapea: rrhh.asistencia
 *
 * Un registro por empleado por día. El estado se calcula automáticamente
 * comparando la hora de entrada contra la hora esperada del empleado.
 */
@Data
@Entity
@Table(name = "asistencia", schema = "rrhh")
public class Asistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_entrada")
    private LocalTime horaEntrada; // null si fue marcado como Ausente directamente

    @Column(name = "minutos_tardanza")
    private Integer minutosTardanza = 0;

    // "Presente" | "Tardanza" | "Ausente"
    @Column(nullable = false, length = 20)
    private String estado;

    @Column(length = 200)
    private String observacion;
}
