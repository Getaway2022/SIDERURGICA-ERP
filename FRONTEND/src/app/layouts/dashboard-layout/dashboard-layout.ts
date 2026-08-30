import { Component, DestroyRef, OnInit, ViewEncapsulation, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../../core/auth/services/auth.service';
import { DataService } from '../../core/services/data.service';
import { AuthUser } from '../../core/auth/models/auth.models';
import { hasPermission, ROLE_PERMISSIONS } from '../../core/auth/models/role-permissions';

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  templateUrl: './dashboard-layout.html',
  styleUrl: './dashboard-layout.scss',
  // Los estilos del dashboard son compartidos por las vistas del router-outlet.
  encapsulation: ViewEncapsulation.None,
})
export class DashboardLayoutComponent implements OnInit {
  private router = inject(Router);
  private authService = inject(AuthService);
  private destroyRef = inject(DestroyRef);
  ds = inject(DataService); // expuesto al template

  activeView = 'dashboard';
  usuarioActual: AuthUser | null = null;
  rolActual: string | null = null;

  // Toast global
  toastMsg = '';
  toastType: 'success' | 'error' | 'info' = 'success';
  toastVisible = false;

  // Notificaciones
  showNotif = false;

  get notificaciones() {
    const base = [
      {
        icono: '⚠️',
        titulo: 'Orden pendiente',
        msg: 'Hay órdenes de compra pendientes de aprobación',
        tiempo: 'Ahora',
        leida: false,
        link: 'abastecimiento',
      },
      {
        icono: '🚚',
        titulo: 'Despachos pendientes',
        msg: `${this.ds.despachosRegistrados()} despachos activos en el sistema`,
        tiempo: 'Ahora',
        leida: false,
        link: 'despacho',
      },
    ];
    // Alertas dinámicas por SKUs críticos — datos reales del inventario
    const criticos = this.ds.skusCriticos();
    criticos.forEach((sku) =>
      base.unshift({
        icono: '📦',
        titulo: 'Stock crítico',
        msg: `${sku.producto} — stock: ${sku.stock} ${sku.unidad}`,
        tiempo: 'Ahora',
        leida: false,
        link: 'inventario',
      }),
    );
    return base;
  }

  readonly permisosPorRol = ROLE_PERMISSIONS;

  ngOnInit() {
    this.usuarioActual = this.authService.getUser();
    this.rolActual = this.authService.getRol();
    this.syncActiveView();
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.syncActiveView());
    // ← carga datos reales del backend al entrar al dashboard
    this.ds.cargarDatos();
  }

  irANotif(n: any) {
    n.leida = true;
    this.showNotif = false;
    if (n.link && this.puedeVer(n.link)) this.activeView = n.link;
  }

  // ── Permisos ──────────────────────────────────────────────────────────────
  puedeVer(view: string): boolean {
    return hasPermission(this.rolActual, view);
  }
  puedeVerAlguno(views: string[]): boolean {
    return views.some((v) => this.puedeVer(v));
  }

  // ── Navegación ────────────────────────────────────────────────────────────
  showView(view: string) {
    if (!this.puedeVer(view)) return;
    void this.router.navigate(view === 'dashboard' ? ['/dashboard'] : ['/dashboard', view]);
  }

  private syncActiveView(): void {
    const segment = this.router.url.split('?')[0].split('/')[2];
    this.activeView = segment || 'dashboard';
  }

  getCurrentTitle(): string {
    const titles: Record<string, string> = {
      dashboard: 'Dashboard',
      ventas: 'Gestión de Ventas',
      cotizaciones: 'Gestión de Cotizaciones',
      pedidos: 'Gestión de Pedidos',
      despacho: 'Gestión de Despacho',
      inventario: 'Gestión de Inventario',
      abastecimiento: 'Gestión de Abastecimiento',
      proveedores: 'Gestión de Proveedores',
      rrhh: 'Gestión de Recursos Humanos',
      planillas: 'Gestión de Planillas',
      reportes: 'Gestión de Reportes',
      mantenimiento: 'Mantenimiento del Sistema',
    };
    return titles[this.activeView] || '';
  }

  getCurrentSub(): string {
    const subs: Record<string, string> = {
      dashboard: 'Resumen ejecutivo del sistema',
      ventas: 'Control de pedidos y transacciones comerciales',
      cotizaciones: 'Registro, cálculo automático y consulta de cotizaciones',
      pedidos: 'Registro, validación y comprobantes de pedidos de venta',
      despacho: 'Seguimiento de envíos y entregas',
      inventario: 'Control de stock y almacén',
      abastecimiento: 'Órdenes de compra y proveedores',
      proveedores: 'Directorio de proveedores estratégicos',
      rrhh: 'Control de asistencia y personal',
      planillas: 'Gestión salarial',
      reportes: 'Indicadores clave del negocio',
      mantenimiento: 'Administración de usuarios, roles y configuración',
    };
    return subs[this.activeView] || '';
  }

  logout() {
    this.authService.logout();
  }

  // ── Notificaciones ────────────────────────────────────────────────────────
  get notifNoLeidas(): number {
    return this.notificaciones.filter((n) => !n.leida).length;
  }
  toggleNotif() {
    this.showNotif = !this.showNotif;
  }
  marcarTodasLeidas() {
    this.notificaciones.forEach((n) => (n.leida = true));
  }
  cerrarNotif() {
    this.showNotif = false;
  }

  showToast(msg: string, type: 'success' | 'error' | 'info' = 'success') {
    this.toastMsg = msg;
    this.toastType = type;
    this.toastVisible = true;
    setTimeout(() => (this.toastVisible = false), 3000);
  }
}
