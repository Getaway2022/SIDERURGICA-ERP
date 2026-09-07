import { Component, ChangeDetectorRef } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/auth/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class LoginComponent {
  username = ''; // ← cambió de email a username
  password = '';
  rememberMe = true;
  loading = false;
  errorMsg = '';

  constructor(
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  goHome(): void {
    this.router.navigate(['/']);
  }

  onLogin(): void {
    if (!this.username || !this.password) {
      this.errorMsg = 'Por favor ingrese usuario y contraseña.';
      return;
    }

    this.loading = true;
    this.errorMsg = '';

    this.authService
      .login(this.username, this.password)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: () => {
          this.router.navigate(['/dashboard']);
        },
        error: (err: HttpErrorResponse) => {
          if (err.status === 0) {
            this.errorMsg = 'No se pudo conectar con el servidor. Intente nuevamente más tarde.';
          } else if (err.status === 401) {
            this.errorMsg = 'Credenciales incorrectas. Intente nuevamente.';
          } else {
            this.errorMsg = 'No se pudo iniciar sesión. Intente nuevamente más tarde.';
          }
          this.cdr.detectChanges();
        },
      });
  }
}
