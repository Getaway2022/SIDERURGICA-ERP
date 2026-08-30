package dsw.sigconbackend.service;

import dsw.sigconbackend.dto.CotizacionRequest;
import dsw.sigconbackend.model.Cotizacion;
import dsw.sigconbackend.repository.CotizacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * HU03 — Registrar cotización
 * HU04 — Calcular descuentos automáticamente
 * HU05 — Consultar cotizaciones
 */
@Service
public class CotizacionService {

    @Autowired
    private CotizacionRepository cotizacionRepository;

    // HU04 — misma lógica que tu Node
    private int calcularDescuento(int cantidad) {
        if (cantidad >= 100) return 10;
        if (cantidad >= 50)  return 7;
        if (cantidad >= 10)  return 5;
        return 0;
    }

    // HU05 — Consultar cotizaciones
    public List<Map<String, Object>> listarCotizaciones() {
        List<Object[]> rows = cotizacionRepository.listarCotizaciones();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id",                   row[0]);
            map.put("codigo",               row[1]);
            map.put("cliente",              row[2]);
            map.put("ruc",                  row[3]);
            map.put("producto",             row[4]);
            map.put("cantidad",             row[5]);
            map.put("precio_unitario",      row[6]);
            map.put("subtotal",             row[7]);
            map.put("descuento_porcentaje", row[8]);
            map.put("descuento_monto",      row[9]);
            map.put("total",                row[10]);
            map.put("estado",               row[11]);
            map.put("fecha_registro",       row[12]);
            map.put("vendedor",             row[13]);
            result.add(map);
        }
        return result;
    }

    // HU03 — Registrar cotización
    public Cotizacion registrarCotizacion(CotizacionRequest req) {
        int cantidadNum       = req.getCantidad();
        BigDecimal precio     = req.getPrecio_unitario();
        BigDecimal subtotal   = precio.multiply(BigDecimal.valueOf(cantidadNum));
        int descPct           = calcularDescuento(cantidadNum);
        BigDecimal descMonto  = subtotal.multiply(BigDecimal.valueOf(descPct))
                                        .divide(BigDecimal.valueOf(100));
        BigDecimal total      = subtotal.subtract(descMonto);

        Cotizacion c = new Cotizacion();
        c.setCodigo("COT-" + System.currentTimeMillis());
        c.setCliente(req.getCliente());
        c.setRuc(req.getRuc());
        c.setProducto(req.getProducto());
        c.setCantidad(cantidadNum);
        c.setPrecioUnitario(precio);
        c.setSubtotal(subtotal);
        c.setDescuentoPorcentaje(descPct);
        c.setDescuentoMonto(descMonto);
        c.setTotal(total);
        c.setEstado("PENDIENTE");
        c.setUsuarioId(req.getUsuario_id());
        return cotizacionRepository.save(c);
    }

    public Optional<Cotizacion> cambiarEstado(Long id, String estado) {
        List<String> permitidos = List.of("PENDIENTE", "APROBADA", "RECHAZADA");
        if (!permitidos.contains(estado))
            throw new IllegalArgumentException("Estado no válido");
        return cotizacionRepository.findById(id).map(c -> {
            c.setEstado(estado);
            return cotizacionRepository.save(c);
        });
    }
}
