package dsw.sigconbackend.controller;
import dsw.sigconbackend.model.PagoProveedor;
import dsw.sigconbackend.service.PagoProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pago-proveedor")
public class PagoProveedorController {
    @Autowired private PagoProveedorService service;
    @PostMapping public PagoProveedor registrar(@RequestBody PagoProveedor p) { return service.registrar(p); }
}