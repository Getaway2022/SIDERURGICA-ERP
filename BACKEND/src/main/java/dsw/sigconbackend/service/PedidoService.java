package dsw.sigconbackend.service;

import dsw.sigconbackend.model.Pedido;
import dsw.sigconbackend.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final FlujoComercialService flujo;
    private final OperacionAutomatica operacion;

    private static final BigDecimal IGV = new BigDecimal("0.18");

    // ─── Código correlativo ───────────────────────────────────────────────────
    private String generarCodigo() {
        return operacion.codigo("PED");
    }

    // ─── Listar ───────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAllByOrderByFechaRegistroDesc();
    }

    // ─── Obtener ──────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Pedido obtenerPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + id));
    }

    // ─── Registrar pedido directo (desde frontend) ────────────────────────────
    @Transactional
    public Pedido registrarPedido(Pedido pedido) {
        operacion.bloquear();
        if (pedido.getCantidad() == null || pedido.getCantidad() <= 0 || pedido.getPrecioUnitario() == null || pedido.getPrecioUnitario().signum() <= 0)
            throw new IllegalArgumentException("Cantidad y precio deben ser mayores a cero");
        pedido.setId(null);
        pedido.setCotizacionId(null);
        pedido.setEstadoStock("SIN_RESERVA");
        pedido.setCodigo(generarCodigo());
        pedido.setEstado("PENDIENTE");
        calcularMontos(pedido);
        return pedidoRepository.save(pedido);
    }

    // ─── Crear desde cotización ───────────────────────────────────────────────
    @Transactional
    public Pedido crearDesdeCotizacion(Long cotizacionId) {
        return flujo.convertirCotizacion(cotizacionId);
    }

    // ─── Actualizar estado con resultado enriquecido ──────────────────────────
    @Transactional
    public Map<String, Object> actualizarEstadoConResultado(Long id, String nuevoEstado) {
        return flujo.cambiarPedido(id, nuevoEstado);
    }

    // ─── Calcular subtotal, IGV, total ────────────────────────────────────────
    private void calcularMontos(Pedido pedido) {
        if (pedido.getPrecioUnitario() == null || pedido.getCantidad() == null) return;
        BigDecimal subtotal = pedido.getPrecioUnitario()
                .multiply(new BigDecimal(pedido.getCantidad()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal igv = subtotal.multiply(IGV).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(igv).setScale(2, RoundingMode.HALF_UP);
        pedido.setSubtotal(subtotal);
        pedido.setIgv(igv);
        pedido.setTotal(total);
    }
}
