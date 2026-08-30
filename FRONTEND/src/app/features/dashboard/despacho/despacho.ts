import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormsModule,
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { DespachoService, Despacho } from './services/despacho.service';
import { CustomValidators } from '../../../shared/validators/custom-validators';

// ── Interfaz eliminada de aquí — ahora vive en despacho.service.ts ──

@Component({
  selector: 'app-despacho',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './despacho.html',
  styles: [],
})
export class DespachoComponent implements OnInit {
  despachos: Despacho[] = [];
  despachoSearch = '';
  despachoFiltroEstado = '';
  showModal = false;
  modalMode: 'crear' | 'editar' | 'eliminar' | 'comprobante' = 'crear';
  despachoForm: Partial<Despacho> = {};
  comprobanteTexto = '';
  deleteTarget: Despacho | null = null;
  despachoActivo: Despacho | null = null;
  toastMsg = '';
  toastType: 'success' | 'error' = 'success';
  toastVisible = false;

  // ── Reactive Form ────────────────────────────────────────────────────────
  form!: FormGroup;

  constructor(
    private despachoService: DespachoService,
    private cdr: ChangeDetectorRef,
    private fb: FormBuilder,
  ) {}

  ngOnInit() {
    this.initForm();
    this.cargar();
  }

  initForm() {
    this.form = this.fb.group({
      cliente: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      producto: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      direccion: ['', []],
      peso: ['', []],
      transportista: ['', []],
    });
  }

  isInvalid(campo: string): boolean {
    return CustomValidators.showError(this.form.get(campo));
  }
  errorMsg(campo: string, label: string): string {
    return CustomValidators.getErrorMessage(this.form.get(campo), label);
  }

  cargar() {
    this.despachoService.listar().subscribe((d) => {
      this.despachos = d;
      this.cdr.detectChanges();
    });
  }

  get despachosFiltrados(): Despacho[] {
    return this.despachos.filter((d) => {
      const matchSearch =
        !this.despachoSearch ||
        (d.cliente || '').toLowerCase().includes(this.despachoSearch.toLowerCase()) ||
        (d.codigo || '').toLowerCase().includes(this.despachoSearch.toLowerCase());
      return matchSearch && (!this.despachoFiltroEstado || d.estado === this.despachoFiltroEstado);
    });
  }

  getBadgeClass(estado: string): string {
    const map: Record<string, string> = {
      ENTREGADO: 'badge-green',
      ENVIADO: 'badge-blue',
      PREPARADO: 'badge-yellow',
      PENDIENTE: 'badge-orange',
    };
    return map[estado] || 'badge-yellow';
  }

  openModalCrear() {
    this.modalMode = 'crear';
    this.form.reset();
    this.despachoForm = { estado: 'PENDIENTE' };
    this.showModal = true;
  }

  openModalEditar(d: Despacho) {
    this.modalMode = 'editar';
    this.despachoForm = { ...d };
    this.form.patchValue({
      cliente: d.cliente,
      producto: d.producto,
      direccion: d.direccion,
      peso: d.peso,
      transportista: d.transportista,
    });
    this.showModal = true;
  }

  pedirEliminar(d: Despacho) {
    this.modalMode = 'eliminar';
    this.deleteTarget = d;
    this.showModal = true;
  }
  closeModal() {
    this.showModal = false;
    this.deleteTarget = null;
    this.despachoActivo = null;
  }

  guardarDespacho() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const data = { ...this.despachoForm, ...this.form.value };
    if (this.modalMode === 'crear') {
      this.despachoService.crear(data).subscribe({
        next: () => {
          this.showToast('Despacho registrado');
          this.cargar();
          this.closeModal();
        },
        error: () => this.showToast('Error al registrar', 'error'),
      });
    } else {
      this.despachoService.actualizar(this.despachoForm.id!, data).subscribe({
        next: () => {
          this.showToast('Despacho actualizado');
          this.cargar();
          this.closeModal();
        },
        error: () => this.showToast('Error al actualizar', 'error'),
      });
    }
  }

  preparar(d: Despacho) {
    if (!d.id) return;
    this.despachoService.preparar(d.id).subscribe({
      next: () => {
        this.showToast(`Guía ${d.codigo} preparada`);
        this.cargar();
      },
      error: (err) => this.showToast(err.error?.message || 'Error al preparar', 'error'),
    });
  }

  abrirComprobante(d: Despacho) {
    this.despachoActivo = d;
    this.comprobanteTexto = '';
    this.modalMode = 'comprobante';
    this.showModal = true;
  }

  validarComprobante() {
    if (!this.despachoActivo?.id || !this.comprobanteTexto.trim()) {
      this.showToast('Ingrese un código o referencia de comprobante', 'error');
      return;
    }
    this.despachoService
      .validarComprobante(this.despachoActivo.id, this.comprobanteTexto)
      .subscribe({
        next: () => {
          this.showToast('Comprobante validado');
          this.cargar();
          this.closeModal();
        },
        error: (err) =>
          this.showToast(err.error?.message || 'Error al validar comprobante', 'error'),
      });
  }

  confirmarEntrega(d: Despacho) {
    if (!d.id) return;
    this.despachoService.confirmarEntrega(d.id).subscribe({
      next: () => {
        this.showToast(`Entrega confirmada: ${d.codigo}`);
        this.cargar();
      },
      error: (err) => this.showToast(err.error?.message || 'Falta validar comprobante', 'error'),
    });
  }

  confirmarEliminar() {
    if (!this.deleteTarget?.id) return;
    this.despachoService.eliminar(this.deleteTarget.id).subscribe(() => {
      this.despachos = this.despachos.filter((d) => d !== this.deleteTarget);
      this.cdr.detectChanges();
      this.showToast('Despacho eliminado');
      this.closeModal();
    });
  }

  showToast(msg: string, type: 'success' | 'error' = 'success') {
    this.toastMsg = msg;
    this.toastType = type;
    this.toastVisible = true;
    setTimeout(() => (this.toastVisible = false), 3000);
  }
}
