import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';

export interface Pedido {
  id?: number;
  codigo?: string;
  cliente: string;
  ruc?: string;
  producto: string;
  cantidad: number;
  precio_unitario: number;
  subtotal?: number;
  igv?: number;
  total?: number;
  estado?: string;
  vendedor?: string;
  usuario_id?: number | null;
  fecha_registro?: string;
}

@Injectable({ providedIn: 'root' })
export class PedidoService {
  private url = `${environment.apiUrl}/pedidos`;

  constructor(private http: HttpClient) {}

  listarPedidos(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(this.url);
  }

  registrarPedido(pedido: Pedido): Observable<any> {
    return this.http.post(this.url, pedido);
  }

  actualizarEstado(id: number, estado: string): Observable<any> {
    return this.http.patch(`${this.url}/${id}/estado`, { estado });
  }
}
