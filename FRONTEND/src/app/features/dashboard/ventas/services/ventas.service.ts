import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';

// ── Interfaces del dominio de Ventas ─────────────────────────────────────────

export interface Venta {
  id?: number;
  codigo?: string;
  cliente: string;
  ruc: string;
  producto: string;
  cantidad: number;
  precioUnitario?: number;
  precio_unitario?: number;
  subtotal?: number;
  igv?: number;
  total?: number;
  vendedor?: string;
  estado?: string;
  fechaVenta?: string;
  pedidoId?: number;
  cotizacionId?: number;
  createdAt?: string;
}

// ── Service ──────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class VentaService {
  private base = environment.apiUrl;

  constructor(private http: HttpClient) {}

  listarVentas(): Observable<Venta[]> {
    return this.http.get<Venta[]>(`${this.base}/ventas`);
  }

  cambiarEstado(id: number, estado: string): Observable<Venta> {
    return this.http.patch<Venta>(`${this.base}/ventas/${id}/estado`, { estado });
  }

  generarDesdePedido(pedidoId: number): Observable<Venta> {
    return this.http.post<Venta>(`${this.base}/ventas/desde-pedido/${pedidoId}`, {});
  }

  generarDespacho(ventaId: number): Observable<any> {
    return this.http.post<any>(`${this.base}/despachos/desde-venta/${ventaId}`, {});
  }
}
