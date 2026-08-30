package dsw.sigconbackend.repository;

import dsw.sigconbackend.model.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    // HU09 — listar todos ordenado por producto
    @Query("SELECT i FROM Inventario i ORDER BY i.categoria, i.producto")
    List<Inventario> findAllOrdenado();

    // HU11 — solo los productos con stock <= stock_minimo
    @Query("SELECT i FROM Inventario i WHERE i.stock <= i.stockMinimo ORDER BY i.stock ASC")
    List<Inventario> findBajoStock();

    // Buscar por categoría
    @Query("SELECT i FROM Inventario i WHERE i.categoria = :categoria ORDER BY i.producto")
    List<Inventario> findByCategoria(String categoria);

    // NUEVO — buscar por nombre de producto (case-insensitive) para descuento de stock
    Optional<Inventario> findByProductoIgnoreCase(String producto);
}