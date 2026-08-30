import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { Empleado } from '../../rrhh/services/rrhh.service';

// ── Interfaces del dominio de Planillas ──────────────────────────────────────

export interface Planilla {
  id: number;
  periodo: string;
  fechaInicio: string;
  fechaFin: string;
  estado: 'Borrador' | 'Validado' | 'Pagado';
  fechaCreacion: string;
  fechaValidacion?: string;
  observacion?: string;
}

export interface PlanillaDetalle {
  id: number;
  planilla: Planilla;
  empleado: Empleado;
  diasLaborables: number;
  diasPresentes: number;
  diasTardanza: number;
  diasAusente: number;
  totalMinutosTardanza: number;
  salarioBase: number;
  descuentoFaltas: number;
  descuentoTardanzas: number;
  descuentoAfp: number;
  descuentoEssalud: number;
  bonificacion: number;
  totalDescuentos: number;
  sueldoNeto: number;
  validado: boolean;
  observacionValidacion?: string;
}

export interface PagoPlanilla {
  id: number;
  planillaDetalle: PlanillaDetalle;
  empleado: Empleado;
  montoPagado: number;
  fechaPago: string;
  metodoPago: string;
  numeroOperacion?: string;
  confirmado: boolean;
}

export interface GenerarPlanillaResponse {
  success: boolean;
  mensaje?: string;
  planilla?: Planilla;
  detalles?: PlanillaDetalle[];
}

// ── Service ──────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class PlanillaService {
  private base = environment.apiUrl;
  private opts = { withCredentials: true };

  constructor(private http: HttpClient) {}

  // ── Planillas ─────────────────────────────────────────────────────────────

  listarPlanillas(): Observable<Planilla[]> {
    return this.http.get<Planilla[]>(`${this.base}/planillas`, this.opts);
  }

  generarPlanilla(periodo: string, creadoPorUsuario = 1): Observable<GenerarPlanillaResponse> {
    return this.http.post<GenerarPlanillaResponse>(
      `${this.base}/planillas`,
      { periodo, creadoPorUsuario },
      this.opts,
    );
  }

  // ── Detalles ──────────────────────────────────────────────────────────────

  listarDetalles(planillaId: number): Observable<PlanillaDetalle[]> {
    return this.http.get<PlanillaDetalle[]>(
      `${this.base}/planillas/${planillaId}/detalles`,
      this.opts,
    );
  }

  actualizarBonificacion(
    planillaId: number,
    detalleId: number,
    bonificacion: number,
  ): Observable<any> {
    return this.http.patch<any>(
      `${this.base}/planillas/${planillaId}/detalles/${detalleId}/bonificacion`,
      { bonificacion },
      this.opts,
    );
  }

  validarDetalle(planillaId: number, detalleId: number, observacion: string): Observable<any> {
    return this.http.patch<any>(
      `${this.base}/planillas/${planillaId}/detalles/${detalleId}/validar`,
      { observacion },
      this.opts,
    );
  }

  validarTodaLaPlanilla(planillaId: number, validadoPorUsuario = 1): Observable<any> {
    return this.http.post<any>(
      `${this.base}/planillas/${planillaId}/validar-todo`,
      { validadoPorUsuario },
      this.opts,
    );
  }

  // ── Pagos ─────────────────────────────────────────────────────────────────

  listarPagos(planillaId: number): Observable<PagoPlanilla[]> {
    return this.http.get<PagoPlanilla[]>(`${this.base}/planillas/${planillaId}/pagos`, this.opts);
  }

  registrarPago(
    planillaId: number,
    detalleId: number,
    metodoPago: string,
    numeroOperacion: string | null,
    registradoPorUsuario = 1,
  ): Observable<any> {
    return this.http.post<any>(
      `${this.base}/planillas/${planillaId}/detalles/${detalleId}/pagar`,
      { metodoPago, numeroOperacion, registradoPorUsuario },
      this.opts,
    );
  }
}
