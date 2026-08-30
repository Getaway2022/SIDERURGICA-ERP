package dsw.sigconbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Respuesta de GET /asistencia/incidencias/resumen
// Un resumen por empleado dentro del rango de fechas consultado.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenIncidenciasDTO {
    private Long empleadoId;
    private String empleadoCodigo;
    private String empleadoNombre;
    private String area;
    private long totalTardanzas;
    private long totalFaltas;
    private long totalMinutosTardanza;
    private long diasRegistrados;
}
