package dsw.sigconbackend.controller;
import dsw.sigconbackend.model.Presupuesto;
import dsw.sigconbackend.service.PresupuestoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/presupuesto")
public class PresupuestoController {
    @Autowired private PresupuestoService service;
    @GetMapping public Presupuesto actual() { return service.obtenerActual(); }
}