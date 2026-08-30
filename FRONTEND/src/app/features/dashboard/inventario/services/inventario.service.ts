import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
export interface Inventario {
  id?: number;
  producto: string;
  categoria?: string;
  stock: number;
  stock_minimo?: number;
  stockMinimo?: number;
  precio_unitario?: number;
  precioUnitario?: number;
  unidad?: string;
  fecha_actualizacion?: string;
  fechaActualizacion?: string;
  bajoStock?: boolean;
}

export interface BajoStockResponse {
  total: number;
  productos: Inventario[];
}

@Injectable({ providedIn: 'root' })
export class InventarioService {
  private url = `${environment.apiUrl}/inventario`;

  constructor(private http: HttpClient) {}

  listarInventario(): Observable<Inventario[]> {
    return this.http.get<Inventario[]>(this.url);
  }

  buscarPorId(id: number): Observable<Inventario> {
    return this.http.get<Inventario>(`${this.url}/${id}`);
  }

  crearProducto(inv: Inventario): Observable<any> {
    return this.http.post<any>(this.url, inv);
  }

  actualizarProducto(id: number, inv: Inventario): Observable<any> {
    return this.http.put<any>(`${this.url}/${id}`, inv);
  }

  actualizarStock(id: number, cantidad: number, tipo: 'ENTRADA' | 'SALIDA'): Observable<any> {
    return this.http.patch<any>(`${this.url}/${id}/stock`, { cantidad, tipo });
  }

  listarBajoStock(): Observable<BajoStockResponse> {
    return this.http.get<BajoStockResponse>(`${this.url}/bajo-stock`);
  }

  obtenerValorTotal(): Observable<any> {
    return this.http.get(`${this.url}/valor-total`);
  }
}
