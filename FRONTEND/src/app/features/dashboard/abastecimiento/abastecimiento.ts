import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormsModule,
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import {
  AbastecimientoService,
  OrdenCompra,
  Presupuesto,
  RegistrarCompraResultado,
} from './services/abastecimiento.service';
import { ProveedorService, Proveedor } from '../proveedores/services/proveedor.service';
import { InventarioService, Inventario } from '../inventario/services/inventario.service';
import { CustomValidators } from '../../../shared/validators/custom-validators';

// OrdenCompra ya NO se define aquí — viene del service ↑

@Component({
  selector: 'app-abastecimiento',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './abastecimiento.html',
  styles: [],
})
export class AbastecimientoComponent implements OnInit {
  ordenes: OrdenCompra[] = [];
  proveedores: Proveedor[] = [];
  inventarios: Inventario[] = [];
  presupuesto: Presupuesto | null = null;

  abastSearch = '';
  abastFiltroEstado = '';
  showModal = false;
  modalMode: 'crear' | 'editar' | 'eliminar' | 'compra' = 'crear';
  ordenForm: Partial<OrdenCompra> = {};
  compraForm: { inventarioId?: number; cantidad?: number; precioUnitario?: number } = {};
  deleteTarget: OrdenCompra | null = null;
  ordenActivaCompra: OrdenCompra | null = null;
  toastMsg = '';
  toastType: 'success' | 'error' | 'info' = 'success';
  toastVisible = false;
  mostrarSugerenciaOrden = false;
  showModalResultado = false;
  resultadoCompra: RegistrarCompraResultado | null = null;

  form!: FormGroup;

  constructor(
    private abastService: AbastecimientoService,
    private proveedorService: ProveedorService,
    private inventarioService: InventarioService,
    private cdr: ChangeDetectorRef,
    private fb: FormBuilder,
  ) {}

  ngOnInit() {
    this.initForm();
    this.proveedorService.listar().subscribe((p) => {
      this.proveedores = p;
      this.cdr.detectChanges();
      this.cargarOrdenes();
    });
    this.cargarPresupuesto();
    this.cargarInventarios();
  }

  initForm() {
    this.form = this.fb.group({
      proveedorId: [null, Validators.required],
      producto: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      cantidad: ['', []],
      total: [null, [Validators.required, Validators.min(0.01)]],
      fechaEntrega: ['', []],
    });
  }

  isInvalid(campo: string): boolean {
    return CustomValidators.showError(this.form.get(campo));
  }
  errorMsg(campo: string, label: string): string {
    return CustomValidators.getErrorMessage(this.form.get(campo), label);
  }

  cargarOrdenes() {
    this.abastService.listarOrdenes().subscribe((o) => {
      this.ordenes = o;
      this.cdr.detectChanges();
    });
  }

  cargarInventarios() {
    this.inventarioService.listarInventario().subscribe((i) => {
      this.inventarios = i;
      this.cdr.detectChanges();
    });
  }

  cargarPresupuesto() {
    this.abastService.obtenerPresupuesto().subscribe((p) => {
      this.presupuesto = p;
      this.cdr.detectChanges();
    });
  }

  nombreProveedor(id: number): string {
    return this.proveedores.find((p) => p.id === id)?.nombre || '—';
  }

  openModalCrear() {
    this.modalMode = 'crear';
    this.form.reset();
    this.ordenForm = { estado: 'Pendiente' };
    this.showModal = true;
  }

  openModalEditar(o: OrdenCompra) {
    this.modalMode = 'editar';
    this.ordenForm = { ...o };
    this.form.patchValue({
      proveedorId: o.proveedorId,
      producto: o.producto,
      cantidad: o.cantidad,
      total: o.total,
      fechaEntrega: o.fechaEntrega,
    });
    this.showModal = true;
  }

  pedirEliminar(o: OrdenCompra) {
    this.deleteTarget = o;
    this.modalMode = 'eliminar';
    this.showModal = true;
  }

  abrirRegistrarCompra(o: OrdenCompra) {
    this.ordenActivaCompra = o;
    this.compraForm = {};
    this.mostrarSugerenciaOrden = false;
    this.modalMode = 'compra';
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.deleteTarget = null;
    this.ordenActivaCompra = null;
    this.mostrarSugerenciaOrden = false;
  }

  toggleSugerenciaOrden() {
    this.mostrarSugerenciaOrden = !this.mostrarSugerenciaOrden;
  }

  cantidadSugerida(): number | null {
    const texto = this.ordenActivaCompra?.cantidad;
    if (!texto) return null;
    const match = texto.match(/[\d.]+/);
    return match ? parseFloat(match[0]) : null;
  }

  precioUnitarioSugerido(): number | null {
    const cantidad = this.cantidadSugerida();
    const total = this.ordenActivaCompra?.total;
    if (!cantidad || !total) return null;
    return Math.round((total / cantidad) * 100) / 100;
  }

  usarDatosDeOrden() {
    const cantidad = this.cantidadSugerida();
    const precio = this.precioUnitarioSugerido();
    if (cantidad != null) this.compraForm.cantidad = cantidad;
    if (precio != null) this.compraForm.precioUnitario = precio;
    this.mostrarSugerenciaOrden = false;
  }

  guardarOrden() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const data = { ...this.ordenForm, ...this.form.value } as OrdenCompra;
    if (this.modalMode === 'crear') {
      this.abastService.crearOrden(data).subscribe({
        next: () => {
          this.showToast('Orden de compra creada');
          this.cargarOrdenes();
          this.cargarPresupuesto();
          this.closeModal();
        },
        error: (err) => this.showToast(err.error?.message || 'Capital insuficiente', 'error'),
      });
    } else {
      const idx = this.ordenes.findIndex((o) => o.id === this.ordenForm.id);
      if (idx !== -1) this.ordenes[idx] = { ...this.ordenes[idx], ...data };
      this.showToast('Orden actualizada');
      this.closeModal();
    }
  }

  registrarCompra() {
    if (!this.ordenActivaCompra || !this.compraForm.cantidad || !this.compraForm.precioUnitario) {
      this.showToast('Complete cantidad y precio unitario', 'error');
      return;
    }
    const o = this.ordenActivaCompra;
    this.abastService
      .registrarCompra({
        ordenCompraId: o.id,
        proveedorId: o.proveedorId,
        inventarioId: this.compraForm.inventarioId,
        producto: o.producto,
        cantidad: this.compraForm.cantidad,
        precioUnitario: this.compraForm.precioUnitario,
        total: this.compraForm.cantidad * this.compraForm.precioUnitario,
      })
      .subscribe({
        next: (resultado) => {
          this.closeModal();
          this.cargarInventarios();
          this.resultadoCompra = resultado;
          this.showModalResultado = true;
          this.cdr.markForCheck();
        },
        error: (err) => this.showToast(err.error?.message || 'Error al registrar compra', 'error'),
      });
  }

  cerrarModalResultado() {
    this.showModalResultado = false;
    this.resultadoCompra = null;
  }

  confirmarEliminar() {
    if (!this.deleteTarget?.id) return;
    this.abastService.eliminarOrden(this.deleteTarget.id).subscribe(() => {
      this.cargarOrdenes();
      this.cargarPresupuesto();
      this.showToast('Orden eliminada');
      this.closeModal();
    });
  }

  cambiarEstado(o: OrdenCompra, estado: string) {
    if (!o.id) return;
    this.abastService.cambiarEstadoOrden(o.id, estado).subscribe({
      next: () => {
        o.estado = estado as OrdenCompra['estado'];
        o._open = false;
        this.showToast(`Orden ${o.codigo} → ${estado}`);
        this.cargarOrdenes();
        this.cargarPresupuesto();
      },
      error: (err) => this.showToast(err.error?.message || 'Error al cambiar estado', 'error'),
    });
  }

  get ordenesFiltradas(): OrdenCompra[] {
    return this.ordenes.filter((o) => {
      const matchSearch =
        !this.abastSearch ||
        this.nombreProveedor(o.proveedorId)
          .toLowerCase()
          .includes(this.abastSearch.toLowerCase()) ||
        (o.codigo || '').toLowerCase().includes(this.abastSearch.toLowerCase());
      return matchSearch && (!this.abastFiltroEstado || o.estado === this.abastFiltroEstado);
    });
  }

  formatMoney(n: number): string {
    return 'S/ ' + (n || 0).toLocaleString('es-PE');
  }

  getBadgeClass(estado: string): string {
    const map: Record<string, string> = {
      Aprobada: 'badge-green',
      Pendiente: 'badge-orange',
      'En tránsito': 'badge-blue',
      Rechazada: 'badge-red',
    };
    return map[estado] || 'badge-yellow';
  }

  showToast(msg: string, type: 'success' | 'error' | 'info' = 'success') {
    this.toastMsg = msg;
    this.toastType = type;
    this.toastVisible = true;
    this.cdr.markForCheck();
    setTimeout(() => {
      this.toastVisible = false;
      this.cdr.markForCheck();
    }, 3000);
  }
}
