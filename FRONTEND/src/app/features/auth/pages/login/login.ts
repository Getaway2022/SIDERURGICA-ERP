import { Component, ChangeDetectorRef } from '@angular/core';
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
        error: (err) => {
          this.errorMsg = err.error?.mensaje || 'Credenciales incorrectas. Intente nuevamente.';
          this.cdr.detectChanges();
        },
      });
  }
}
