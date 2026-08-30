// src/app/features/dashboard/pedidos/pedidos.ts
import { Component, OnInit, ChangeDetectorRef, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormsModule,
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { PedidoService, Pedido } from './services/pedido.service';
import { InventarioService, Inventario } from '../inventario/services/inventario.service';
import { AuthService } from '../../../core/auth/services/auth.service';
import { HttpClient } from '@angular/common/http';
import { CustomValidators } from '../../../shared/validators/custom-validators';
import jsPDF from 'jspdf';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-pedidos',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './pedidos.html',
  styles: [],
  encapsulation: ViewEncapsulation.None,
})
export class PedidosComponent implements OnInit {
  private readonly API = environment.apiUrl;

  pedidos: Pedido[] = [];
  pedidoSearch = '';
  pedidoFiltroEstado = '';
  loadingPedidos = false;
  loadingAction = false;
  showModal = false;
  modalMode: 'crear' | 'ver' = 'crear';
  pedidoForm: Partial<Pedido> = {};
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

  constructor(
    private pedidoService: PedidoService,
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
    this.cargarPedidos();
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

  cargarPedidos() {
    this.loadingPedidos = true;
    this.pedidoService.listarPedidos().subscribe({
      next: (data) => {
        this.pedidos = data;
        this.loadingPedidos = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingPedidos = false;
        this.showToast('Error al cargar pedidos', 'error');
      },
    });
  }

  puedeRegistrar(): boolean {
    return this.rolActual === 'ADMIN' || this.rolActual === 'VENTAS';
  }

  openModalCrear() {
    this.modalMode = 'crear';
    this.form.reset({ cantidad: 1, precio_unitario: 0 });
    this.pedidoForm = {};
    this.productoSearch = '';
    this.productoSeleccionado = null;
    this.showDropdownProducto = false;
    this.showModal = true;
  }

  openModalVer(p: Pedido) {
    this.modalMode = 'ver';
    this.pedidoForm = { ...p };
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.showDropdownProducto = false;
  }

  guardarPedido() {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.showToast('Corrija los errores del formulario antes de continuar.', 'error');
      return;
    }
    this.guardando = true;
    const val = this.form.value;
    const nuevo = {
      cliente: val.cliente,
      ruc: val.ruc || '',
      producto: val.producto,
      cantidad: Number(val.cantidad),
      precio_unitario: Number(val.precio_unitario),
      usuario_id: this.usuarioActual?.id || null,
    };
    this.pedidoService.registrarPedido(nuevo).subscribe({
      next: () => {
        this.guardando = false;
        this.closeModal();
        this.showToast('✅ Pedido registrado exitosamente', 'success');
        this.cargarPedidos();
        this.cargarInventario();
      },
      error: () => {
        this.guardando = false;
        this.showToast('Error al registrar pedido', 'error');
      },
    });
  }

  // CN-P03 + DEF-09 — stock insuficiente y alerta bajo stock con mensajes claros
  cambiarEstado(p: Pedido, estado: string) {
    this.loadingAction = true;
    this.http.patch<any>(`${this.API}/pedidos/${p.id}/estado`, { estado }).subscribe({
      next: (res) => {
        const pedidoActualizado: Pedido = res.pedido || res;
        if (res.warning) {
          // Producto no encontrado en inventario
          this.showToast(`Pedido ${pedidoActualizado.codigo} ${estado}. ⚠️ ${res.warning}`, 'info');
        } else if (estado === 'APROBADO') {
          // DEF-09 — alerta de bajo stock visible claramente
          if (res.bajoPstock) {
            this.showToast(
              `✅ Pedido ${pedidoActualizado.codigo} aprobado. ⚠️ Stock de "${p.producto}" quedó por debajo del mínimo.`,
              'info',
            );
          } else {
            this.showToast(
              `✅ Pedido ${pedidoActualizado.codigo} aprobado. Stock actualizado correctamente.`,
              'success',
            );
          }
        } else {
          this.showToast(`Pedido ${pedidoActualizado.codigo} → ${estado}`);
        }
        this.cargarPedidos();
        this.cargarInventario();
        this.loadingAction = false;
      },
      error: (err) => {
        // CN-P03 — ahora el backend retorna 400 con mensaje legible
        const msg = err.error?.error || 'Error al cambiar el estado del pedido.';
        this.showToast(`❌ ${msg}`, 'error');
        this.cargarPedidos();
        this.loadingAction = false;
        this.cdr.detectChanges();
      },
    });
  }

  generarVenta(p: Pedido) {
    if (
      !confirm(
        `¿Generar venta para el pedido ${p.codigo}?\nCliente: ${p.cliente}\nProducto: ${p.producto} × ${p.cantidad}`,
      )
    )
      return;
    this.loadingAction = true;
    this.http.post<any>(`${this.API}/ventas/desde-pedido/${p.id}`, {}).subscribe({
      next: (venta) => {
        this.showToast(
          `✅ Venta ${venta.codigo} generada. Total: S/ ${Number(venta.total).toFixed(2)}`,
          'success',
        );
        this.cargarPedidos();
        this.loadingAction = false;
      },
      error: (err) => {
        this.showToast(err.error?.error || 'Error al generar la venta', 'error');
        this.loadingAction = false;
        this.cdr.detectChanges();
      },
    });
  }

  get pedidosFiltrados(): Pedido[] {
    return this.pedidos.filter((p) => {
      const search = this.pedidoSearch.toLowerCase();
      const matchSearch =
        !this.pedidoSearch ||
        p.codigo?.toLowerCase().includes(search) ||
        p.cliente?.toLowerCase().includes(search) ||
        p.producto?.toLowerCase().includes(search) ||
        p.ruc?.toLowerCase().includes(search);
      const matchEstado = !this.pedidoFiltroEstado || p.estado === this.pedidoFiltroEstado;
      return matchSearch && matchEstado;
    });
  }

  getPedidosPendientes(): number {
    return this.pedidos.filter((p) => p.estado === 'PENDIENTE').length;
  }
  getTotalPedidos(): number {
    return this.pedidos.reduce((s, p) => s + Number(p.total || 0), 0);
  }
  formatMoney(n: number): string {
    return 'S/ ' + Number(n).toLocaleString('es-PE', { minimumFractionDigits: 2 });
  }

  getBadgeClass(estado: string): string {
    const map: Record<string, string> = {
      PENDIENTE: 'badge-orange',
      APROBADO: 'badge-green',
      RECHAZADO: 'badge-red',
      FACTURADO: 'badge-blue',
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

  generarComprobantePDF(pedido: Partial<Pedido>) {
    const doc = new jsPDF();
    const fecha = pedido.fecha_registro
      ? new Date(pedido.fecha_registro).toLocaleDateString('es-PE')
      : new Date().toLocaleDateString('es-PE');
    const azul = [26, 54, 93] as [number, number, number];
    const grisClaro = [245, 247, 250] as [number, number, number];
    const grisLinea = [220, 220, 220] as [number, number, number];
    const verde = [56, 161, 105] as [number, number, number];
    const negro = [30, 30, 30] as [number, number, number];

    doc.setFillColor(...azul);
    doc.rect(0, 0, 210, 35, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(18);
    doc.setFont('helvetica', 'bold');
    doc.text('SIDERÚRGICA PERÚ S.A.C.', 14, 14);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    doc.text('RUC: 20100012345', 14, 21);
    doc.text('Av. Industrial 1234, Lima - Perú', 14, 27);
    doc.text('Tel: (01) 234-5678  |  ventas@siderurgica.pe', 14, 33);
    doc.setFillColor(255, 255, 255);
    doc.roundedRect(140, 8, 58, 16, 3, 3, 'F');
    doc.setTextColor(...azul);
    doc.setFontSize(10);
    doc.setFont('helvetica', 'bold');
    doc.text('COMPROBANTE DE PAGO', 169, 18, { align: 'center' });
    doc.setFillColor(...grisClaro);
    doc.rect(0, 35, 210, 22, 'F');
    doc.setTextColor(...negro);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'bold');
    doc.text('N° COMPROBANTE:', 14, 44);
    doc.text('FECHA:', 80, 44);
    doc.text('ESTADO:', 150, 44);
    doc.setFont('helvetica', 'normal');
    doc.text(pedido.codigo || '—', 14, 51);
    doc.text(fecha, 80, 51);
    const estado = pedido.estado || 'PENDIENTE';
    const colorEstado =
      estado === 'APROBADO'
        ? verde
        : estado === 'RECHAZADO'
          ? ([229, 62, 62] as [number, number, number])
          : ([221, 107, 32] as [number, number, number]);
    doc.setFillColor(...colorEstado);
    doc.roundedRect(150, 46, 32, 7, 2, 2, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(8);
    doc.setFont('helvetica', 'bold');
    doc.text(estado, 166, 51, { align: 'center' });
    let y = 68;
    doc.setTextColor(...azul);
    doc.setFontSize(10);
    doc.setFont('helvetica', 'bold');
    doc.text('DATOS DEL CLIENTE', 14, y);
    doc.setDrawColor(...grisLinea);
    doc.line(14, y + 2, 196, y + 2);
    doc.setFillColor(...grisClaro);
    doc.rect(14, y + 4, 182, 20, 'F');
    doc.setTextColor(...negro);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'bold');
    doc.text('CLIENTE:', 18, y + 12);
    doc.text('RUC:', 110, y + 12);
    doc.text('VENDEDOR:', 18, y + 19);
    doc.setFont('helvetica', 'normal');
    doc.text(pedido.cliente || '—', 40, y + 12);
    doc.text(pedido.ruc || '—', 122, y + 12);
    doc.text(pedido.vendedor || '—', 44, y + 19);
    y += 32;
    doc.setTextColor(...azul);
    doc.setFontSize(10);
    doc.setFont('helvetica', 'bold');
    doc.text('DETALLE DEL PEDIDO', 14, y);
    doc.line(14, y + 2, 196, y + 2);
    y += 6;
    doc.setFillColor(...azul);
    doc.rect(14, y, 182, 9, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(8);
    doc.setFont('helvetica', 'bold');
    doc.text('DESCRIPCIÓN', 18, y + 6);
    doc.text('CANT.', 120, y + 6, { align: 'center' });
    doc.text('P. UNIT.', 148, y + 6, { align: 'center' });
    doc.text('SUBTOTAL', 185, y + 6, { align: 'right' });
    y += 9;
    doc.setFillColor(255, 255, 255);
    doc.rect(14, y, 182, 12, 'F');
    doc.setDrawColor(...grisLinea);
    doc.rect(14, y, 182, 12);
    doc.setTextColor(...negro);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    doc.text(pedido.producto || '—', 18, y + 8);
    doc.text(String(pedido.cantidad || 0), 120, y + 8, { align: 'center' });
    doc.text(`S/ ${Number(pedido.precio_unitario || 0).toFixed(2)}`, 148, y + 8, {
      align: 'center',
    });
    doc.text(`S/ ${Number(pedido.subtotal || 0).toFixed(2)}`, 185, y + 8, { align: 'right' });
    y += 22;
    doc.setFillColor(...grisClaro);
    doc.rect(110, y, 86, 36, 'F');
    doc.setDrawColor(...grisLinea);
    doc.rect(110, y, 86, 36);
    doc.setTextColor(...negro);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    doc.text('Subtotal:', 115, y + 9);
    doc.text(`S/ ${Number(pedido.subtotal || 0).toFixed(2)}`, 193, y + 9, { align: 'right' });
    doc.text('IGV (18%):', 115, y + 18);
    doc.text(`S/ ${Number(pedido.igv || 0).toFixed(2)}`, 193, y + 18, { align: 'right' });
    doc.line(115, y + 21, 193, y + 21);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(11);
    doc.setTextColor(...azul);
    doc.text('TOTAL:', 115, y + 30);
    doc.text(`S/ ${Number(pedido.total || 0).toFixed(2)}`, 193, y + 30, { align: 'right' });
    doc.setFillColor(...grisClaro);
    doc.rect(0, 272, 210, 25, 'F');
    doc.setTextColor(120, 120, 120);
    doc.setFontSize(8);
    doc.setFont('helvetica', 'normal');
    doc.text('Este comprobante es generado electrónicamente — Siderúrgica Perú S.A.C.', 105, 280, {
      align: 'center',
    });
    doc.text(`Generado el: ${new Date().toLocaleString('es-PE')}`, 105, 292, { align: 'center' });
    doc.save(`Comprobante_${pedido.codigo}.pdf`);
    this.showToast(`Comprobante ${pedido.codigo} generado en PDF`);
  }
}
