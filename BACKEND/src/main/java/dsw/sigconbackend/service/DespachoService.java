package dsw.sigconbackend.service;

import dsw.sigconbackend.model.Despacho;
import dsw.sigconbackend.repository.DespachoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DespachoService {

    private final DespachoRepository despachoRepository;
    private final FlujoComercialService flujo;
    private final OperacionAutomatica operacion;

    public Despacho buscarPorId(Long id) {
        return despachoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Despacho no encontrado con id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Despacho> listarDespachos() {
        return despachoRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Despacho obtenerPorId(Long id) { return buscarPorId(id); }

    @Transactional
    public Despacho crearDesdeVenta(Long ventaId) {
        return flujo.crearDespacho(ventaId);
    }

    @Transactional
    public Despacho crear(Despacho d) {
        if (d.getVentaId() == null) throw new IllegalArgumentException("Genere el despacho aprobando una venta");
        return flujo.crearDespacho(d.getVentaId());
    }

    @Transactional
    public Despacho actualizar(Long id, Despacho datos) {
        operacion.bloquear();
        Despacho d = buscarPorId(id);
        if (!List.of("PENDIENTE", "PREPARADO").contains(d.getEstado())) throw new IllegalArgumentException("Solo puede editar despachos previos al envío");
        d.setDireccion(datos.getDireccion()); d.setPeso(datos.getPeso()); d.setTransportista(datos.getTransportista());
        return despachoRepository.save(d);
    }

    @Transactional
    public void eliminar(Long id) {
        flujo.cambiarDespacho(id, "ANULADO", null);
    }

    @Transactional
    public Despacho actualizarEstado(Long id, String nuevoEstado) {
        return flujo.cambiarDespacho(id, nuevoEstado, null);
    }

    @Transactional
    public Despacho preparar(Long id) {
        return flujo.cambiarDespacho(id, "PREPARADO", null);
    }

    @Transactional
    public Despacho validarComprobante(Long id, String comprobante) {
        return flujo.cambiarDespacho(id, "ENVIADO", comprobante);
    }

    @Transactional
    public Despacho confirmarEntrega(Long id) {
        return flujo.cambiarDespacho(id, "ENTREGADO", null);
    }
}
