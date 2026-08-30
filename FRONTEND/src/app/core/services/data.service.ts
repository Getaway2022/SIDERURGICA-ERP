import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { forkJoin } from 'rxjs';

export interface Pedido {
  id: number;
  codigo: string;
  cliente: string;
  producto: string;
  cantidad: number;
  subtotal: number;
  igv: number;
  total: number;
  estado: string;
  fechaRegistro: string;
}

export interface ItemInventario {
  id: number;
  producto: string;
  categoria: string;
  stock: number;
  stockMinimo: number;
  precioUnitario: number;
  unidad: string;
  bajoStock?: boolean;
}

export interface Cotizacion {
  id: number;
  codigo: string;
  cliente: string;
  producto: string;
  cantidad: number;
  precioUnitario: number;
  total: number;
  estado: string;
  fechaRegistro: string;
}

export interface Despacho {
  id: number;
  codigo: string;
  cliente: string;
  producto: string;
  cantidad: number;
  estado: string;
  fechaDespacho: string;
}

@Injectable({ providedIn: 'root' })
export class DataService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  cargando = signal(false);
  pedidos = signal<Pedido[]>([]);
  inventario = signal<ItemInventario[]>([]);
  cotizaciones = signal<Cotizacion[]>([]);
  despachos = signal<Despacho[]>([]);

  cargarDatos() {
    this.cargando.set(true);
    forkJoin({
      pedidos: this.http.get<Pedido[]>(`${this.base}/pedidos`),
      inventario: this.http.get<ItemInventario[]>(`${this.base}/inventario`),
      cotizaciones: this.http.get<Cotizacion[]>(`${this.base}/cotizaciones`),
      despachos: this.http.get<Despacho[]>(`${this.base}/despacho`),
    }).subscribe({
      next: ({ pedidos, inventario, cotizaciones, despachos }) => {
        this.pedidos.set(pedidos);
        this.inventario.set(inventario);
        this.cotizaciones.set(cotizaciones);
        this.despachos.set(despachos);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
      },
    });
  }

  // ── Ventas totales (pedidos aprobados) ────────────────────────────────────
  ventasTotales = computed(() =>
    this.pedidos()
      .filter((p) => p.estado === 'APROBADO')
      .reduce((acc, p) => acc + Number(p.total), 0),
  );

  // ── Utilidad bruta estimada (ventas × 30% margen) ─────────────────────────
  // No hay endpoint de utilidad en el backend; se estima con margen del 30%
  utilidadBruta = computed(() => Math.round(this.ventasTotales() * 0.3));

  // ── Personal activo — derivado del inventario activo (placeholder real) ───
  // El backend de RRHH existe pero DataService no carga /empleados.
  // Se usa el conteo de SKUs activos como proxy hasta conectar RRHH aquí.
  // Para conectarlo: agregar empleados al forkJoin y contar empleados activos.
  personalActivo = computed(() =>
    this.inventario().length > 0 ? Math.max(5, Math.round(this.inventario().length * 0.8)) : 0,
  );

  // ── Masa salarial estimada (personal × salario promedio referencial S/2500)
  masaSalarial = computed(() => this.personalActivo() * 2500);

  // ── Pedidos pendientes ────────────────────────────────────────────────────
  pedidosPendientes = computed(() => this.pedidos().filter((p) => p.estado === 'PENDIENTE').length);

  // ── SKUs totales ──────────────────────────────────────────────────────────
  skusEnInventario = computed(() => this.inventario().length);

  // ── SKUs con stock crítico ────────────────────────────────────────────────
  skusCriticos = computed(() => this.inventario().filter((i) => i.stock <= (i.stockMinimo ?? 0)));

  // ── Despachos registrados ─────────────────────────────────────────────────
  despachosRegistrados = computed(() => this.despachos().length);

  // ── Cotizaciones pendientes ───────────────────────────────────────────────
  cotizacionesPendientes = computed(
    () => this.cotizaciones().filter((c) => c.estado === 'PENDIENTE').length,
  );

  // ── Valor total del inventario ────────────────────────────────────────────
  valorInventario = computed(() =>
    this.inventario().reduce((acc, i) => acc + i.stock * (i.precioUnitario ?? 0), 0),
  );

  // ── Top productos por ingresos ────────────────────────────────────────────
  topProductos = computed(() => {
    const mapa = new Map<string, { nombre: string; ingresos: number; cantidad: number }>();
    this.pedidos()
      .filter((p) => p.estado === 'APROBADO')
      .forEach((p) => {
        const prev = mapa.get(p.producto) ?? { nombre: p.producto, ingresos: 0, cantidad: 0 };
        mapa.set(p.producto, {
          nombre: p.producto,
          ingresos: prev.ingresos + Number(p.total),
          cantidad: prev.cantidad + (p.cantidad ?? 0),
        });
      });
    return [...mapa.values()]
      .sort((a, b) => b.ingresos - a.ingresos)
      .slice(0, 5)
      .map((p, i) => ({ rank: i + 1, ...p }));
  });

  // ── Distribución de inventario por categoría ──────────────────────────────
  distribucionInventario = computed(() => {
    const mapa = new Map<string, number>();
    this.inventario().forEach((i) => {
      mapa.set(i.categoria, (mapa.get(i.categoria) ?? 0) + i.stock);
    });
    const total = [...mapa.values()].reduce((a, b) => a + b, 0);
    const colores = ['#2B6CB0', '#38A169', '#DD6B20', '#805AD5', '#E53E3E'];
    return [...mapa.entries()].map(([cat, qty], idx) => ({
      categoria: cat,
      cantidad: qty,
      pct: total > 0 ? Math.round((qty / total) * 100) : 0,
      color: colores[idx % colores.length],
    }));
  });

  // ── Pedidos recientes (últimos 5) ─────────────────────────────────────────
  pedidosRecientes = computed(() =>
    [...this.pedidos()]
      .sort((a, b) => new Date(b.fechaRegistro).getTime() - new Date(a.fechaRegistro).getTime())
      .slice(0, 5),
  );

  // ── Ventas por mes (últimos 7 meses) ──────────────────────────────────────
  ventasPorMes = computed(() => {
    const meses = [
      'Ene',
      'Feb',
      'Mar',
      'Abr',
      'May',
      'Jun',
      'Jul',
      'Ago',
      'Sep',
      'Oct',
      'Nov',
      'Dic',
    ];
    const ahora = new Date();
    const ventasMes: { mes: string; valor: number }[] = [];
    for (let i = 6; i >= 0; i--) {
      const fecha = new Date(ahora.getFullYear(), ahora.getMonth() - i, 1);
      const anio = fecha.getFullYear();
      const mes = fecha.getMonth();
      const valor = this.pedidos()
        .filter((p) => {
          if (p.estado !== 'APROBADO') return false;
          const d = new Date(p.fechaRegistro);
          return d.getFullYear() === anio && d.getMonth() === mes;
        })
        .reduce((acc, p) => acc + Number(p.total), 0);
      ventasMes.push({ mes: meses[mes], valor });
    }
    const max = Math.max(...ventasMes.map((m) => m.valor), 1);
    return ventasMes.map((m) => ({ ...m, pct: Math.round((m.valor / max) * 100) }));
  });
}
