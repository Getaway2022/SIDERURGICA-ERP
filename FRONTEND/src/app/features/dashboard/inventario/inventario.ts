// src/app/features/dashboard/inventario/inventario.ts
import { Component, OnInit, ChangeDetectorRef, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormsModule,
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { InventarioService, Inventario as InventarioBD } from './services/inventario.service';
import { AuthService } from '../../../core/auth/services/auth.service';
import { CustomValidators } from '../../../shared/validators/custom-validators';

@Component({
  selector: 'app-inventario',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './inventario.html',
  styles: [],
  encapsulation: ViewEncapsulation.None,
})
export class InventarioComponent implements OnInit {
  inventarioBD: InventarioBD[] = [];
  alertasBajoStock = 0;
  valorTotalInventario = 0;
  loadingInventario = false;
  inventarioSearch = '';
  inventarioFiltroEstado = '';
  showModal = false;
  modalMode: 'crear' | 'editar' = 'crear';
  inventarioForm: Partial<InventarioBD> = {};
  guardando = false;
  toastMsg = '';
  toastType: 'success' | 'error' | 'info' = 'success';
  toastVisible = false;
  rolActual: string | null = null;

  // ── Reactive Form ────────────────────────────────────────────────────────
  form!: FormGroup;

  constructor(
    private inventarioService: InventarioService,
    private authService: AuthService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.rolActual = this.authService.getRol();
    this.initForm();
    this.cargarInventario();
    this.cargarAlertas();
    this.cargarValorTotal();
  }

  // ── Inicializar formulario ───────────────────────────────────────────────
  initForm() {
    this.form = this.fb.group({
      producto: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      categoria: ['', [Validators.maxLength(60)]],
      unidad: ['unidad', [Validators.required, Validators.maxLength(30)]],
      stock: [0, [Validators.required, Validators.min(0)]],
      stockMinimo: [10, [Validators.required, Validators.min(0)]],
      precioUnitario: [0, [Validators.required, Validators.min(0.01)]],
    });
  }

  // ── Helpers de validación ────────────────────────────────────────────────
  isInvalid(campo: string): boolean {
    return CustomValidators.showError(this.form.get(campo));
  }

  errorMsg(campo: string, label: string): string {
    const control = this.form.get(campo);
    if (!control || !control.errors || !(control.touched || control.dirty)) return '';
    return CustomValidators.getErrorMessage(control, label);
  }

  // ── Carga de datos ───────────────────────────────────────────────────────
  cargarInventario() {
    this.loadingInventario = true;
    this.inventarioService.listarInventario().subscribe({
      next: (data) => {
        this.inventarioBD = data;
        this.loadingInventario = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingInventario = false;
        this.showToast('Error al cargar inventario', 'error');
      },
    });
  }

  cargarAlertas() {
    this.inventarioService.listarBajoStock().subscribe({
      next: (data) => {
        this.alertasBajoStock = data.total;
        this.cdr.detectChanges();
      },
      error: () => {},
    });
  }

  cargarValorTotal() {
    this.inventarioService.obtenerValorTotal().subscribe({
      next: (data) => {
        this.valorTotalInventario = data.valorTotal;
        this.cdr.detectChanges();
      },
      error: () => {},
    });
  }

  puedeRegistrar(): boolean {
    return this.rolActual === 'ADMIN' || this.rolActual === 'ALMACEN';
  }

  // ── Modal ────────────────────────────────────────────────────────────────
  openModalCrear() {
    this.modalMode = 'crear';
    this.form.reset({ stock: 0, stockMinimo: 10, precioUnitario: 0, unidad: 'unidad' });
    this.inventarioForm = {};
    this.showModal = true;
  }

  openModalEditar(p: InventarioBD) {
    this.modalMode = 'editar';
    this.inventarioForm = { ...p };
    this.form.patchValue({
      producto: p.producto,
      categoria: p.categoria || '',
      unidad: p.unidad || 'unidad',
      stock: p.stock,
      stockMinimo: p.stockMinimo,
      precioUnitario: p.precioUnitario || 0,
    });
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.inventarioForm = {};
  }

  // ── Guardar ──────────────────────────────────────────────────────────────
  guardarInventario() {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.showToast('Corrija los errores del formulario antes de continuar.', 'error');
      return;
    }
    this.guardando = true;
    const val = this.form.value;
    const item: InventarioBD = {
      producto: val.producto,
      categoria: val.categoria || undefined,
      unidad: val.unidad,
      stock: Number(val.stock),
      stockMinimo: Number(val.stockMinimo),
      precioUnitario: Number(val.precioUnitario),
    };

    if (this.modalMode === 'crear') {
      this.inventarioService.crearProducto(item).subscribe({
        next: () => {
          this.guardando = false;
          this.closeModal();
          this.showToast('✅ Producto agregado correctamente', 'success');
          this.cargarInventario();
          this.cargarAlertas();
          this.cargarValorTotal();
        },
        error: () => {
          this.guardando = false;
          this.showToast('Error al agregar producto', 'error');
        },
      });
    } else {
      this.inventarioService.actualizarProducto(this.inventarioForm.id!, item).subscribe({
        next: () => {
          this.guardando = false;
          this.closeModal();
          this.showToast('✅ Producto actualizado correctamente', 'success');
          this.cargarInventario();
          this.cargarAlertas();
          this.cargarValorTotal();
        },
        error: () => {
          this.guardando = false;
          this.showToast('Error al actualizar producto', 'error');
        },
      });
    }
  }

  // ── Filtros ──────────────────────────────────────────────────────────────
  get inventarioBDFiltrado(): InventarioBD[] {
    return this.inventarioBD.filter((p) => {
      const search = this.inventarioSearch.toLowerCase();
      const matchSearch =
        !this.inventarioSearch ||
        p.producto?.toLowerCase().includes(search) ||
        p.categoria?.toLowerCase().includes(search);
      const estado = this.getEstadoInventario(p);
      const matchEstado = !this.inventarioFiltroEstado || estado === this.inventarioFiltroEstado;
      return matchSearch && matchEstado;
    });
  }

  getEstadoInventario(p: InventarioBD): string {
    if (p.stock === 0) return 'Sin stock';
    if (p.stock <= (p.stockMinimo || 0)) return 'Bajo stock';
    return 'Normal';
  }

  formatMoney(n: number): string {
    return 'S/ ' + Number(n).toLocaleString('es-PE', { minimumFractionDigits: 2 });
  }

  getBadgeClass(estado: string): string {
    const map: Record<string, string> = {
      Normal: 'badge-green',
      'Bajo stock': 'badge-orange',
      'Sin stock': 'badge-red',
    };
    return map[estado] || 'badge-yellow';
  }

  showToast(msg: string, type: 'success' | 'error' | 'info' = 'success') {
    this.toastMsg = msg;
    this.toastType = type;
    this.toastVisible = true;
    setTimeout(() => {
      this.toastVisible = false;
      this.cdr.detectChanges();
    }, 4000);
  }
}
