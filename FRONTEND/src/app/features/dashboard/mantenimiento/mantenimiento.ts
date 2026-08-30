import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormsModule,
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { UsuarioService, Usuario } from './services/usuario.service';
import { CustomValidators } from '../../../shared/validators/custom-validators';

@Component({
  selector: 'app-mantenimiento',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './mantenimiento.html',
  styles: [],
})
export class MantenimientoComponent implements OnInit {
  usuariosSistema: Usuario[] = [];
  loadingUsuarios = false;
  showModal = false;
  showModalEliminar = false;
  modalMode: 'crear' | 'editar' = 'crear';
  guardando = false;
  eliminando = false;

  // Usuario activo en edición o eliminación
  private usuarioActivo: Usuario | null = null;

  toastMsg = '';
  toastType: 'success' | 'error' | 'info' = 'success';
  toastVisible = false;

  form!: FormGroup;

  constructor(
    private usuarioService: UsuarioService,
    private cdr: ChangeDetectorRef,
    private fb: FormBuilder,
  ) {}

  ngOnInit() {
    this.initForm();
    this.cargarUsuarios();
  }

  initForm() {
    this.form = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(30)]],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.maxLength(20),
          Validators.pattern(CustomValidators.REGEX.password),
        ],
      ],
      rolId: [1, Validators.required],
    });
  }

  // ── Helpers template ──────────────────────────────────────────────────────
  isInvalid(campo: string): boolean {
    return CustomValidators.showError(this.form.get(campo));
  }
  errorMsg(campo: string, label: string): string {
    return CustomValidators.getErrorMessage(this.form.get(campo), label);
  }

  // Nombre legible del rol para el select
  getRolNombre(rolId: number): string {
    const roles: Record<number, string> = { 1: 'ADMIN', 2: 'VENTAS', 3: 'ALMACEN', 4: 'RRHH' };
    return roles[rolId] || String(rolId);
  }

  // ── Cargar ────────────────────────────────────────────────────────────────
  cargarUsuarios() {
    this.loadingUsuarios = true;
    this.usuarioService.listarUsuarios().subscribe({
      next: (data) => {
        this.usuariosSistema = data;
        this.loadingUsuarios = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingUsuarios = false;
        this.showToast('Error al cargar usuarios', 'error');
      },
    });
  }

  // ── Crear ─────────────────────────────────────────────────────────────────
  openModalCrear() {
    this.modalMode = 'crear';
    this.usuarioActivo = null;
    this.form.reset({ rolId: 1 });
    // Password obligatoria al crear
    this.form
      .get('password')
      ?.setValidators([
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(20),
        Validators.pattern(CustomValidators.REGEX.password),
      ]);
    this.form.get('password')?.updateValueAndValidity();
    this.showModal = true;
  }

  // ── Editar ────────────────────────────────────────────────────────────────
  openModalEditar(u: Usuario) {
    this.modalMode = 'editar';
    this.usuarioActivo = u; // ← guardamos el usuario a editar
    this.form.patchValue({
      username: u.username,
      password: '',
      rolId: u.rol?.id ?? 1,
    });
    // Password opcional al editar — solo se envía si se escribe algo
    this.form
      .get('password')
      ?.setValidators([
        Validators.minLength(8),
        Validators.maxLength(20),
        Validators.pattern(CustomValidators.REGEX.password),
      ]);
    this.form.get('password')?.updateValueAndValidity();
    this.showModal = true;
  }

  // ── Eliminar ──────────────────────────────────────────────────────────────
  pedirEliminar(u: Usuario) {
    this.usuarioActivo = u;
    this.showModalEliminar = true;
  }

  confirmarEliminar() {
    if (!this.usuarioActivo?.id) return;
    this.eliminando = true;
    this.usuarioService.eliminarUsuario(this.usuarioActivo.id).subscribe({
      next: () => {
        this.eliminando = false;
        this.showModalEliminar = false;
        this.showToast(`✅ Usuario "${this.usuarioActivo?.username}" eliminado`, 'success');
        this.usuarioActivo = null;
        this.cargarUsuarios();
      },
      error: () => {
        this.eliminando = false;
        this.showToast('Error al eliminar usuario', 'error');
      },
    });
  }

  closeModal() {
    this.showModal = false;
    this.showModalEliminar = false;
    this.usuarioActivo = null;
  }

  // ── Guardar (crear o editar según modalMode) ──────────────────────────────
  guardarUsuario() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando = true;

    if (this.modalMode === 'crear') {
      this.usuarioService.crearUsuario(this.form.value).subscribe({
        next: () => {
          this.guardando = false;
          this.closeModal();
          this.showToast('✅ Usuario creado exitosamente', 'success');
          this.cargarUsuarios();
        },
        error: (err) => {
          this.guardando = false;
          this.showToast(err.error?.message || 'Error al crear usuario', 'error');
        },
      });
    } else {
      // Editar — solo envía password si se escribió algo
      const { username, password, rolId } = this.form.value;
      this.usuarioService
        .editarUsuario(this.usuarioActivo!.id!, {
          username,
          password: password || undefined,
          rolId,
        })
        .subscribe({
          next: () => {
            this.guardando = false;
            this.closeModal();
            this.showToast('✅ Usuario actualizado exitosamente', 'success');
            this.cargarUsuarios();
          },
          error: (err) => {
            this.guardando = false;
            this.showToast(err.error?.message || 'Error al actualizar usuario', 'error');
          },
        });
    }
  }

  // ── Utilidades ────────────────────────────────────────────────────────────
  // Nombre del usuario activo para el modal de confirmación
  get nombreUsuarioActivo(): string {
    return this.usuarioActivo?.username || '';
  }

  getBadgeClass(estado: string): string {
    return this.esActivo(estado) ? 'badge-green' : 'badge-red';
  }

  esActivo(estado?: string): boolean {
    return estado?.trim().toUpperCase() === 'ACTIVO';
  }

  showToast(msg: string, type: 'success' | 'error' | 'info' = 'success') {
    this.toastMsg = msg;
    this.toastType = type;
    this.toastVisible = true;
    setTimeout(() => {
      this.toastVisible = false;
      this.cdr.detectChanges();
    }, 3000);
  }
}
