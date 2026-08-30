import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormsModule,
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { ProveedorService, Proveedor } from './services/proveedor.service';
import { CustomValidators } from '../../../shared/validators/custom-validators';

@Component({
  selector: 'app-proveedores',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './proveedores.html',
  styles: [],
})
export class ProveedoresComponent implements OnInit {
  proveedores: Proveedor[] = [];
  provSearch = '';
  provFiltroEstado = '';
  showModal = false;
  modalMode: 'crear' | 'editar' | 'ver' | 'eliminar' = 'crear';
  proveedorForm: Partial<Proveedor> = {}; // sigue usándose para 'ver' y 'eliminar'
  deleteTarget: Proveedor | null = null;
  toastMsg = '';
  toastType: 'success' | 'error' | 'info' = 'success';
  toastVisible = false;

  // ── Reactive Form (solo para crear/editar) ──────────────────────────────
  form!: FormGroup;

  constructor(
    private proveedorService: ProveedorService,
    private cdr: ChangeDetectorRef,
    private fb: FormBuilder,
  ) {}

  ngOnInit() {
    this.initForm();
    this.cargar();
  }

  initForm() {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      ruc: [
        '',
        [
          Validators.required,
          Validators.pattern(CustomValidators.REGEX.ruc), // exactamente 11 dígitos
          CustomValidators.noSameDigits(),
        ],
      ],
      origen: ['', []],
      contacto: ['', []],
      telefono: [
        '',
        [
          Validators.pattern(CustomValidators.REGEX.telefono), // 9 dígitos
          CustomValidators.noSameDigits(),
        ],
      ],
      categoria: ['', []],
      calificacion: [4.0, [Validators.required, Validators.min(1), Validators.max(5)]],
      estado: ['Activo', Validators.required],
    });
  }

  // ── Helpers para el template ────────────────────────────────────────────
  isInvalid(campo: string): boolean {
    return CustomValidators.showError(this.form.get(campo));
  }

  errorMsg(campo: string, label: string): string {
    return CustomValidators.getErrorMessage(this.form.get(campo), label);
  }

  soloNumeros(event: Event, campo: string) {
    const input = event.target as HTMLInputElement;
    input.value = input.value.replace(/[^0-9]/g, '');
    this.form.get(campo)?.setValue(input.value, { emitEvent: true });
  }

  // ── Lógica existente (sin cambios) ─────────────────────────────────────
  cargar() {
    this.proveedorService.listar().subscribe({
      next: (p) => {
        this.proveedores = p;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('ERROR proveedores:', err),
    });
  }

  get proveedoresFiltrados(): Proveedor[] {
    return this.proveedores.filter((p) => {
      const matchSearch =
        !this.provSearch ||
        (p.nombre || '').toLowerCase().includes(this.provSearch.toLowerCase()) ||
        (p.ruc || '').toLowerCase().includes(this.provSearch.toLowerCase());
      return matchSearch && (!this.provFiltroEstado || p.estado === this.provFiltroEstado);
    });
  }

  getBadgeClass(estado: string): string {
    const map: Record<string, string> = { Activo: 'badge-green', Inactivo: 'badge-red' };
    return map[estado] || 'badge-yellow';
  }

  openModalCrear() {
    this.modalMode = 'crear';
    this.form.reset({ estado: 'Activo', calificacion: 4.0 });
    this.showModal = true;
  }

  openModalEditar(p: any) {
    this.modalMode = 'editar';
    this.proveedorForm = { ...p };
    this.form.patchValue({
      nombre: p.nombre,
      ruc: p.ruc,
      origen: p.origen,
      contacto: p.contacto,
      telefono: p.telefono,
      categoria: p.categoria,
      calificacion: p.calificacion,
      estado: p.estado,
    });
    this.showModal = true;
  }

  openModalVer(p: Proveedor) {
    this.modalMode = 'ver';
    this.proveedorForm = { ...p };
    this.showModal = true;
  }
  pedirEliminar(p: Proveedor) {
    this.modalMode = 'eliminar';
    this.deleteTarget = p;
    this.showModal = true;
  }
  closeModal() {
    this.showModal = false;
    this.deleteTarget = null;
  }

  guardarProveedor() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const data = { ...this.proveedorForm, ...this.form.value } as Proveedor;

    if (this.modalMode === 'crear') {
      this.proveedorService.crear(data).subscribe({
        next: () => {
          this.showToast('Proveedor agregado');
          this.cargar();
          this.closeModal();
        },
        error: (err) => this.showToast(err.error?.message || 'Error al guardar', 'error'),
      });
    } else {
      this.proveedorService.actualizar(this.proveedorForm.id!, data).subscribe({
        next: () => {
          this.showToast('Proveedor actualizado');
          this.cargar();
          this.closeModal();
        },
        error: () => this.showToast('Error al actualizar', 'error'),
      });
    }
  }

  confirmarEliminar() {
    if (!this.deleteTarget?.id) return;
    this.proveedorService.eliminar(this.deleteTarget.id).subscribe(() => {
      this.showToast('Proveedor eliminado');
      this.cargar();
      this.closeModal();
    });
  }

  showToast(msg: string, type: 'success' | 'error' | 'info' = 'success') {
    this.toastMsg = msg;
    this.toastType = type;
    this.toastVisible = true;
    setTimeout(() => (this.toastVisible = false), 3000);
  }
}
