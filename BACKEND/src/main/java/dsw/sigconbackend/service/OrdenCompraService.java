package dsw.sigconbackend.service;

import dsw.sigconbackend.model.OrdenCompra;
import dsw.sigconbackend.repository.OrdenCompraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrdenCompraService {

    @Autowired private OrdenCompraRepository repo;
    @Autowired private PresupuestoService presupuestoService;
    @Autowired private OperacionAutomatica operacion;
    @Autowired private dsw.sigconbackend.repository.SolicitudReposicionRepository solicitudes;

    public List<OrdenCompra> listar() { return repo.findAll(); }

    public OrdenCompra buscarPorId(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Orden no encontrada"));
    }

    public OrdenCompra crear(OrdenCompra oc) {
        operacion.bloquear();
        if (oc.getTotal() == null || oc.getTotal().signum() <= 0 || oc.getProveedorId() == null)
            throw new IllegalArgumentException("Proveedor y total positivo son obligatorios");
        presupuestoService.comprometer(oc.getTotal());
        oc.setId(null);
        oc.setCodigo(operacion.codigo("OC"));
        oc.setPresupuestoPeriodo(java.time.LocalDate.now().toString().substring(0, 7));
        oc.setEstado("Pendiente");
        return repo.save(oc);
    }

    public OrdenCompra cambiarEstado(Long id, String estado) {
        operacion.bloquear();
        OrdenCompra oc = buscarPorId(id);
        if (oc.getEstado().equals(estado)) return oc;
        boolean valida = switch (oc.getEstado()) {
            case "Pendiente" -> List.of("Aprobada", "Rechazada", "Cancelada").contains(estado);
            case "Aprobada" -> List.of("En tránsito", "Cancelada").contains(estado);
            case "En tránsito" -> "Cancelada".equals(estado);
            default -> false;
        };
        if (!valida) throw new IllegalArgumentException("Transición de orden no permitida");
        if ("Rechazada".equals(estado) || "Cancelada".equals(estado)) {
            presupuestoService.liberar(oc.getTotal(), oc.getPresupuestoPeriodo());
            solicitudes.findByOrdenCompraId(id).ifPresent(s -> {
                s.setEstado("CANCELADA"); s.setActualizadaEn(java.time.LocalDateTime.now());
                s.setMensaje("Orden cancelada; se volverá a evaluar la necesidad de reposición."); solicitudes.save(s);
            });
        }
        oc.setEstado(estado);
        return repo.save(oc);
    }

    public void eliminar(Long id) {
        cambiarEstado(id, "Cancelada");
    }
}
