import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';

export interface Usuario {
  id?: number;
  username: string;
  password?: string;
  rol: { id: number; nombre: string };
  rolNombre?: string;
  estadoId?: number;
  estado?: string;
}

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private url = `${environment.apiUrl}/usuarios`;

  constructor(private http: HttpClient) {}

  listarUsuarios(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(this.url);
  }

  crearUsuario(data: { username: string; password: string; rolId: number }): Observable<any> {
    return this.http.post(this.url, {
      username: data.username,
      password: data.password,
      rol: { id: data.rolId },
    });
  }

  editarUsuario(
    id: number,
    data: { username: string; password?: string; rolId: number },
  ): Observable<any> {
    const body: any = { username: data.username, rol: { id: data.rolId } };
    if (data.password) body.password = data.password;
    return this.http.put(`${this.url}/${id}`, body);
  }

  eliminarUsuario(id: number): Observable<any> {
    return this.http.delete(`${this.url}/${id}`);
  }
}
