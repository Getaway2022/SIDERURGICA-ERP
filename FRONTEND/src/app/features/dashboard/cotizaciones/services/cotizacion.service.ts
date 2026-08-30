import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
export interface Cotizacion {
  id?: number;
  codigo?: string;
  cliente: string;
  ruc?: string;
  producto: string;
  cantidad: number;
  precio_unitario: number;
  subtotal?: number;
  descuento_porcentaje?: number;
  descuento_monto?: number;
  total?: number;
  estado?: string;
  usuario_id?: string;
  vendedor?: string;
  fecha_registro?: string;
}

@Injectable({
  providedIn: 'root',
})
export class CotizacionService {
  private apiurl = `${environment.apiUrl}/cotizaciones`;

  constructor(private http: HttpClient) {}

  listarCotizaciones(): Observable<Cotizacion[]> {
    return this.http.get<Cotizacion[]>(this.apiurl);
  }

  registrarCotizacion(cotizacion: Cotizacion): Observable<any> {
    return this.http.post<any>(this.apiurl, cotizacion);
  }

  actualizarEstado(id: number, estado: string): Observable<any> {
    return this.http.patch<any>(`${this.apiurl}/${id}/estado`, { estado });
  }
}
