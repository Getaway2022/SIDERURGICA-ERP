import { inject } from '@angular/core';
import { CanActivateChildFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { hasPermission } from '../models/role-permissions';

export const roleGuard: CanActivateChildFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const permission = route.data['permission'] as string | undefined;

  if (!permission || hasPermission(authService.getRol(), permission)) return true;

  return router.createUrlTree(['/dashboard']);
};
