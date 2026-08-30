package dsw.sigconbackend.controller;
import dsw.sigconbackend.model.OrdenCompra;
import dsw.sigconbackend.service.OrdenCompraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orden-compra")
public class OrdenCompraController {
    @Autowired private OrdenCompraService service;

    @GetMapping public List<OrdenCompra> listar() { return service.listar(); }
    @GetMapping("/{id}") public OrdenCompra buscar(@PathVariable Long id) { return service.buscarPorId(id); }
    @PostMapping public OrdenCompra crear(@RequestBody OrdenCompra oc) { return service.crear(oc); }
    @PatchMapping("/{id}/estado") public OrdenCompra cambiarEstado(@PathVariable Long id, @RequestBody Map<String,String> body) { return service.cambiarEstado(id, body.get("estado")); }
    @DeleteMapping("/{id}") public void eliminar(@PathVariable Long id) { service.eliminar(id); }
}