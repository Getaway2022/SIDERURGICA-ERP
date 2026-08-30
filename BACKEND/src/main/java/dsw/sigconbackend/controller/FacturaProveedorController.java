package dsw.sigconbackend.controller;
import dsw.sigconbackend.model.FacturaProveedor;
import dsw.sigconbackend.service.FacturaProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/factura-proveedor")
public class FacturaProveedorController {
    @Autowired private FacturaProveedorService service;
    @GetMapping public List<FacturaProveedor> listar() { return service.listar(); }
    @GetMapping("/proveedor/{id}") public List<FacturaProveedor> porProveedor(@PathVariable Long id) { return service.listarPorProveedor(id); }
    @PostMapping public FacturaProveedor registrar(@RequestBody FacturaProveedor f) { return service.registrar(f); }
}