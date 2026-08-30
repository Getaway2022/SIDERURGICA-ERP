import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';

export interface Empleado {
  id?: number;
  codigo?: string;
  nombre: string;
  cargo?: string;
  area?: string;
  horaEntradaEsperada?: string;
  activo?: boolean;
}

export interface Asistencia {
  id?: number;
  empleado: Empleado;
  fecha: string;
  horaEntrada?: string;
  minutosTardanza?: number;
  estado: 'Presente' | 'Tardanza' | 'Ausente';
  observacion?: string;
}

export interface ResumenIncidencias {
  empleadoId: number;
  empleadoCodigo: string;
  empleadoNombre: string;
  area: string;
  totalTardanzas: number;
  totalFaltas: number;
  totalMinutosTardanza: number;
  diasRegistrados: number;
}

export type TipoIncidenciaPersonal = 'Permiso' | 'Licencia' | 'Sancion' | 'Accidente' | 'Otro';
export type EstadoIncidenciaPersonal = 'Pendiente' | 'Aprobado' | 'Rechazado';

export interface IncidenciaPersonal {
  id?: number;
  empleado: Empleado;
  tipo: TipoIncidenciaPersonal;
  fechaInicio: string;
  fechaFin?: string;
  descripcion: string;
  estado: EstadoIncidenciaPersonal;
  registradoPorUsuarioId?: number;
  resueltoPorUsuarioId?: number;
  fechaRegistro?: string;
  fechaResolucion?: string;
  comentarioResolucion?: string;
}

@Injectable({ providedIn: 'root' })
export class RrhhService {
  private empleadosUrl = `${environment.apiUrl}/empleados`;
  private asistenciaUrl = `${environment.apiUrl}/asistencia`;
  private incidenciasPersonalUrl = `${environment.apiUrl}/incidencias-personal`;

  constructor(private http: HttpClient) {}

  // ── Empleados ─────────────────────────────────────────────────────────────

  listarEmpleados(area?: string): Observable<Empleado[]> {
    const url = area ? `${this.empleadosUrl}?area=${encodeURIComponent(area)}` : this.empleadosUrl;
    return this.http.get<Empleado[]>(url);
  }

  crearEmpleado(empleado: Empleado): Observable<any> {
    return this.http.post<any>(this.empleadosUrl, empleado);
  }

  actualizarEmpleado(id: number, empleado: Empleado): Observable<any> {
    return this.http.put<any>(`${this.empleadosUrl}/${id}`, empleado);
  }

  eliminarEmpleado(id: number): Observable<any> {
    return this.http.delete<any>(`${this.empleadosUrl}/${id}`);
  }

  obtenerHoraEntradaGeneral(): Observable<{ horaEntradaGeneral: string }> {
    return this.http.get<{ horaEntradaGeneral: string }>(
      `${this.empleadosUrl}/hora-entrada-general`,
    );
  }

  actualizarHoraEntradaGeneral(
    hora: string,
  ): Observable<{ success: boolean; mensaje: string; horaEntradaGeneral: string }> {
    return this.http.patch<{ success: boolean; mensaje: string; horaEntradaGeneral: string }>(
      `${this.empleadosUrl}/hora-entrada-general`,
      { hora },
    );
  }

  // ── HU21 — Asistencia ─────────────────────────────────────────────────────

  listarAsistenciaDelDia(fecha?: string): Observable<Asistencia[]> {
    const url = fecha ? `${this.asistenciaUrl}?fecha=${fecha}` : this.asistenciaUrl;
    return this.http.get<Asistencia[]>(url);
  }

  registrarAsistencia(
    empleadoId: number,
    horaEntrada?: string,
    observacion?: string,
  ): Observable<{ success: boolean; mensaje: string; asistencia: Asistencia }> {
    return this.http.post<{ success: boolean; mensaje: string; asistencia: Asistencia }>(
      this.asistenciaUrl,
      { empleadoId, horaEntrada, observacion },
    );
  }

  actualizarEstadoAsistencia(
    id: number,
    estado: string,
    observacion?: string,
  ): Observable<{ success: boolean; mensaje: string; asistencia: Asistencia }> {
    return this.http.patch<{ success: boolean; mensaje: string; asistencia: Asistencia }>(
      `${this.asistenciaUrl}/${id}/estado`,
      { estado, observacion },
    );
  }

  corregirHoraEntrada(
    id: number,
    horaEntrada?: string,
    observacion?: string,
  ): Observable<{ success: boolean; mensaje: string; asistencia: Asistencia }> {
    return this.http.patch<{ success: boolean; mensaje: string; asistencia: Asistencia }>(
      `${this.asistenciaUrl}/${id}/hora-entrada`,
      { horaEntrada, observacion },
    );
  }

  historialEmpleado(empleadoId: number): Observable<Asistencia[]> {
    return this.http.get<Asistencia[]>(`${this.asistenciaUrl}/empleado/${empleadoId}`);
  }

  // ── HU22 — Tardanzas y faltas ─────────────────────────────────────────────

  listarIncidencias(
    desde?: string,
    hasta?: string,
  ): Observable<{ success: boolean; total: number; incidencias: Asistencia[] }> {
    const params = this.armarParamsFecha(desde, hasta);
    return this.http.get<{ success: boolean; total: number; incidencias: Asistencia[] }>(
      `${this.asistenciaUrl}/incidencias${params}`,
    );
  }

  listarIncidenciasPorEmpleado(
    empleadoId: number,
    desde?: string,
    hasta?: string,
  ): Observable<{ success: boolean; total: number; incidencias: Asistencia[] }> {
    const params = this.armarParamsFecha(desde, hasta);
    return this.http.get<{ success: boolean; total: number; incidencias: Asistencia[] }>(
      `${this.asistenciaUrl}/incidencias/empleado/${empleadoId}${params}`,
    );
  }

  resumenIncidencias(
    desde?: string,
    hasta?: string,
  ): Observable<{ success: boolean; resumen: ResumenIncidencias[] }> {
    const params = this.armarParamsFecha(desde, hasta);
    return this.http.get<{ success: boolean; resumen: ResumenIncidencias[] }>(
      `${this.asistenciaUrl}/incidencias/resumen${params}`,
    );
  }

  private armarParamsFecha(desde?: string, hasta?: string): string {
    const partes: string[] = [];
    if (desde) partes.push(`desde=${desde}`);
    if (hasta) partes.push(`hasta=${hasta}`);
    return partes.length ? `?${partes.join('&')}` : '';
  }

  // ── HU23 — Incidencias del personal ──────────────────────────────────────

  listarIncidenciasPersonal(
    estado?: EstadoIncidenciaPersonal,
    tipo?: TipoIncidenciaPersonal,
  ): Observable<IncidenciaPersonal[]> {
    const partes: string[] = [];
    if (estado) partes.push(`estado=${estado}`);
    if (tipo) partes.push(`tipo=${tipo}`);
    const url = partes.length
      ? `${this.incidenciasPersonalUrl}?${partes.join('&')}`
      : this.incidenciasPersonalUrl;
    return this.http.get<IncidenciaPersonal[]>(url);
  }

  legajoEmpleado(empleadoId: number): Observable<IncidenciaPersonal[]> {
    return this.http.get<IncidenciaPersonal[]>(
      `${this.incidenciasPersonalUrl}/empleado/${empleadoId}`,
    );
  }

  registrarIncidenciaPersonal(datos: {
    empleadoId: number;
    tipo: TipoIncidenciaPersonal;
    fechaInicio: string;
    fechaFin?: string;
    descripcion: string;
  }): Observable<{ success: boolean; mensaje: string; incidencia: IncidenciaPersonal }> {
    const usuario = JSON.parse(localStorage.getItem('erp_user') || '{}');
    return this.http.post<{ success: boolean; mensaje: string; incidencia: IncidenciaPersonal }>(
      this.incidenciasPersonalUrl,
      { ...datos, usuario_id: usuario.id },
    );
  }

  resolverIncidenciaPersonal(
    id: number,
    estado: 'Aprobado' | 'Rechazado',
    comentario?: string,
  ): Observable<{ success: boolean; mensaje: string; incidencia: IncidenciaPersonal }> {
    const usuario = JSON.parse(localStorage.getItem('erp_user') || '{}');
    return this.http.patch<{ success: boolean; mensaje: string; incidencia: IncidenciaPersonal }>(
      `${this.incidenciasPersonalUrl}/${id}/resolver`,
      { estado, comentario, usuario_id: usuario.id },
    );
  }

  eliminarIncidenciaPersonal(id: number): Observable<any> {
    return this.http.delete<any>(`${this.incidenciasPersonalUrl}/${id}`);
  }
}
