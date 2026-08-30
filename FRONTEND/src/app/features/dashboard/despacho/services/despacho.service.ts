import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';

// ── Interfaz movida desde despacho.ts (componente) ──
export interface Despacho {
  id?: number;
  codigo: string;
  cliente: string;
  direccion: string;
  producto: string;
  peso: string;
  transportista: string;
  estado: 'ENTREGADO' | 'ENVIADO' | 'PREPARADO' | 'PENDIENTE';
  comprobante?: string;
  comprobanteValidado?: boolean;
  pedidoId?: number;
}

@Injectable({ providedIn: 'root' })
export class DespachoService {
  private url = `${environment.apiUrl}/despacho`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Despacho[]> {
    return this.http.get<Despacho[]>(this.url);
  }

  crear(d: Partial<Despacho>): Observable<Despacho> {
    return this.http.post<Despacho>(this.url, d);
  }

  actualizar(id: number, d: Partial<Despacho>): Observable<Despacho> {
    return this.http.put<Despacho>(`${this.url}/${id}`, d);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  preparar(id: number): Observable<Despacho> {
    return this.http.patch<Despacho>(`${this.url}/${id}/preparar`, {});
  }

  validarComprobante(id: number, comprobante: string): Observable<Despacho> {
    return this.http.patch<Despacho>(`${this.url}/${id}/comprobante`, { comprobante });
  }

  confirmarEntrega(id: number): Observable<Despacho> {
    return this.http.patch<Despacho>(`${this.url}/${id}/confirmar-entrega`, {});
  }
}
