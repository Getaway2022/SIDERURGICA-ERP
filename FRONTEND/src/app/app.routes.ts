import { Routes } from '@angular/router';
import { authGuard } from './core/auth/guards/auth.guard';
import { roleGuard } from './core/auth/guards/role.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home').then((m) => m.HomeComponent),
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/pages/login/login').then((m) => m.LoginComponent),
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./layouts/dashboard-layout/dashboard-layout').then((m) => m.DashboardLayoutComponent),
    canActivate: [authGuard],
    canActivateChild: [roleGuard],
    children: [
      {
        path: 'ventas',
        data: { permission: 'ventas' },
        loadComponent: () =>
          import('./features/dashboard/ventas/ventas').then((m) => m.VentasComponent),
      },
      {
        path: 'cotizaciones',
        data: { permission: 'cotizaciones' },
        loadComponent: () =>
          import('./features/dashboard/cotizaciones/cotizaciones').then(
            (m) => m.CotizacionesComponent,
          ),
      },
      {
        path: 'pedidos',
        data: { permission: 'pedidos' },
        loadComponent: () =>
          import('./features/dashboard/pedidos/pedidos').then((m) => m.PedidosComponent),
      },
      {
        path: 'despacho',
        data: { permission: 'despacho' },
        loadComponent: () =>
          import('./features/dashboard/despacho/despacho').then((m) => m.DespachoComponent),
      },
      {
        path: 'inventario',
        data: { permission: 'inventario' },
        loadComponent: () =>
          import('./features/dashboard/inventario/inventario').then((m) => m.InventarioComponent),
      },
      {
        path: 'abastecimiento',
        data: { permission: 'abastecimiento' },
        loadComponent: () =>
          import('./features/dashboard/abastecimiento/abastecimiento').then(
            (m) => m.AbastecimientoComponent,
          ),
      },
      {
        path: 'proveedores',
        data: { permission: 'proveedores' },
        loadComponent: () =>
          import('./features/dashboard/proveedores/proveedores').then(
            (m) => m.ProveedoresComponent,
          ),
      },
      {
        path: 'rrhh',
        data: { permission: 'rrhh' },
        loadComponent: () => import('./features/dashboard/rrhh/rrhh').then((m) => m.RrhhComponent),
      },
      {
        path: 'planillas',
        data: { permission: 'planillas' },
        loadComponent: () =>
          import('./features/dashboard/planillas/planillas').then((m) => m.PlanillasComponent),
      },
      {
        path: 'reportes',
        data: { permission: 'reportes' },
        loadComponent: () =>
          import('./features/dashboard/reportes/reportes').then((m) => m.ReportesComponent),
      },
      {
        path: 'mantenimiento',
        data: { permission: 'mantenimiento' },
        loadComponent: () =>
          import('./features/dashboard/mantenimiento/mantenimiento').then(
            (m) => m.MantenimientoComponent,
          ),
      },
      { path: '**', redirectTo: '' },
    ],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
