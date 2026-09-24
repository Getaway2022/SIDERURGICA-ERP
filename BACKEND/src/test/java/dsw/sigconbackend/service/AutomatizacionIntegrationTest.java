package dsw.sigconbackend.service;

import dsw.sigconbackend.model.*;
import dsw.sigconbackend.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

/** Exclusivamente una base local desechable llamada automation_test; nunca usa DB_URL. */
@EnabledIfEnvironmentVariable(named = "AUTOMATION_TEST_DB_URL", matches = ".+")
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=classpath:automation-schema.sql",
    "spring.datasource.username=automation_test", "spring.datasource.password=",
    "jwt.secret=automation-test-secret-with-at-least-32-characters",
    "logging.level.root=WARN", "logging.level.org.springframework=WARN", "logging.level.org.hibernate=WARN",
    "automatizacion.reposicion.intervalo-ms=3600000"
})
class AutomatizacionIntegrationTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = System.getenv("AUTOMATION_TEST_DB_URL");
        if (url == null || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/automation_test"))
            throw new IllegalArgumentException("Se requiere una base local desechable automation_test");
        registry.add("spring.datasource.url", () -> url);
    }
    @Autowired FlujoComercialService flujo;
    @Autowired PedidoService pedidoService;
    @Autowired CompraService compraService;
    @Autowired ReposicionService reposicion;
    @Autowired OrdenCompraService ordenService;
    @Autowired InventarioRepository inventarios;
    @Autowired PedidoRepository pedidos;
    @Autowired CotizacionRepository cotizaciones;
    @Autowired VentaRepository ventas;
    @Autowired DespachoRepository despachos;
    @Autowired SolicitudReposicionRepository solicitudes;
    @Autowired PresupuestoRepository presupuestos;
    @Autowired ProveedorRepository proveedores;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;

    @BeforeEach void limpiar() {
        jdbc.execute("TRUNCATE compras.solicitud_reposicion, compras.compra, compras.orden_compra, compras.presupuesto, compras.proveedor, comercial.despacho, comercial.venta, comercial.pedido, comercial.cotizacion, almacen.inventario RESTART IDENTITY CASCADE");
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS reposicion_activa_unica ON compras.solicitud_reposicion(inventario_id) WHERE estado IN ('PENDIENTE', 'APROBADA')");
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS venta_pedido_unico ON comercial.venta(pedido_id)");
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS despacho_venta_unica ON comercial.despacho(venta_id)");
    }
    private Inventario producto(int stock) {
        Inventario i = new Inventario(); i.setProducto("Acero"); i.setStock(stock);
        i.setStockMinimo(10); i.setStockObjetivo(20); i.setPrecioUnitario(BigDecimal.TEN);
        return inventarios.save(i);
    }
    private Pedido pedido(int cantidad) {
        Pedido p = new Pedido(); p.setCliente("Cliente"); p.setProducto("Acero");
        p.setCantidad(cantidad); p.setPrecioUnitario(BigDecimal.TEN);
        return pedidoService.registrarPedido(p);
    }
    @Test void cotizacionAprobadaCreaUnPedidoYConservaDescuento() {
        Cotizacion c = new Cotizacion(); c.setEstado("PENDIENTE"); c.setCantidad(10);
        c.setProducto("Acero"); c.setCliente("Cliente"); c.setPrecioUnitario(BigDecimal.TEN);
        c.setTotal(new BigDecimal("95.00")); c = cotizaciones.save(c);
        flujo.cambiarCotizacion(c.getId(), "APROBADA");
        flujo.cambiarCotizacion(c.getId(), "APROBADA");
        Pedido p = flujo.convertirCotizacion(c.getId());
        assertThat(pedidos.count()).isEqualTo(1);
        assertThat(p.getEstado()).isEqualTo("PENDIENTE");
        assertThat(p.getSubtotal()).isEqualByComparingTo("95.00");
        assertThat(p.getTotal()).isEqualByComparingTo("112.10");
    }
    @Test void aprobarReservarEnviarYEntregarUnaSolaVez() {
        Inventario i = producto(20); Pedido p = pedido(12);
        flujo.cambiarPedido(p.getId(), "APROBADO"); flujo.cambiarPedido(p.getId(), "APROBADO");
        Inventario reservado = inventarios.findById(i.getId()).orElseThrow();
        assertThat(reservado.getStock()).isEqualTo(20);
        assertThat(reservado.getStockReservado()).isEqualTo(12);
        assertThat(reservado.getStockDisponible()).isEqualTo(8);
        assertThat(ventas.count()).isEqualTo(1);
        assertThat(solicitudes.findAll()).singleElement().satisfies(s -> assertThat(s.getCantidad()).isEqualTo(12));
        Venta v = ventas.findAll().get(0);
        flujo.cambiarVenta(v.getId(), "APROBADO"); flujo.cambiarVenta(v.getId(), "APROBADO");
        assertThat(despachos.count()).isEqualTo(1);
        Despacho d = despachos.findAll().get(0);
        assertThatThrownBy(() -> flujo.cambiarDespacho(d.getId(), "ENTREGADO", null)).isInstanceOf(IllegalArgumentException.class);
        flujo.cambiarDespacho(d.getId(), "PREPARADO", null);
        flujo.cambiarDespacho(d.getId(), "ENVIADO", "GUIA-1");
        flujo.cambiarDespacho(d.getId(), "ENVIADO", "GUIA-1");
        Inventario enviado = inventarios.findById(i.getId()).orElseThrow();
        assertThat(enviado.getStock()).isEqualTo(8);
        assertThat(enviado.getStockReservado()).isZero();
        assertThatThrownBy(() -> flujo.cambiarPedido(p.getId(), "ANULADO")).isInstanceOf(IllegalArgumentException.class);
        flujo.cambiarDespacho(d.getId(), "ENTREGADO", null);
        assertThat(ventas.findById(v.getId()).orElseThrow().getEstado()).isEqualTo("COMPLETADO");
        assertThat(pedidos.findById(p.getId()).orElseThrow().getEstado()).isEqualTo("ENTREGADO");
    }
    @Test void cancelarLiberaReservaYResuelveAvisoSinCambiarStockFisico() {
        Inventario i = producto(20); Pedido p = pedido(12);
        flujo.cambiarPedido(p.getId(), "APROBADO");
        Venta v = ventas.findAll().get(0); flujo.cambiarVenta(v.getId(), "APROBADO");
        flujo.cambiarVenta(v.getId(), "ANULADO"); flujo.cambiarPedido(p.getId(), "ANULADO");
        Inventario actual = inventarios.findById(i.getId()).orElseThrow();
        assertThat(actual.getStock()).isEqualTo(20); assertThat(actual.getStockReservado()).isZero();
        assertThat(despachos.findAll()).allMatch(d -> d.getEstado().equals("ANULADO"));
        assertThat(solicitudes.findAll()).allMatch(s -> s.getEstado().equals("RESUELTA"));
    }
    @Test void stockInsuficienteNoCreaVentaNiReserva() {
        producto(5); Pedido p = pedido(6);
        assertThatThrownBy(() -> flujo.cambiarPedido(p.getId(), "APROBADO")).hasMessageContaining("insuficiente");
        assertThat(ventas.count()).isZero(); assertThat(inventarios.findAll().get(0).getStockReservado()).isZero();
        assertThat(pedidos.findById(p.getId()).orElseThrow().getEstado()).isEqualTo("PENDIENTE");
    }
    @Test void productoInexistenteNoSeAprueba() {
        Pedido p = pedido(2);
        assertThatThrownBy(() -> flujo.cambiarPedido(p.getId(), "APROBADO")).hasMessageContaining("no encontrado");
        assertThat(ventas.count()).isZero();
    }
    @Test void dosAprobacionesConcurrentesNoSobrevenden() throws Exception {
        Inventario i = producto(10); Pedido a = pedido(7), b = pedido(7);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            Callable<Boolean> primera = () -> { inicio.await(); try { flujo.cambiarPedido(a.getId(), "APROBADO"); return true; } catch (IllegalArgumentException e) { return false; } };
            Callable<Boolean> segunda = () -> { inicio.await(); try { flujo.cambiarPedido(b.getId(), "APROBADO"); return true; } catch (IllegalArgumentException e) { return false; } };
            Future<Boolean> f1 = executor.submit(primera), f2 = executor.submit(segunda); inicio.countDown();
            assertThat(f1.get(10, TimeUnit.SECONDS) ^ f2.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(inventarios.findById(i.getId()).orElseThrow().getStockReservado()).isEqualTo(7);
            assertThat(ventas.count()).isEqualTo(1);
        } finally { executor.shutdownNow(); }
    }
    private Proveedor prepararPresupuesto() {
        Presupuesto p = new Presupuesto(); p.setPeriodo(LocalDate.now().toString().substring(0, 7));
        p.setMontoTotal(new BigDecimal("1000")); p.setMontoDisponible(new BigDecimal("1000")); presupuestos.save(p);
        Proveedor proveedor = new Proveedor(); proveedor.setNombre("Proveedor"); proveedor.setRuc("12345678901");
        return proveedores.save(proveedor);
    }
    @Test void reposicionAprobadaYRecibidaNoDuplicaCompraNiStock() {
        Inventario inv = producto(8); Proveedor prov = prepararPresupuesto();
        reposicion.revisarInventario(); reposicion.revisarInventario();
        SolicitudReposicion s = solicitudes.findAll().get(0);
        s = reposicion.aprobar(s.getId(), prov.getId(), BigDecimal.TEN, 12);
        reposicion.aprobar(s.getId(), prov.getId(), BigDecimal.TEN, 12);
        assertThat(presupuestos.findAll().get(0).getMontoDisponible()).isEqualByComparingTo("880");
        Compra c = new Compra(); c.setOrdenCompraId(s.getOrdenCompraId()); c.setProducto("Acero");
        c.setProveedorId(prov.getId()); c.setCantidad(12); c.setPrecioUnitario(BigDecimal.TEN);
        compraService.registrar(c); compraService.registrar(c);
        assertThat(inventarios.findById(inv.getId()).orElseThrow().getStock()).isEqualTo(20);
        assertThat(solicitudes.findById(s.getId()).orElseThrow().getEstado()).isEqualTo("RECIBIDA");
        assertThatThrownBy(() -> ordenService.cambiarEstado(c.getOrdenCompraId(), "Cancelada")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void cancelarOrdenDosVecesLiberaPresupuestoUnaVez() {
        producto(8); Proveedor prov = prepararPresupuesto(); reposicion.revisarInventario();
        SolicitudReposicion s = reposicion.aprobar(solicitudes.findAll().get(0).getId(), prov.getId(), BigDecimal.TEN, 12);
        ordenService.cambiarEstado(s.getOrdenCompraId(), "Cancelada"); ordenService.cambiarEstado(s.getOrdenCompraId(), "Cancelada");
        assertThat(presupuestos.findAll().get(0).getMontoDisponible()).isEqualByComparingTo("1000");
        reposicion.revisarInventario();
        assertThat(solicitudes.findAll().stream().filter(x -> x.getEstado().equals("PENDIENTE")).count()).isEqualTo(1);
    }
    @Test void falloAlCrearVentaRevierteReservaYAviso() {
        Inventario inv = producto(20); Pedido p = pedido(12);
        p.setCliente(null); pedidos.save(p); // La venta exige cliente: falla después de reservar.
        assertThatThrownBy(() -> flujo.cambiarPedido(p.getId(), "APROBADO")).isInstanceOf(RuntimeException.class);
        assertThat(inventarios.findById(inv.getId()).orElseThrow().getStockReservado()).isZero();
        assertThat(solicitudes.count()).isZero(); assertThat(ventas.count()).isZero();
    }
    @Test void pedidoHistoricoNoDescuentaSinConciliar() {
        producto(20); Pedido p = pedido(12); p.setEstado("FACTURADO"); p.setEstadoStock("REVISION_LEGADO"); pedidos.save(p);
        assertThatThrownBy(() -> flujo.cambiarPedido(p.getId(), "ANULADO")).hasMessageContaining("concilie");
        assertThat(inventarios.findAll().get(0).getStock()).isEqualTo(20);
    }

    @Test void falloDePresupuestoNoApruebaReposicionNiCreaOrden() {
        producto(8); Proveedor prov = prepararPresupuesto(); reposicion.revisarInventario();
        Long id = solicitudes.findAll().get(0).getId();
        assertThatThrownBy(() -> reposicion.aprobar(id, prov.getId(), new BigDecimal("1000"), 12)).hasMessageContaining("insuficiente");
        assertThat(solicitudes.findById(id).orElseThrow().getEstado()).isEqualTo("PENDIENTE");
        assertThat(ordenService.listar()).isEmpty();
        assertThat(presupuestos.findAll().get(0).getMontoDisponible()).isEqualByComparingTo("1000");
    }

    @Test void cambioDeObjetivoActualizaSolicitudPendienteSinDuplicarla() {
        Inventario i = producto(8); reposicion.revisarInventario();
        i.setStockObjetivo(30); inventarios.save(i); reposicion.revisarInventario();
        assertThat(solicitudes.findAll()).singleElement().satisfies(s -> assertThat(s.getCantidad()).isEqualTo(22));
    }

    @Test void cantidadRevisadaDebeCoincidirAntesDeComprometerPresupuesto() {
        Inventario i = producto(8); Proveedor prov = prepararPresupuesto(); reposicion.revisarInventario();
        Long id = solicitudes.findAll().get(0).getId();
        i.setStockObjetivo(30); inventarios.save(i); reposicion.revisarInventario();
        assertThatThrownBy(() -> reposicion.aprobar(id, prov.getId(), BigDecimal.TEN, 12)).hasMessageContaining("cantidad a reponer cambió");
        assertThat(ordenService.listar()).isEmpty();
        assertThat(presupuestos.findAll().get(0).getMontoDisponible()).isEqualByComparingTo("1000");
    }

    @Test void aprobarVentaDirectaReservaYGeneraDespacho() {
        Inventario i = producto(20);
        Venta v = new Venta(); v.setCodigo("DIRECTA-1"); v.setCliente("Cliente"); v.setProducto("Acero");
        v.setCantidad(5); v.setPrecioUnitario(BigDecimal.TEN); v.setSubtotal(new BigDecimal("50"));
        v.setIgv(new BigDecimal("9")); v.setTotal(new BigDecimal("59")); v = ventas.save(v);
        flujo.cambiarVenta(v.getId(), "APROBADO");
        assertThat(inventarios.findById(i.getId()).orElseThrow().getStockReservado()).isEqualTo(5);
        assertThat(despachos.findAll()).singleElement().satisfies(d -> assertThat(d.getPedidoId()).isNotNull());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "VENTAS")
    void ventasNoPuedeAprobarComprasNiAlterarStockFisico() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/reposiciones/1/aprobar")
            .contentType("application/json").content("{\"proveedorId\":1,\"precioUnitario\":10}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/inventario/1/stock")
            .contentType("application/json").content("{\"cantidad\":1,\"tipo\":\"ENTRADA\"}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "ALMACEN")
    void almacenPuedeConsultarReposicionesYDespachos() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/reposiciones"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/despacho"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }
}
