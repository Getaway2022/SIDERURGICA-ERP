package dsw.sigconbackend.dto;

import lombok.Data;

// Para POST /incidencias-personal
// Angular envía: { empleadoId, tipo, fechaInicio, fechaFin, descripcion, usuario_id }
@Data
public class IncidenciaPersonalRequest {
    private Long empleadoId;
    private String tipo;          // Permiso | Licencia | Sancion | Accidente | Otro
    private String fechaInicio;   // "yyyy-MM-dd"
    private String fechaFin;      // opcional, "yyyy-MM-dd"
    private String descripcion;
    private Long usuario_id;      // quién registra la incidencia
}
