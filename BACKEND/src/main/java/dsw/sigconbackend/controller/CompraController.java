package dsw.sigconbackend.controller;
import dsw.sigconbackend.model.Compra;
import dsw.sigconbackend.service.CompraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/compra")
public class CompraController {
    @Autowired private CompraService service;

    @GetMapping public List<Compra> listar() { return service.listar(); }
    @PostMapping public CompraService.RegistrarCompraResultado registrar(@RequestBody Compra c) { return service.registrar(c); }
}