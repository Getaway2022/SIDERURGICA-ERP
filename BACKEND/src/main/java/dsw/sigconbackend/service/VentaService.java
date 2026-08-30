package dsw.sigconbackend.service;

import dsw.sigconbackend.model.Pedido;
import dsw.sigconbackend.model.Venta;
import dsw.sigconbackend.repository.PedidoRepository;
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
    private final PedidoRepository pedidoRepository;

    private static final BigDecimal IGV_RATE = new BigDecimal("0.18");

    // ─── Código correlativo ───────────────────────────────────────────────────
    private String generarCodigo() {
        Integer max = ventaRepository.findMaxCodigoNumber();
        int siguiente = (max == null ? 0 : max) + 1;
        return String.format("VEN-%04d", siguiente);
    }

    // ─── Crear venta desde pedido ─────────────────────────────────────────────
    @Transactional
    public Venta crearDesdePedido(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con id: " + pedidoId));

        if (!"APROBADO".equalsIgnoreCase(pedido.getEstado())) {
            throw new RuntimeException(
                    "Solo se pueden generar ventas desde pedidos APROBADOS. Estado actual: "
                    + pedido.getEstado());
        }

        List<Venta> existentes = ventaRepository.findByPedidoId(pedidoId);
        if (!existentes.isEmpty()) {
            throw new RuntimeException(
                    "Ya existe una venta generada para el pedido: " + pedido.getCodigo());
        }

        BigDecimal precioUnitario = pedido.getPrecioUnitario();
        BigDecimal cantidad = new BigDecimal(pedido.getCantidad());
        BigDecimal subtotal = precioUnitario.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
        BigDecimal igv     = subtotal.multiply(IGV_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total   = subtotal.add(igv).setScale(2, RoundingMode.HALF_UP);

        Venta venta = new Venta();
        venta.setCodigo(generarCodigo());
        venta.setCliente(pedido.getCliente());
        venta.setRuc(pedido.getRuc());
        venta.setProducto(pedido.getProducto());
        venta.setCantidad(pedido.getCantidad());
        venta.setPrecioUnitario(precioUnitario);
        venta.setSubtotal(subtotal);
        venta.setIgv(igv);
        venta.setTotal(total);
        venta.setEstado("PENDIENTE");
        venta.setPedidoId(pedidoId);
        venta.setUsuarioId(pedido.getUsuarioId());

        Venta ventaGuardada = ventaRepository.save(venta);

        // Marcar pedido como FACTURADO
        pedido.setEstado("FACTURADO");
        pedidoRepository.save(pedido);

        log.info("Venta {} creada desde pedido {}", ventaGuardada.getCodigo(), pedido.getCodigo());
        return ventaGuardada;
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
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con id: " + id));

        validarTransicion(venta.getEstado(), nuevoEstado);
        venta.setEstado(nuevoEstado.toUpperCase());
        Venta actualizada = ventaRepository.save(venta);
        log.info("Venta {} → {}", venta.getCodigo(), nuevoEstado);
        return actualizada;
    }

    // ─── Crear venta directa ──────────────────────────────────────────────────
    @Transactional
    public Venta crearVenta(Venta venta) {
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

    // ─── Validar transición de estado ─────────────────────────────────────────
    private void validarTransicion(String actual, String nuevo) {
        String a = actual.toUpperCase();
        String n = nuevo.toUpperCase();
        boolean valida = switch (a) {
            case "PENDIENTE"  -> n.equals("APROBADO") || n.equals("ANULADO");
            case "APROBADO"   -> n.equals("COMPLETADO") || n.equals("ANULADO");
            case "COMPLETADO", "ANULADO" -> false;
            default -> false;
        };
        if (!valida) {
            throw new RuntimeException("Transición no permitida: " + a + " → " + n);
        }
    }
}