package dsw.sigconbackend.service;

import dsw.sigconbackend.model.OrdenCompra;
import dsw.sigconbackend.repository.OrdenCompraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class OrdenCompraService {

    @Autowired private OrdenCompraRepository repo;
    @Autowired private PresupuestoService presupuestoService;

    public List<OrdenCompra> listar() { return repo.findAll(); }

    public OrdenCompra buscarPorId(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Orden no encontrada"));
    }

    public OrdenCompra crear(OrdenCompra oc) {
        presupuestoService.comprometer(oc.getTotal());
        oc.setCodigo("OC-" + String.format("%04d", repo.count() + 1));
        oc.setEstado("Pendiente");
        return repo.save(oc);
    }

    public OrdenCompra cambiarEstado(Long id, String estado) {
        OrdenCompra oc = buscarPorId(id);
        if ("Rechazada".equals(estado) || "Cancelada".equals(estado)) {
            presupuestoService.liberar(oc.getTotal());
        }
        oc.setEstado(estado);
        return repo.save(oc);
    }

    public void eliminar(Long id) {
        OrdenCompra oc = buscarPorId(id);
        presupuestoService.liberar(oc.getTotal());
        repo.deleteById(id);
    }
}