import {
  Component,
  OnInit,
  OnDestroy,
  ChangeDetectorRef,
  EventEmitter,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Subscription, forkJoin, interval } from 'rxjs';
import { environment } from '../../../../environments/environment';

interface Solicitud {
  id: number;
  inventarioId: number;
  producto: string;
  cantidad: number;
  estado: string;
  ordenCompraId?: number;
  mensaje: string;
  creadaEn: string;
}

@Component({
  selector: 'app-reposiciones',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section
      class="table-card"
      style="margin: 20px 0; padding: 16px"
      aria-label="Avisos de reposición"
    >
      <h3>Reposiciones y avisos internos</h3>
      <p>
        Las solicitudes se calculan con el stock disponible. Aprobar compromete presupuesto y crea
        una orden de compra.
      </p>
      <button class="btn btn-secondary btn-sm" (click)="cargar()" [disabled]="ocupado">
        Actualizar
      </button>
      <p role="status">{{ mensaje }}</p>
      <div style="overflow-x: auto">
        <table>
          <thead>
            <tr>
              <th>Producto</th>
              <th>Cantidad</th>
              <th>Estado</th>
              <th>Aviso</th>
              <th>Acción</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let s of solicitudes">
              <td>{{ s.producto }}</td>
              <td>{{ s.cantidad }}</td>
              <td>{{ s.estado }}</td>
              <td>{{ s.mensaje }}</td>
              <td>
                <div *ngIf="s.estado === 'PENDIENTE'">
                  <label
                    >Proveedor
                    <select [(ngModel)]="proveedor[s.id]" [disabled]="ocupado">
                      <option [ngValue]="undefined">Seleccionar</option>
                      <option *ngFor="let p of proveedores" [ngValue]="p.id">{{ p.nombre }}</option>
                    </select>
                  </label>
                  <label
                    >Precio unitario (S/)
                    <input
                      type="number"
                      min="0.01"
                      step="0.01"
                      [(ngModel)]="precio[s.id]"
                      [disabled]="ocupado"
                    />
                  </label>
                  <button
                    class="btn btn-primary btn-sm"
                    (click)="aprobar(s)"
                    [disabled]="ocupado || !proveedor[s.id] || !(precio[s.id] > 0)"
                  >
                    Aprobar compra
                  </button>
                </div>
                <button
                  *ngIf="s.estado === 'APROBADA'"
                  class="btn btn-secondary btn-sm"
                  (click)="recibir(s)"
                  [disabled]="ocupado"
                >
                  Confirmar recepción completa
                </button>
              </td>
            </tr>
            <tr *ngIf="!solicitudes.length">
              <td colspan="5">Sin solicitudes de reposición.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  `,
})
export class ReposicionesComponent implements OnInit, OnDestroy {
  @Output() cambio = new EventEmitter<void>();
  solicitudes: Solicitud[] = [];
  proveedores: { id: number; nombre: string; estado: string }[] = [];
  proveedor: Record<number, number> = {};
  precio: Record<number, number> = {};
  ocupado = false;
  mensaje = '';
  private subs = new Subscription();
  private base = environment.apiUrl;
  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
  ) {}
  ngOnInit() {
    this.cargar();
    this.subs.add(
      interval(30000).subscribe(() => {
        if (!this.ocupado) this.cargar();
      }),
    );
  }
  ngOnDestroy() {
    this.subs.unsubscribe();
  }
  cargar() {
    this.subs.add(
      forkJoin({
        solicitudes: this.http.get<Solicitud[]>(`${this.base}/reposiciones`),
        proveedores: this.http.get<{ id: number; nombre: string; estado: string }[]>(
          `${this.base}/proveedor`,
        ),
      }).subscribe({
        next: (data) => {
          this.solicitudes = data.solicitudes;
          this.proveedores = data.proveedores.filter((p) => p.estado === 'Activo');
          this.cdr.detectChanges();
        },
        error: (err) => this.error(err),
      }),
    );
  }
  aprobar(s: Solicitud) {
    if (
      this.ocupado ||
      !confirm(
        `¿Aprobar la compra de ${s.cantidad} unidades de ${s.producto}? Se comprometerá presupuesto.`,
      )
    )
      return;
    this.ocupado = true;
    this.subs.add(
      this.http
        .post<Solicitud>(`${this.base}/reposiciones/${s.id}/aprobar`, {
          proveedorId: this.proveedor[s.id],
          precioUnitario: this.precio[s.id],
          cantidadEsperada: s.cantidad,
        })
        .subscribe({
          next: (actual) => this.terminar(actual.mensaje),
          error: (err) => this.error(err),
        }),
    );
  }
  recibir(s: Solicitud) {
    if (
      this.ocupado ||
      !confirm(
        `¿Confirmar que llegaron las ${s.cantidad} unidades de ${s.producto}? Se sumarán al stock físico.`,
      )
    )
      return;
    this.ocupado = true;
    this.subs.add(
      this.http
        .get<{ proveedorId: number; total: number }>(`${this.base}/orden-compra/${s.ordenCompraId}`)
        .subscribe({
          next: (orden) => {
            this.subs.add(
              this.http
                .post(`${this.base}/compra`, {
                  ordenCompraId: s.ordenCompraId,
                  proveedorId: orden.proveedorId,
                  inventarioId: s.inventarioId,
                  producto: s.producto,
                  cantidad: s.cantidad,
                  precioUnitario: Number(orden.total) / s.cantidad,
                })
                .subscribe({
                  next: () => this.terminar('Recepción registrada e inventario actualizado.'),
                  error: (err) => this.error(err),
                }),
            );
          },
          error: (err) => this.error(err),
        }),
    );
  }
  private terminar(mensaje: string) {
    this.ocupado = false;
    this.mensaje = mensaje;
    this.cargar();
    this.cambio.emit();
    this.cdr.detectChanges();
  }
  private error(err: any) {
    this.ocupado = false;
    this.mensaje = err.error?.error || err.error?.message || 'No se pudo completar la operación.';
    this.cdr.detectChanges();
  }
}
