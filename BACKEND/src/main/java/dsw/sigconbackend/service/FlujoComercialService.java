package dsw.sigconbackend.service;

import dsw.sigconbackend.model.*;
import dsw.sigconbackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** Cada transición y sus documentos derivados se confirman en una sola transacción. */
@Service
@RequiredArgsConstructor
@Transactional
public class FlujoComercialService {
    private final CotizacionRepository cotizaciones;
    private final PedidoRepository pedidos;
    private final VentaRepository ventas;
    private final DespachoRepository despachos;
    private final InventarioRepository inventarios;
    private final OperacionAutomatica operacion;
    private final ReposicionService reposicion;

    public Optional<Cotizacion> cambiarCotizacion(Long id, String estado) {
        operacion.bloquear();
        String nuevo = normalizar(estado);
        if (!List.of("APROBADA", "RECHAZADA", "PENDIENTE").contains(nuevo))
            throw new IllegalArgumentException("Estado no válido");
        return cotizaciones.findById(id).map(c -> {
            if ("APROBADA".equals(nuevo) && "CONVERTIDA".equals(c.getEstado())) return c;
            if (nuevo.equals(c.getEstado()) && !"APROBADA".equals(nuevo)) return c;
            if (!List.of("PENDIENTE", "APROBADA").contains(c.getEstado()))
                throw new IllegalArgumentException("La cotización ya fue atendida");
            c.setEstado(nuevo);
            cotizaciones.save(c);
            if ("APROBADA".equals(nuevo)) convertirCotizacion(id);
            return c;
        });
    }

    public Pedido convertirCotizacion(Long id) {
        operacion.bloquear();
        Pedido existente = pedidos.findByCotizacionId(id).orElse(null);
        if (existente != null) return existente;
        Cotizacion c = cotizaciones.findById(id).orElseThrow(() -> new IllegalArgumentException("Cotización no encontrada"));
        if (!"APROBADA".equals(c.getEstado())) throw new IllegalArgumentException("La cotización debe estar aprobada");
        Pedido p = new Pedido();
        p.setCodigo(operacion.codigo("PED"));
        p.setCotizacionId(c.getId());
        p.setInventarioId(c.getInventarioId());
        p.setCliente(c.getCliente()); p.setRuc(c.getRuc()); p.setProducto(c.getProducto());
        p.setCantidad(c.getCantidad()); p.setPrecioUnitario(c.getPrecioUnitario()); p.setUsuarioId(c.getUsuarioId());
        // La cotización expresa el importe neto sin IGV, conservando su descuento.
        p.setSubtotal(c.getTotal());
        p.setIgv(c.getTotal().multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP));
        p.setTotal(p.getSubtotal().add(p.getIgv()));
        p.setEstado("PENDIENTE");
        pedidos.save(p);
        c.setEstado("CONVERTIDA"); cotizaciones.save(c);
        return p;
    }

    public Map<String, Object> cambiarPedido(Long id, String estado) {
        operacion.bloquear();
        Pedido p = pedido(id);
        String nuevo = normalizar(estado);
        if ("APROBADO".equals(nuevo)) {
            if (List.of("APROBADO", "FACTURADO", "ENVIADO", "ENTREGADO").contains(p.getEstado()))
                return Map.of("pedido", p);
            if (!"PENDIENTE".equals(p.getEstado())) throw new IllegalArgumentException("El pedido no está pendiente");
            reservar(p);
            p.setEstado("APROBADO"); pedidos.save(p);
            Venta v = crearVenta(id);
            Inventario inv = inventarios.findById(p.getInventarioId()).orElseThrow();
            return Map.of("pedido", p, "venta", v, "stockReservado", true,
                    "stockDisponible", inv.getStockDisponible(), "bajoPstock", inv.isBajoStock());
        }
        if ("ANULADO".equals(nuevo)) { anular(p); return Map.of("pedido", p); }
        throw new IllegalArgumentException("Solo puede aprobar o anular un pedido; envío y entrega dependen del despacho");
    }

    private void reservar(Pedido p) {
        if (!"SIN_RESERVA".equals(p.getEstadoStock())) throw new IllegalArgumentException("El pedido ya tiene movimiento de stock");
        if (p.getCantidad() == null || p.getCantidad() <= 0) throw new IllegalArgumentException("Cantidad inválida");
        Inventario inv = p.getInventarioId() == null
                ? inventarios.findByProductoIgnoreCase(p.getProducto()).orElseThrow(() -> new IllegalArgumentException("Producto no encontrado en inventario"))
                : inventarios.findById(p.getInventarioId()).orElseThrow(() -> new IllegalArgumentException("Producto no encontrado en inventario"));
        if (inv.getStockDisponible() < p.getCantidad()) throw new IllegalArgumentException("Stock disponible insuficiente: " + inv.getStockDisponible());
        p.setInventarioId(inv.getId());
        p.setEstadoStock("RESERVADO");
        inv.setStockReservado(inv.getStockReservado() + p.getCantidad());
        guardarInventario(inv);
    }

    public Venta crearVenta(Long pedidoId) {
        operacion.bloquear();
        Pedido p = pedido(pedidoId);
        List<Venta> existentes = ventas.findByPedidoId(pedidoId);
        if (!existentes.isEmpty()) return existentes.get(0);
        if (!"APROBADO".equals(p.getEstado())) throw new IllegalArgumentException("El pedido debe estar aprobado");
        validarStockLegado(p);
        Venta v = new Venta();
        v.setCodigo(operacion.codigo("VEN")); v.setPedidoId(p.getId());
        v.setCliente(p.getCliente()); v.setRuc(p.getRuc()); v.setProducto(p.getProducto());
        v.setCantidad(p.getCantidad()); v.setPrecioUnitario(p.getPrecioUnitario());
        v.setSubtotal(p.getSubtotal()); v.setIgv(p.getIgv()); v.setTotal(p.getTotal());
        v.setUsuarioId(p.getUsuarioId()); v.setEstado("PENDIENTE");
        ventas.save(v);
        p.setEstado("FACTURADO"); pedidos.save(p);
        return v;
    }

    public Venta cambiarVenta(Long id, String estado) {
        operacion.bloquear();
        Venta v = ventas.findById(id).orElseThrow(() -> new IllegalArgumentException("Venta no encontrada"));
        String nuevo = normalizar(estado);
        if (nuevo.equals(v.getEstado())) return v;
        if ("APROBADO".equals(nuevo) && "PENDIENTE".equals(v.getEstado())) {
            if (v.getPedidoId() == null) {
                Pedido p = new Pedido();
                p.setCodigo(operacion.codigo("PED")); p.setCliente(v.getCliente()); p.setRuc(v.getRuc());
                p.setProducto(v.getProducto()); p.setCantidad(v.getCantidad()); p.setPrecioUnitario(v.getPrecioUnitario());
                p.setSubtotal(v.getSubtotal()); p.setIgv(v.getIgv()); p.setTotal(v.getTotal()); p.setUsuarioId(v.getUsuarioId());
                reservar(p); p.setEstado("FACTURADO"); pedidos.save(p); v.setPedidoId(p.getId());
            }
            Pedido p = pedido(v.getPedidoId());
            validarStockLegado(p);
            if (!List.of("RESERVADO", "DESCONTADO_LEGADO").contains(p.getEstadoStock()))
                throw new IllegalArgumentException("La venta no tiene una reserva vigente");
            v.setEstado(nuevo); ventas.save(v); crearDespacho(id);
        } else if ("ANULADO".equals(nuevo) && List.of("PENDIENTE", "APROBADO").contains(v.getEstado())) {
            if (v.getPedidoId() != null) anular(pedido(v.getPedidoId()));
            v.setEstado(nuevo); ventas.save(v);
        } else throw new IllegalArgumentException("Transición de venta no permitida");
        return v;
    }

    public Despacho crearDespacho(Long ventaId) {
        operacion.bloquear();
        List<Despacho> existentes = despachos.findByVentaId(ventaId);
        if (!existentes.isEmpty()) return existentes.get(0);
        Venta v = ventas.findById(ventaId).orElseThrow(() -> new IllegalArgumentException("Venta no encontrada"));
        if (!"APROBADO".equals(v.getEstado())) throw new IllegalArgumentException("La venta debe estar aprobada");
        if (v.getPedidoId() == null) throw new IllegalArgumentException("La venta no tiene un pedido reservado");
        validarStockLegado(pedido(v.getPedidoId()));
        Despacho d = new Despacho();
        d.setCodigo(operacion.codigo("DES")); d.setVentaId(ventaId); d.setPedidoId(v.getPedidoId());
        d.setCliente(v.getCliente()); d.setRuc(v.getRuc()); d.setProducto(v.getProducto());
        d.setCantidad(v.getCantidad()); d.setUsuarioId(v.getUsuarioId()); d.setEstado("PENDIENTE");
        return despachos.save(d);
    }

    public Despacho cambiarDespacho(Long id, String estado, String comprobante) {
        operacion.bloquear();
        Despacho d = despachos.findById(id).orElseThrow(() -> new IllegalArgumentException("Despacho no encontrado"));
        String nuevo = normalizar(estado);
        if (nuevo.equals(d.getEstado())) return d;
        if ("PREPARADO".equals(nuevo) && "PENDIENTE".equals(d.getEstado())) d.setEstado(nuevo);
        else if ("ENVIADO".equals(nuevo) && "PREPARADO".equals(d.getEstado())) {
            if (comprobante == null || comprobante.isBlank()) throw new IllegalArgumentException("Debe adjuntar un comprobante");
            if (d.getPedidoId() == null) throw new IllegalArgumentException("Despacho histórico sin pedido: requiere conciliación de stock");
            Pedido p = pedido(d.getPedidoId()); validarStockLegado(p);
            if ("RESERVADO".equals(p.getEstadoStock())) {
                Inventario inv = inventarios.findById(p.getInventarioId()).orElseThrow();
                if (inv.getStockReservado() < p.getCantidad() || inv.getStock() < p.getCantidad())
                    throw new IllegalArgumentException("Reserva inconsistente; revise el inventario");
                inv.setStock(inv.getStock() - p.getCantidad());
                inv.setStockReservado(inv.getStockReservado() - p.getCantidad());
                guardarInventario(inv);
            } else if (!"DESCONTADO_LEGADO".equals(p.getEstadoStock())) throw new IllegalArgumentException("El pedido no tiene reserva vigente");
            p.setEstadoStock("ENVIADO"); p.setEstado("ENVIADO"); pedidos.save(p);
            d.setComprobante(comprobante); d.setComprobanteValidado(true);
            d.setFechaValidacionComprobante(LocalDateTime.now()); d.setFechaDespacho(LocalDateTime.now()); d.setEstado(nuevo);
        } else if ("ENTREGADO".equals(nuevo) && "ENVIADO".equals(d.getEstado())) {
            d.setEstado(nuevo); d.setFechaEntrega(LocalDateTime.now());
            if (d.getPedidoId() != null) { Pedido p = pedido(d.getPedidoId()); p.setEstado("ENTREGADO"); pedidos.save(p); }
            if (d.getVentaId() != null) { Venta v = ventas.findById(d.getVentaId()).orElseThrow(); v.setEstado("COMPLETADO"); ventas.save(v); }
        } else if ("ANULADO".equals(nuevo) && List.of("PENDIENTE", "PREPARADO").contains(d.getEstado())) {
            if (d.getPedidoId() == null) throw new IllegalArgumentException("Despacho histórico sin pedido: requiere conciliación");
            anular(pedido(d.getPedidoId())); d.setEstado(nuevo);
        } else throw new IllegalArgumentException("Transición de despacho no permitida");
        return despachos.save(d);
    }

    private void anular(Pedido p) {
        if ("ANULADO".equals(p.getEstado())) return;
        if (List.of("ENVIADO", "ENTREGADO").contains(p.getEstado())) throw new IllegalArgumentException("No puede cancelar después del envío");
        validarStockLegado(p);
        for (Venta v : ventas.findByPedidoId(p.getId())) {
            for (Despacho d : despachos.findByVentaId(v.getId())) {
                if (List.of("ENVIADO", "ENTREGADO").contains(d.getEstado())) throw new IllegalArgumentException("El despacho ya fue enviado");
                d.setEstado("ANULADO"); despachos.save(d);
            }
            v.setEstado("ANULADO"); ventas.save(v);
        }
        if (List.of("RESERVADO", "DESCONTADO_LEGADO").contains(p.getEstadoStock())) {
            Inventario inv = inventarios.findById(p.getInventarioId()).orElseThrow();
            if ("RESERVADO".equals(p.getEstadoStock())) inv.setStockReservado(inv.getStockReservado() - p.getCantidad());
            else inv.setStock(inv.getStock() + p.getCantidad());
            guardarInventario(inv); p.setEstadoStock("LIBERADO");
        }
        p.setEstado("ANULADO"); pedidos.save(p);
    }

    private void guardarInventario(Inventario inv) {
        inv.setFechaActualizacion(LocalDateTime.now()); inventarios.save(inv); reposicion.evaluar(inv);
    }
    private void validarStockLegado(Pedido p) {
        if ("REVISION_LEGADO".equals(p.getEstadoStock()))
            throw new IllegalArgumentException("Pedido anterior a la automatización: concilie su descuento de stock antes de continuar");
    }
    private Pedido pedido(Long id) { return pedidos.findById(id).orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado")); }
    private String normalizar(String estado) {
        if (estado == null || estado.isBlank()) throw new IllegalArgumentException("Estado obligatorio");
        return estado.toUpperCase(Locale.ROOT);
    }
}
