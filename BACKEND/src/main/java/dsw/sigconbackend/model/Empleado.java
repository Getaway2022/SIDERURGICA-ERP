package dsw.sigconbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Soporte de HU21/HU22/HU23 — Gestión de Recursos Humanos
 * Mapea: rrhh.empleado
 */
@Data
@Entity
@Table(name = "empleado", schema = "rrhh")
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String codigo; // EMP-0041

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 100)
    private String cargo;

    @Column(length = 100)
    private String area;

    @Column(name = "hora_entrada_esperada", length = 5)
    private String horaEntradaEsperada; // "08:00" — referencia para calcular tardanza

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "salario_base", precision = 10, scale = 2)
    private BigDecimal salarioBase = BigDecimal.ZERO;
}
