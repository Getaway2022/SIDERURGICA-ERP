package dsw.sigconbackend.service;

import dsw.sigconbackend.model.Cotizacion;
import dsw.sigconbackend.model.Inventario;
import dsw.sigconbackend.model.Pedido;
import dsw.sigconbackend.repository.CotizacionRepository;
import dsw.sigconbackend.repository.InventarioRepository;
import dsw.sigconbackend.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final CotizacionRepository cotizacionRepository;
    private final InventarioRepository inventarioRepository;

    private static final BigDecimal IGV = new BigDecimal("0.18");

    // ─── Código correlativo ───────────────────────────────────────────────────
    private String generarCodigo() {
        Integer max = pedidoRepository.findMaxCodigoNumber();
        int sig = (max == null ? 0 : max) + 1;
        return String.format("PED-%04d", sig);
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
        pedido.setCodigo(generarCodigo());
        pedido.setEstado("PENDIENTE");
        calcularMontos(pedido);
        return pedidoRepository.save(pedido);
    }

    // ─── Crear desde cotización ───────────────────────────────────────────────
    @Transactional
    public Pedido crearDesdeCotizacion(Long cotizacionId) {
        Cotizacion cot = cotizacionRepository.findById(cotizacionId)
                .orElseThrow(() -> new RuntimeException("Cotización no encontrada: " + cotizacionId));

        if (!"APROBADA".equalsIgnoreCase(cot.getEstado())) {
            throw new RuntimeException(
                    "Solo cotizaciones APROBADAS pueden convertirse a pedido. Estado actual: "
                    + cot.getEstado());
        }

        // Verificar que no existe ya un pedido para esta cotización
        // (protección ante doble clic)
        Pedido pedido = new Pedido();
        pedido.setCodigo(generarCodigo());
        pedido.setCliente(cot.getCliente());
        pedido.setRuc(cot.getRuc());
        pedido.setProducto(cot.getProducto());
        pedido.setCantidad(cot.getCantidad());
        pedido.setPrecioUnitario(cot.getPrecioUnitario());
        pedido.setEstado("PENDIENTE");
        pedido.setUsuarioId(cot.getUsuarioId());
        calcularMontos(pedido);

        Pedido guardado = pedidoRepository.save(pedido);

        cot.setEstado("CONVERTIDA");
        cotizacionRepository.save(cot);

        log.info("Pedido {} creado desde cotización {}", guardado.getCodigo(), cot.getCodigo());
        return guardado;
    }

    // ─── Actualizar estado con resultado enriquecido ──────────────────────────
    @Transactional
    public Map<String, Object> actualizarEstadoConResultado(Long id, String nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + id));

        String estadoUp = nuevoEstado.toUpperCase();
        Map<String, Object> resultado = new HashMap<>();

        if ("APROBADO".equals(estadoUp)) {
            Optional<Inventario> inventarioOpt =
                    inventarioRepository.findByProductoIgnoreCase(pedido.getProducto());

            if (inventarioOpt.isEmpty()) {
                log.warn("Producto '{}' no encontrado en inventario al aprobar pedido {}",
                        pedido.getProducto(), pedido.getCodigo());
                resultado.put("warning", "Producto no encontrado en inventario");
            } else {
                Inventario inv = inventarioOpt.get();

                if (inv.getStock() < pedido.getCantidad()) {
                    throw new RuntimeException(
                            "Stock insuficiente. Stock actual: " + inv.getStock());
                }

                int stockNuevo = inv.getStock() - pedido.getCantidad();
                inv.setStock(stockNuevo);
                inventarioRepository.save(inv);

                resultado.put("stockActualizado", true);
                resultado.put("stockNuevo", stockNuevo);

                if (inv.getStockMinimo() != null && stockNuevo <= inv.getStockMinimo()) {
                    resultado.put("bajoPstock", true);
                    log.warn("ALERTA BAJO STOCK: '{}' → stock={}, mínimo={}",
                            inv.getProducto(), stockNuevo, inv.getStockMinimo());
                }

                log.info("Inventario descontado: '{}' -{} → stock={}",
                        inv.getProducto(), pedido.getCantidad(), stockNuevo);
            }
        }

        pedido.setEstado(estadoUp);
        Pedido guardado = pedidoRepository.save(pedido);
        resultado.put("pedido", guardado);
        return resultado;
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