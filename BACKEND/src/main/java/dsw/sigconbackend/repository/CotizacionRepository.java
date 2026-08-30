package dsw.sigconbackend.repository;

import dsw.sigconbackend.model.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    @Query(value = """
        SELECT c.id, c.codigo, c.cliente, c.ruc, c.producto,
               c.cantidad, c.precio_unitario, c.subtotal,
               c.descuento_porcentaje, c.descuento_monto,
               c.total, c.estado, c.fecha_registro,
               u.username AS vendedor
        FROM comercial.cotizacion c
        LEFT JOIN seguridad.usuario u ON c.usuario_id = u.id
        ORDER BY c.id DESC
        """, nativeQuery = true)
    List<Object[]> listarCotizaciones();
}
