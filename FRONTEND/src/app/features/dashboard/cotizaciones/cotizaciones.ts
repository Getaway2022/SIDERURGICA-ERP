// src/app/features/dashboard/cotizaciones/cotizaciones.ts
import { Component, OnInit, ChangeDetectorRef, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormsModule,
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { CotizacionService, Cotizacion } from './services/cotizacion.service';
import { InventarioService, Inventario } from '../inventario/services/inventario.service';
import { AuthService } from '../../../core/auth/services/auth.service';
import { CustomValidators } from '../../../shared/validators/custom-validators';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-cotizaciones',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './cotizaciones.html',
  styles: [],
  encapsulation: ViewEncapsulation.None,
})
export class CotizacionesComponent implements OnInit {
  private readonly API = environment.apiUrl;

  cotizaciones: Cotizacion[] = [];
  cotizacionSearch = '';
  cotizacionFiltroEstado = '';
  loadingCotizaciones = false;
  loadingAction = false;
  showModal = false;
  modalMode: 'crear' | 'ver' | 'confirmar' = 'crear';
  cotizacionForm: Partial<Cotizacion> = {};
  guardando = false;
  toastMsg = '';
  toastType: 'success' | 'error' | 'info' = 'success';
  toastVisible = false;
  usuarioActual: any = null;
  rolActual: string | null = null;

  form!: FormGroup;

  productosInventario: Inventario[] = [];
  productosFiltrados: Inventario[] = [];
  productoSearch = '';
  showDropdownProducto = false;
  productoSeleccionado: Inventario | null = null;

  cotizacionGuardada: Cotizacion | null = null;

  constructor(
    private cotizacionService: CotizacionService,
    private inventarioService: InventarioService,
    private authService: AuthService,
    private http: HttpClient,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.usuarioActual = this.authService.getUser();
    this.rolActual = this.authService.getRol();
    this.initForm();
    this.cargarCotizaciones();
    this.cargarInventario();
    document.addEventListener('click', () => {
      this.showDropdownProducto = false;
      this.cdr.detectChanges();
    });
  }

  initForm() {
    this.form = this.fb.group({
      cliente: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      ruc: ['', [Validators.pattern(CustomValidators.REGEX.ruc), CustomValidators.noSameDigits()]],
      producto: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      cantidad: [1, [Validators.required, Validators.min(1)]],
      precio_unitario: [0, [Validators.required, Validators.min(0.01)]],
    });
  }

  isInvalid(campo: string): boolean {
    return CustomValidators.showError(this.form.get(campo));
  }

  errorMsg(campo: string, label: string): string {
    const control = this.form.get(campo);
    if (!control || !control.errors || !(control.touched || control.dirty)) return '';
    if (campo === 'cantidad' && control.errors['max']) {
      return `Stock disponible: ${this.productoSeleccionado?.stock ?? 0} ${this.productoSeleccionado?.unidad ?? 'unidades'}`;
    }
    if (campo === 'cantidad' && control.errors['min']) {
      return 'La cantidad debe ser mayor a 0.';
    }
    return CustomValidators.getErrorMessage(control, label);
  }

  cargarInventario() {
    this.inventarioService.listarInventario().subscribe({
      next: (data) => {
        this.productosInventario = data.filter((p) => p.stock > 0);
        this.productosFiltrados = [...this.productosInventario];
      },
      error: () => {},
    });
  }

  onProductoInput(event: Event) {
    const val = (event.target as HTMLInputElement).value;
    this.productoSearch = val;
    this.form.get('producto')?.setValue(val, { emitEvent: false });
    this.productoSeleccionado = null;
    this.form.get('precio_unitario')?.setValue(0);
    this._actualizarMaxCantidad(null);
    this.productosFiltrados = this.productosInventario.filter(
      (p) =>
        p.producto.toLowerCase().includes(val.toLowerCase()) ||
        (p.categoria || '').toLowerCase().includes(val.toLowerCase()),
    );
    this.showDropdownProducto = this.productosFiltrados.length > 0;
    this.cdr.detectChanges();
  }

  seleccionarProducto(p: Inventario, event: Event) {
    event.stopPropagation();
    this.productoSeleccionado = p;
    this.productoSearch = p.producto;
    this.form.get('producto')?.setValue(p.producto);
    this.form.get('precio_unitario')?.setValue(p.precioUnitario || p.precio_unitario || 0);
    this._actualizarMaxCantidad(p.stock);
    this.showDropdownProducto = false;
    this.cdr.detectChanges();
  }

  onAbrirDropdown(event: Event) {
    event.stopPropagation();
    this.productosFiltrados = this.productosInventario.filter(
      (p) =>
        !this.productoSearch ||
        p.producto.toLowerCase().includes(this.productoSearch.toLowerCase()),
    );
    this.showDropdownProducto = true;
    this.cdr.detectChanges();
  }

  private _actualizarMaxCantidad(maxStock: number | null) {
    const cantidadCtrl = this.form.get('cantidad');
    if (!cantidadCtrl) return;
    if (maxStock !== null) {
      cantidadCtrl.setValidators([
        Validators.required,
        Validators.min(1),
        Validators.max(maxStock),
      ]);
    } else {
      cantidadCtrl.setValidators([Validators.required, Validators.min(1)]);
    }
    cantidadCtrl.updateValueAndValidity();
  }

  cargarCotizaciones() {
    this.loadingCotizaciones = true;
    this.cotizacionService.listarCotizaciones().subscribe({
      next: (data) => {
        this.cotizaciones = data;
        this.loadingCotizaciones = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingCotizaciones = false;
        this.showToast('Error al cargar cotizaciones', 'error');
      },
    });
  }

  puedeRegistrar(): boolean {
    return this.rolActual === 'ADMIN' || this.rolActual === 'VENTAS';
  }

  openModalCrear() {
    this.modalMode = 'crear';
    this.form.reset({ cantidad: 1, precio_unitario: 0 });
    this.cotizacionForm = {};
    this.productoSearch = '';
    this.productoSeleccionado = null;
    this.showDropdownProducto = false;
    this.showModal = true;
  }

  openModalVer(c: Cotizacion) {
    this.modalMode = 'ver';
    this.cotizacionForm = { ...c };
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.cotizacionGuardada = null;
    this.showDropdownProducto = false;
  }

  guardarCotizacion() {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.showToast('Corrija los errores del formulario antes de continuar.', 'error');
      return;
    }
    this.guardando = true;
    const val = this.form.value;
    const nueva = {
      cliente: val.cliente,
      ruc: val.ruc || '',
      producto: val.producto,
      cantidad: Number(val.cantidad),
      precio_unitario: Number(val.precio_unitario),
      usuario_id: this.usuarioActual?.id || null,
    };
    this.cotizacionService.registrarCotizacion(nueva).subscribe({
      next: (res) => {
        this.guardando = false;
        const cotCreada: Cotizacion = res.cotizacion || {
          ...nueva,
          codigo: res.codigo,
          id: res.id,
          estado: 'PENDIENTE',
        };
        this.cotizacionGuardada = cotCreada;
        this.modalMode = 'confirmar';
        this.cargarCotizaciones();
        this.cargarInventario();
      },
      error: () => {
        this.guardando = false;
        this.showToast('Error al registrar cotización', 'error');
      },
    });
  }

  cambiarEstado(c: any, estado: string) {
    this.cotizacionService.actualizarEstado(c.id, estado).subscribe({
      next: () => {
        this.showToast(`Cotización ${c.codigo} → ${estado}`);
        this.cargarCotizaciones();
      },
      error: () => this.showToast('Error al cambiar estado', 'error'),
    });
  }

  // CN-C07 — mensaje claro según el estado de la cotización
  convertirAPedido(c: Cotizacion) {
    if (c.estado === 'RECHAZADA') {
      this.showToast(
        'No se puede convertir: la cotización está RECHAZADA. Debe estar APROBADA.',
        'error',
      );
      return;
    }
    if (c.estado === 'PENDIENTE') {
      this.showToast('La cotización debe estar APROBADA antes de convertirla a pedido.', 'info');
      return;
    }
    if (c.estado === 'CONVERTIDA') {
      this.showToast('Esta cotización ya fue convertida a pedido anteriormente.', 'info');
      return;
    }
    this.loadingAction = true;
    this.http.post<any>(`${this.API}/cotizaciones/${c.id}/convertir-pedido`, {}).subscribe({
      next: (res) => {
        const pedidoCodigo = res.pedido?.codigo || res.codigo || '—';
        this.showToast(`✅ Pedido ${pedidoCodigo} creado desde cotización ${c.codigo}`, 'success');
        this.cargarCotizaciones();
        this.loadingAction = false;
        this.closeModal();
      },
      error: (err) => {
        // Mensaje del backend como fallback por si algo inesperado llega al servidor
        const msg = err.error?.error || 'Error al convertir la cotización.';
        this.showToast(msg, 'error');
        this.loadingAction = false;
        this.cdr.detectChanges();
      },
    });
  }

  convertirDesdeModal() {
    if (this.cotizacionForm?.id) this.convertirAPedido(this.cotizacionForm as Cotizacion);
  }

  convertirGuardada() {
    if (this.cotizacionGuardada?.id) {
      this.convertirAPedido(this.cotizacionGuardada);
    } else {
      this.cargarCotizaciones();
      this.closeModal();
      this.showToast('Cotización registrada. Apruébala para convertir a pedido.', 'info');
    }
  }

  get cotizacionesFiltradas(): Cotizacion[] {
    return this.cotizaciones.filter((c) => {
      const search = this.cotizacionSearch.toLowerCase();
      const matchSearch =
        !this.cotizacionSearch ||
        c.codigo?.toLowerCase().includes(search) ||
        c.cliente?.toLowerCase().includes(search) ||
        c.producto?.toLowerCase().includes(search) ||
        c.ruc?.toLowerCase().includes(search);
      const matchEstado = !this.cotizacionFiltroEstado || c.estado === this.cotizacionFiltroEstado;
      return matchSearch && matchEstado;
    });
  }

  getCotizacionesPendientes(): number {
    return this.cotizaciones.filter((c) => c.estado === 'PENDIENTE').length;
  }
  getTotalCotizado(): number {
    return this.cotizaciones.reduce((s, c) => s + Number(c.total || 0), 0);
  }
  formatMoney(n: number): string {
    return 'S/ ' + Number(n).toLocaleString('es-PE', { minimumFractionDigits: 2 });
  }

  getBadgeClass(estado: string): string {
    const map: Record<string, string> = {
      PENDIENTE: 'badge-orange',
      APROBADA: 'badge-green',
      RECHAZADA: 'badge-red',
      CONVERTIDA: 'badge-blue',
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
