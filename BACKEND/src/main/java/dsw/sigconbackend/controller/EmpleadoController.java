package dsw.sigconbackend.controller;

import dsw.sigconbackend.model.Empleado;
import dsw.sigconbackend.service.EmpleadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Soporte de HU21/HU22/HU23 — Gestión de empleados
 * GET    /empleados                       -> listar activos
 * GET    /empleados?area=Planta           -> listar por área
 * GET    /empleados/{id}                  -> buscar uno
 * POST   /empleados                       -> crear
 * PUT    /empleados/{id}                  -> editar
 * DELETE /empleados/{id}                  -> baja lógica
 * GET    /empleados/hora-entrada-general  -> hora máxima de entrada vigente
 * PATCH  /empleados/hora-entrada-general  -> el admin la cambia para todos los empleados
 */
@RestController
@RequestMapping("/empleados")
public class EmpleadoController {

    @Autowired
    private EmpleadoService empleadoService;

    // IMPORTANTE: esta ruta debe declararse antes que "/{id}" para que Spring no la
    // confunda con una búsqueda por id (que espera un número).
    @GetMapping("/hora-entrada-general")
    public ResponseEntity<Map<String, Object>> obtenerHoraEntradaGeneral() {
        return ResponseEntity.ok(Map.of("horaEntradaGeneral", empleadoService.obtenerHoraEntradaGeneral()));
    }

    @PatchMapping("/hora-entrada-general")
    public ResponseEntity<Map<String, Object>> actualizarHoraEntradaGeneral(@RequestBody Map<String, String> body) {
        try {
            empleadoService.actualizarHoraEntradaGeneral(body.get("hora"));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "mensaje", "Hora máxima de entrada actualizada para todos los empleados",
                    "horaEntradaGeneral", body.get("hora")
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Empleado>> listar(@RequestParam(required = false) String area) {
        if (area != null && !area.isBlank()) {
            return ResponseEntity.ok(empleadoService.listarPorArea(area));
        }
        return ResponseEntity.ok(empleadoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(@PathVariable Long id) {
        Optional<Empleado> e = empleadoService.buscarPorId(id);
        if (e.isPresent()) return ResponseEntity.ok(e.get());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "mensaje", "Empleado no encontrado"));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crear(@RequestBody Empleado empleado) {
        try {
            Empleado creado = empleadoService.crear(empleado);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "mensaje", "Empleado registrado correctamente",
                    "empleado", creado
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizar(@PathVariable Long id, @RequestBody Empleado empleado) {
        Optional<Empleado> resultado = empleadoService.actualizar(id, empleado);
        if (resultado.isPresent()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "mensaje", "Empleado actualizado correctamente",
                    "empleado", resultado.get()
            ));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "mensaje", "Empleado no encontrado"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminar(@PathVariable Long id) {
        empleadoService.eliminar(id);
        return ResponseEntity.ok(Map.of("success", true, "mensaje", "Empleado eliminado"));
    }
}
