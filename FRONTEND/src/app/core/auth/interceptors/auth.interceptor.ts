import {
  HttpInterceptorFn,
  HttpRequest,
  HttpHandlerFn,
  HttpErrorResponse,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, filter, take, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';
import { RefreshTokenResponse } from '../models/auth.models';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const authReq = addToken(req, authService.getAccessToken());

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (
        error.status === 401 &&
        !req.url.includes('/login') &&
        !req.url.includes('/auth/refresh')
      ) {
        return handle401(req, next, authService, router);
      }
      return throwError(() => error);
    }),
  );
};

function addToken(req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  if (!token) return req;
  return req.clone({
    setHeaders: { Authorization: `Bearer ${token}` },
    withCredentials: true,
  });
}

function handle401(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService,
  router: Router,
) {
  const refreshing$ = authService.refreshing$;

  if (!refreshing$.getValue()) {
    refreshing$.next(true);

    return authService.refreshToken().pipe(
      switchMap((response: RefreshTokenResponse) => {
        refreshing$.next(false);
        return next(addToken(req, response.accessToken));
      }),
      catchError((err) => {
        refreshing$.next(false);
        authService.logout();
        return throwError(() => err);
      }),
    );
  } else {
    return refreshing$.pipe(
      filter((isRefreshing) => !isRefreshing),
      take(1),
      switchMap(() => next(addToken(req, authService.getAccessToken()))),
    );
  }
}
