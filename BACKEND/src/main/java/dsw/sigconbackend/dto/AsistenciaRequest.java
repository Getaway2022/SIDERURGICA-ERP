package dsw.sigconbackend.dto;

import lombok.Data;

// Para POST /asistencia
// Angular envía: { empleadoId: 1, horaEntrada: "08:18", observacion: "Tráfico" }
// Si no se envía horaEntrada, se registra como Ausente.
@Data
public class AsistenciaRequest {
    private Long empleadoId;
    private String horaEntrada; // formato "HH:mm", opcional
    private String observacion; // opcional
}
