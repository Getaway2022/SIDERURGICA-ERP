export type AppRole = 'ADMIN' | 'VENTAS' | 'ALMACEN' | 'RRHH' | 'CONSULTA';

export const ROLE_PERMISSIONS: Record<AppRole, readonly string[]> = {
  ADMIN: [
    'dashboard',
    'ventas',
    'cotizaciones',
    'pedidos',
    'despacho',
    'inventario',
    'abastecimiento',
    'proveedores',
    'rrhh',
    'planillas',
    'reportes',
    'mantenimiento',
  ],
  VENTAS: ['dashboard', 'ventas', 'cotizaciones', 'pedidos', 'reportes'],
  ALMACEN: ['dashboard', 'despacho', 'inventario', 'abastecimiento', 'proveedores'],
  RRHH: ['dashboard', 'rrhh', 'planillas'],
  CONSULTA: ['dashboard', 'reportes'],
};

export function hasPermission(role: string | null, permission: string): boolean {
  return !!role && ROLE_PERMISSIONS[role as AppRole]?.includes(permission);
}
