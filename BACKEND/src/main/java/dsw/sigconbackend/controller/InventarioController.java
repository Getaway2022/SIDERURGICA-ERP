package dsw.sigconbackend.controller;

import dsw.sigconbackend.dto.ActualizarStockRequest;
import dsw.sigconbackend.dto.InventarioRequest;
import dsw.sigconbackend.model.Inventario;
import dsw.sigconbackend.service.InventarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HU09 — GET  /inventario              → listar todo el stock
 * HU09 — GET  /inventario/bajo-stock   → solo productos con alerta
 * HU10 — POST /inventario              → crear producto
 * HU10 — PUT  /inventario/:id          → editar producto completo
 * HU10 — PATCH /inventario/:id/stock   → entrada/salida de stock
 * HU11 — GET  /inventario/bajo-stock   → alertas de bajo stock
 */
@RestController
@RequestMapping("/inventario")
public class InventarioController {

    @Autowired
    private InventarioService inventarioService;

    // ─────────────────────────────────────────
    // HU09 — Visualizar stock disponible
    // ─────────────────────────────────────────

    // GET /inventario
    @GetMapping
    public ResponseEntity<List<Inventario>> listar() {
        return ResponseEntity.ok(inventarioService.listarInventario());
    }

    // GET /inventario/:id
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        Optional<Inventario> inv = inventarioService.buscarPorId(id);
        if (inv.isPresent()) return ResponseEntity.ok(inv.get());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "mensaje", "Producto no encontrado"));
    }

    // ─────────────────────────────────────────
    // HU11 — Alertar bajo stock
    // ─────────────────────────────────────────

    // GET /inventario/bajo-stock
    @GetMapping("/bajo-stock")
    public ResponseEntity<Map<String, Object>> bajoStock() {
        List<Inventario> lista = inventarioService.listarBajoStock();
        return ResponseEntity.ok(Map.of(
                "total",     lista.size(),
                "productos", lista
        ));
    }
     @GetMapping("/valor-total")
public ResponseEntity<Map<String, Object>> valorTotal() {
    double total = inventarioService.calcularValorTotal();
    return ResponseEntity.ok(Map.of(
        "success", true,
        "valorTotal", total
    ));
}
    // ─────────────────────────────────────────
    // HU10 — Actualizar inventario
    // ─────────────────────────────────────────

    // POST /inventario — crear nuevo producto
    @PostMapping
    public ResponseEntity<Map<String, Object>> crear(@RequestBody InventarioRequest req) {
        try {
            Inventario inv = inventarioService.crearProducto(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success",    true,
                    "mensaje",    "Producto registrado correctamente",
                    "inventario", inv
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }

    // PUT /inventario/:id — editar producto completo
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizar(
            @PathVariable Long id, @RequestBody InventarioRequest req) {
        try {
            Optional<Inventario> resultado = inventarioService.actualizarProducto(id, req);
            if (resultado.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success",    true,
                        "mensaje",    "Producto actualizado correctamente",
                        "inventario", resultado.get()
                ));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "mensaje", "Producto no encontrado"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }

    // PATCH /inventario/:id/stock — entrada o salida de stock
    @PatchMapping("/{id}/stock")
    public ResponseEntity<Map<String, Object>> actualizarStock(
            @PathVariable Long id, @RequestBody ActualizarStockRequest req) {
        try {
            Optional<Inventario> resultado = inventarioService.actualizarStock(id, req);
            if (resultado.isPresent()) {
                Inventario inv = resultado.get();
                return ResponseEntity.ok(Map.of(
                        "success",    true,
                        "mensaje",    "Stock actualizado correctamente",
                        "inventario", inv,
                        "bajo_stock", inv.isBajoStock()
                ));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "mensaje", "Producto no encontrado"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }
}
