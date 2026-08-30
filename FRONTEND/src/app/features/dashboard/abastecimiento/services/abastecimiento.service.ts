import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';

// ── Interfaces del dominio de Abastecimiento ─────────────────────────────────

export interface OrdenCompra {
  id?: number;
  codigo?: string;
  proveedorId: number;
  producto: string;
  cantidad?: string;
  total: number;
  fechaEntrega?: string;
  estado?: 'Aprobada' | 'Pendiente' | 'En tránsito' | 'Rechazada';
  _open?: boolean;
}

export interface Compra {
  id?: number;
  ordenCompraId?: number;
  proveedorId: number;
  inventarioId?: number;
  producto: string;
  cantidad: number;
  precioUnitario: number;
  total: number;
}

export interface InventarioResumen {
  id: number;
  producto: string;
  categoria: string;
  stock: number;
  stockMinimo: number;
  precioUnitario: number;
  unidad: string;
}

export interface RegistrarCompraResultado {
  compra: Compra;
  inventario: InventarioResumen;
  productoCreado: boolean;
  stockAnterior: number;
}

export interface Presupuesto {
  periodo: string;
  montoTotal: number;
  montoDisponible: number;
}

// ── Service ──────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class AbastecimientoService {
  private base = environment.apiUrl;

  constructor(private http: HttpClient) {}

  listarOrdenes(): Observable<OrdenCompra[]> {
    return this.http.get<OrdenCompra[]>(`${this.base}/orden-compra`);
  }

  crearOrden(oc: OrdenCompra): Observable<OrdenCompra> {
    return this.http.post<OrdenCompra>(`${this.base}/orden-compra`, oc);
  }

  cambiarEstadoOrden(id: number, estado: string): Observable<OrdenCompra> {
    return this.http.patch<OrdenCompra>(`${this.base}/orden-compra/${id}/estado`, { estado });
  }

  eliminarOrden(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/orden-compra/${id}`);
  }

  obtenerPresupuesto(): Observable<Presupuesto> {
    return this.http.get<Presupuesto>(`${this.base}/presupuesto`);
  }

  registrarCompra(c: Compra): Observable<RegistrarCompraResultado> {
    return this.http.post<RegistrarCompraResultado>(`${this.base}/compra`, c);
  }
}
