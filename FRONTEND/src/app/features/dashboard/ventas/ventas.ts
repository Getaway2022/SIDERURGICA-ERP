// src/app/features/dashboard/ventas/ventas.ts
import { Component, OnInit, ChangeDetectorRef, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { VentaService, Venta } from './services/ventas.service';
import { AuthService } from '../../../core/auth/services/auth.service';

// Venta ya NO se define aquí — viene de venta.service.ts ↑

@Component({
  selector: 'app-ventas',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './ventas.html',
  styles: [],
  encapsulation: ViewEncapsulation.None,
})
export class VentasComponent implements OnInit {
  ventas: Venta[] = [];
  ventaSearch = '';
  ventaFiltroEstado = '';
  loadingVentas = false;
  loadingAction = false;

  showModal = false;
  modalMode: 'ver' | 'eliminar' = 'ver';
  ventaForm: Partial<Venta> = {};
  deleteTarget: Venta | null = null;

  toastMsg = '';
  toastType: 'success' | 'error' | 'info' = 'success';
  toastVisible = false;

  usuarioActual: any = null;
  rolActual: string | null = null;

  constructor(
    private ventaService: VentaService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.usuarioActual = this.authService.getUser();
    this.rolActual = this.authService.getRol();
    this.cargarVentas();
  }

  // ── Cargar ventas ─────────────────────────────────────────────────────────
  cargarVentas() {
    this.loadingVentas = true;
    this.ventaService.listarVentas().subscribe({
      next: (data) => {
        this.ventas = data;
        this.loadingVentas = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingVentas = false;
        this.showToast('Error al cargar ventas', 'error');
      },
    });
  }

  // ── Cambiar estado ────────────────────────────────────────────────────────
  cambiarEstado(v: Venta, estado: string) {
    if (!v.id) return;
    this.ventaService.cambiarEstado(v.id, estado).subscribe({
      next: (actualizada) => {
        v.estado = actualizada.estado;
        this.showToast(`Venta ${v.codigo} → ${estado}`);
        this.cargarVentas();
      },
      error: (err) => this.showToast(err.error?.error || 'Error al cambiar estado', 'error'),
    });
  }

  // ── Aprobar / Anular ──────────────────────────────────────────────────────
  aprobarVenta(v: Venta) {
    if (!confirm(`¿Aprobar la venta ${v.codigo}?`)) return;
    this.cambiarEstado(v, 'APROBADO');
  }

  anularVenta(v: Venta) {
    if (!confirm(`¿Anular la venta ${v.codigo}? Esta acción no se puede deshacer.`)) return;
    this.cambiarEstado(v, 'ANULADO');
  }

  // ── Generar Despacho ──────────────────────────────────────────────────────
  generarDespacho(v: Venta) {
    if (!v.id || !confirm(`¿Generar despacho para la venta ${v.codigo}?`)) return;
    this.loadingAction = true;
    this.ventaService.generarDespacho(v.id).subscribe({
      next: (despacho) => {
        this.showToast(`✅ Despacho ${despacho.codigo} generado correctamente`, 'success');
        this.cargarVentas();
        this.loadingAction = false;
      },
      error: (err) => {
        this.showToast(err.error?.error || 'Error al generar el despacho', 'error');
        this.loadingAction = false;
        this.cdr.detectChanges();
      },
    });
  }

  // ── Generar Venta desde Pedido ────────────────────────────────────────────
  generarVentaDesdePedido(pedidoId: number, pedidoCodigo: string) {
    if (!confirm(`¿Generar venta para el pedido ${pedidoCodigo}?`)) return;
    this.loadingAction = true;
    this.ventaService.generarDesdePedido(pedidoId).subscribe({
      next: (venta) => {
        this.showToast(
          `✅ Venta ${venta.codigo} generada. Total: S/ ${Number(venta.total).toFixed(2)}`,
          'success',
        );
        this.cargarVentas();
        this.loadingAction = false;
      },
      error: (err) => {
        this.showToast(err.error?.error || 'Error al generar la venta', 'error');
        this.loadingAction = false;
        this.cdr.detectChanges();
      },
    });
  }

  // ── Modal ─────────────────────────────────────────────────────────────────
  openModalVer(v: Venta) {
    this.modalMode = 'ver';
    this.ventaForm = { ...v };
    this.showModal = true;
  }

  pedirEliminar(v: Venta) {
    this.modalMode = 'eliminar';
    this.deleteTarget = v;
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.deleteTarget = null;
  }

  onAprobarVentaModal() {
    this.aprobarVenta(this.ventaForm as Venta);
    this.closeModal();
  }
  onAnularVentaModal() {
    this.anularVenta(this.ventaForm as Venta);
    this.closeModal();
  }
  onGenerarDespachoModal() {
    this.generarDespacho(this.ventaForm as Venta);
    this.closeModal();
  }

  // ── Filtros ───────────────────────────────────────────────────────────────
  get ventasFiltradas(): Venta[] {
    return this.ventas.filter((v) => {
      const s = this.ventaSearch.toLowerCase();
      const matchSearch =
        !this.ventaSearch ||
        v.codigo?.toLowerCase().includes(s) ||
        v.cliente?.toLowerCase().includes(s) ||
        v.producto?.toLowerCase().includes(s) ||
        v.ruc?.toLowerCase().includes(s);
      const matchEstado = !this.ventaFiltroEstado || v.estado === this.ventaFiltroEstado;
      return matchSearch && matchEstado;
    });
  }

  // ── Estadísticas ──────────────────────────────────────────────────────────
  getVentasTotales(): number {
    return this.ventas.reduce((s, v) => s + Number(v.total || 0), 0);
  }
  getVentasPendientes(): number {
    return this.ventas.filter((v) => v.estado === 'PENDIENTE').length;
  }
  getVentasAprobadas(): number {
    return this.ventas.filter((v) => v.estado === 'APROBADO').length;
  }
  getVentasCompletadas(): number {
    return this.ventas.filter((v) => v.estado === 'COMPLETADO').length;
  }

  // ── Utilidades ────────────────────────────────────────────────────────────
  formatMoney(n: number): string {
    return 'S/ ' + Number(n).toLocaleString('es-PE', { minimumFractionDigits: 2 });
  }

  formatDate(d?: string): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('es-PE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }

  getBadgeClass(estado: string): string {
    const map: Record<string, string> = {
      PENDIENTE: 'badge-orange',
      APROBADO: 'badge-green',
      COMPLETADO: 'badge-blue',
      ANULADO: 'badge-red',
    };
    return map[estado] || 'badge-yellow';
  }

  // ── Toast ─────────────────────────────────────────────────────────────────
  showToast(msg: string, type: 'success' | 'error' | 'info' = 'success') {
    this.toastMsg = msg;
    this.toastType = type;
    this.toastVisible = true;
    setTimeout(() => {
      this.toastVisible = false;
      this.cdr.detectChanges();
    }, 4000);
  }
}
