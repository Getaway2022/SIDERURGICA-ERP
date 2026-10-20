package dsw.sigconbackend.service;

import dsw.sigconbackend.model.Venta;
import dsw.sigconbackend.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VentaService {

    private final VentaRepository ventaRepository;
    private final FlujoComercialService flujo;
    private final OperacionAutomatica operacion;

    private static final BigDecimal IGV_RATE = new BigDecimal("0.18");

    // ─── Código correlativo ───────────────────────────────────────────────────
    private String generarCodigo() {
        return operacion.codigo("VEN");
    }

    // ─── Crear venta desde pedido ─────────────────────────────────────────────
    @Transactional
    public Venta crearDesdePedido(Long pedidoId) {
        return flujo.crearVenta(pedidoId);
    }

    // ─── Listar ───────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Venta> listarVentas() {
        return ventaRepository.findAllByOrderByCreatedAtDesc();
    }

    // ─── Obtener por ID ───────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Venta obtenerPorId(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con id: " + id));
    }

    // ─── Actualizar estado ────────────────────────────────────────────────────
    @Transactional
    public Venta actualizarEstado(Long id, String nuevoEstado) {
        return flujo.cambiarVenta(id, nuevoEstado);
    }

    // ─── Crear venta directa ──────────────────────────────────────────────────
    @Transactional
    public Venta crearVenta(Venta venta) {
        operacion.bloquear();
        if (venta.getCantidad() == null || venta.getCantidad() <= 0 || venta.getPrecioUnitario() == null || venta.getPrecioUnitario().signum() <= 0)
            throw new IllegalArgumentException("Cantidad y precio deben ser mayores a cero");
        venta.setId(null); venta.setPedidoId(null); venta.setCotizacionId(null);
        venta.setEstado("PENDIENTE"); venta.setCodigo(null);
        if (venta.getCodigo() == null || venta.getCodigo().isBlank()) {
            venta.setCodigo(generarCodigo());
        }
        if (venta.getEstado() == null || venta.getEstado().isBlank()) {
            venta.setEstado("PENDIENTE");
        }
        BigDecimal subtotal = venta.getPrecioUnitario()
                .multiply(new BigDecimal(venta.getCantidad()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal igv   = subtotal.multiply(IGV_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(igv).setScale(2, RoundingMode.HALF_UP);
        venta.setSubtotal(subtotal);
        venta.setIgv(igv);
        venta.setTotal(total);
        return ventaRepository.save(venta);
    }

}
