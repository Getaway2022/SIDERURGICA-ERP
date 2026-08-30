import { Component, NgZone, ChangeDetectorRef, OnInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  PlanillaService,
  Planilla,
  PlanillaDetalle,
  PagoPlanilla,
} from './services/planilla.service';
import { Empleado } from '../rrhh/services/rrhh.service';

// Planilla, PlanillaDetalle, PagoPlanilla, Empleado ya NO se definen aquí
// vienen de planilla.service.ts y rrhh.service.ts ↑

@Component({
  selector: 'app-planillas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './planillas.html',
  styles: [],
})
export class PlanillasComponent implements OnInit {
  @ViewChild('boletaRef') boletaRef!: ElementRef<HTMLDivElement>;

  constructor(
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef,
    private planillaService: PlanillaService,
  ) {}

  // ─── Estado general ────────────────────────────────────────────────────────
  planillas: Planilla[] = [];
  detalles: PlanillaDetalle[] = [];
  pagos: PagoPlanilla[] = [];

  planillaSeleccionada: Planilla | null = null;
  detalleSeleccionado: PlanillaDetalle | null = null;

  loadingPlanillas = false;
  loadingDetalles = false;
  guardando = false;
  descargandoPdf = false;

  // ─── Modales ───────────────────────────────────────────────────────────────
  showModalGenerar = false;
  showModalValidar = false;
  showModalPagar = false;
  showModalBoleta = false;
  boletaDetalle: PlanillaDetalle | null = null;

  // ─── Formularios ──────────────────────────────────────────────────────────
  formPeriodo = '';
  formObsValidacion = '';
  formMetodoPago = 'Transferencia';
  formNumeroOperacion = '';
  formBonificacion = 0;

  // ─── Toast ────────────────────────────────────────────────────────────────
  toastMsg = '';
  toastType: 'success' | 'error' | 'info' = 'success';
  toastVisible = false;

  // ═══════════════════════════════════════════════════════════════════════════
  ngOnInit() {
    this.cargarPlanillas();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CARGAR DATOS
  // ═══════════════════════════════════════════════════════════════════════════

  cargarPlanillas() {
    this.loadingPlanillas = true;
    this.planillaService.listarPlanillas().subscribe({
      next: (data) => {
        this.planillas = data;
        this.loadingPlanillas = false;
        if (this.planillaSeleccionada) {
          const updated = data.find((p) => p.id === this.planillaSeleccionada!.id);
          if (updated) {
            this.planillaSeleccionada = updated;
            this.cargarDetalles(updated.id);
          }
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingPlanillas = false;
        this.showToast('Error al cargar planillas', 'error');
        this.cdr.detectChanges();
      },
    });
  }

  seleccionarPlanilla(p: Planilla) {
    this.planillaSeleccionada = p;
    this.detalles = [];
    this.pagos = [];
    this.cargarDetalles(p.id);
    this.cargarPagos(p.id);
  }

  cargarDetalles(planillaId: number) {
    this.loadingDetalles = true;
    this.planillaService.listarDetalles(planillaId).subscribe({
      next: (data) => {
        this.detalles = data;
        this.loadingDetalles = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingDetalles = false;
        this.cdr.detectChanges();
      },
    });
  }

  cargarPagos(planillaId: number) {
    this.planillaService.listarPagos(planillaId).subscribe({
      next: (data) => {
        this.pagos = data;
        this.cdr.detectChanges();
      },
      error: () => {},
    });
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HU24 — GENERAR PLANILLA
  // ═══════════════════════════════════════════════════════════════════════════

  abrirModalGenerar() {
    const now = new Date();
    this.formPeriodo = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    this.showModalGenerar = true;
  }

  generarPlanilla() {
    if (!this.formPeriodo) {
      this.showToast('Ingrese el período', 'error');
      return;
    }
    this.guardando = true;
    this.planillaService.generarPlanilla(this.formPeriodo).subscribe({
      next: (res) => {
        this.guardando = false;
        if (res.success === false) {
          this.showToast(res.mensaje || 'Error al generar planilla', 'error');
        } else {
          this.showModalGenerar = false;
          this.showToast(
            `✅ Planilla ${this.formPeriodo} generada con ${res.detalles?.length ?? 0} empleados`,
            'success',
          );
          this.cargarPlanillas();
          if (res.planilla) setTimeout(() => this.seleccionarPlanilla(res.planilla!), 400);
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.guardando = false;
        this.showToast('Error al generar planilla', 'error');
        this.cdr.detectChanges();
      },
    });
  }

  guardarBonificacion(det: PlanillaDetalle) {
    this.planillaService
      .actualizarBonificacion(det.planilla.id, det.id, det.bonificacion)
      .subscribe({
        next: (res) => {
          if (res.success !== false) {
            this.showToast('Bonificación actualizada', 'success');
            this.cargarDetalles(det.planilla.id);
          } else {
            this.showToast(res.mensaje || 'Error', 'error');
          }
          this.cdr.detectChanges();
        },
        error: () => {
          this.showToast('Error al actualizar', 'error');
          this.cdr.detectChanges();
        },
      });
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HU25 — VALIDAR
  // ═══════════════════════════════════════════════════════════════════════════

  abrirModalValidar(det: PlanillaDetalle) {
    this.detalleSeleccionado = det;
    this.formObsValidacion = '';
    this.showModalValidar = true;
  }

  validarDetalle() {
    if (!this.detalleSeleccionado) return;
    this.guardando = true;
    const det = this.detalleSeleccionado;
    this.planillaService
      .validarDetalle(det.planilla.id, det.id, this.formObsValidacion || 'Revisado y conforme')
      .subscribe({
        next: (res) => {
          this.guardando = false;
          if (res.success !== false) {
            this.showModalValidar = false;
            this.showToast(`✅ ${det.empleado.nombre} validado`, 'success');
            this.cargarPlanillas();
            this.cargarDetalles(det.planilla.id);
          } else {
            this.showToast(res.mensaje || 'Error al validar', 'error');
          }
          this.cdr.detectChanges();
        },
        error: () => {
          this.guardando = false;
          this.showToast('Error al validar', 'error');
          this.cdr.detectChanges();
        },
      });
  }

  validarTodaLaPlanilla() {
    if (!this.planillaSeleccionada) return;
    this.guardando = true;
    const p = this.planillaSeleccionada;
    this.planillaService.validarTodaLaPlanilla(p.id).subscribe({
      next: (res) => {
        this.guardando = false;
        if (res.success !== false) {
          this.showToast('✅ Planilla aprobada correctamente', 'success');
          this.cargarPlanillas();
          this.cargarDetalles(p.id);
        } else {
          this.showToast(res.mensaje || 'Error al aprobar', 'error');
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.guardando = false;
        this.showToast('Error al aprobar planilla', 'error');
        this.cdr.detectChanges();
      },
    });
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HU26 — REGISTRAR PAGO
  // ═══════════════════════════════════════════════════════════════════════════

  abrirModalPagar(det: PlanillaDetalle) {
    this.detalleSeleccionado = det;
    this.formMetodoPago = 'Transferencia';
    this.formNumeroOperacion = '';
    this.showModalPagar = true;
  }

  registrarPago() {
    if (!this.detalleSeleccionado) return;
    this.guardando = true;
    const det = this.detalleSeleccionado;
    this.planillaService
      .registrarPago(det.planilla.id, det.id, this.formMetodoPago, this.formNumeroOperacion || null)
      .subscribe({
        next: (res) => {
          this.guardando = false;
          if (res.success !== false) {
            this.showModalPagar = false;
            this.showToast(
              `✅ Pago de ${this.formatMoney(det.sueldoNeto)} registrado para ${det.empleado.nombre}`,
              'success',
            );
            this.cargarPlanillas();
            this.cargarDetalles(det.planilla.id);
            this.cargarPagos(det.planilla.id);
          } else {
            this.showToast(res.mensaje || 'Error al registrar pago', 'error');
          }
          this.cdr.detectChanges();
        },
        error: () => {
          this.guardando = false;
          this.showToast('Error al registrar pago', 'error');
          this.cdr.detectChanges();
        },
      });
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BOLETA + DESCARGA PDF
  // ═══════════════════════════════════════════════════════════════════════════

  verBoleta(det: PlanillaDetalle) {
    this.boletaDetalle = det;
    this.showModalBoleta = true;
  }

  async descargarPdf() {
    if (!this.boletaDetalle || !this.boletaRef) return;
    this.descargandoPdf = true;
    this.cdr.detectChanges();
    try {
      await this.cargarScript(
        'https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js',
      );
      await this.cargarScript(
        'https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js',
      );

      const html2canvas = (window as any).html2canvas;
      const { jsPDF } = (window as any).jspdf;

      const canvas = await html2canvas(this.boletaRef.nativeElement, {
        scale: 2,
        useCORS: true,
        backgroundColor: '#ffffff',
      });

      const imgData = canvas.toDataURL('image/png');
      const pdf = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
      const pdfW = pdf.internal.pageSize.getWidth();
      const pdfH = pdf.internal.pageSize.getHeight();
      const margin = 15;
      const imgW = pdfW - margin * 2;
      const imgH = (canvas.height * imgW) / canvas.width;
      const offsetY = imgH < pdfH - margin * 2 ? (pdfH - imgH) / 2 : margin;

      pdf.addImage(imgData, 'PNG', margin, offsetY, imgW, imgH);

      const nombre = this.boletaDetalle.empleado.nombre.replace(/ /g, '_');
      const periodo = this.boletaDetalle.planilla.periodo;
      pdf.save(`Boleta_${nombre}_${periodo}.pdf`);

      this.showToast('✅ PDF descargado correctamente', 'success');
    } catch (err) {
      console.error('Error generando PDF:', err);
      this.showToast('Error al generar el PDF', 'error');
    } finally {
      this.ngZone.run(() => {
        this.descargandoPdf = false;
        this.cdr.detectChanges();
      });
    }
  }

  private cargarScript(src: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (document.querySelector(`script[src="${src}"]`)) {
        resolve();
        return;
      }
      const s = document.createElement('script');
      s.src = src;
      s.onload = () => resolve();
      s.onerror = () => reject(new Error(`No se pudo cargar: ${src}`));
      document.head.appendChild(s);
    });
  }

  getPagoDeDetalle(det: PlanillaDetalle): PagoPlanilla | undefined {
    return this.pagos.find((p) => p.planillaDetalle?.id === det.id);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // UTILIDADES
  // ═══════════════════════════════════════════════════════════════════════════

  closeModal() {
    this.showModalGenerar = false;
    this.showModalValidar = false;
    this.showModalPagar = false;
    this.showModalBoleta = false;
    this.detalleSeleccionado = null;
    this.boletaDetalle = null;
  }

  formatMoney(n: number): string {
    return (
      'S/ ' +
      (n ?? 0).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    );
  }

  formatPeriodo(p: string): string {
    const meses = [
      'Enero',
      'Febrero',
      'Marzo',
      'Abril',
      'Mayo',
      'Junio',
      'Julio',
      'Agosto',
      'Septiembre',
      'Octubre',
      'Noviembre',
      'Diciembre',
    ];
    const [anio, mes] = p.split('-');
    return `${meses[+mes - 1]} ${anio}`;
  }

  getBadgeEstado(estado: string): string {
    if (estado === 'Pagado') return 'badge-green';
    if (estado === 'Validado') return 'badge-blue';
    return 'badge-orange';
  }

  getIniciales(nombre: string): string {
    return nombre
      .split(' ')
      .slice(0, 2)
      .map((w) => w[0])
      .join('')
      .toUpperCase();
  }

  getColor(nombre: string): string {
    const colores = ['#1A365D', '#38A169', '#DD6B20', '#805AD5', '#2B6CB0', '#C05621'];
    let sum = 0;
    for (const c of nombre) sum += c.charCodeAt(0);
    return colores[sum % colores.length];
  }

  totalNeto(): number {
    return this.detalles.reduce((s, d) => s + (d.sueldoNeto ?? 0), 0);
  }
  totalPagados(): number {
    return this.detalles.filter((d) => !!this.getPagoDeDetalle(d)).length;
  }
  totalPendientesPago(): number {
    return this.detalles.filter((d) => d.validado && !this.getPagoDeDetalle(d)).length;
  }
  hoy(): string {
    return new Date().toLocaleDateString('es-PE', {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
    });
  }

  showToast(msg: string, type: 'success' | 'error' | 'info' = 'success') {
    this.toastMsg = msg;
    this.toastType = type;
    this.toastVisible = true;
    setTimeout(() => {
      this.toastVisible = false;
      this.cdr.detectChanges();
    }, 3500);
  }
}
