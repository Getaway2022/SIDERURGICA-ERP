package dsw.sigconbackend.controller;

import dsw.sigconbackend.dto.IncidenciaPersonalRequest;
import dsw.sigconbackend.dto.ResolverIncidenciaRequest;
import dsw.sigconbackend.model.IncidenciaPersonal;
import dsw.sigconbackend.service.IncidenciaPersonalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HU23 — Registrar incidencias del personal
 * GET    /incidencias-personal                     -> listar todas (más reciente primero)
 * GET    /incidencias-personal?estado=Pendiente     -> filtrar por estado (bandeja de aprobación)
 * GET    /incidencias-personal?tipo=Sancion         -> filtrar por tipo
 * GET    /incidencias-personal/empleado/{id}        -> legajo de un empleado
 * POST   /incidencias-personal                      -> registrar nueva incidencia (nace "Pendiente")
 * PATCH  /incidencias-personal/{id}/resolver        -> aprobar o rechazar
 * DELETE /incidencias-personal/{id}                 -> eliminar (solo si sigue Pendiente)
 */
@RestController
@RequestMapping("/incidencias-personal")
public class IncidenciaPersonalController {

    @Autowired
    private IncidenciaPersonalService incidenciaService;

    @GetMapping
    public ResponseEntity<List<IncidenciaPersonal>> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipo) {
        return ResponseEntity.ok(incidenciaService.listar(estado, tipo));
    }

    @GetMapping("/empleado/{empleadoId}")
    public ResponseEntity<List<IncidenciaPersonal>> legajo(@PathVariable Long empleadoId) {
        return ResponseEntity.ok(incidenciaService.legajoPorEmpleado(empleadoId));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> registrar(@RequestBody IncidenciaPersonalRequest req) {
        try {
            IncidenciaPersonal incidencia = incidenciaService.registrar(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "mensaje", "Incidencia registrada correctamente",
                    "incidencia", incidencia
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/resolver")
    public ResponseEntity<Map<String, Object>> resolver(@PathVariable Long id, @RequestBody ResolverIncidenciaRequest req) {
        try {
            Optional<IncidenciaPersonal> resultado = incidenciaService.resolver(id, req);
            if (resultado.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "mensaje", "Incidencia " + resultado.get().getEstado().toLowerCase(),
                        "incidencia", resultado.get()
                ));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "mensaje", "Incidencia no encontrada"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminar(@PathVariable Long id) {
        try {
            incidenciaService.eliminar(id);
            return ResponseEntity.ok(Map.of("success", true, "mensaje", "Incidencia eliminada"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }
}
