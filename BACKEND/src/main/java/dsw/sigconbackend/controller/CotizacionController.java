package dsw.sigconbackend.controller;

import dsw.sigconbackend.dto.CotizacionRequest;
import dsw.sigconbackend.dto.EstadoRequest;
import dsw.sigconbackend.model.Cotizacion;
import dsw.sigconbackend.model.Pedido;
import dsw.sigconbackend.service.CotizacionService;
import dsw.sigconbackend.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/cotizaciones")
public class CotizacionController {

    @Autowired
    private CotizacionService cotizacionService;

    @Autowired
    private PedidoService pedidoService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar() {
        return ResponseEntity.ok(cotizacionService.listarCotizaciones());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> registrar(@RequestBody CotizacionRequest req) {
        if (req.getCliente() == null || req.getProducto() == null ||
            req.getCantidad() == null || req.getPrecio_unitario() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false,
                                 "mensaje", "Cliente, producto, cantidad y precio son obligatorios."));
        }
        if (req.getCantidad() <= 0 || req.getPrecio_unitario().doubleValue() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false,
                                 "mensaje", "La cantidad y el precio deben ser mayores a 0."));
        }
        try {
            Cotizacion c = cotizacionService.registrarCotizacion(req);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("success", true,
                                 "mensaje", "Cotización registrada correctamente",
                                 "cotizacion", c));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "mensaje", "Error al registrar cotización"));
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Map<String, Object>> cambiarEstado(
            @PathVariable Long id, @RequestBody EstadoRequest req) {
        try {
            Optional<Cotizacion> resultado = cotizacionService.cambiarEstado(id, req.getEstado());
            if (resultado.isPresent()) {
                return ResponseEntity.ok(Map.of("success", true,
                        "mensaje", "Estado actualizado", "cotizacion", resultado.get()));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "mensaje", "Cotización no encontrada"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }

    @PostMapping("/{id}/convertir-pedido")
    public ResponseEntity<?> convertirAPedido(@PathVariable Long id) {
        try {
            Pedido pedido = pedidoService.crearDesdeCotizacion(id);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("pedido", pedido));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}