import { Component, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DataService } from '../../../core/services/data.service';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reportes.html',
  styles: [],
})
export class ReportesComponent {
  private ds = inject(DataService);

  // ── Período seleccionado (UI) ──
  periodos = ['Julio 2026', 'Junio 2026', 'Mayo 2026', 'Abril 2026'];
  periodoSeleccionado = 'Julio 2026';

  // ── Métricas principales ──
  ventasTotales = this.ds.ventasTotales;
  utilidadBruta = this.ds.utilidadBruta;
  skusEnInventario = this.ds.skusEnInventario;
  personalActivo = this.ds.personalActivo;

  // ── Comparativas (período anterior simulado) ──
  ventasAnterior = computed(() => Math.round(this.ds.ventasTotales() * 0.878)); // -12.4%
  utilidadAnterior = computed(() => Math.round(this.ds.utilidadBruta() * 0.95)); // -5.2%
  skusAnterior = computed(() => this.ds.skusEnInventario() - 2);
  personalAnterior = computed(() => this.ds.personalActivo() + 1);

  pctVentas = computed(() => this.calcPct(this.ds.ventasTotales(), this.ventasAnterior()));
  pctUtilidad = computed(() => this.calcPct(this.ds.utilidadBruta(), this.utilidadAnterior()));
  pctSkus = computed(() => this.calcPct(this.ds.skusEnInventario(), this.skusAnterior()));
  pctPersonal = computed(() => this.calcPct(this.ds.personalActivo(), this.personalAnterior()));

  private calcPct(actual: number, anterior: number): number {
    if (anterior === 0) return 0;
    return Math.round(((actual - anterior) / anterior) * 1000) / 10;
  }

  // ── Datos de charts y tablas ──
  topProductos = this.ds.topProductos;
  ventasPorMes = this.ds.ventasPorMes;
  distribucionInventario = this.ds.distribucionInventario;

  // ── Resumen operativo ──
  pedidosPendientes = this.ds.pedidosPendientes;
  skusCriticos = this.ds.skusCriticos;
  despachosRegistrados = this.ds.despachosRegistrados;
  masaSalarial = this.ds.masaSalarial;

  cotizaciones = this.ds.cotizaciones;
  cotizAprobadas = computed(
    () => this.ds.cotizaciones().filter((c) => c.estado === 'Aprobada').length,
  );
  cotizTasa = computed(() => {
    const total = this.ds.cotizaciones().length;
    return total > 0 ? Math.round((this.cotizAprobadas() / total) * 100) : 0;
  });

  // ── Donut SVG helper ──
  // Radio 50, circunferencia ~314.16
  readonly CIRC = 314.16;

  donutSegments = computed(() => {
    let offset = 0;
    return this.distribucionInventario().map((d) => {
      const dash = Math.round((d.pct / 100) * this.CIRC);
      const seg = { ...d, dash, offset };
      offset += dash;
      return seg;
    });
  });

  // ── Exportar (placeholder) ──
  exportar() {
    alert('Función de exportación — integrar con librería PDF/Excel según requerimiento.');
  }
}
