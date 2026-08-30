    package dsw.sigconbackend.service;

    import dsw.sigconbackend.dto.ActualizarStockRequest;
    import dsw.sigconbackend.dto.InventarioRequest;
    import dsw.sigconbackend.model.Inventario;
    import dsw.sigconbackend.repository.InventarioRepository;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;

    import java.time.LocalDateTime;
    import java.util.List;
    import java.util.Optional;

    @Service
    public class InventarioService {

        @Autowired
        private InventarioRepository inventarioRepository;

        // ─────────────────────────────────────────
        // HU09 — Visualizar stock disponible
        // ─────────────────────────────────────────

        public List<Inventario> listarInventario() {
            return inventarioRepository.findAllOrdenado();
        }

        public Optional<Inventario> buscarPorId(Long id) {
            return inventarioRepository.findById(id);
        }

        public List<Inventario> listarPorCategoria(String categoria) {
            return inventarioRepository.findByCategoria(categoria);
        }

        // ─────────────────────────────────────────
        // HU10 — Actualizar inventario
        // ─────────────────────────────────────────

        public Inventario crearProducto(InventarioRequest req) {
            validarRequest(req);

            Inventario inv = new Inventario();
            inv.setProducto(req.getProducto());
            inv.setCategoria(req.getCategoria());
            inv.setStock(req.getStock() != null ? req.getStock() : 0);
            inv.setStockMinimo(req.getStockMinimo() != null ? req.getStockMinimo() : 10);
            inv.setPrecioUnitario(req.getPrecioUnitario());
            inv.setUnidad(req.getUnidad() != null ? req.getUnidad() : "unidad");
            inv.setFechaActualizacion(LocalDateTime.now());

            return inventarioRepository.save(inv);
        }

        public Optional<Inventario> actualizarProducto(Long id, InventarioRequest req) {
            validarRequest(req);

            return inventarioRepository.findById(id).map(inv -> {
                inv.setProducto(req.getProducto());
                inv.setCategoria(req.getCategoria());
                inv.setStock(req.getStock());
                inv.setStockMinimo(req.getStockMinimo());
                inv.setPrecioUnitario(req.getPrecioUnitario());
                inv.setUnidad(req.getUnidad());
                inv.setFechaActualizacion(LocalDateTime.now());
                return inventarioRepository.save(inv);
            });
        }

        // PATCH /inventario/:id/stock — entrada o salida de stock
        public Optional<Inventario> actualizarStock(Long id, ActualizarStockRequest req) {
            if (req.getCantidad() == null || req.getCantidad() <= 0)
                throw new IllegalArgumentException("La cantidad debe ser mayor a 0.");
            if (!"ENTRADA".equals(req.getTipo()) && !"SALIDA".equals(req.getTipo()))
                throw new IllegalArgumentException("Tipo debe ser ENTRADA o SALIDA.");

            return inventarioRepository.findById(id).map(inv -> {
                int nuevoStock;
                if ("ENTRADA".equals(req.getTipo())) {
                    nuevoStock = inv.getStock() + req.getCantidad();
                } else {
                    nuevoStock = inv.getStock() - req.getCantidad();
                    if (nuevoStock < 0)
                        throw new IllegalArgumentException("Stock insuficiente. Stock actual: " + inv.getStock());
                }
                inv.setStock(nuevoStock);
                inv.setFechaActualizacion(LocalDateTime.now());
                return inventarioRepository.save(inv);
            });
        }

        // ─────────────────────────────────────────
        // HU11 — Alertar bajo stock
        // ─────────────────────────────────────────

        public List<Inventario> listarBajoStock() {
            return inventarioRepository.findBajoStock();
        }

        public long contarBajoStock() {
            return inventarioRepository.findBajoStock().size();
        }

        // ─────────────────────────────────────────
        // Validaciones comunes
        // ─────────────────────────────────────────
        private void validarRequest(InventarioRequest req) {
            if (req.getProducto() == null || req.getProducto().trim().isEmpty())
                throw new IllegalArgumentException("El nombre del producto es obligatorio.");
            if (req.getStock() != null && req.getStock() < 0)
                throw new IllegalArgumentException("El stock no puede ser negativo.");
            if (req.getStockMinimo() != null && req.getStockMinimo() < 0)
                throw new IllegalArgumentException("El stock mínimo no puede ser negativo.");
        }
            public double calcularValorTotal() {
    return inventarioRepository.findAllOrdenado()
            .stream()
            .mapToDouble(inv -> inv.getStock() * inv.getPrecioUnitario().doubleValue())
            .sum();
}

    }
