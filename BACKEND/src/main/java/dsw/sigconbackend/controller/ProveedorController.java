package dsw.sigconbackend.controller;
import dsw.sigconbackend.model.Proveedor;
import dsw.sigconbackend.service.ProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/proveedor")
public class ProveedorController {
    @Autowired private ProveedorService service;

    @GetMapping public List<Proveedor> listar() { return service.listar(); }
    @GetMapping("/{id}") public Proveedor buscar(@PathVariable Long id) { return service.buscarPorId(id); }
    @PostMapping public Proveedor crear(@RequestBody Proveedor p) { return service.crear(p); }
    @PutMapping("/{id}") public Proveedor actualizar(@PathVariable Long id, @RequestBody Proveedor p) { return service.actualizar(id, p); }
    @DeleteMapping("/{id}") public void eliminar(@PathVariable Long id) { service.eliminar(id); }
}