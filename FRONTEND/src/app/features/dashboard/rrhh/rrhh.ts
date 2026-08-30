import { Component, OnInit, HostListener, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormsModule,
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import {
  RrhhService,
  Empleado as EmpleadoBackend,
  Asistencia as AsistenciaBackend,
  ResumenIncidencias,
  IncidenciaPersonal,
  TipoIncidenciaPersonal,
  EstadoIncidenciaPersonal,
} from './services/rrhh.service';
import { CustomValidators } from '../../../shared/validators/custom-validators';

export interface Empleado {
  id: string;
  empleadoId: number;
  asistenciaId: number | null;
  iniciales: string;
  color: string;
  nombre: string;
  cargo: string;
  area: string;
  horaEntrada: string;
  tardanza: string;
  asistencia: 'Presente' | 'Tardanza' | 'Ausente' | 'Sin marcar';
  observacion: string;
  horaEntradaEditar?: string;
}

const COLORES = ['#1A365D', '#38A169', '#DD6B20', '#E53E3E', '#805AD5'];

function capitalizarTexto(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

@Component({
  selector: 'app-rrhh',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule], // ← agregado
  templateUrl: './rrhh.html',
  styles: [],
})
export class RrhhComponent implements OnInit {
  empleados: Empleado[] = [];
  cargando = false;
  resumenIncidencias: ResumenIncidencias[] = [];

  fechaHoyTexto = capitalizarTexto(
    new Date().toLocaleDateString('es-PE', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    }),
  );

  horaEntradaEstablecida = '08:00';
  editandoHoraEntrada = false;
  horaEntradaEditTemp = '08:00';

  incidenciasPersonal: IncidenciaPersonal[] = [];
  incidenciaFiltroEstado: EstadoIncidenciaPersonal | '' = '';
  showModalIncidencia = false;
  incidenciaForm: {
    empleadoId: number | null;
    tipo: TipoIncidenciaPersonal;
    fechaInicio: string;
    fechaFin: string;
    descripcion: string;
  } = { empleadoId: null, tipo: 'Permiso', fechaInicio: '', fechaFin: '', descripcion: '' };

  rrhhFiltroArea = '';
  showModal = false;
  modalMode: 'crear' | 'editar' | 'eliminar' = 'crear';
  empleadoForm: Partial<Empleado> = {};
  deleteTarget: Empleado | null = null;
  toastMsg = '';
  toastType: 'success' | 'error' | 'info' = 'success';
  toastVisible = false;
  dropdownAsistenciaAbierto: string | null = null;

  // ── Reactive Form — solo modal crear/editar empleado ──────────────────────
  form!: FormGroup;

  constructor(
    private rrhhService: RrhhService,
    private cdr: ChangeDetectorRef,
    private fb: FormBuilder, // ← agregado
  ) {}

  ngOnInit() {
    this.initForm(); // ← agregado
    this.cargarDatos();
    this.cargarResumenIncidencias();
    this.cargarIncidenciasPersonal();
    this.cargarHoraEntradaGeneral();
  }

  // ── Form init ─────────────────────────────────────────────────────────────
  initForm() {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
      cargo: ['', []],
      area: ['Planta', []],
      horaEntradaEditar: ['', []], // solo visible en modo editar
      observacion: ['', []], // solo visible en modo editar
    });
  }

  // ── Helpers template ──────────────────────────────────────────────────────
  isInvalid(campo: string): boolean {
    return CustomValidators.showError(this.form.get(campo));
  }
  errorMsg(campo: string, label: string): string {
    return CustomValidators.getErrorMessage(this.form.get(campo), label);
  }

  // ── Modales empleado ──────────────────────────────────────────────────────
  openModalCrear() {
    this.modalMode = 'crear';
    this.empleadoForm = {};
    this.form.reset({ area: 'Planta' });
    this.showModal = true;
  }

  openModalEditar(e: Empleado) {
    this.modalMode = 'editar';
    const horaActual = e.horaEntrada !== '—' ? e.horaEntrada : '';
    this.empleadoForm = { ...e, horaEntradaEditar: horaActual };
    this.form.patchValue({
      nombre: e.nombre,
      cargo: e.cargo,
      area: e.area,
      horaEntradaEditar: horaActual,
      observacion: e.observacion !== '—' ? e.observacion : '',
    });
    this.showModal = true;
  }

  pedirEliminar(e: Empleado) {
    this.deleteTarget = e;
    this.modalMode = 'eliminar';
    this.showModal = true;
  }
  closeModal() {
    this.showModal = false;
    this.deleteTarget = null;
  }

  guardarEmpleado() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.value;

    if (this.modalMode === 'crear') {
      this.rrhhService
        .crearEmpleado({
          nombre: v.nombre,
          cargo: v.cargo,
          area: v.area,
        })
        .subscribe({
          next: () => {
            this.showToast('Empleado registrado');
            this.cargarDatos();
            this.closeModal();
            this.cdr.markForCheck();
          },
          error: (err) => {
            this.showToast(err.error?.mensaje || 'Error al registrar empleado', 'error');
            this.cdr.markForCheck();
          },
        });
      return;
    }

    // Modo editar — usa empleadoForm para los ids (asistenciaId, empleadoId)
    this.rrhhService
      .actualizarEmpleado(this.empleadoForm.empleadoId!, {
        nombre: v.nombre,
        cargo: v.cargo,
        area: v.area,
      })
      .subscribe({
        next: () => {
          // reutilizamos empleadoForm para pasar horaEntradaEditar y observacion
          this.empleadoForm.horaEntradaEditar = v.horaEntradaEditar;
          this.empleadoForm.observacion = v.observacion;
          this.guardarHoraEntradaSiAplica();
        },
        error: (err) => {
          this.showToast(err.error?.mensaje || 'Error al actualizar empleado', 'error');
          this.cdr.markForCheck();
        },
      });
  }

  // ── Todo lo demás sin cambios ─────────────────────────────────────────────

  toggleDropdownAsistencia(empleadoId: string, event: MouseEvent) {
    event.stopPropagation();
    this.dropdownAsistenciaAbierto =
      this.dropdownAsistenciaAbierto === empleadoId ? null : empleadoId;
    this.cdr.markForCheck(); // ← ¡Esta es la línea clave que faltaba!
  }

  @HostListener('document:click')
  cerrarDropdownsAlClickFuera() {
    if (this.dropdownAsistenciaAbierto !== null) {
      this.dropdownAsistenciaAbierto = null;
      this.cdr.markForCheck(); // ← También hay que avisar cuando se cierra
    }
  }

  cargarHoraEntradaGeneral() {
    this.rrhhService.obtenerHoraEntradaGeneral().subscribe({
      next: (res) => {
        this.horaEntradaEstablecida = res.horaEntradaGeneral;
        this.cdr.markForCheck();
      },
      error: () => {},
    });
  }

  abrirEdicionHoraEntrada() {
    this.horaEntradaEditTemp = this.horaEntradaEstablecida;
    this.editandoHoraEntrada = true;
  }
  cancelarEdicionHoraEntrada() {
    this.editandoHoraEntrada = false;
  }

  guardarHoraEntradaGeneral() {
    if (!this.horaEntradaEditTemp) {
      this.showToast('Indique una hora válida', 'error');
      return;
    }
    this.rrhhService.actualizarHoraEntradaGeneral(this.horaEntradaEditTemp).subscribe({
      next: () => {
        this.horaEntradaEstablecida = this.horaEntradaEditTemp;
        this.editandoHoraEntrada = false;
        this.showToast('Hora máxima de entrada actualizada para todos los empleados');
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.showToast(err.error?.mensaje || 'Error al actualizar la hora', 'error');
        this.cdr.markForCheck();
      },
    });
  }

  cargarDatos() {
    this.cargando = true;
    this.rrhhService.listarEmpleados().subscribe({
      next: (empleadosBackend) => {
        this.rrhhService.listarAsistenciaDelDia().subscribe({
          next: (asistenciasHoy) => {
            this.empleados = empleadosBackend.map((e, idx) =>
              this.combinar(
                e,
                asistenciasHoy.find((a) => a.empleado.id === e.id),
                idx,
              ),
            );
            this.cargando = false;
            this.cdr.markForCheck();
          },
          error: () => {
            this.empleados = empleadosBackend.map((e, idx) => this.combinar(e, undefined, idx));
            this.cargando = false;
            this.cdr.markForCheck();
          },
        });
      },
      error: () => {
        this.showToast('No se pudo cargar la lista de empleados', 'error');
        this.cargando = false;
        this.cdr.markForCheck();
      },
    });
  }

  private combinar(
    e: EmpleadoBackend,
    asistencia: AsistenciaBackend | undefined,
    idx: number,
  ): Empleado {
    return {
      id: e.codigo || `EMP-${e.id}`,
      empleadoId: e.id!,
      asistenciaId: asistencia?.id ?? null,
      iniciales: (e.nombre || '')
        .split(' ')
        .map((n) => n[0])
        .join('')
        .slice(0, 2)
        .toUpperCase(),
      color: COLORES[idx % COLORES.length],
      nombre: e.nombre,
      cargo: e.cargo || '—',
      area: e.area || '—',
      horaEntrada: asistencia?.horaEntrada || '—',
      tardanza: asistencia?.minutosTardanza ? `${asistencia.minutosTardanza} min` : '—',
      asistencia: asistencia ? asistencia.estado : 'Sin marcar',
      observacion: asistencia?.observacion || '—',
    };
  }

  cargarResumenIncidencias() {
    this.rrhhService.resumenIncidencias().subscribe({
      next: (res) => {
        this.resumenIncidencias = res.resumen;
        this.cdr.markForCheck();
      },
      error: () => {
        this.resumenIncidencias = [];
        this.cdr.markForCheck();
      },
    });
  }

  private guardarHoraEntradaSiAplica() {
    const asistenciaId = this.empleadoForm.asistenciaId;
    const empleadoId = this.empleadoForm.empleadoId!;
    const hora = this.empleadoForm.horaEntradaEditar;
    const obs = this.empleadoForm.observacion;

    // Si no hay hora, simplemente cierra (es como un Ausente sin marcar)
    if (!hora && !obs) {
      this.showToast('Empleado actualizado');
      this.cargarDatos();
      this.closeModal();
      this.cdr.markForCheck();
      return;
    }

    const cb = {
      next: () => {
        this.showToast('Empleado y asistencia actualizados');
        this.cargarDatos();
        this.closeModal();
        this.cdr.markForCheck();
      },
      error: (err: any) => {
        this.showToast(err.error?.mensaje || 'Error al actualizar la asistencia', 'error');
        this.cdr.markForCheck();
      },
    };

    // Si ya existía un registro de asistencia hoy, lo corregimos
    if (asistenciaId) {
      this.rrhhService.corregirHoraEntrada(asistenciaId, hora, obs).subscribe(cb);
    }
    // Si no existía, lo creamos registrando la asistencia
    else {
      this.rrhhService.registrarAsistencia(empleadoId, hora, obs).subscribe(cb);
    }
  }

  confirmarEliminar() {
    if (!this.deleteTarget) return;
    this.rrhhService.eliminarEmpleado(this.deleteTarget.empleadoId).subscribe({
      next: () => {
        this.showToast('Empleado eliminado');
        this.cargarDatos();
        this.closeModal();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.showToast(err.error?.mensaje || 'Error al eliminar', 'error');
        this.cdr.markForCheck();
      },
    });
  }

  marcarEntrada(emp: Empleado) {
    const horaActual = new Date().toTimeString().slice(0, 5);
    this.marcarHoraEntrada(emp, horaActual);
  }

  cambiarAsistencia(emp: Empleado, estado: 'Ausente') {
    this.marcarHoraEntrada(emp, undefined);
  }

  empleadosActualizandoAsistencia = new Set<number>();

  private marcarHoraEntrada(emp: Empleado, horaEntrada?: string) {
    this.dropdownAsistenciaAbierto = null;
    this.empleadosActualizandoAsistencia.add(emp.empleadoId);
    this.cdr.markForCheck();
    const cb = {
      next: (res: any) => {
        this.showToast(`${emp.nombre} → ${res.asistencia.estado}`);
        this.actualizarFilaLocal(emp, res.asistencia);
        this.cargarResumenIncidencias();
        this.empleadosActualizandoAsistencia.delete(emp.empleadoId);
        this.cdr.markForCheck();
      },
      error: (err: any) => {
        this.showToast(err.error?.mensaje || 'Error al actualizar asistencia', 'error');
        this.empleadosActualizandoAsistencia.delete(emp.empleadoId);
        this.cdr.markForCheck();
      },
    };
    if (emp.asistenciaId) {
      this.rrhhService.corregirHoraEntrada(emp.asistenciaId, horaEntrada).subscribe(cb);
    } else {
      this.rrhhService.registrarAsistencia(emp.empleadoId, horaEntrada).subscribe(cb);
    }
  }

  private actualizarFilaLocal(emp: Empleado, a: AsistenciaBackend) {
    const idx = this.empleados.findIndex((e) => e.empleadoId === emp.empleadoId);
    if (idx === -1) return;
    this.empleados[idx] = {
      ...this.empleados[idx],
      asistenciaId: a.id ?? null,
      horaEntrada: a.horaEntrada || '—',
      tardanza: a.minutosTardanza ? `${a.minutosTardanza} min` : '—',
      asistencia: a.estado,
      observacion: a.observacion || '—',
    };
    this.empleados = [...this.empleados];
  }

  areasUnicas(): string[] {
    return [...new Set(this.empleados.map((e) => e.area))];
  }
  get empleadosFiltrados(): Empleado[] {
    return this.empleados.filter((e) => !this.rrhhFiltroArea || e.area === this.rrhhFiltroArea);
  }
  estaActualizandoAsistencia(empleadoId: number): boolean {
    return this.empleadosActualizandoAsistencia.has(empleadoId);
  }

  getBadgeClass(estado: string): string {
    const map: Record<string, string> = {
      Presente: 'badge-green',
      Tardanza: 'badge-yellow',
      Ausente: 'badge-red',
      'Sin marcar': 'badge-gray',
    };
    return map[estado] || 'badge-gray';
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

  cargarIncidenciasPersonal(nuevoFiltro?: EstadoIncidenciaPersonal | '') {
    if (nuevoFiltro !== undefined) this.incidenciaFiltroEstado = nuevoFiltro;
    const estado = this.incidenciaFiltroEstado || undefined;
    this.rrhhService.listarIncidenciasPersonal(estado).subscribe({
      next: (lista) => {
        this.incidenciasPersonal = lista;
        this.cdr.markForCheck();
      },
      error: () => {
        this.incidenciasPersonal = [];
        this.showToast('No se pudieron cargar las incidencias', 'error');
        this.cdr.markForCheck();
      },
    });
  }

  openModalIncidencia() {
    this.incidenciaForm = {
      empleadoId: null,
      tipo: 'Permiso',
      fechaInicio: '',
      fechaFin: '',
      descripcion: '',
    };
    this.showModalIncidencia = true;
  }
  closeModalIncidencia() {
    this.showModalIncidencia = false;
  }

  guardarIncidencia() {
    const f = this.incidenciaForm;
    if (!f.empleadoId || !f.fechaInicio || !f.descripcion.trim()) {
      this.showToast('Complete empleado, fecha de inicio y descripción', 'error');
      return;
    }
    this.rrhhService
      .registrarIncidenciaPersonal({
        empleadoId: f.empleadoId,
        tipo: f.tipo,
        fechaInicio: f.fechaInicio,
        fechaFin: f.fechaFin || undefined,
        descripcion: f.descripcion.trim(),
      })
      .subscribe({
        next: (res) => {
          this.showToast('Incidencia registrada');
          if (
            !this.incidenciaFiltroEstado ||
            res.incidencia.estado === this.incidenciaFiltroEstado
          ) {
            this.incidenciasPersonal = [res.incidencia, ...this.incidenciasPersonal];
          }
          this.closeModalIncidencia();
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.showToast(err.error?.mensaje || 'Error al registrar la incidencia', 'error');
          this.cdr.markForCheck();
        },
      });
  }

  showModalRechazo = false;
  incidenciaARechazar: IncidenciaPersonal | null = null;
  motivoRechazo = '';

  resolverIncidencia(inc: IncidenciaPersonal, estado: 'Aprobado' | 'Rechazado') {
    if (estado === 'Rechazado') {
      this.incidenciaARechazar = inc;
      this.motivoRechazo = '';
      this.showModalRechazo = true;
      return;
    }
    this.rrhhService.resolverIncidenciaPersonal(inc.id!, 'Aprobado').subscribe({
      next: (res) => {
        this.showToast('Incidencia aprobada');
        this.actualizarIncidenciaLocal(res.incidencia);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.showToast(err.error?.mensaje || 'Error al aprobar la incidencia', 'error');
        this.cdr.markForCheck();
      },
    });
  }

  confirmarRechazo() {
    if (!this.incidenciaARechazar) return;
    this.rrhhService
      .resolverIncidenciaPersonal(
        this.incidenciaARechazar.id!,
        'Rechazado',
        this.motivoRechazo.trim() || undefined,
      )
      .subscribe({
        next: (res) => {
          this.showToast('Incidencia rechazada');
          this.actualizarIncidenciaLocal(res.incidencia);
          this.cerrarModalRechazo();
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.showToast(err.error?.mensaje || 'Error al rechazar la incidencia', 'error');
          this.cdr.markForCheck();
        },
      });
  }

  private actualizarIncidenciaLocal(incidenciaActualizada: IncidenciaPersonal) {
    if (
      this.incidenciaFiltroEstado &&
      incidenciaActualizada.estado !== this.incidenciaFiltroEstado
    ) {
      this.incidenciasPersonal = this.incidenciasPersonal.filter(
        (i) => i.id !== incidenciaActualizada.id,
      );
      return;
    }
    this.incidenciasPersonal = this.incidenciasPersonal.map((i) =>
      i.id === incidenciaActualizada.id ? incidenciaActualizada : i,
    );
  }

  cerrarModalRechazo() {
    this.showModalRechazo = false;
    this.incidenciaARechazar = null;
  }

  getEstadoIncidenciaClass(estado: string): string {
    const map: Record<string, string> = {
      Pendiente: 'badge-yellow',
      Aprobado: 'badge-green',
      Rechazado: 'badge-red',
    };
    return map[estado] || 'badge-gray';
  }
}
