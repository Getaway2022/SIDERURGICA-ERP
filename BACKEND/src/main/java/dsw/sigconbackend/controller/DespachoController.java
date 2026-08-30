package dsw.sigconbackend.controller;

import dsw.sigconbackend.model.Despacho;
import dsw.sigconbackend.service.DespachoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/despacho")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class DespachoController {

    private final DespachoService despachoService;

    @GetMapping
    public ResponseEntity<List<Despacho>> listar() {
        return ResponseEntity.ok(despachoService.listarDespachos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Despacho> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(despachoService.obtenerPorId(id));
    }

    @PostMapping("/desde-venta/{ventaId}")
    public ResponseEntity<?> crearDesdeVenta(@PathVariable Long ventaId) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(despachoService.crearDesdeVenta(ventaId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Despacho d) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(despachoService.crear(d));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Despacho d) {
        try {
            return ResponseEntity.ok(despachoService.actualizar(id, d));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        despachoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstado(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(despachoService.actualizarEstado(id, body.get("estado")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/preparar")
    public ResponseEntity<?> preparar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(despachoService.preparar(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/comprobante")
    public ResponseEntity<?> validarComprobante(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(despachoService.validarComprobante(id, body.get("comprobante")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/confirmar-entrega")
    public ResponseEntity<?> confirmarEntrega(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(despachoService.confirmarEntrega(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}