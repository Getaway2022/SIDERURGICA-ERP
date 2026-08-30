package dsw.sigconbackend.service;

import dsw.sigconbackend.dto.IncidenciaPersonalRequest;
import dsw.sigconbackend.dto.ResolverIncidenciaRequest;
import dsw.sigconbackend.model.Empleado;
import dsw.sigconbackend.model.IncidenciaPersonal;
import dsw.sigconbackend.repository.EmpleadoRepository;
import dsw.sigconbackend.repository.IncidenciaPersonalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * HU23 — Registrar incidencias del personal
 *
 * Reglas de negocio:
 *  - Toda incidencia nace en estado "Pendiente".
 *  - Solo se puede resolver (Aprobar/Rechazar) una incidencia que esté Pendiente.
 *  - fechaFin, si se indica, no puede ser anterior a fechaInicio.
 */
@Service
public class IncidenciaPersonalService {

    @Autowired
    private IncidenciaPersonalRepository incidenciaRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    private static final List<String> TIPOS_VALIDOS =
            List.of("Permiso", "Licencia", "Sancion", "Accidente", "Otro");

    public List<IncidenciaPersonal> listar(String estado, String tipo) {
        if (estado != null && !estado.isBlank()) return incidenciaRepository.findByEstado(estado);
        if (tipo != null && !tipo.isBlank()) return incidenciaRepository.findByTipo(tipo);
        return incidenciaRepository.findAllOrdenado();
    }

    public List<IncidenciaPersonal> legajoPorEmpleado(Long empleadoId) {
        return incidenciaRepository.findByEmpleadoId(empleadoId);
    }

    public IncidenciaPersonal registrar(IncidenciaPersonalRequest req) {
        if (req.getEmpleadoId() == null)
            throw new IllegalArgumentException("Debe indicar el empleado.");
        if (req.getTipo() == null || !TIPOS_VALIDOS.contains(req.getTipo()))
            throw new IllegalArgumentException("Tipo inválido. Use: " + String.join(", ", TIPOS_VALIDOS));
        if (req.getDescripcion() == null || req.getDescripcion().trim().isEmpty())
            throw new IllegalArgumentException("La descripción es obligatoria.");
        if (req.getFechaInicio() == null || req.getFechaInicio().trim().isEmpty())
            throw new IllegalArgumentException("La fecha de inicio es obligatoria.");

        Empleado empleado = empleadoRepository.findById(req.getEmpleadoId())
                .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado."));

        LocalDate fechaInicio = LocalDate.parse(req.getFechaInicio());
        LocalDate fechaFin = (req.getFechaFin() != null && !req.getFechaFin().isBlank())
                ? LocalDate.parse(req.getFechaFin())
                : null;

        if (fechaFin != null && fechaFin.isBefore(fechaInicio))
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio.");

        IncidenciaPersonal incidencia = new IncidenciaPersonal();
        incidencia.setEmpleado(empleado);
        incidencia.setTipo(req.getTipo());
        incidencia.setFechaInicio(fechaInicio);
        incidencia.setFechaFin(fechaFin);
        incidencia.setDescripcion(req.getDescripcion().trim());
        incidencia.setEstado("Pendiente");
        incidencia.setRegistradoPorUsuarioId(req.getUsuario_id());
        incidencia.setFechaRegistro(LocalDateTime.now());

        return incidenciaRepository.save(incidencia);
    }

    public Optional<IncidenciaPersonal> resolver(Long id, ResolverIncidenciaRequest req) {
        if (!List.of("Aprobado", "Rechazado").contains(req.getEstado()))
            throw new IllegalArgumentException("Estado inválido. Use Aprobado o Rechazado.");

        return incidenciaRepository.findById(id).map(inc -> {
            if (!"Pendiente".equals(inc.getEstado()))
                throw new IllegalArgumentException("Esta incidencia ya fue resuelta (" + inc.getEstado() + ").");

            inc.setEstado(req.getEstado());
            inc.setComentarioResolucion(req.getComentario());
            inc.setResueltoPorUsuarioId(req.getUsuario_id());
            inc.setFechaResolucion(LocalDateTime.now());
            return incidenciaRepository.save(inc);
        });
    }

    public void eliminar(Long id) {
        IncidenciaPersonal incidencia = incidenciaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incidencia no encontrada."));
        if (!"Pendiente".equals(incidencia.getEstado()))
            throw new IllegalArgumentException("Solo se pueden eliminar incidencias pendientes; esta ya fue resuelta.");
        incidenciaRepository.delete(incidencia);
    }
}
