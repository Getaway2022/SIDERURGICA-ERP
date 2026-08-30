import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';

export interface Proveedor {
  id?: number;
  nombre: string;
  origen?: string;
  ruc: string;
  contacto?: string;
  telefono?: string;
  categoria?: string;
  calificacion?: number;
  estado: 'Activo' | 'Inactivo';
}

@Injectable({ providedIn: 'root' })
export class ProveedorService {
  private url = `${environment.apiUrl}/proveedor`;
  constructor(private http: HttpClient) {}

  listar(): Observable<Proveedor[]> {
    return this.http.get<Proveedor[]>(this.url);
  }
  crear(p: Proveedor): Observable<Proveedor> {
    return this.http.post<Proveedor>(this.url, p);
  }
  actualizar(id: number, p: Proveedor): Observable<Proveedor> {
    return this.http.put<Proveedor>(`${this.url}/${id}`, p);
  }
  eliminar(id: number): Observable<any> {
    return this.http.delete(`${this.url}/${id}`);
  }
}
