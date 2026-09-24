package dsw.sigconbackend.service;

import dsw.sigconbackend.model.*;
import dsw.sigconbackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReposicionService {
    private final SolicitudReposicionRepository solicitudes;
    private final InventarioRepository inventarios;
    private final OrdenCompraService ordenes;
    private final OperacionAutomatica operacion;
    private final ProveedorRepository proveedores;

    /** El llamador conserva el bloqueo durante el movimiento y esta evaluación. */
    public void evaluar(Inventario inv) {
        operacion.bloquear();
        SolicitudReposicion s = solicitudes.findByInventarioIdAndEstadoIn(inv.getId(),
                List.of("PENDIENTE", "APROBADA")).orElse(null);
        // Una compra aprobada ya cubre la necesidad hasta su recepción o cancelación.
        if (s != null && "APROBADA".equals(s.getEstado())) return;
        int objetivo = inv.getStockObjetivo() == null ? Math.multiplyExact(inv.getStockMinimo(), 2) : inv.getStockObjetivo();
        int cantidad = objetivo - inv.getStockDisponible();
        if (!inv.isBajoStock() || cantidad <= 0) {
            if (s != null) {
                s.setEstado("RESUELTA");
                s.setMensaje("La reposición ya no es necesaria por recuperación del stock disponible.");
                s.setActualizadaEn(LocalDateTime.now());
                solicitudes.save(s);
            }
            return;
        }
        if (s == null) {
            s = new SolicitudReposicion();
            s.setInventarioId(inv.getId());
        }
        s.setProducto(inv.getProducto());
        s.setCantidad(cantidad);
        s.setActualizadaEn(LocalDateTime.now());
        s.setMensaje("Stock disponible: " + inv.getStockDisponible() + ". Reponer " + cantidad
                + " para alcanzar el objetivo " + objetivo + ". Requiere aprobación de almacén.");
        solicitudes.save(s);
    }

    @Transactional(readOnly = true)
    public List<SolicitudReposicion> listar() { return solicitudes.findAllByOrderByCreadaEnDesc(); }

    public void revisarInventario() {
        operacion.bloquear();
        inventarios.findAllOrdenado().forEach(this::evaluar);
    }

    public SolicitudReposicion aprobar(Long id, Long proveedorId, BigDecimal precioUnitario, Integer cantidadEsperada) {
        operacion.bloquear();
        SolicitudReposicion s = solicitudes.findById(id).orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        if ("APROBADA".equals(s.getEstado())) return s;
        if (!"PENDIENTE".equals(s.getEstado())) throw new IllegalArgumentException("La solicitud ya fue atendida");
        Inventario inv = inventarios.findById(s.getInventarioId()).orElseThrow();
        evaluar(inv);
        if (!"PENDIENTE".equals(s.getEstado())) return s;
        if (!s.getCantidad().equals(cantidadEsperada))
            throw new IllegalArgumentException("La cantidad a reponer cambió. Actualice la bandeja y revise la solicitud antes de aprobar");
        if (proveedorId == null || precioUnitario == null || precioUnitario.signum() <= 0)
            throw new IllegalArgumentException("Seleccione proveedor y precio unitario mayor a cero");
        Proveedor proveedor = proveedores.findById(proveedorId).orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));
        if (!"Activo".equals(proveedor.getEstado())) throw new IllegalArgumentException("El proveedor debe estar activo");
        precioUnitario = precioUnitario.setScale(2, java.math.RoundingMode.HALF_UP);
        OrdenCompra oc = new OrdenCompra();
        oc.setProveedorId(proveedorId);
        oc.setProducto(inv.getProducto());
        oc.setCantidad(s.getCantidad().toString());
        oc.setTotal(precioUnitario.multiply(BigDecimal.valueOf(s.getCantidad())));
        oc = ordenes.crear(oc);
        ordenes.cambiarEstado(oc.getId(), "Aprobada");
        s.setOrdenCompraId(oc.getId());
        s.setEstado("APROBADA");
        s.setActualizadaEn(LocalDateTime.now());
        s.setMensaje("Reposición aprobada. Orden " + oc.getCodigo() + " pendiente de recepción.");
        return solicitudes.save(s);
    }
}
