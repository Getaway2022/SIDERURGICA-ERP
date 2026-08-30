import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Persona 1 — Validators personalizados compartidos
 * Validadores reutilizables por diferentes dominios de la aplicación.
 */
export class CustomValidators {
  // ─── Regex centralizados ────────────────────────────────────────────────────
  static readonly REGEX = {
    email: /^[a-z0-9._%+\-]+@[a-z0-9.\-]+\.[a-z]{2,4}$/,
    ruc: /^[0-9]{11}$/,
    telefono: /^[0-9]{9}$/,
    password: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,20}$/,
  };

  // ─── noSameDigitsValidator ──────────────────────────────────────────────────
  /** Rechaza valores donde todos los dígitos son iguales (ej. "111111111") */
  static noSameDigits(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const value: string = control.value?.toString() ?? '';
      if (!value) return null;
      const allSame = value.split('').every((c) => c === value[0]);
      return allSame ? { sameDigits: true } : null;
    };
  }

  // ─── ageRangeValidator ──────────────────────────────────────────────────────
  /** Valida que la fecha de nacimiento esté entre minAge y maxAge años */
  static ageRange(minAge: number = 18, maxAge: number = 80): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      const today = new Date();
      const birth = new Date(control.value);
      if (isNaN(birth.getTime())) return { ageRange: true };

      let age = today.getFullYear() - birth.getFullYear();
      const m = today.getMonth() - birth.getMonth();
      if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;

      return age < minAge || age > maxAge
        ? { ageRange: { min: minAge, max: maxAge, actual: age } }
        : null;
    };
  }

  // ─── Helpers de mensaje de error ────────────────────────────────────────────
  /** Devuelve el mensaje de error para un campo dado su nombre de control */
  static getErrorMessage(control: AbstractControl | null, label: string = 'Campo'): string {
    if (!control || !control.errors) return '';
    const e = control.errors;

    if (e['required']) return `${label} es obligatorio.`;
    if (e['minlength'])
      return `${label} debe tener al menos ${e['minlength'].requiredLength} caracteres.`;
    if (e['maxlength'])
      return `${label} no puede superar ${e['maxlength'].requiredLength} caracteres.`;
    if (e['pattern']) return `${label} tiene un formato inválido.`;
    if (e['email']) return `Ingrese un correo válido.`;
    if (e['sameDigits']) return `No se permiten todos los dígitos iguales.`;
    if (e['ageRange']) return `Debe tener entre ${e['ageRange'].min} y ${e['ageRange'].max} años.`;
    return 'Valor inválido.';
  }

  /** Helper: ¿debe mostrarse el error? (touched o dirty) */
  static showError(control: AbstractControl | null): boolean {
    return !!control && control.invalid && (control.touched || control.dirty);
  }
}
