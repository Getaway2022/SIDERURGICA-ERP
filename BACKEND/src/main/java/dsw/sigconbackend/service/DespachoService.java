package dsw.sigconbackend.service;

import dsw.sigconbackend.model.Despacho;
import dsw.sigconbackend.model.Venta;
import dsw.sigconbackend.repository.DespachoRepository;
import dsw.sigconbackend.repository.PedidoRepository;
import dsw.sigconbackend.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DespachoService {

    private final DespachoRepository despachoRepository;
    private final VentaRepository ventaRepository;
    private final PedidoRepository pedidoRepository;

    private String generarCodigo() {
        Integer max = despachoRepository.findMaxCodigoNumber();
        int siguiente = (max == null ? 0 : max) + 1;
        return String.format("DES-%04d", siguiente);
    }

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
        Venta venta = ventaRepository.findById(ventaId)
            .orElseThrow(() -> new RuntimeException("Venta no encontrada con id: " + ventaId));
        if (!"APROBADO".equalsIgnoreCase(venta.getEstado()))
            throw new RuntimeException("Solo se pueden generar despachos desde ventas APROBADAS. Estado actual: " + venta.getEstado());
        if (!despachoRepository.findByVentaId(ventaId).isEmpty())
            throw new RuntimeException("Ya existe un despacho para la venta: " + venta.getCodigo());

        Despacho d = new Despacho();
        d.setCodigo(generarCodigo());
        d.setCliente(venta.getCliente());
        d.setRuc(venta.getRuc());
        d.setProducto(venta.getProducto());
        d.setCantidad(venta.getCantidad());
        d.setEstado("PENDIENTE");
        d.setVentaId(ventaId);
        d.setUsuarioId(venta.getUsuarioId());
        Despacho guardado = despachoRepository.save(d);
        venta.setEstado("COMPLETADO");
        ventaRepository.save(venta);
        log.info("Despacho {} creado desde venta {}", guardado.getCodigo(), venta.getCodigo());
        return guardado;
    }

    @Transactional
    public Despacho crear(Despacho d) {
        if (d.getCodigo() == null || d.getCodigo().isBlank())
            d.setCodigo(generarCodigo());
        d.setEstado("PENDIENTE");
        return despachoRepository.save(d);
    }

    @Transactional
    public Despacho actualizar(Long id, Despacho datos) {
        Despacho d = buscarPorId(id);
        d.setCliente(datos.getCliente());
        d.setProducto(datos.getProducto());
        d.setDireccion(datos.getDireccion());
        d.setPeso(datos.getPeso());
        d.setTransportista(datos.getTransportista());
        return despachoRepository.save(d);
    }

    @Transactional
    public void eliminar(Long id) { despachoRepository.deleteById(id); }

    @Transactional
    public Despacho actualizarEstado(Long id, String nuevoEstado) {
        Despacho d = buscarPorId(id);
        d.setEstado(nuevoEstado.toUpperCase());
        log.info("Despacho {} → {}", d.getCodigo(), nuevoEstado);
        return despachoRepository.save(d);
    }

    @Transactional
    public Despacho preparar(Long id) {
        Despacho d = buscarPorId(id);
        if (!"PENDIENTE".equals(d.getEstado())) throw new RuntimeException("Solo se puede preparar un despacho PENDIENTE");
        d.setEstado("PREPARADO");
        return despachoRepository.save(d);
    }

    @Transactional
    public Despacho validarComprobante(Long id, String comprobante) {
        Despacho d = buscarPorId(id);
        if (comprobante == null || comprobante.isBlank()) throw new RuntimeException("Debe adjuntar un comprobante");
        d.setComprobante(comprobante);
        d.setComprobanteValidado(true);
        d.setFechaValidacionComprobante(LocalDateTime.now());
        if ("PREPARADO".equals(d.getEstado())) d.setEstado("ENVIADO");
        return despachoRepository.save(d);
    }

    @Transactional
    public Despacho confirmarEntrega(Long id) {
        Despacho d = buscarPorId(id);
        if (!Boolean.TRUE.equals(d.getComprobanteValidado())) throw new RuntimeException("Falta validar comprobante");
        d.setEstado("ENTREGADO");
        d.setFechaEntrega(LocalDateTime.now());
        despachoRepository.save(d);
        if (d.getPedidoId() != null)
            pedidoRepository.findById(d.getPedidoId()).ifPresent(p -> { p.setEstado("ENTREGADO"); pedidoRepository.save(p); });
        return d;
    }
}