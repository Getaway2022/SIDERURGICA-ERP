package dsw.sigconbackend.controller;

import dsw.sigconbackend.dto.AsistenciaRequest;
import dsw.sigconbackend.model.Asistencia;
import dsw.sigconbackend.service.AsistenciaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HU21 — Registrar asistencia del personal
 * GET   /asistencia                 -> listar asistencia de hoy
 * GET   /asistencia?fecha=2026-06-19 -> listar asistencia de una fecha
 * GET   /asistencia/empleado/{id}   -> historial de un empleado
 * POST  /asistencia                 -> registrar asistencia (calcula el estado automáticamente)
 * PATCH /asistencia/{id}/estado     -> corregir manualmente un registro del día
 *
 * HU22 — Registrar tardanzas y faltas
 * GET /asistencia/incidencias?desde=&hasta=                 -> listado de tardanzas/faltas en un rango
 * GET /asistencia/incidencias/empleado/{id}?desde=&hasta=    -> incidencias de un empleado
 * GET /asistencia/incidencias/resumen?desde=&hasta=          -> totales agrupados por empleado
 */
@RestController
@RequestMapping("/asistencia")
public class AsistenciaController {

    @Autowired
    private AsistenciaService asistenciaService;

    @GetMapping
    public ResponseEntity<List<Asistencia>> listar(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(asistenciaService.listarPorFecha(fecha));
    }

    @GetMapping("/empleado/{empleadoId}")
    public ResponseEntity<List<Asistencia>> historial(@PathVariable Long empleadoId) {
        return ResponseEntity.ok(asistenciaService.historialPorEmpleado(empleadoId));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> registrar(@RequestBody AsistenciaRequest req) {
        try {
            Asistencia asistencia = asistenciaService.registrar(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "mensaje", "Asistencia registrada: " + asistencia.getEstado(),
                    "asistencia", asistencia
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Map<String, Object>> actualizarEstado(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            Optional<Asistencia> resultado = asistenciaService.actualizarEstado(id, body.get("estado"), body.get("observacion"));
            if (resultado.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "mensaje", "Estado actualizado",
                        "asistencia", resultado.get()
                ));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "mensaje", "Registro no encontrado"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }

    // Corrige la hora de entrada de un registro existente y recalcula Presente/Tardanza/Ausente
    // automáticamente (a diferencia de /estado, que solo cambia la etiqueta de texto).
    @PatchMapping("/{id}/hora-entrada")
    public ResponseEntity<Map<String, Object>> corregirHoraEntrada(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            Optional<Asistencia> resultado = asistenciaService.corregirHoraEntrada(id, body.get("horaEntrada"), body.get("observacion"));
            if (resultado.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "mensaje", "Hora de entrada actualizada: " + resultado.get().getEstado(),
                        "asistencia", resultado.get()
                ));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "mensaje", "Registro no encontrado"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // HU22 — Registrar tardanzas y faltas
    // ─────────────────────────────────────────

    @GetMapping("/incidencias")
    public ResponseEntity<Map<String, Object>> listarIncidencias(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate hasta) {
        try {
            List<Asistencia> incidencias = asistenciaService.listarIncidencias(desde, hasta);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "total", incidencias.size(),
                    "incidencias", incidencias
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }

    @GetMapping("/incidencias/empleado/{empleadoId}")
    public ResponseEntity<Map<String, Object>> listarIncidenciasPorEmpleado(
            @PathVariable Long empleadoId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate hasta) {
        try {
            List<Asistencia> incidencias = asistenciaService.listarIncidenciasPorEmpleado(empleadoId, desde, hasta);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "total", incidencias.size(),
                    "incidencias", incidencias
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }

    @GetMapping("/incidencias/resumen")
    public ResponseEntity<Map<String, Object>> resumenIncidencias(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate hasta) {
        try {
            List<dsw.sigconbackend.dto.ResumenIncidenciasDTO> resumen = asistenciaService.resumenIncidencias(desde, hasta);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "resumen", resumen
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }
}
