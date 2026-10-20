package dsw.sigconbackend.controller;

import dsw.sigconbackend.model.SolicitudReposicion;
import dsw.sigconbackend.service.ReposicionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/reposiciones")
@RequiredArgsConstructor
public class ReposicionController {
    private final ReposicionService service;
    public record Aprobacion(Long proveedorId, BigDecimal precioUnitario, Integer cantidadEsperada) {}
    @GetMapping public List<SolicitudReposicion> listar() { return service.listar(); }
    @PostMapping("/revisar") public List<SolicitudReposicion> revisar() {
        service.revisarInventario(); return service.listar();
    }
    @PostMapping("/{id}/aprobar") public SolicitudReposicion aprobar(@PathVariable Long id, @RequestBody Aprobacion req) {
        return service.aprobar(id, req.proveedorId(), req.precioUnitario(), req.cantidadEsperada());
    }
}
