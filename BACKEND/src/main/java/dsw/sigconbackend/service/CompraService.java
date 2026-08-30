package dsw.sigconbackend.service;

import dsw.sigconbackend.model.Compra;
import dsw.sigconbackend.model.Inventario;
import dsw.sigconbackend.repository.CompraRepository;
import dsw.sigconbackend.repository.InventarioRepository;
import lombok.Data;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CompraService {

    private final CompraRepository repo;
    private final InventarioRepository inventarioRepo;

    public CompraService(CompraRepository repo, InventarioRepository inventarioRepo) {
        this.repo = repo;
        this.inventarioRepo = inventarioRepo;
    }

    // Valores por defecto para productos creados automáticamente al registrar
    // una compra sin vincular a un producto de inventario existente.
    private static final String CATEGORIA_POR_DEFECTO = "General";
    private static final Integer STOCK_MINIMO_POR_DEFECTO = 10;

    public List<Compra> listar() { return repo.findAll(); }

    // Resultado enriquecido que el frontend usa para mostrar el modal de
    // confirmación (producto creado vs actualizado, stock antes/después).
    @Data
    @AllArgsConstructor
    public static class RegistrarCompraResultado {
        private Compra compra;
        private Inventario inventario;
        private boolean productoCreado;
        private int stockAnterior;
    }

    @Transactional
    public RegistrarCompraResultado registrar(Compra c) {
        Inventario inv;
        boolean creado = false;
        int stockAnterior;

        if (c.getInventarioId() != null) {
            // Caso 1: el usuario vinculó explícitamente un producto existente.
            inv = inventarioRepo.findById(c.getInventarioId())
                .orElseThrow(() -> new RuntimeException("Producto de inventario no encontrado"));
            stockAnterior = inv.getStock();
        } else {
            // Caso 2: no se vinculó nada. Buscamos primero por nombre exacto
            // (case-insensitive) para no crear duplicados; si no existe, se crea.
            Inventario existente = inventarioRepo.findByProductoIgnoreCase(c.getProducto()).orElse(null);
            if (existente != null) {
                inv = existente;
                stockAnterior = inv.getStock();
            } else {
                inv = crearProductoDesdeCompra(c);
                stockAnterior = 0;
                creado = true;
            }
            // Vinculamos la compra al producto encontrado/creado para que quede
            // registrado el vínculo real, igual que si el usuario lo hubiera elegido.
            c.setInventarioId(inv.getId());
        }

        inv.setStock(inv.getStock() + c.getCantidad());
        inv.setFechaActualizacion(LocalDateTime.now());
        inventarioRepo.save(inv);

        Compra guardada = repo.save(c);

        return new RegistrarCompraResultado(guardada, inv, creado, stockAnterior);
    }

    private Inventario crearProductoDesdeCompra(Compra c) {
        Inventario nuevo = new Inventario();
        nuevo.setProducto(c.getProducto());
        nuevo.setCategoria(CATEGORIA_POR_DEFECTO);
        nuevo.setStock(0); // el stock real se suma después, en registrar()
        nuevo.setStockMinimo(STOCK_MINIMO_POR_DEFECTO);
        nuevo.setPrecioUnitario(c.getPrecioUnitario());
        nuevo.setUnidad("unidad");
        nuevo.setFechaActualizacion(LocalDateTime.now());
        return inventarioRepo.save(nuevo);
    }
}
