package dsw.sigconbackend.dto;

import lombok.Data;

// Para PATCH /incidencias-personal/{id}/resolver
// Angular envía: { estado: "Aprobado" | "Rechazado", comentario: "...", usuario_id }
@Data
public class ResolverIncidenciaRequest {
    private String estado;      // Aprobado | Rechazado
    private String comentario;  // opcional, especialmente útil si es Rechazado
    private Long usuario_id;    // quién aprueba o rechaza
}
